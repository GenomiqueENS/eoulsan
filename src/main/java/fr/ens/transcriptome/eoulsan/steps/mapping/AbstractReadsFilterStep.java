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

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.QualityReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.TrimReadFilter;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.ProcessSampleExecutor;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define an abstract step for read filtering.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadsFilterStep extends AbstractStep {

  protected static final String STEP_NAME = "filterreads";

  protected static final String COUNTER_GROUP = "reads_filtering";

  private Map<String, String> readsFiltersParameters;
  private int localThreads;
  private int maxLocalThreads;

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
  public InputPorts getInputPorts() {
    return singleInputPort(READS_FASTQ);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(READS_FASTQ);
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    final MultiReadFilterBuilder mrfb = new MultiReadFilterBuilder();

    for (Parameter p : stepParameters) {

      if ("local.threads".equals(p.getName()))
        this.localThreads = p.getIntValue();
      else if ("max.local.threads".equals(p.getName()))
        this.maxLocalThreads = p.getIntValue();
      else
        mrfb.addParameter(convertCompatibilityFilterKey(p.getName()),
            p.getStringValue());
    }

    // Force parameter checking
    mrfb.getReadFilter();

    this.readsFiltersParameters = mrfb.getParameters();
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
      return TrimReadFilter.FILTER_NAME + ".length.threshold";

    if ("qualitythreshold".equals(key))
      return QualityReadFilter.FILTER_NAME + ".threshold";
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
  protected MultiReadFilter getReadFilter(
      final ReporterIncrementer incrementer, final String counterGroup)
      throws EoulsanException {

    // As filters are not thread safe, create a new MultiReadFilterBuilder
    // with a new instance of each filter
    return new MultiReadFilterBuilder(this.readsFiltersParameters)
        .getReadFilter(incrementer, counterGroup);
  }

  /**
   * Get the parameters of the read filter.
   * @return a map with all the parameters of the filter
   */
  protected Map<String, String> getReadFilterParameters() {

    return this.readsFiltersParameters;
  }

  /**
   * Get the number of threads to use in local mode.
   * @return Returns the mapperThreads
   */
  protected int getLocalThreads() {

    return ProcessSampleExecutor.getThreadsNumber(this.localThreads,
        this.maxLocalThreads);
  }

}
