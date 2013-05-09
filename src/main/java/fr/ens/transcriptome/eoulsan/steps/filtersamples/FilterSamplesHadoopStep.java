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

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.LogReader;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class is the main class for filtering samples after mapping in hadoop
 * mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class FilterSamplesHadoopStep extends AbstractFilterSamplesStep {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private Configuration conf;

  //
  // Step methods
  //

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {

    super.configure(stepParameters);
    this.conf = CommonHadoop.createConfiguration(EoulsanRuntime.getSettings());
  }

  @Override
  public StepResult execute(Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    final double threshold = getThreshold() / 100.0;
    final Path logDirPath = new Path(context.getLogPathname());

    try {

      final Configuration conf = this.conf;
      final FileSystem fs = logDirPath.getFileSystem(conf);

      final Reporter mappingReporter;
      final Reporter samFilterReporter;

      // Read soapmapreads.log
      final Path mappingLogPath = new Path(logDirPath, "filtersam.log");

      if (!PathUtils.exists(mappingLogPath, conf)) {

        final Path filterandmapLogPath =
            new Path(logDirPath, "filterandmap.log");

        LOGGER.info("Read filter and map log: " + filterandmapLogPath);
        mappingReporter = new LogReader(fs.open(filterandmapLogPath)).read();
        samFilterReporter = mappingReporter;

      } else {

        final Path samFilterLogPath = new Path(logDirPath, "filtersam.log");
        // if (!PathUtils.exists(mapReadsLogPath, conf))
        // mapReadsLogPath = new Path(logDirPath, "filterandmapreads.log");

        LOGGER.info("Read mapping log: " + mappingLogPath);
        mappingReporter = new LogReader(fs.open(mappingLogPath)).read();

        LOGGER.info("Read sam filtering log: " + samFilterLogPath);
        samFilterReporter = new LogReader(fs.open(samFilterLogPath)).read();
      }

      int removedSampleCount = 0;
      final StringBuilder sb = new StringBuilder();

      final Map<String, Long> mappingMap =
          parseReporter(mappingReporter,
              MappingCounters.OUTPUT_FILTERED_READS_COUNTER.counterName());
      final Map<String, Long> samFilterMap =
          parseReporter(samFilterReporter,
              MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName());

      // Compute ration and filter samples
      for (final Map.Entry<String, Long> e : mappingMap.entrySet()) {

        final String sampleName = e.getKey();
        final long inputReads = e.getValue();
        final long oneLocus = samFilterMap.get(e.getKey());

        final double ratio = (double) oneLocus / (double) inputReads;

        LOGGER.info("Check Reads with only one match: "
            + sampleName + " " + oneLocus + "/" + inputReads + "=" + ratio
            + " threshold=" + threshold);

        if (ratio < threshold) {
          design.removeSample(sampleName);
          LOGGER.info("Remove sample: " + sampleName);
          sb.append("Remove sample: " + sampleName + "\n");
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

  private static Map<String, Long> parseReporter(final Reporter reporter,
      final String counterGroup) {

    final Map<String, Long> result = Maps.newHashMap();

    // Compute ration and filter samples
    for (String group : reporter.getCounterGroups()) {

      final int pos1 = group.indexOf('(');
      final int pos2 = group.indexOf(',');

      if (pos1 == -1 || pos2 == -1)
        continue;

      final String sample = group.substring(pos1 + 1, pos2).trim();

      result.put(sample, reporter.getCounterValue(group, counterGroup));

    }

    return result;
  }

}
