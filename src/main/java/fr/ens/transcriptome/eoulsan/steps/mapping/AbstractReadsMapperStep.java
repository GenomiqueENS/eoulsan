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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;

public abstract class AbstractReadsMapperStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String STEP_NAME = "mapreads";

  protected static final String COUNTER_GROUP = "reads_mapping";
  
  public static final int HADOOP_TIMEOUT = 60 * 60 * 1000;

  private SequenceReadsMapper mapper;
  private String mapperArguments;
  private int mapperThreads = -1;

  //
  // Getters
  //

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperName() {
    return this.mapper.getMapperName();
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected String getMapperArguments() {
    return this.mapperArguments;
  }

  /**
   * Get the name of the mapper to use.
   * @return Returns the mapperName
   */
  protected int getMapperThreads() {
    return this.mapperThreads;
  }

  /**
   * Get the mapper object.
   * @return the mapper object
   */
  protected SequenceReadsMapper getMapper() {

    return this.mapper;
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

    return "This step filters reads.";
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {MAPPER_RESULTS_SAM};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    String mapperName = null;

    for (Parameter p : stepParameters) {

      if ("mapper".equals(p.getName()))
        mapperName = p.getStringValue();
      else if ("mapperArguments".equals(p.getName()))
        this.mapperArguments = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (mapperName == null) {
      throw new EoulsanException("No mapper set.");
    }

    this.mapper =
        SequenceReadsMapperService.getInstance().getMapper(mapperName);

    if (this.mapper == null) {
      throw new EoulsanException("Unknown mapper: " + mapperName);
    }

    // Log Step parameters
    LOGGER.info("In " + getName() + ", mapper=" + this.mapper.getMapperName());
    LOGGER
        .info("In " + getName() + ", mapperarguments=" + this.mapperArguments);
  }

}
