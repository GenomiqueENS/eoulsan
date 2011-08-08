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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.READS_REJECTED_BY_FILTERS_COUNTER;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.io.FastQWriter;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ReadFilter;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsFilterStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * Main class for filter reads program.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@LocalOnly
public class ReadsFilterLocalStep extends AbstractReadsFilterStep {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      for (Sample s : design.getSamples()) {

        // Create the reporter
        final Reporter reporter = new Reporter();

        // get input file count for the sample
        final int inFileCount =
            context.getDataFileCount(DataFormats.READS_FASTQ, s);

        if (inFileCount < 1)
          throw new IOException("No reads file found.");

        if (inFileCount > 2)
          throw new IOException(
              "Cannot handle more than 2 reads files at the same time.");

        if (inFileCount == 1) {

          // Single end mode

          // Get the source
          final DataFile inFile =
              context.getDataFile(DataFormats.READS_FASTQ, s, 0);

          // Get the dest
          final DataFile outFile =
              context.getDataFile(FILTERED_READS_FASTQ, s, 0);

          // Filter reads
          filterFile(inFile, outFile, reporter, s.getMetadata()
              .getFastqFormat());

          // Add counters for this sample to log file
          log.append(reporter.countersValuesToString(COUNTER_GROUP,
              "Filter reads (" + s.getName() + ", " + inFile + ")"));
        } else {

          // Pair-end mode

          // Get the source
          final DataFile inFile1 =
              context.getDataFile(DataFormats.READS_FASTQ, s, 0);
          final DataFile inFile2 =
              context.getDataFile(DataFormats.READS_FASTQ, s, 1);

          // Get the dest
          final DataFile outFile1 =
              context.getDataFile(FILTERED_READS_FASTQ, s, 0);
          final DataFile outFile2 =
              context.getDataFile(FILTERED_READS_FASTQ, s, 1);

          // Filter reads
          filterFile(inFile1, inFile2, outFile1, outFile2, reporter, s
              .getMetadata().getFastqFormat());

          // Add counters for this sample to log file
          log.append(reporter.countersValuesToString(COUNTER_GROUP,
              "Filter reads ("
                  + s.getName() + ", " + inFile1 + ", " + inFile2 + ")"));

        }
      }

      return new StepResult(context, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }
  }

  /**
   * Filter a file in single end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param fastqFormat FastqFormat
   * @throws IOException if an error occurs while filtering data
   */
  private void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final FastqFormat fastqFormat)
      throws IOException {

    LOGGER.info("Filter file: " + inFile);
    LOGGER.info("FastqFormat: " + fastqFormat);

    final ReadFilter filter;

    try {
      filter = this.getReadFilter(reporter, COUNTER_GROUP);
    } catch (EoulsanException e) {
      throw new IOException(e.getMessage());
    }

    final FastqReader reader = new FastqReader(inFile.open());
    final FastQWriter writer = new FastQWriter(outFile.create());

    // Set Fastq format
    reader.setFastqFormat(fastqFormat);
    writer.setFastqFormat(fastqFormat);

    try {
      while (reader.readEntry()) {

        reporter.incrCounter(COUNTER_GROUP,
            INPUT_RAW_READS_COUNTER.counterName(), 1);

        if (filter.accept(reader)) {
          writer.set(reader);
          writer.write();
          reporter.incrCounter(COUNTER_GROUP,
              OUTPUT_FILTERED_READS_COUNTER.counterName(), 1);
        } else {
          reporter.incrCounter(COUNTER_GROUP,
              READS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
        }

      }
    } catch (BadBioEntryException e) {

      throw new IOException("Invalid Fastq format: " + e.getEntry());

    } finally {

      reader.close();
      writer.close();
    }

  }

  /**
   * Filter a file in pair-end mode.
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param phredOffset PHRED offset
   * @throws IOException if an error occurs while filtering data
   */
  private void filterFile(final DataFile inFile1, final DataFile inFile2,
      final DataFile outFile1, final DataFile outFile2,
      final Reporter reporter, final FastqFormat fastqFormat)
      throws IOException {

    LOGGER.info("Filter files: "
        + inFile1 + ", " + inFile2 + ", Fastq format: " + fastqFormat);

    final ReadFilter filter;

    try {
      filter = this.getReadFilter(reporter, COUNTER_GROUP);
    } catch (EoulsanException e) {
      throw new IOException(e.getMessage());
    }

    final FastqReader reader1 = new FastqReader(inFile1.open());
    final FastqReader reader2 = new FastqReader(inFile2.open());
    final FastQWriter writer1 = new FastQWriter(outFile1.create());
    final FastQWriter writer2 = new FastQWriter(outFile2.create());

    // Set PHRED offset
    reader1.setFastqFormat(fastqFormat);
    reader2.setFastqFormat(fastqFormat);
    writer1.setFastqFormat(fastqFormat);
    writer2.setFastqFormat(fastqFormat);

    try {
      while (reader1.readEntry()) {

        if (!reader2.readEntry())
          throw new IOException("Excepted end of the second reads file.");

        reporter.incrCounter(COUNTER_GROUP,
            INPUT_RAW_READS_COUNTER.counterName(), 1);

        if (filter.accept(reader1, reader2)) {
          writer1.set(reader1);
          writer1.write();
          writer2.set(reader2);
          writer2.write();
          reporter.incrCounter(COUNTER_GROUP,
              OUTPUT_FILTERED_READS_COUNTER.counterName(), 1);
        } else {
          reporter.incrCounter(COUNTER_GROUP,
              READS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
        }

      }

      if (reader2.readEntry())
        throw new IOException("Excepted end of the first reads file.");

    } catch (BadBioEntryException e) {

      throw new IOException("Invalid Fastq format: " + e.getEntry());

    } finally {

      reader1.close();
      reader2.close();
      writer1.close();
      writer2.close();
    }

  }

}
