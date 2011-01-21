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

package fr.ens.transcriptome.eoulsan.steps;

import java.util.Date;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define the result of a step.
 * @author Laurent Jourdren
 */
public class StepResult {

  private Step step;
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
  public Step getStep() {

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

  private static String createLogMessage(final Context context, long startTime,
      final String logMsg) {

    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;
    final Step step = context.getCurrentStep();

    return "Job Id:"
        + context.getJobId() + " [" + context.getJobUUID()
        + "]\nJob description: " + context.getJobDescription() + "\nStep: "
        + step.getName() + " [" + step.getClass().getCanonicalName()
        + "]\nStart time: " + new Date(startTime) + "\nEnd time: "
        + new Date(endTime) + "\nDuration: "
        + StringUtils.toTimeHumanReadable(duration) + "\n" + logMsg;
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
   * @param Exception exception of the error
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
   * @param Exception exception of the error
   * @param errorMsg Error message
   */
  public StepResult(final Context context, final Exception exception,
      final String errorMsg) {

    this(context, exception, errorMsg, null);
  }

  /**
   * Public constructor.
   * @param context context of the step result
   * @param Exception exception of the error
   * @param errorMsg Error message
   */
  public StepResult(final Context context, final Exception exception) {

    this(context, exception, exception.getMessage(), null);
  }

}
