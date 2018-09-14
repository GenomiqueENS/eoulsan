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
package fr.ens.biologie.genomique.eoulsan.galaxytools;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.List;

import com.google.common.base.Joiner;

/**
 * The class define a result on execution tool.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolExecutorResult {

  private static final int NO_EXIT_VALUE = Integer.MIN_VALUE;

  /** The command line tool. */
  private final List<String> commandLineTool;

  /** The exit value. */
  private final int exitValue;

  /** The exception. */
  private final Throwable exception;

  /**
   * Gets the exception.
   * @return the exception
   */
  public Throwable getException() {
    return this.exception;
  }

  /**
   * Test if an exception has been thrown.
   * @return false, if successful
   */
  public boolean isException() {
    return this.exception == null;
  }

  /**
   * Gets the exit value.
   * @return the exit value
   */
  public int getExitValue() {
    return this.exitValue;
  }

  /**
   * Gets the command line as a list of string arguments.
   * @return the command line as a list of string arguments
   */
  public List<String> getCommandLine() {
    return this.commandLineTool;
  }

  /**
   * Gets the command line as a String.
   * @return the command line as a String
   */
  public String getCommandLineAsString() {
    return Joiner.on(' ').join(this.commandLineTool);
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
   * @param exitValue exit code
   */
  public ToolExecutorResult(final List<String> commandLineTool,
      final int exitValue) {

    requireNonNull(commandLineTool, "Command line can not be null");
    checkArgument(!commandLineTool.isEmpty(), "Command line can not be empty");

    this.commandLineTool = commandLineTool;
    this.exitValue = exitValue;
    this.exception = null;
  }

  /**
   * Public constructor, command line can not be null or empty
   * @param commandLineTool the command line tool
   * @param e exception
   */
  public ToolExecutorResult(final List<String> commandLineTool,
      final Throwable e) {

    requireNonNull(commandLineTool, "Command line can not be null.");
    checkArgument(!commandLineTool.isEmpty(), "Command line can not be empty");

    this.commandLineTool = commandLineTool;
    this.exitValue = NO_EXIT_VALUE;
    this.exception = e;
  }
}
