/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_FASTQ;

import java.io.FileNotFoundException;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class allow to test the multi threading of local Steps
 * @author Laurent Jourdren
 */
public class TestThreadStep extends AbstractStep {

  private static final String COUNTER_GROUP = "TestThread";

  @Override
  public StepResult execute(final Design design, final Context context) {

    return ProcessSampleExecutor.processAllSamples(context, design, new ProcessSample() {

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
        return reporter.countersValuesToString(COUNTER_GROUP, "Filter reads ("
            + sample.getName() + ", " + inFile + ")");
      }
    });

  }

  @Override
  public String getName() {

    return "TestThread";
  }

  private void filterFile(DataFile inFile, DataFile outFile,
      Reporter reporter) throws IOException {

    // Do Nothing

  }

}
