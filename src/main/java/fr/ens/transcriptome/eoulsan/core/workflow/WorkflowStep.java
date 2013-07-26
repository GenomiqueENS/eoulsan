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

package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

public interface WorkflowStep {

  public static enum StepType {
    STANDARD_STEP, DESIGN_STEP, FIRST_STEP, TERMINAL_STEP, GENERATOR_STEP
  };

  public static enum StepState {
    CREATED, CONFIGURED, WORKING, DONE
  };

  /**
   * Get step id.
   * @return the step id
   */
  String getId();

  /**
   * Test if the step must be skipped
   * @return true if the step must be skipped
   */
  boolean isSkip();

  /**
   * Get the type of the step.
   * @return the type of the step;
   */
  StepType getType();

  /**
   * Get the step.
   * @return the step object
   */
  Step getStep();

  /**
   * Get the duration of the execution of the step.
   * @return the duration of the step in milliseconds
   */
  long getDuration();

  /**
   * Get the state of the step.
   * @return the state of the step
   */
  StepState getState();

  /**
   * Configure the step.
   * @throws EoulsanException if an error occurs while configuring a step
   */
  void configure() throws EoulsanException;

  /**
   * Execute the step.
   * @return a Step result object.
   */
  StepResult execute();

  /**
   * Get the previous steps.
   * @return the previous steps
   */
  Set<WorkflowStep> getPreviousSteps();

  /**
   * Get the next steps.
   * @return the next steps
   */
  Set<WorkflowStep> getNextSteps();

}