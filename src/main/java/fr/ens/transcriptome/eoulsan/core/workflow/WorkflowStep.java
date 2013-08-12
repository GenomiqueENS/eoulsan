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

import java.io.Serializable;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepResult;

/**
 * This interface define a step of the workflow.
 * @author Laurent Jourdren
 * @since 1.3
 */
public interface WorkflowStep extends Serializable {

  /**
   * This enum define the type of step.
   * @author Laurent Jourdren
   * @since 1.3
   */
  public static enum StepType {
    ROOT_STEP(0), DESIGN_STEP(1), CHECKER_STEP(2), GENERATOR_STEP(3),
    FIRST_STEP(4), STANDARD_STEP(5), TERMINAL_STEP(6);

    private int priority;

    /**
     * Get the priority of the step.
     * @return the priority of the step
     */
    public int getPriority() {
      return this.priority;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param priority priority of the step
     */
    StepType(final int priority) {

      this.priority = priority;
    }
  };

  /**
   * This enum define the states of the steps.
   * @author Laurent Jourdren
   * @since 1.3
   */
  public static enum StepState {
    CREATED, CONFIGURED, WAITING, READY, WORKING, DONE
  };

  /**
   * Get the workflow of the step.
   * @return the workflow of the step
   */
  Workflow getWorkflow();

  /**
   * Get the context used by the workflow.
   * @return a Context object
   */
  Context getContext();

  /**
   * Get the unique numerical identifier of the step.
   * @return the number of the setp
   */
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

  /**
   * Get the parameter of the step.
   * @return a Set with the parameters of the step
   */
  Set<Parameter> getParameters();

  /**
   * Get the state of the step.
   * @return the state of the step
   */
  StepState getState();

  /**
   * Get step result.
   * @return the result object
   */
  StepResult getResult();

}