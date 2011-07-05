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

import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.QualityReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.TrimReadFilter;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public abstract class AbstractReadsFilterStep extends AbstractStep {

  protected static final String STEP_NAME = "filterreads";

  protected static final String COUNTER_GROUP = "reads_filtering";

  /** filter reads length threshold. */
  public static final int LENGTH_THRESHOLD = 15;

  /** filter reads quality threshold. */
  public static final double QUALITY_THRESHOLD = 15;

  private MultiReadFilterBuilder readFilterBuilder;

  // private int lengthThreshold = -1;
  // private double qualityThreshold = -1;
  // private boolean pairEnd;

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step filters reads.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.READS_FASTQ};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_READS_FASTQ};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    final MultiReadFilterBuilder mrfb = new MultiReadFilterBuilder();

    for (Parameter p : stepParameters)
      mrfb.addParameter(convertCompatibilityFilterKey(p.getName()),
          p.getStringValue());

    // Force parameter checking
    mrfb.getReadFilter();

    this.readFilterBuilder = mrfb;
  }

  /**
   * Convert old key names to new names
   * @param key key to convert
   * @return the new key name if necessary
   */
  static String convertCompatibilityFilterKey(final String key) {

    if (key == null)
      return null;

    if ("lengththreshold".equals(key))
      return TrimReadFilter.FILTER_NAME + ".lengththreshold";

    if ("qualitythreshold".equals(key))
      return QualityReadFilter.FILTER_NAME + ".qualitythreshold";
    return key;
  }

  /**
   * Get the ReadFilter object.
   * @param incrementer incrementer to use
   * @param counterGroup counter group for the incrementer
   * @return a new ReadFilter object
   * @throws EoulsanException if an error occurs while initialize one of the
   *           filter
   */
  protected ReadFilter getReadFilter(final ReporterIncrementer incrementer,
      final String counterGroup) throws EoulsanException {

    return this.readFilterBuilder.getReadFilter(incrementer, counterGroup);
  }

  /**
   * Get the parameters of the read filter.
   * @return a map with all the parameters of the filter
   */
  protected Map<String, String> getReadFilterParameters() {

    return this.readFilterBuilder.getParameters();
  }

}
