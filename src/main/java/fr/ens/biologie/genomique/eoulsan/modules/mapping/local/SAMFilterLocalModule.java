/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.SAMComparator;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractSAMFilterModule;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

/**
 * This class define a Step for alignments filtering.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@LocalOnly
public class SAMFilterLocalModule extends AbstractSAMFilterModule {

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Create the reporter
    final Reporter reporter = new LocalReporter();

    try {

      // Get the read filter
      final MultiReadAlignmentsFilter filter =
          getAlignmentsFilter(reporter, COUNTER_GROUP);
      getLogger().info("Read alignments filters to apply: "
          + Joiner.on(", ").join(filter.getFilterNames()));

      filterSample(context, reporter, status, filter);

    } catch (IOException e) {
      status.createTaskResult(e,
          "Error while filtering alignments: " + e.getMessage());
    } catch (EoulsanException e) {
      status.createTaskResult(e,
          "Error while initializing filter: " + e.getMessage());
    }

    return status.createTaskResult();
  }

  /**
   * Filter a sample data in single-end mode and in paired-end mode.
   * @param context Eoulsan context
   * @param reporter reporter to use
   * @param status task status
   * @param filter alignments filter to use
   * @throws IOException if an error occurs while filtering reads
   */
  private static void filterSample(final TaskContext context,
      final Reporter reporter, final TaskStatus status,
      final ReadAlignmentsFilter filter) throws IOException {

    // Get input and output data
    final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_SAM);
    final Data outData =
        context.getOutputData(DataFormats.MAPPER_RESULTS_SAM, inData);

    // Get the source
    final DataFile inFile = inData.getDataFile();

    // Get the dest
    final DataFile outFile = outData.getDataFile();

    // Filter alignments in single-end mode or in paired-end mode
    filterFile(inFile, outFile, reporter, filter,
        context.getLocalTempDirectory());

    // Set the description of the context
    status.setDescription(
        "Filter SAM file (" + inData.getName() + ", " + inFile.getName() + ")");

    // Add counters for this sample to log file
    status.setCounters(reporter, COUNTER_GROUP);
  }

  /**
   * Filter a file in single-end mode or paired-end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param filter alignments filter to use
   * @param tmpDir temporary directory
   * @throws IOException if an error occurs while filtering data
   */
  private static void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final ReadAlignmentsFilter filter,
      final File tmpDir) throws IOException {

    final List<SAMRecord> records = new ArrayList<>();
    int counterInput = 0;
    int counterOutput = 0;
    int counterInvalid = 0;
    boolean pairedEnd = false;

    // Creation of a buffer object to store alignments with the same read name
    final ReadAlignmentsFilterBuffer rafb =
        new ReadAlignmentsFilterBuffer(filter);

    getLogger().info("Filter SAM file: " + inFile);

    // Get reader
    final SamReader inputSam =
        SamReaderFactory.makeDefault().open(SamInputResource.of(inFile.open()));

    // Get Writer
    final SAMFileWriter outputSam =
        new SAMFileWriterFactory().setTempDirectory(tmpDir)
            .makeSAMWriter(inputSam.getFileHeader(), false, outFile.create());

    final SAMRecordIterator it = inputSam.iterator();

    while (it.hasNext()) {

      final SAMRecord samRecord;

      // Check if SAM entry is correct
      try {
        samRecord = it.next();

      } catch (SAMFormatException e) {
        counterInvalid++;
        continue;
      }

      // single-end or paired-end mode ?
      if (counterInput == 0) {
        if (samRecord.getReadPairedFlag()) {
          pairedEnd = true;
        }
      }

      counterInput++;

      // storage and filtering of all the alignments of a read in the list
      // "records"
      if (!rafb.addAlignment(samRecord)) {

        records.clear();
        records.addAll(rafb.getFilteredAlignments());

        // sort alignments of the current read
        records.sort(new SAMComparator());

        // writing records
        for (SAMRecord r : records) {
          outputSam.addAlignment(r);
          counterOutput++;
        }

        rafb.addAlignment(samRecord);
      }

    }

    // treatment of the last record
    records.clear();
    records.addAll(rafb.getFilteredAlignments());

    // sort alignments of the last read
    records.sort(new SAMComparator());

    // writing records
    for (SAMRecord r : records) {
      outputSam.addAlignment(r);
      counterOutput++;
    }

    // paired-end mode
    if (pairedEnd) {
      int nbInput = counterInput / 2;
      int nbOutput = counterOutput / 2;
      reporter.incrCounter(COUNTER_GROUP,
          INPUT_ALIGNMENTS_COUNTER.counterName(), nbInput);
      reporter.incrCounter(COUNTER_GROUP,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), nbOutput);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), counterInvalid / 2);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(),
          nbInput - nbOutput);
    }

    // single-end mode
    else {
      reporter.incrCounter(COUNTER_GROUP,
          INPUT_ALIGNMENTS_COUNTER.counterName(), counterInput);
      reporter.incrCounter(COUNTER_GROUP,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), counterOutput);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), counterInvalid);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(),
          counterInput - counterOutput);
    }

    // Close files
    inputSam.close();
    outputSam.close();
  }
}
