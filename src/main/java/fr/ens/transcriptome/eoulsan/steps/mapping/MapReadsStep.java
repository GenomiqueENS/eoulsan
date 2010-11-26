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

package fr.ens.transcriptome.eoulsan.steps.mapping;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

/**
 * This abstract class define and parse arguments for the map reads step.
 * @author Laurent Jourdren
 */
public abstract class MapReadsStep extends AbstractStep {

  private static final String STEP_NAME = "mapreads";

  private String mapperName;
  private int threads = -1;

  //
  // Getter
  //

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperName() {
    return this.mapperName;
  }

  /**
   * Get the number of threads to use.
   * @return Returns the number of threads to use
   */
  protected int getThreads() {
    return this.threads;
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

    return "This step map reads.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_READS_FASTQ,
        DataFormats.SOAP_INDEX_ZIP};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_SOAP_RESULTS_TXT};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      if ("mapper".equals(p.getName()))
        this.mapperName = p.getStringValue();
      else if ("threads".equals(p.getName()))
        this.threads = p.getIntValue() > 0 ? p.getIntValue() : 1;
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.mapperName == null)
      throw new EoulsanException("No mapper set.");

    if (!"soap".equals(this.mapperName))
      throw new EoulsanException("Unknown mapper: " + this.mapperName);

  }

}
