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

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.mapping.FilterReadsStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * Main class for filter reads program.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public final class FilterReadsLocalStep extends FilterReadsStep {

  @Override
  public ExecutionMode getExecutionMode() {
    
    return Step.ExecutionMode.LOCAL;
  }
  
  @Override
  public String getLogName() {

    return "filterreads";
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      for (Sample s : design.getSamples()) {

        final Reporter reporter = new Reporter();
        final DataSource ds = DataSourceUtils.identifyDataSource(s.getSource());
        final FilterReadsLocal filter = new FilterReadsLocal(ds);
        final File outputFile =
            new File(Common.SAMPLE_FILTERED_PREFIX
                + s.getId() + Common.FASTQ_EXTENSION);

        if (getLengthThreshold() != -1)
          filter.setLengthThreshold(getLengthThreshold());

        if (getQualityThreshold() != -1)
          filter.setQualityThreshold(getQualityThreshold());

        // Filter reads
        filter.filter(outputFile, reporter);

        // Add counters for this sample to log file
        log.append(reporter.countersValuesToString(
            FilterReadsLocal.COUNTER_GROUP, "Filter reads ("
                + s.getName() + ", " + ds + ")"));
      }

      return new StepResult(this, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(this, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(this, e, "error while filtering: " + e.getMessage());
    }
  }

}
