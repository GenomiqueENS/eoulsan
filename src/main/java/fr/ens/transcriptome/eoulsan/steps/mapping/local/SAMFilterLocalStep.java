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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.SAMComparator;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAMFilterStep;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a Step for alignments filtering.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@LocalOnly
public class SAMFilterLocalStep extends AbstractSAMFilterStep {

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create the reporter
    final Reporter reporter = new LocalReporter();

    try {

      // Get the read filter
      final MultiReadAlignmentsFilter filter =
          getAlignmentsFilter(reporter, COUNTER_GROUP);
      getLogger().info(
          "Read alignments filters to apply: "
              + Joiner.on(", ").join(filter.getFilterNames()));

      filterSample(context, reporter, status, filter);

    } catch (IOException e) {
      status.createStepResult(e, "Error while filtering: " + e.getMessage());
    } catch (EoulsanException e) {
      status.createStepResult(e,
          "Error while initializing filter: " + e.getMessage());
    }

    return status.createStepResult();
  }

  /**
   * Filter a sample data in single-end mode and in paired-end mode.
   * @param context Eoulsan context
   * @param reporter reporter to use
   * @param status step status
   * @param filter alignments filter to use
   * @throws IOException if an error occurs while filtering reads
   */
  private static void filterSample(final StepContext context,
      final Reporter reporter, final StepStatus status,
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
    filterFile(inFile, outFile, reporter, filter);

    // Set the description of the context
    status.setDescription("Filter SAM file ("
        + inData.getName() + ", " + inFile.getName() + ")");

    // Add counters for this sample to log file
    status.setCounters(reporter, COUNTER_GROUP);
  }

  /**
   * Filter a file in single-end mode or paired-end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param filter alignments filter to use
   * @throws IOException if an error occurs while filtering data
   */
  private static void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final ReadAlignmentsFilter filter)
      throws IOException {

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
    final SAMFileReader inputSam = new SAMFileReader(inFile.open());

    // Get Writer
    final SAMFileWriter outputSam =
        new SAMFileWriterFactory().makeSAMWriter(inputSam.getFileHeader(),
            false, outFile.create());

    try {

      for (SAMRecord samRecord : inputSam) {

        // single-end or paired-end mode ?
        if (counterInput == 0) {
          if (samRecord.getReadPairedFlag())
            pairedEnd = true;
        }

        counterInput++;

        // storage and filtering of all the alignments of a read in the list
        // "records"
        if (!rafb.addAlignment(samRecord)) {

          records.clear();
          records.addAll(rafb.getFilteredAlignments());

          // sort alignments of the current read
          Collections.sort(records, new SAMComparator());

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
      Collections.sort(records, new SAMComparator());

      // writing records
      for (SAMRecord r : records) {
        outputSam.addAlignment(r);
        counterOutput++;
      }

    } catch (SAMFormatException e) {
      counterInvalid++;
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
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), nbInput
              - nbOutput);
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
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), counterInput
              - counterOutput);
    }

    // Close files
    inputSam.close();
    outputSam.close();
  }
}
