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

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.core.StepContext;
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
    ROOT_STEP(0, "root"), DESIGN_STEP(1, "design"), CHECKER_STEP(2, "checker"),
    GENERATOR_STEP(3, null), FIRST_STEP(4, "first"), STANDARD_STEP(5, null),
    TERMINAL_STEP(6, "terminal");

    private int priority;
    private String defaultStepId;

    /**
     * Get the priority of the step.
     * @return the priority of the step
     */
    public int getPriority() {
      return this.priority;
    }

    /**
     * Get default step id.
     * @return the default step id or null if not exists
     */
    public String getDefaultStepId() {

      return this.defaultStepId;
    }

    /**
     * Return the available default step ids of the step types.
     * @return a set with the values
     */
    public static final Set<String> getAllDefaultStepId() {

      final Set<String> result = Sets.newHashSet();

      for (StepType type : values()) {

        final String stepId = type.getDefaultStepId();
        if (stepId != null)
          result.add(stepId);
      }

      return result;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param priority priority of the ste
     * @param defaultStepId default step id
     */
    StepType(final int priority, final String defaultStepId) {

      this.priority = priority;
      this.defaultStepId = defaultStepId;
    }
  };

  /**
   * This enum define the states of the steps.
   * @author Laurent Jourdren
   * @since 1.3
   */
  public static enum StepState {
    CREATED, CONFIGURED, WAITING, READY, WORKING, DONE, FAIL
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
  StepContext getContext();

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