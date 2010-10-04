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

  private static String createLogMessage(final long startTime,
      final String logMsg) {

    final long endTime = System.currentTimeMillis();
    final long duration = endTime - startTime;

    return "Start time: "
        + new Date(startTime) + "\nEnd time: " + new Date(endTime)
        + "\nDuration: " + StringUtils.toTimeHumanReadable(duration) + "\n"
        + logMsg;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param step the step for this result
   * @param success true if the step is successful
   * @param logMsg log message
   */
  public StepResult(final Step step, final boolean success, final String logMsg) {

    this.step = step;
    this.success = success;
    this.logMessage = logMsg;
  }

  /**
   * Public constructor.
   * @param step the step for this result
   * @param success true if the step is successful
   * @param logMsg log message
   */
  public StepResult(final Step step, final long startTime, final String logMsg) {

    this(step, true, createLogMessage(startTime, logMsg));
  }

  /**
   * Public constructor.
   * @param step the step for this result
   * @param Exception exception of the error
   * @param errorMsg Error message
   * @param logMsg log message
   */
  public StepResult(final Step step, final Exception exception,
      final String errorMsg, final String logMsg) {

    this(step, false, logMsg);
    this.exception = exception;
    this.errorMessage = errorMsg;
  }

  /**
   * Public constructor.
   * @param step the step for this result
   * @param Exception exception of the error
   * @param errorMsg Error message
   */
  public StepResult(final Step step, final Exception exception,
      final String errorMsg) {

    this(step, exception, errorMsg, null);
  }

}
