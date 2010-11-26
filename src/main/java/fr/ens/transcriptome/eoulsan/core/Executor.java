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
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

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
  protected abstract SimpleContext getExecutorInfo();

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
    final SimpleContext info = getExecutorInfo();

    // Check base path
    if (info.getBasePathname() == null)
      throw new EoulsanException("The base path is null");

    // Load design
    final Design design = loadDesign();

    // Check design
    if (design == null)
      throw new EoulsanException("The design is null");

    // Check samples count
    if (design.getSampleCount() == 0)
      throw new EoulsanException(
          "Nothing to do, no samples found in design file");

    // Add executor info
    info.addCommandInfo(command);

    // Create the workflow
    final Workflow workflow = new Workflow(command, design, info, hadoopMode);

    // Add the workflow to ExecutorInfo
    info.setWorkflow(workflow);

    // Insert terminal steps (e.g. upload hdfs/S3, start local/Amazon hadoop)
    workflow.addFirstSteps(firstSteps);
    workflow.addEndSteps(endSteps);

    // Check workflow
    workflow.check();

    // Init steps of the workflow
    workflow.init();

    logger.info("Date: " + new Date(System.currentTimeMillis()));
    logger.info("Host: " + SystemUtils.getHostName());
    logger.info("Operating system name: " + System.getProperty("os.name"));
    logger.info("Operating system arch: " + System.getProperty("os.arch"));
    logger
        .info("Operating system version: " + System.getProperty("os.version"));
    logger.info("Java version: " + System.getProperty("java.version"));
    logger.info("Log level: " + logger.getLevel());

    final long startTime = System.currentTimeMillis();

    // Execute steps
    for (Step s : workflow.getSteps()) {

      logStartStep(s.getName());
      final StepResult r = s.execute(design, info);
      logEndStep(s.getName());

      if (r == null) {
        logger.severe("No result for step: " + s.getName());
        throw new EoulsanException("No result for step: " + s.getName());
      }

      // Write step logs
      writeStepLogs(r);

      // End of the analysis if the analysis fail
      if (!r.isSuccess()) {
        logger.severe("Fail of the analysis: " + r.getErrorMessage());
        logEndAnalysis(false, startTime);

        if (r.getException() != null)
          Common.errorExit(r.getException(), r.getErrorMessage());
        else
          Common.errorExit(new EoulsanException("Fail of the analysis."), r
              .getErrorMessage());
      }

      // If the step is terminal step, end of the execution of the worflow
      if (s.isTerminalStep())
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
    logger.info("Start " + stepName + " step.");
  }

  /**
   * Add log entry for end step.
   * @param stepName Name of current the step
   */
  private void logEndStep(final String stepName) {

    final long endTimePhase = System.currentTimeMillis();

    logger.info("Process step "
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

    logger.info(success ? "Successful" : "Unsuccessful"
        + " end of the analysis in "
        + StringUtils.toTimeHumanReadable(endTime - startTime) + " s.");
  }

}
