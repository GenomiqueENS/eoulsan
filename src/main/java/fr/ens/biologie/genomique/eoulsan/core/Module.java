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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;

/**
 * This interface define a Module.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface Module {

  /**
   * Get the name of the module.
   * @return the name of the module
   */
  String getName();

  /**
   * Get the description of the module
   * @return the description of the module
   */
  String getDescription();

  /**
   * Get version of the Module.
   * @return a Version object with the version of the Module
   */
  Version getVersion();

  /**
   * Get the required Version of the application to run the Module.
   * @return a Version object with the required version of the Module
   */
  Version getRequiredEoulsanVersion();

  /**
   * Get the input data format.
   * @return a set with DataFormat or null if the module does not any need input
   *         format
   */
  InputPorts getInputPorts();

  /**
   * Get the output data format.
   * @return an set with DataFormat or null if the module does not produce any
   *         output data
   */
  OutputPorts getOutputPorts();

  /**
   * Get the requirements of the module.
   * @return a set with the requirements of the module
   */
  Set<Requirement> getRequirements();

  /**
   * Get the checker for the module
   * @return the checker for the module
   */
  Checker getChecker();

  /**
   * Set the parameters of the step to configure the module.
   * @param context configuration context. The context can be null for generator
   *          steps
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  void configure(StepConfigurationContext context,
      Set<Parameter> stepParameters) throws EoulsanException;

  /**
   * Execute a task step.
   * @param context Execution context
   * @param status of the task
   */
  TaskResult execute(TaskContext context, TaskStatus status);

  /**
   * Get the parallelization mode of the module.
   * @return a ParallelizationMode enum
   */
  ParallelizationMode getParallelizationMode();
}
