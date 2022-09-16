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
package fr.ens.biologie.genomique.eoulsan.it;

import static fr.ens.biologie.genomique.eoulsan.Globals.DEFAULT_CHARSET;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

/**
 * This internal class allow to save Process outputs.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ITCommandResult {

  private final File directory;
  private final String commandLine;
  private final String desc;

  private final StringBuilder message;

  private Throwable exception;
  private int exitValue = -1;
  private long duration = -1;
  private final long durationMaxToInterruptProcess;

  @SuppressWarnings("unused")
  private String exceptionMessage;
  private boolean interruptedProcess = false;
  private File stderrFile;

  private boolean isReportCreated = false;

  public boolean isEmpty() {
    return this.message.toString().isEmpty();
  }

  /**
   * Gets the report on execution command line.
   * @return the report
   */
  public String getReport() {

    if (isReportCreated) {
      return this.message.toString();
    }

    this.message.append("\nExecute ");
    this.message.append(this.desc);
    this.message.append(":");

    this.message.append("\n\tCommand line: ");
    this.message.append(this.commandLine);

    this.message.append("\n\tDirectory: ");
    this.message.append(this.directory.getAbsolutePath());

    this.message.append("\n\tDuration: ");
    this.message.append(
        this.duration == -1 ? "none" : toTimeHumanReadable(this.duration));

    this.message.append("\n\tMessage: exit value ");
    this.message.append(this.exitValue);

    // Add standard error on script save un stderr file
    this.message.append(getSTDERRMessageOnProcess());

    // TODO
    // this.message.append("\n\tMessage: interrupted " +
    // isInterruptedProcess());

    if (isInterruptedProcess()) {
      this.message.append("\n\tInterrupt process after: ");
      this.message.append(toTimeHumanReadable(durationMaxToInterruptProcess));
    }

    this.message.append("\n");

    isReportCreated = true;

    return this.message.toString();
  }

  /**
   * Adds the stderr message on process in the report.
   * @return the string
   */
  public String getSTDERRMessageOnProcess() {

    if (stderrFile == null || !stderrFile.exists()) {
      return "";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("\n\tCopy content of standard error file from: ");
    sb.append(this.stderrFile.getAbsolutePath());
    sb.append('\n');

    // Read error file
    try (BufferedReader br = Files.newReader(stderrFile, DEFAULT_CHARSET)) {

      String line = "";

      // Add all lines
      while ((line = br.readLine()) != null) {
        sb.append("\n\t\t");
        sb.append(line);
      }

      sb.append("\nEnd file\n\n");

    } catch (IOException e) {
      // Add warning message in report file
      sb.append("\nAn error occurs during read file ");
      sb.append(stderrFile.getAbsolutePath());
      sb.append("\n\n");
    }

    return sb.toString();
  }

  //
  // Getter & setter
  //

  /**
   * Checks if is caught exception.
   * @return true, if is caught exception
   */
  public boolean isCaughtException() {
    return this.exception != null;
  }

  /**
   * Set the exit value.
   * @param exitValue the new exit value
   */
  public void setExitValue(final int exitValue) {
    this.exitValue = exitValue;
  }

  /**
   * Set the duration.
   * @param duration the new duration
   */
  public void setDuration(final long duration) {
    this.duration = duration;
  }

  /**
   * Get the exception.
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

  /**
   * Set the exception.
   * @param exception the new exception
   */
  public void setException(final Exception exception) {
    setException(exception, "");
  }

  /**
   * Set the exception.
   * @param exception the exception
   * @param message the message
   */
  public void setException(final Exception exception, final String message) {
    this.exception = exception;
    this.exceptionMessage = message;
  }

  /**
   * Sets the error file on process.
   * @param stderrFile the new error file on process
   */
  public void setErrorFileOnProcess(final File stderrFile) {
    this.stderrFile = stderrFile;
  }

  /**
   * As error file save.
   * @return true, if successful
   */
  public boolean asErrorFileSave() {
    return this.stderrFile != null && this.stderrFile.exists();
  }

  /**
   * Checks if is interrupted process.
   * @return true, if is interrupted process
   */
  public boolean isInterruptedProcess() {
    return interruptedProcess;
  }

  /**
   * Sets the interrupted process at true.
   */
  public void asInterruptedProcess() {
    this.interruptedProcess = true;
  }

  //
  // Constructor
  //
  /**
   * Constructor.
   * @param commandLine the command line
   * @param directory the directory
   * @param desc the description on command line
   */
  ITCommandResult(final String commandLine, final File directory,
      final String desc, final int duration) {

    this.commandLine = commandLine;
    this.directory = directory;
    this.desc = desc;
    this.durationMaxToInterruptProcess = duration * 60 * 1000;

    this.message = new StringBuilder();

  }

}
