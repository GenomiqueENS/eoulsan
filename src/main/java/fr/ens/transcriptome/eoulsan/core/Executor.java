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

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This class is the executor for running all the steps of an analysis.
 * @author Laurent Jourdren
 */
public abstract class Executor {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private Command command;

  private long startTimeCurrentStep;

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
  protected abstract SimpleContext getContext();

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
    final SimpleContext context = getContext();

    // Check base path
    if (context.getBasePathname() == null)
      throw new EoulsanException("The base path is null");

    // Load design
    final Design design = loadDesign();

    // Check design
    checkDesign(design);

    // Add executor info
    context.addCommandInfo(command);

    // Create the workflow
    final Workflow workflow =
        new Workflow(command, design, context, hadoopMode);

    // Add the workflow to Context
    context.setWorkflow(workflow);

    // Add the design to the context
    context.setDesign(design);

    // Insert terminal steps (e.g. upload hdfs/S3, start local/Amazon hadoop)
    workflow.addFirstSteps(firstSteps);
    workflow.addEndSteps(endSteps);

    // Init steps of the workflow
    workflow.init();

    // Check workflow
    workflow.check();

    LOGGER.info("Date: " + new Date(System.currentTimeMillis()));
    LOGGER.info("Host: " + SystemUtils.getHostName());
    LOGGER.info("Operating system name: " + System.getProperty("os.name"));
    LOGGER.info("Operating system arch: " + System.getProperty("os.arch"));
    LOGGER
        .info("Operating system version: " + System.getProperty("os.version"));
    LOGGER.info("Java version: " + System.getProperty("java.version"));
    LOGGER.info("Log level: " + LOGGER.getLevel());

    final long startTime = System.currentTimeMillis();

    // Execute steps
    for (Step step : workflow.getSteps()) {

      final String stepName = step.getName();

      context.setStep(step);
      logStartStep(stepName);

      // execute step
      final StepResult result = step.execute(design, context);

      logEndStep(stepName);
      context.setStep(null);

      if (result == null) {
        LOGGER.severe("No result for step: " + stepName);
        throw new EoulsanException("No result for step: " + stepName);
      }

      // Write step logs
      writeStepLogs(result);

      // End of the analysis if the analysis fail
      if (!result.isSuccess()) {
        LOGGER.severe("Fail of the analysis: " + result.getErrorMessage());
        logEndAnalysis(false, startTime);

        if (result.getException() != null)
          Common.errorExit(result.getException(), result.getErrorMessage());
        else
          Common.errorExit(new EoulsanException("Fail of the analysis."),
              result.getErrorMessage());
      }

      // If the step is terminal step, end of the execution of the workflow
      if (step.isTerminalStep())
        break;
    }

    logEndAnalysis(true, startTime);
  }

  //
  // Utility methods
  //

  /**
   * Add log entry for step phase.
   * @param stepName Name of current the phase
   */
  private void logStartStep(final String stepName) {

    this.startTimeCurrentStep = System.currentTimeMillis();
    LOGGER.info("Start " + stepName + " step.");
  }

  /**
   * Add log entry for end step.
   * @param stepName Name of current the step
   */
  private void logEndStep(final String stepName) {

    final long endTimePhase = System.currentTimeMillis();

    LOGGER.info("Process step "
        + stepName
        + " in "
        + StringUtils.toTimeHumanReadable(endTimePhase
            - this.startTimeCurrentStep) + " s.");
  }

  /**
   * Log the state and the time of the analysis
   * @param success true if analysis was successful
   * @param startTime start time of the analysis is milliseconds since Java
   *          epoch
   */
  private void logEndAnalysis(final boolean success, final long startTime) {

    final long endTime = System.currentTimeMillis();

    LOGGER.info((success ? "Successful" : "Unsuccessful")
        + " end of the analysis in "
        + StringUtils.toTimeHumanReadable(endTime - startTime) + " s.");
  }

}
