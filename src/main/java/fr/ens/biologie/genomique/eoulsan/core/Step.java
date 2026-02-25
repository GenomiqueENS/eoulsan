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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * This interface define a step of the workflow.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface Step extends Serializable {

  /**
   * This enum define the type of step.
   *
   * @author Laurent Jourdren
   * @since 2.0
   */
  enum StepType {
    ROOT_STEP(0, "root"),
    DESIGN_STEP(1, "design"),
    CHECKER_STEP(2, "checker"),
    GENERATOR_STEP(3, null),
    FIRST_STEP(4, "first"),
    STANDARD_STEP(5, null),
    TERMINAL_STEP(6, "terminal");

    private final int priority;
    private final String defaultStepId;

    /**
     * Get the priority of the step.
     *
     * @return the priority of the step
     */
    public int getPriority() {
      return this.priority;
    }

    /**
     * Get default step id.
     *
     * @return the default step id or null if not exists
     */
    public String getDefaultStepId() {

      return this.defaultStepId;
    }

    /**
     * Return the available default step ids of the step types.
     *
     * @return a set with the values
     */
    public static Set<String> getAllDefaultStepId() {

      final Set<String> result = new HashSet<>();

      for (StepType type : values()) {

        final String stepId = type.getDefaultStepId();
        if (stepId != null) {
          result.add(stepId);
        }
      }

      return result;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     *
     * @param priority priority of the ste
     * @param defaultStepId default step id
     */
    StepType(final int priority, final String defaultStepId) {

      this.priority = priority;
      this.defaultStepId = defaultStepId;
    }
  }

  /**
   * This enum define the value of the discard output attribute of the step tag in the Eoulsan
   * workflow file.
   *
   * @author Laurent Jourdren
   * @since 2.0
   */
  enum DiscardOutput {
    NO,
    ASAP,
    SUCCESS;

    /**
     * Test if result must be copied to output.
     *
     * @return true if result must be copied to output
     */
    public boolean isCopyResultsToOutput() {

      return this == NO;
    }

    /**
     * This method define a parser for the values of the "discardoutput" attribute of the Eoulsan
     * workflow.
     *
     * @param s the input string
     * @return a DiscardOutput value
     * @throws EoulsanException if the attribute value is unknown
     */
    public static DiscardOutput parse(final String s) throws EoulsanException {

      if (s == null) {
        throw new NullPointerException("s argument cannot be null");
      }

      switch (s.toLowerCase(Globals.DEFAULT_LOCALE).trim()) {
        case "no":
        case "false":
        case "":
          return NO;

        case "asap":
          return ASAP;

        case "yes":
        case "completed":
        case "true":
          return SUCCESS;

        default:
          throw new EoulsanException("Unknown value for discardouput attribute: " + s);
      }
    }
  }

  /**
   * This enum define the states of the steps.
   *
   * @author Laurent Jourdren
   * @since 2.0
   */
  enum StepState {
    CREATED,
    CONFIGURED,
    WAITING,
    READY,
    WORKING,
    PARTIALLY_DONE,
    DONE,
    FAILED,
    ABORTED;

    /**
     * Test if the state is a final state.
     *
     * @return true if the state is a final state
     */
    public boolean isFinalState() {

      return this == DONE || this == FAILED || this == ABORTED;
    }

    /**
     * Test if the state is a working state.
     *
     * @return true if the state is a working state
     */
    public boolean isWorkingState() {

      return this == WORKING || this == PARTIALLY_DONE;
    }

    /**
     * Test if the state is a done state.
     *
     * @return true if the state is a done state
     */
    public boolean isDoneState() {

      return this == PARTIALLY_DONE || this == DONE;
    }
  }

  /**
   * Get the workflow of the step.
   *
   * @return the workflow of the step
   */
  Workflow getWorkflow();

  /**
   * Get the unique numerical identifier of the step.
   *
   * @return the number of the step
   */
  int getNumber();

  /**
   * Get step id.
   *
   * @return the step id
   */
  String getId();

  /**
   * Test if the step must be skipped
   *
   * @return true if the step must be skipped
   */
  boolean isSkip();

  /**
   * Get the type of the step.
   *
   * @return the type of the step;
   */
  StepType getType();

  /**
   * Get the module name.
   *
   * @return the module object
   */
  String getModuleName();

  /**
   * Get the step version required by user.
   *
   * @return a string with the version of the step required by the user
   */
  String getStepVersion();

  /**
   * Get the parameter of the step.
   *
   * @return a Set with the parameters of the step
   */
  Set<Parameter> getParameters();

  /**
   * Get the required memory for the step.
   *
   * @return the required memory of the step in MB or -1 if the default setting must be used
   */
  int getRequiredMemory();

  /**
   * Get the required processors for the step.
   *
   * @return the required processors count for the step in MB or -1 if the default setting must be
   *     used
   */
  int getRequiredProcessors();

  /**
   * Get the input ports of the step.
   *
   * @return the input ports of the step
   */
  InputPorts getInputPorts();

  /**
   * Get the output ports of the step.
   *
   * @return the output ports of the step
   */
  OutputPorts getOutputPorts();

  /**
   * Get the state of the step.
   *
   * @return the state of the step
   */
  StepState getState();

  /**
   * Get Checker.
   *
   * @return the checker for the step
   */
  Checker getChecker();
}
