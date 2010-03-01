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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * Utility class for launching external process.
 * @author Laurent Jourdren
 */
public final class ProcessUtils {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /**
   * Execute a command with the OS.
   * @param cmd Command to execute
   * @param stdOutput don't show the result of the command on the standard
   *          output
   * @throws IOException if an error occurs while running the process
   */
  public static void exec(final String cmd, final boolean stdOutput)
      throws IOException {

    logger.fine("execute (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    InputStream std = p.getInputStream();
    BufferedReader stdr = new BufferedReader(new InputStreamReader(std));

    String l = null;

    while ((l = stdr.readLine()) != null) {
      if (stdOutput)
        System.out.println(l);
    }

    InputStream err = p.getInputStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err));

    String l2 = null;

    while ((l2 = errr.readLine()) != null)
      System.err.println(l2);

    stdr.close();
    errr.close();

    logEndTime(p, cmd, startTime);
  }

  /**
   * Execute a command with the OS and save the output in file.
   * @param cmd Command to execute
   * @param outputFile The output file
   * @throws IOException if an error occurs while running the process
   */
  public static void execWriteOutput(String cmd, File outputFile)
      throws IOException {

    logger.fine("execute (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    InputStream std = p.getInputStream();

    FileOutputStream fos = new FileOutputStream(outputFile);

    FileUtils.copy(std, fos);

    InputStream err = p.getInputStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err));

    String l2 = null;

    while ((l2 = errr.readLine()) != null)
      System.err.println(l2);

    fos.close();
    errr.close();

    logEndTime(p, cmd, startTime);
  }

  /**
   * Execute a command with the OS and return the output in a string.
   * @param cmd Command to execute
   * @return a string with the output the command
   * @throws IOException if an error occurs while running the process
   */
  public static String execToString(String cmd) throws IOException {

    logger.fine("execute (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    InputStream std = p.getInputStream();

    BufferedReader stdr = new BufferedReader(new InputStreamReader(std));

    StringBuffer sb = new StringBuffer();
    String l1 = null;

    while ((l1 = stdr.readLine()) != null) {
      sb.append(l1);
      sb.append('\n');
    }

    InputStream err = p.getInputStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err));

    String l2 = null;

    while ((l2 = errr.readLine()) != null)
      System.err.println(l2);

    stdr.close();
    errr.close();

    logEndTime(p, cmd, startTime);

    return sb.toString();
  }

  /**
   * Log the time of execution of a process.
   * @param p Process to log
   * @param cmd Command of the process
   * @param startTime Start time in ms
   * @throws IOException if an error occurs at the end of the process
   */
  private static final void logEndTime(final Process p, final String cmd,
      final long startTime) throws IOException {

    try {

      final int exitValue = p.waitFor();
      final long endTime = System.currentTimeMillis();

      if (exitValue == 1)
        throw new IOException("Error while executing: " + cmd);

      if (exitValue == 126)
        throw new IOException("Command invoked cannot execute: " + cmd);

      if (exitValue == 127)
        throw new IOException("Command not found: " + cmd);

      if (exitValue == 134)
        throw new IOException("Abort: " + cmd);

      if (exitValue == 139)
        throw new IOException("Segmentation fault: " + cmd);

      logger.fine("Done (Thread "
          + Thread.currentThread().getId() + ", exit code: " + exitValue
          + ") in " + (endTime - startTime) + " ms.");
    } catch (InterruptedException e) {

      logger.severe("Interrupted exception: " + e.getMessage());
    }

  }

  private ProcessUtils() {
  }

}
