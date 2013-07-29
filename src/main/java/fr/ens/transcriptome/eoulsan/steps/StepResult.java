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

package fr.ens.transcriptome.eoulsan.steps;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define the result of a step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class StepResult {

  private WorkflowStep step;
  private boolean success;
  private String logMessage;
  private Exception exception;
  private String errorMessage;

  //
  // Getter
  //

  /**
   * Test the result of the step is successful.
   * @return Returns the success
   */
  public boolean isSuccess() {

    return this.success;
  }

  /**
   * Get the log message for the step.
   * @return Returns the logMessage
   */
  public String getLogMessage() {

    return this.logMessage;
  }

  /**
   * Get the step object.
   * @return Returns the step
   */
  public WorkflowStep getStep() {

    return step;
  }

  /**
   * Get the exception.
   * @return Returns the exception
   */
  public Exception getException() {

    return exception;
  }

  /**
   * Get the error message.
   * @return Returns the errorMessage
   */
  public String getErrorMessage() {

    return errorMessage;
  }

  /**
   * Create the log message.
   * @param context Context for the step
   * @param startTime start time of the step
   * @param logMsg custom message for the log setp
   * @return the log message
   */
  private static final String createLogMessage(final Context context,
      long startTime, final String logMsg) {

    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;
    final WorkflowStep step = context.getCurrentStep();

    return "Job Id: "
        + context.getJobId() + " [" + context.getJobUUID()
        + "]\nJob description: " + context.getJobDescription()
        + "\nJob environment: " + context.getJobEnvironment() + "\nStep: "
        + step.getId() + " [" + step.getClass().getCanonicalName()
        + "]\nParameters:\n" + parametersToString(step.getParameters())
        + "Start time: " + new Date(startTime) + "\nEnd time: "
        + new Date(endTime) + "\nDuration: "
        + StringUtils.toTimeHumanReadable(duration) + "\n" + logMsg;
  }

  /**
   * Return a string with a sorted list of parameters.
   * @param parameters set of parameter
   * @return a string with a sorted list of parameters
   */
  private static final String parametersToString(final Set<Parameter> parameters) {

    if (parameters == null) {
      return "\tno parameters.\n";
    }

    final List<Parameter> parametersList = Lists.newArrayList(parameters);
    Collections.sort(parametersList, new Comparator<Parameter>() {

      @Override
      public int compare(final Parameter p1, final Parameter p2) {

        return p1.getName().compareTo(p2.getName());
      }
    });

    final StringBuilder sb = new StringBuilder();

    for (Parameter p : parametersList) {

      sb.append('\t');
      sb.append(p.getName());
      sb.append(": ");
      sb.append(p.getStringValue());
      sb.append("\n");
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param context context of the step result
   * @param success true if the step is successful
   * @param logMsg log message
   */
  public StepResult(final Context context, final boolean success,
      final String logMsg) {

    this.step = context.getCurrentStep();
    this.success = success;
    this.logMessage = logMsg;
  }

  /**
   * Public constructor.
   * @param context context of the step result
   * @param startTime the time of the start of the step
   * @param success true if the step is successful
   * @param logMsg log message
   */
  public StepResult(final Context context, final boolean success,
      final long startTime, final String logMsg) {

    this(context, success, createLogMessage(context, startTime, logMsg));
  }

  /**
   * Public constructor.
   * @param context context of the step result
   * @param startTime the time of the start of the step
   * @param success true if the step is successful
   * @param logMsg log message
   */
  public StepResult(final Context context, final boolean success,
      final long startTime, final String logMsg, final String errorMsg) {

    this(context, success, createLogMessage(context, startTime, logMsg));
    this.errorMessage = errorMsg;
  }

  /**
   * Public constructor for a successful step
   * @param context context of the step result
   * @param startTime the time of the start of the step
   * @param logMsg log message
   */
  public StepResult(final Context context, final long startTime,
      final String logMsg) {

    this(context, true, createLogMessage(context, startTime, logMsg));
  }

  /**
   * Public constructor.
   * @param context context of the step result
   * @param exception exception of the error
   * @param errorMsg Error message
   * @param logMsg log message
   */
  public StepResult(final Context context, final Exception exception,
      final String errorMsg, final String logMsg) {

    this(context, false, logMsg);
    this.exception = exception;
    this.errorMessage = errorMsg;
  }

  /**
   * Public constructor.
   * @param context context of the step result
   * @param exception exception of the error
   * @param errorMsg Error message
   */
  public StepResult(final Context context, final Exception exception,
      final String errorMsg) {

    this(context, exception, errorMsg, null);
  }

  /**
   * Public constructor.
   * @param context context of the step result
   * @param exception exception of the error
   */
  public StepResult(final Context context, final Exception exception) {

    this(context, exception, exception.getMessage(), null);
  }

}
