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

package fr.ens.transcriptome.eoulsan.steps;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.util.Version;

public interface Step {

  enum ExecutionMode {
    LOCAL, HADOOP, BOTH
  };

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
  Version getRequiedEoulsanVersion();

  /**
   * Get the execution mode of the Step.
   * @return the execution mode of the Step
   */
  ExecutionMode getExecutionMode();

  /**
   * Get the input data format.
   * @return an array of DataFormat
   */
  DataFormat[] getInputFormats();

  /**
   * Get the output data format.
   * @return an array of DataFormat
   */
  DataFormat[] getOutputFormats();

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
   * @param context Execution context
   * @throws EoulsanException if an error occurs while executing step
   */
  StepResult execute(Design design, Context context);

  /**
   * Test if the workflow must end after this step.
   * @return true if this step the last step of the workflow
   */
  boolean isTerminalStep();

}
