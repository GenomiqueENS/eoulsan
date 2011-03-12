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

package fr.ens.transcriptome.eoulsan.steps;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.READS_REJECTED_BY_FILTERS_COUNTER;

import java.io.FileNotFoundException;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastQWriter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.PairEndReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.QualityReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.TrimReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ValidReadFilter;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsFilterStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class allow to test the multi threading of local Steps
 * @author Laurent Jourdren
 */
public class TestThreadStep extends AbstractReadsFilterStep {

  private static final String COUNTER_GROUP = "TestThread";

  @Override
  public StepResult execute(final Design design, final Context context) {

    return ProcessSampleExecutor.processAllSamples(context, design,
        new ProcessSample() {

          public String processSample(final Context context, final Sample sample)
              throws ProcessSampleException {

            // Create the reporter
            final Reporter reporter = new Reporter();

            // Get the source
            final DataFile inFile = new DataFile(sample.getSource());

            // Get the dest
            final DataFile outFile =
                context.getDataFile(FILTERED_READS_FASTQ, sample);

            // Filter reads
            try {
              filterFile(inFile, outFile, reporter);

            } catch (FileNotFoundException e) {
              throwException(e, "File not found: " + e.getMessage());
            } catch (IOException e) {
              throwException(e, "error while filtering: " + e.getMessage());
            }

            // Add counters for this sample to log file
            return reporter.countersValuesToString(COUNTER_GROUP,
                "Filter reads (" + sample.getName() + ", " + inFile + ")");
          }
        });

  }

  @Override
  public String getName() {

    return "TestThread";
  }

  /**
   * Filter a file
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @throws IOException if an error occurs while filtering data
   */
  private void filterFile(final DataFile inFile, final DataFile outFile,
      final Reporter reporter) throws IOException {

    final MultiReadFilter filter = new MultiReadFilter(reporter, COUNTER_GROUP);
    filter.addFilter(new PairEndReadFilter(isPairend()));
    filter.addFilter(new ValidReadFilter());
    filter.addFilter(new TrimReadFilter(getLengthThreshold()));
    filter.addFilter(new QualityReadFilter(getQualityThreshold()));

    final FastQReader reader = new FastQReader(inFile.open());
    final FastQWriter writer = new FastQWriter(outFile.create());

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
