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
package fr.ens.transcriptome.eoulsan.it;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;

/**
 * This internal class allow to save Process outputs
 * @author Laurent Jourdren
 */
public class ITCommandResult {

  private final File directory;
  private final String commandLine;
  private final File stdout;
  private final File stderr;
  private final String desc;

  private final StringBuilder message;

  private Throwable exception;
  private String exceptionMessage;
  private int exitValue = -1;
  private long duration = -1;

  public boolean isEmpty() {
    return message.toString().isEmpty();
  }

  public String getReport() {

    message.append("\nExecute script for " + this.desc);
    message.append("\n\tcommand line: " + this.commandLine);
    message.append("\n\tin directory: " + this.directory.getAbsolutePath());

    message.append("\n\tduration: "
        + (duration == -1 ? "none" : toTimeHumanReadable(this.duration)));

    message.append("\n\texit value: " + this.exitValue);

    message.append("\n");

    return message.toString();
  }

  public boolean isCatchedException() {
    return exception != null;
  }

  //
  // Getter & setter
  //

  public void setExitValue(final int exitValue) {
    this.exitValue = exitValue;
  }

  public void setDuration(final long duration) {
    this.duration = duration;
  }

  public Throwable getException() {
    return this.exception;
  }

  public void setException(Exception exception) {
    setException(exception, "");
  }

  public void setException(Exception exception, final String message) {
    this.exception = exception;
    this.exceptionMessage = message;
  }

  //
  // Constructor
  //
  ITCommandResult(final String commandLine, final File directory,
      final File stdoutFile, final File stderrFile, final String desc) {

    this.commandLine = commandLine;
    this.directory = directory;
    this.stdout = stdoutFile;
    this.stderr = stderrFile;
    this.desc = desc;

    this.message = new StringBuilder();

  }

}
