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
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastQWriter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ReadFilter;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
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

        // Get the source
        final DataFile inFile = new DataFile(s.getMetadata().getReads());

        // Get the dest
        final DataFile outFile = context.getDataFile(FILTERED_READS_FASTQ, s);

        // Filter reads
        filterFile(inFile, outFile, reporter, s.getMetadata().getPhredOffset());

        // Add counters for this sample to log file
        log.append(reporter.countersValuesToString(COUNTER_GROUP,
            "Filter reads (" + s.getName() + ", " + inFile + ")"));
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
   * Filter a file
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param phredOffset PHRED offset
   * @throws IOException if an error occurs while filtering data
   */
  private void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter, final int phredOffset) throws IOException {

    LOGGER.info("Filter file: " + inFile + ", PHRED offset: " + phredOffset);

    final ReadFilter filter;

    try {
      filter = this.getReadFilter(reporter, COUNTER_GROUP);
    } catch (EoulsanException e) {
      throw new IOException(e.getMessage());
    }

    final FastQReader reader = new FastQReader(inFile.open());
    final FastQWriter writer = new FastQWriter(outFile.create());

    // Set PHRED offset
    reader.setPhredOffset(phredOffset);
    writer.setPhredOffset(phredOffset);

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

}
