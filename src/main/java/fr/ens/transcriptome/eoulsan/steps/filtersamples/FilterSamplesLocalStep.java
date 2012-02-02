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

package fr.ens.transcriptome.eoulsan.steps.filtersamples;

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class is the main class for filtering samples after mapping in local
 * mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class FilterSamplesLocalStep extends AbstractFilterSamplesStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  //
  // Step methods
  //

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    final double threshold = getThreshold() / 100.0;
    final File logDir = new File(context.getLogPathname());

    try {
      // Read filterreads.log
      LogReader logReader = new LogReader(new File(logDir, "filterreads.log"));
      final Reporter filterReadsReporter = logReader.read();

      // Read soapmapreads.log
      logReader = new LogReader(new File(logDir, "filtersam.log"));
      final Reporter soapMapReadsReporter = logReader.read();

      // Get the input reads for each sample
      final Map<String, Long> sampleInputMapReads =
          parseReporter(filterReadsReporter,
              OUTPUT_FILTERED_READS_COUNTER.counterName());

      // Get the number of match with onlt one locus for each sample
      final Map<String, Long> soapAlignementWithOneLocus =
          parseReporter(soapMapReadsReporter,
              MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName());

      int removedSampleCount = 0;
      final StringBuilder sb = new StringBuilder();

      // Compute ration and filter samples
      for (Map.Entry<String, Long> e : sampleInputMapReads.entrySet()) {

        final String sample = e.getKey();

        if (!soapAlignementWithOneLocus.containsKey(sample))
          continue;

        final long inputReads = e.getValue();
        final long oneLocus = soapAlignementWithOneLocus.get(sample);

        final double ratio = (double) oneLocus / (double) inputReads;
        logger.info("Check Reads with only one match: "
            + sample + " " + oneLocus + "/" + inputReads + "=" + ratio
            + " threshold=" + threshold);

        if (ratio < threshold) {
          design.removeSample(sample);
          logger.info("Remove sample: " + sample);
          sb.append("Remove sample: " + sample + "\n");
          removedSampleCount++;
        }
      }

      return new StepResult(context, startTime, "Sample(s) removed: "
          + removedSampleCount + "\n" + sb.toString());

    } catch (IOException e) {

      return new StepResult(context, e, "Error while filtering samples: "
          + e.getMessage());
    }

  }

  //
  // Other method
  //

  private static final Map<String, Long> parseReporter(final Reporter reporter,
      final String counter) {

    final Map<String, Long> result = new HashMap<String, Long>();

    final Set<String> groups = reporter.getCounterGroups();

    for (String group : groups) {

      final int pos1 = group.indexOf('(');
      final int pos2 = group.indexOf(',');

      if (pos1 == -1 || pos2 == -1)
        continue;

      final String sample = group.substring(pos1 + 1, pos2).trim();

      result.put(sample, reporter.getCounterValue(group, counter));
    }

    return result;
  }

}
