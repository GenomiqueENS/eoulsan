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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.core.workflow.AbstractWorkflow.WorkflowStepResultProcessor;
import fr.ens.transcriptome.eoulsan.core.workflow.CommandWorkflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
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

  private final ExecutorArguments arguments;
  private final Command command;
  private final Design design;

  //
  // Getters
  //

  /**
   * Get executor arguments.
   * @return the ExecutorArguments object
   */
  protected ExecutorArguments getArguments() {

    return this.arguments;
  }

  //
  // Abstract methods
  //

  /**
   * Write the log file of the result of a step
   * @param result Step result
   */
  protected abstract void writeStepLogs(final StepResult result);

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

    LOGGER.info("Found "
        + design.getSampleCount() + " sample(s) in design file");
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
   * @param lastSteps steps to add at the end the workflow
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute(final List<Step> firstSteps, final List<Step> lastSteps)
      throws EoulsanException {

    // Add executor info
    logInfo(this.arguments, this.command);

    // Check base path
    if (this.arguments.getBasePathname() == null)
      throw new EoulsanException("The base path is null");

    // Check design
    checkDesign(design);

    // Create Workflow
    final CommandWorkflow workflow =
        new CommandWorkflow(this.arguments, this.command, firstSteps,
            lastSteps, this.design);

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

  /**
   * Log some information about the current execution.
   * @param execArgs execution information
   * @param command parameter file content
   */
  private static void logInfo(ExecutorArguments execArgs, final Command command) {

    LOGGER.info("Design file path: " + execArgs.getDesignPathname());
    LOGGER.info("Workflow parameter file path: "
        + execArgs.getParameterPathname());

    LOGGER.info("Workflow Author: " + command.getAuthor());
    LOGGER.info("Workflow Description: " + command.getDescription());
    LOGGER.info("Workflow Name: " + command.getName());

    LOGGER.info("Job Id: " + execArgs.getJobId());
    LOGGER.info("Job UUID: " + execArgs.getJobUUID());
    LOGGER.info("Job Description: " + execArgs.getJobDescription());
    LOGGER.info("Job Environment: " + execArgs.getJobEnvironment());

    LOGGER.info("Job Base path: " + execArgs.getBasePathname());
    LOGGER.info("Job Output path: " + execArgs.getOutputPathname());
    LOGGER.info("Job Log path: " + execArgs.getLogPathname());
  }

  private static Design loadDesign(final ExecutorArguments arguments)
      throws EoulsanException {

    try {

      // Load design
      LOGGER.info("Read design file");

      // Get input stream of design file from arguments object
      final InputStream is = arguments.openDesignFile();
      checkNotNull(is, "The input stream for design file is null");

      // Read design file and return the design object
      return new SimpleDesignReader(is).read();

    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }
  }

  private static Command loadCommand(final ExecutorArguments arguments)
      throws EoulsanException {

    try {

      // Get input stream of parameter file from arguments object
      final InputStream is = arguments.openParamFile();
      checkNotNull(is, "The input stream for parameter file is null");

      // Parse param file
      final ParamParser pp = new ParamParser(is);
      pp.addConstants(arguments);

      return pp.parse();
    } catch (IOException e) {
      throw new EoulsanException(e.getMessage());
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param arguments arguments for the Executor
   * @throws EoulsanException if an error occurs while loading and parsing
   *           design and parameter files
   */
  protected Executor(final ExecutorArguments arguments) throws EoulsanException {

    checkNotNull(arguments, "The arguments of the executor is null");

    this.arguments = arguments;
    this.design = loadDesign(arguments);
    this.command = loadCommand(arguments);
  }

}
