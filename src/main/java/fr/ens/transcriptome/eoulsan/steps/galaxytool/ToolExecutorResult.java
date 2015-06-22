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
package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static org.python.google.common.base.Preconditions.checkNotNull;

/**
 * The class define a result on execution tool.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolExecutorResult {

  /** The command line tool. */
  private final String commandLineTool;

  /** The exit value. */
  private int exitValue = -100000;

  /** The exception. */
  private Throwable exception = null;

  /**
   * Sets the exception.
   * @param exception the new exception
   */
  public void setException(final Throwable exception) {
    this.exception = exception;
  }

  /**
   * Sets the exit value.
   * @param exitValue the new exit value
   */
  public void setExitValue(final int exitValue) {
    this.exitValue = exitValue;
  }

  /**
   * Gets the exception.
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

  /**
   * As throwed exception.
   * @return true, if successful
   */
  public boolean asThrowedException() {
    return this.exception != null;
  }

  /**
   * Gets the exit value.
   * @return the exit value
   */
  public int getExitValue() {
    return this.exitValue;
  }

  /**
   * Gets the command line.
   * @return the command line
   */
  public String getCommandLine() {
    return this.commandLineTool;
  }

  @Override
  public String toString() {
    return "GalaxyToolResult [commandLineTool="
        + commandLineTool + ", exitValue=" + exitValue + ", exception="
        + exception + "]";
  }

  //
  // Constructor
  //

  /**
   * Public constructor, command line can not be null or empty
   * @param commandLineTool the command line tool
   */
  public ToolExecutorResult(final String commandLineTool) {

    checkNotNull(commandLineTool, "Command line anc not be null.");

    this.commandLineTool = commandLineTool;
  }
}
