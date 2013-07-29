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

import fr.ens.transcriptome.eoulsan.core.Parameter;

public interface WorkflowStep {

  public static enum StepType {
    ROOT_STEP(0), DESIGN_STEP(1), GENERATOR_STEP(2), FIRST_STEP(3),
    STANDARD_STEP(4), TERMINAL_STEP(5);

    private int priority;

    public int getPriority() {
      return this.priority;
    }

    StepType(final int priority) {

      this.priority = priority;
    }
  };

  public static enum StepState {
    CREATED, CONFIGURED, WAITING, READY, WORKING, DONE
  };

  /**
   * Get the workflow of the step.
   * @return the workflow of the step
   */
  Workflow getWorkflow();

  int getNumber();

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
   * Get the step name.
   * @return the step object
   */
  String getStepName();

  
  Set<Parameter> getParameters();
  
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

}