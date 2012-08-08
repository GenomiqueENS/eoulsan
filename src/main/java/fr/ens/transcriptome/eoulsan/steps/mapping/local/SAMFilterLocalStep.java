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

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import net.sf.samtools.SAMComparator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilterBuffer;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSampleExecutor;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAMFilterStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a Step for alignements filtering.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@LocalOnly
public class SAMFilterLocalStep extends AbstractSAMFilterStep {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  public StepResult execute(final Design design, final Context context) {

    final GenomeDescription genomeDescription;

    // Load genome description object
    try {

      if (design.getSampleCount() > 0)
        genomeDescription =
            GenomeDescription.load(context.getInputDataFile(
                DataFormats.GENOME_DESC_TXT, design.getSample(0)).open());
      else
        genomeDescription = null;

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }

    // Get threshold
    // final int mappingQualityThreshold = getMappingQualityThreshold();

    // Process all samples
    return ProcessSampleExecutor.processAllSamples(context, design,
        getLocalThreads(), new ProcessSample() {

          @Override
          public String processSample(Context context, Sample sample)
              throws ProcessSampleException {

            // Define Result
            String resultString = null;

            // Create the reporter
            final Reporter reporter = new Reporter();

            try {

              // Create parser object
              final SAMParser parser = new SAMParser();
              parser.setGenomeDescription(genomeDescription);

              // Get the read filter
              final ReadAlignmentsFilter filter =
                  getAlignmentsFilter(reporter, COUNTER_GROUP);

              resultString =
                  filterSample(context, sample, reporter, parser, filter);

            } catch (IOException e) {
              throwException(e, "Error while filtering: " + e.getMessage());
            } catch (EoulsanException e) {
              throwException(e,
                  "Error while initializing filter: " + e.getMessage());
            }

            return resultString;
          }

        });
  }

  /**
   * Filter a sample data in single-end mode and in paired-end mode.
   * @param context Eoulsan context
   * @param sample sample to process
   * @param reporter reporter to use
   * @param parser SAM parser to use
   * @param filter alignments filter to use
   * @param pairedEnd true if data are in paired-end mode
   * @return a string with information to log
   * @throws IOException if an error occurs while filtering reads
   */
  private static String filterSample(final Context context,
      final Sample sample, final Reporter reporter, final SAMParser parser,
      final ReadAlignmentsFilter filter) throws IOException {

    // Get the source
    final DataFile inFile =
        context.getInputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample);

    // Get the dest
    final DataFile outFile =
        context.getOutputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM,
            sample);

    // filter alignments in single-end mode or in paired-end mode
    filterFile(inFile, outFile, reporter, filter);

    // Add counters for this sample to log file
    return reporter.countersValuesToString(COUNTER_GROUP, "Filter SAM file ("
        + sample.getName() + ", " + inFile + ")");
  }

  /**
   * Filter a file in single-end mode or paired-end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param filter alignments filter to use
   * @param pairedEnd true if data are in paired-end mode
   * @throws IOException if an error occurs while filtering data
   */
  private static void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final ReadAlignmentsFilter filter)
      throws IOException {

    final List<SAMRecord> records = new ArrayList<SAMRecord>();
    int counterInput = 0;
    int counterOutput = 0;
    int counterInvalid = 0;
    boolean pairedEnd = false;

    // Creation of a buffer object to store alignments with the same read name
    final ReadAlignmentsFilterBuffer rafb =
        new ReadAlignmentsFilterBuffer(filter);

    LOGGER.info("Filter SAM file: " + inFile);

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
