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

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflow.WorkflowStepResultProcessor;
import fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowContext;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;

/**
 * This class is the executor for running all the steps of an analysis.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class Executor {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private Command command;

  //
  // Getters
  //

  /**
   * Get the command object
   * @return Returns the command
   */
  protected Command getCommand() {
    return this.command;
  }

  //
  // Setters
  //

  /**
   * Set the command object.
   * @param command The command to set
   */
  protected void setCommand(final Command command) {
    this.command = command;
  }

  //
  // Abstract methods
  //

  /**
   * Load design object.
   */
  protected abstract Design loadDesign() throws EoulsanException;

  /**
   * Write the log file of the result of a step
   * @param result Step result
   */
  protected abstract void writeStepLogs(final StepResult result);

  /**
   * Get the execution context
   * @return the Context
   */
  protected abstract WorkflowContext getContext();

  /**
   * Check temporary directory.
   */
  protected abstract void checkTemporaryDirectory();

  /**
   * Check design.
   * @param design design to check
   * @throws EoulsanException if there is an issue with the design
   */
  private void checkDesign(final Design design) throws EoulsanException {

    if (design == null)
      throw new EoulsanException("The design is null");

    // Check samples count
    if (design.getSampleCount() == 0)
      throw new EoulsanException(
          "Nothing to do, no samples found in design file");
  }

  /**
   * run Eoulsan.
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute() throws EoulsanException {

    execute(null, null);
  }

  /**
   * run Eoulsan.
   * @param firstSteps steps to add at the begin the workflow
   * @param endSteps steps to add at the end the workflow
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute(final List<Step> firstSteps, final List<Step> endSteps)
      throws EoulsanException {

    execute(firstSteps, endSteps, EoulsanRuntime.getRuntime().isHadoopMode());
  }

  /**
   * run Eoulsan.
   * @param firstSteps steps to add at the begin the workflow
   * @param endSteps steps to add at the end the workflow
   * @param hadoopMode true if the steps must be compatible with Hadoop mode
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute(final List<Step> firstSteps, final List<Step> endSteps,
      final boolean hadoopMode) throws EoulsanException {

    // Check command object
    if (this.command == null)
      throw new EoulsanException("The command is null");

    // Get execution context
    final WorkflowContext context = getContext();

    // Add executor info
    context.logInfo();

    // Check base path
    if (context.getBasePathname() == null)
      throw new EoulsanException("The base path is null");

    // Load design
    LOGGER.info("Read design file");
    final Design design = loadDesign();
    LOGGER.info("Found "
        + design.getSampleCount() + " sample(s) in design file");

    // Check design
    checkDesign(design);

    // Create Workflow
    final CommandWorkflow workflow =
        new CommandWorkflow(context, getCommand(), firstSteps, endSteps, design);

    // Check temporary directory
    checkTemporaryDirectory();

    LOGGER.info("Start analysis at " + new Date(System.currentTimeMillis()));

    // Execute Workflow
    workflow.execute(new WorkflowStepResultProcessor() {

      @Override
      public void processResult(final WorkflowStep step, final StepResult result)
          throws EoulsanException {

        final String stepId = step.getId();

        if (result == null) {
          LOGGER.severe("No result for step: " + stepId);
          throw new EoulsanException("No result for step: " + stepId);
        }

        // Write step logs
        writeStepLogs(result);
      }
    });

  }
  //
  // Utility methods
  //

}
