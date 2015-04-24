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

package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.QualityReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define an abstract step for alignments filtering.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public abstract class AbstractSAMFilterStep extends AbstractStep {

  private static final String STEP_NAME = "filtersam";

  protected static final String COUNTER_GROUP = "sam_filtering";

  private Map<String, String> alignmentsFiltersParameters;
  private int reducerTaskCount = -1;

  /**
   * Get the parameters of the alignments filter.
   * @return a map with all the parameters of the filter
   */
  protected Map<String, String> getAlignmentsFilterParameters() {

    return this.alignmentsFiltersParameters;
  }

  /**
   * Get the reducer task count.
   * @return the reducer task count
   */
  protected int getReducerTaskCount() {

    return this.reducerTaskCount;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step filters sam files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return singleInputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final MultiReadAlignmentsFilterBuilder mrafb =
        new MultiReadAlignmentsFilterBuilder();

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME:

        int count = p.getIntValue();

        if (count < 1) {
          throw new EoulsanException("Invalid "
              + HADOOP_REDUCER_TASK_COUNT_PARAMETER_NAME + " parameter value: "
              + p.getValue());
        }

        this.reducerTaskCount = count;

        break;

      default:

        mrafb.addParameter(convertCompatibilityFilterKey(p.getName()),
            p.getStringValue());
        break;
      }
    }

    // Force parameter checking
    mrafb.getAlignmentsFilter();

    this.alignmentsFiltersParameters = mrafb.getParameters();
  }

  //
  // Other methods
  //

  /**
   * Convert old key names to new names
   * @param key key to convert
   * @return the new key name if necessary
   */
  static String convertCompatibilityFilterKey(final String key) {

    if (key == null) {
      return null;
    }

    if ("mappingqualitythreshold".equals(key)) {
      return QualityReadAlignmentsFilter.FILTER_NAME + ".threshold";
    }

    return key;
  }

  /**
   * Get the ReadAlignmentsFilter object.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new ReadAlignmentsFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  protected MultiReadAlignmentsFilter getAlignmentsFilter(
      final ReporterIncrementer incrementer, final String counterGroup)
      throws EoulsanException {

    // As filters are not thread safe, create a new
    // MultiReadAlignmentsFilterBuilder
    // with a new instance of each filter
    return new MultiReadAlignmentsFilterBuilder(
        this.alignmentsFiltersParameters).getAlignmentsFilter(incrementer,
        counterGroup);
  }

}
