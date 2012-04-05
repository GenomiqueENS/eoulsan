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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.MultiReadAlignmentsFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.QualityReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.bio.alignmentsfilters.ReadAlignmentsFilter;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define an abstract step for alignments filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractSAMFilterStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String STEP_NAME = "filtersam";

  private static final int MAX_MAPPING_QUALITY_THRESHOLD = 255;
  protected static final String COUNTER_GROUP = "sam_filtering";

  private MultiReadAlignmentsFilterBuilder readAlignmentsFilterBuilder;

  private int mappingQualityThreshold = -1;

  //
  // Getters
  //

  /**
   * Get the mapping quality threshold.
   * @return the quality mapping threshold !!!!!!!!!!!! Problem for the class
   *         SAMFilterHadoopStep.java : mappingQualityThreshold is no longer
   *         used here (getMappingQualityThreshold() called in this class) (cf.
   *         comments in configure())
   */
  protected int getMappingQualityThreshold() {

    return this.mappingQualityThreshold;
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
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {MAPPER_RESULTS_SAM, GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {FILTERED_MAPPER_RESULTS_SAM};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    final MultiReadAlignmentsFilterBuilder mrafb =
        new MultiReadAlignmentsFilterBuilder();

    for (Parameter p : stepParameters) {
      mrafb.addParameter(convertCompatibilityFilterKey(p.getName()),
          p.getStringValue());
    }

    // Force parameter checking
    mrafb.getAlignmentsFilter();

    this.readAlignmentsFilterBuilder = mrafb;
  }

  /**
   * Convert old key names to new names
   * @param key key to convert
   * @return the new key name if necessary
   */
  static String convertCompatibilityFilterKey(final String key) {

    if (key == null)
      return null;

    if ("mappingqualitythreshold".equals(key))
      return QualityReadAlignmentsFilter.FILTER_NAME + ".threshold";

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
  protected ReadAlignmentsFilter getAlignmentsFilter(
      final ReporterIncrementer incrementer, final String counterGroup)
      throws EoulsanException {

    return this.readAlignmentsFilterBuilder.getAlignmentsFilter(incrementer,
        counterGroup);
  }

  /**
   * Get the parameters of the alignments filter.
   * @return a map with all the parameters of the filter
   */
  protected Map<String, String> getAlignmentsFilterParameters() {

    return this.readAlignmentsFilterBuilder.getParameters();
  }

}
