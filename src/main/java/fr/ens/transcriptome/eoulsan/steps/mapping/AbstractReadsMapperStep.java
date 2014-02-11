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

import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapperService;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.OutputPort;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.ProcessSampleExecutor;

/**
 * This class define an abstract step for read mapping.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadsMapperStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  protected static final String STEP_NAME = "mapreads";

  protected static final String COUNTER_GROUP = "reads_mapping";

  public static final int HADOOP_TIMEOUT = 60 * 60 * 1000;

  private SequenceReadsMapper mapper;
  private String mapperArguments;

  private int hadoopThreads;
  private int localThreads;
  private int maxLocalThreads;

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
   * Get the arguments of the mapper to use.
   * @return Returns the mapperArguments
   */
  protected String getMapperArguments() {
    return this.mapperArguments;
  }

  /**
   * Get the number of threads to use in local mode.
   * @return Returns the mapperThreads
   */
  protected int getMapperLocalThreads() {

    return ProcessSampleExecutor.getThreadsNumber(this.localThreads,
        this.maxLocalThreads);
  }

  /**
   * Get the number of threads to use in local mode.
   * @return Returns the mapperThreads
   */
  protected int getMapperHadoopThreads() {

    return this.hadoopThreads;
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
  public Set<OutputPort> getOutputFormats() {
    return singleOutputPort(MAPPER_RESULTS_SAM);
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    String mapperName = null;

    for (Parameter p : stepParameters) {

      if ("mapper".equals(p.getName()))
        mapperName = p.getStringValue();
      else if ("mapper.arguments".equals(p.getName())
          || "mapperarguments".equals(p.getName()))
        this.mapperArguments = p.getStringValue();
      else if ("hadoop.threads".equals(p.getName()))
        this.hadoopThreads = p.getIntValue();
      else if ("local.threads".equals(p.getName()))
        this.localThreads = p.getIntValue();
      else if ("max.local.threads".equals(p.getName()))
        this.maxLocalThreads = p.getIntValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (mapperName == null) {
      throw new EoulsanException("No mapper set.");
    }

    this.mapper =
        SequenceReadsMapperService.getInstance().newService(mapperName);

    if (this.mapper == null) {
      throw new EoulsanException("Unknown mapper: " + mapperName);
    }

    if (this.mapper.isIndexGeneratorOnly()) {
      throw new EoulsanException(
          "The selected mapper can only be used for index generation: "
              + mapperName);
    }

    // Log Step parameters
    LOGGER.info("In "
        + getName() + ", mapper=" + this.mapper.getMapperName() + " (version: "
        + mapper.getMapperVersion() + ")");
    LOGGER
        .info("In " + getName() + ", mapperarguments=" + this.mapperArguments);

  }

}
