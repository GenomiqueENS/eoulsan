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

package fr.ens.transcriptome.eoulsan.core;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.util.Version;

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
   * Get the input data types.
   * @return an array of DataType
   */
  DataType[] getInputTypes();

  /**
   * Get the output data types.
   * @return an array of DataType
   */
  DataType[] getOutputType();

  /**
   * Get the name of the log file name for this step.
   * @return the name of the file for the log name
   */
  String getLogName();

  /**
   * Set the parameters of the step and the global parameters to configure the
   * step.
   * @param stepParameters parameters of the step
   * @param globalParameters global parameters
   * @throws EoulsanException if a parameter is invalid
   */
  void configure(Set<Parameter> stepParameters, Set<Parameter> globalParameters)
      throws EoulsanException;

  /**
   * Execute the step.
   * @param design the design to use
   * @param info Executor information
   * @throws EoulsanException if an error occurs while executing step
   */
  StepResult execute(Design design, ExecutorInfo info);

}
