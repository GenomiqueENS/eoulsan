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

package fr.ens.transcriptome.eoulsan.core;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This interface define a Step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Step {

  /**
   * Get the name of the step.
   * @return the name of the step
   */
  String getName();

  /**
   * Get the description of the step
   * @return the description of the step
   */
  String getDescription();

  /**
   * Get version of the Step.
   * @return a Version object with the version of the Step
   */
  Version getVersion();

  /**
   * Get the required Version of the application to run the Step.
   * @return a Version object with the required version of the Step
   */
  Version getRequiredEoulsanVersion();

  /**
   * Get the input data format.
   * @return a set with DataFormat or null if the step does not any need input
   *         format
   */
  InputPorts getInputPorts();

  /**
   * Get the output data format.
   * @return an set with DataFormat or null if the step does not produce any
   *         output data
   */
  OutputPorts getOutputPorts();

  /**
   * Set the parameters of the step to configure the step.
   * @param context configuration context. The context can be null for generator
   *          steps
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  void configure(StepConfigurationContext context, Set<Parameter> stepParameters)
      throws EoulsanException;

  /**
   * Execute the step.
   * @param context Execution context
   * @param status of the step
   * @throws EoulsanException if an error occurs while executing step
   */
  StepResult execute(StepContext context, StepStatus status);

  /**
   * Get the parallelization mode of the step.
   * @return a ParallelizationMode enum
   */
  ParallelizationMode getParallelizationMode();
}
