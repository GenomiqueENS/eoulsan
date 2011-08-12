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

package fr.ens.transcriptome.eoulsan.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.Globals;

/**
 * Utility class for launching external process.
 * @author Laurent Jourdren
 */
public final class ProcessUtils {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static Random random;

  public static class ProcessResult {

    private int exitValue;
    private String stdout;
    private String stderr;

    /**
     * Get the exit value of the process.
     * @return Returns the errorResult
     */
    public int getExitValue() {
      return exitValue;
    }

    /**
     * Get the standard output of the process
     * @return Returns the stdout
     */
    public String getStdout() {
      return stdout;
    }

    /**
     * Get the standard error of the process
     * @return Returns the stderr
     */
    public String getStderr() {
      return stderr;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param exitValue the exit value of the process
     * @param stdout the standard output of the process
     * @param stderr the standard error of the process
     */
    private ProcessResult(final int exitValue, final String stdout,
        final String stderr) {

      this.exitValue = exitValue;
      this.stdout = stdout;
      this.stderr = stderr;
    }

  }

  /**
   * Execute a command.
   * @param cmd command to execute
   * @return the exit error of the program
   * @throws IOException
   */
  public static int system(final String cmd) throws IOException {

    logger.fine("execute (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final Process p = Runtime.getRuntime().exec(cmd);

    try {
      return p.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e.getMessage());
    }
  }

  /**
   * Execute a command.
   * @param cmd command to execute
   * @return the exit error of the program
   * @throws IOException
   */
  public static int sh(final String cmd) throws IOException {

    File f = File.createTempFile("sh-", ".sh");
    UnSynchronizedBufferedWriter bw = FileUtils.createFastBufferedWriter(f);
    bw.write("#!/bin/sh\n");
    bw.write(cmd);
    bw.close();
    f.setExecutable(true);

    logger.fine("execute script (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final Process p = Runtime.getRuntime().exec(f.getAbsolutePath());

    try {
      final int result = p.waitFor();
      if (!f.delete())
        logger.warning("Can not remove sh script: " + f.getAbsolutePath());
      return result;
    } catch (InterruptedException e) {
      if (!f.delete())
        logger.warning("Can not remove sh script: " + f.getAbsolutePath());
      throw new IOException(e.getMessage());
    }
  }

  /**
   * Execute a command.
   * @param cmd command to execute
   * @return the exit error of the program
   * @throws IOException
   */
  public static ProcessResult shWithOutputs(final String cmd)
      throws IOException {

    File f = File.createTempFile("sh-", ".sh");
    UnSynchronizedBufferedWriter bw = FileUtils.createFastBufferedWriter(f);
    bw.write("#!/bin/sh\n");
    bw.write(cmd);
    bw.close();
    f.setExecutable(true);

    logger.fine("execute script (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final Process p = Runtime.getRuntime().exec(f.getAbsolutePath());

    final StringBuilder stdout = new StringBuilder();
    final StringBuilder stderr = new StringBuilder();

    try {

      final InputStream std = p.getInputStream();
      final BufferedReader stdr =
          new BufferedReader(new InputStreamReader(std));

      String stdoutLine = null;

      while ((stdoutLine = stdr.readLine()) != null) {
        stdout.append(stdoutLine);
        stdout.append('\n');
      }

      InputStream err = p.getInputStream();
      BufferedReader errr = new BufferedReader(new InputStreamReader(err));

      String stderrLine = null;

      while ((stderrLine = errr.readLine()) != null) {
        stderr.append(stderrLine);
        stderr.append('\n');
      }

      stdr.close();
      errr.close();

      final int exitValue = p.waitFor();

      if (!f.delete())
        logger.warning("Can not remove sh script: " + f.getAbsolutePath());

      // Return result
      return new ProcessResult(exitValue, stdout.toString(), stderr.toString());

    } catch (IOException e) {
      if (!f.delete())
        logger.warning("Can not remove sh script: " + f.getAbsolutePath());
      throw e;
    } catch (InterruptedException e) {
      if (!f.delete())
        logger.warning("Can not remove sh script: " + f.getAbsolutePath());
      throw new IOException(e.getMessage());
    }
  }

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

    final OutputStream fos = FileUtils.createOutputStream(outputFile);

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
  public static String execToString(final String cmd) throws IOException {

    return execToString(cmd, false, true);
  }

  /**
   * Execute a command with the OS and return the output in a string.
   * @param cmd Command to execute
   * @param addStdErr add the output of stderr in the result
   * @return a string with the output the command
   * @throws IOException if an error occurs while running the process
   */
  public static String execToString(final String cmd, final boolean addStdErr,
      final boolean checkExitCode) throws IOException {

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

    InputStream err = p.getErrorStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err));

    String l2 = null;

    while ((l2 = errr.readLine()) != null)
      if (addStdErr) {
        sb.append(l2);
        sb.append('\n');
      } else
        System.err.println(l2);

    stdr.close();
    errr.close();

    if (checkExitCode)
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

  /**
   * Return a set withs pid of existing executable.
   * @return a set of integers with pid of existing executable
   */
  public static Set<Integer> getExecutablePids(final String executableName) {

    if (executableName == null)
      return null;

    Set<Integer> result = new HashSet<Integer>();

    try {
      final String s =
          ProcessUtils.execToString("pgrep " + executableName.trim());
      if (s == null)
        return result;
      final String[] lines = s.split("\n");
      for (String line : lines)
        try {
          result.add(Integer.parseInt(line));
        } catch (NumberFormatException e) {
          continue;
        }

    } catch (IOException e) {
      return result;
    }

    return result;
  }

  /**
   * Wait the end of the execution of all the instance of an executable.
   * @param executableName name of the executable
   */
  public static void waitUntilExecutableRunning(final String executableName) {

    if (executableName == null)
      return;

    while (true) {

      final Set<Integer> pids = getExecutablePids(executableName);

      if (pids.size() == 0)
        return;

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
      }
    }

  }

  /**
   * Wait a random number of milliseconds.
   * @param maxMilliseconds the maximum number of milliseconds to wait
   */
  public static void waitRandom(final int maxMilliseconds) {

    if (maxMilliseconds <= 0)
      return;

    if (random == null)
      random = new Random(System.currentTimeMillis());

    try {
      Thread.sleep(random.nextInt(maxMilliseconds));
    } catch (InterruptedException e) {
    }

  }

  /**
   * This class allow to write on a PrintStream the content of a BuffeReader
   * @author Laurent Jourdren
   */
  private static final class ProcessThreadOutput implements Runnable {

    final BufferedReader reader;
    final PrintStream pw;

    @Override
    public void run() {

      String l = null;

      try {
        while ((l = this.reader.readLine()) != null) {
          this.pw.println(l);
        }
      } catch (IOException e) {

        e.printStackTrace();
      }
    }

    ProcessThreadOutput(final BufferedReader reader, final PrintStream pw) {
      this.reader = reader;
      this.pw = pw;
    }

  }

  /**
   * Execute a command and write the content of the standard output and error to
   * System.out and System.err.
   * @param cmd Command to execute
   * @throws IOException if an error occurs while executing the command
   */
  public static void execThreadOutput(final String cmd) throws IOException {

    logger.fine("execute (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    final BufferedReader stdr =
        new BufferedReader(new InputStreamReader(p.getInputStream()));
    final BufferedReader errr =
        new BufferedReader(new InputStreamReader(p.getErrorStream()));

    new Thread(new ProcessThreadOutput(stdr, System.out)).run();
    new Thread(new ProcessThreadOutput(errr, System.err)).run();

    logEndTime(p, cmd, startTime);
  }

  /**
   * Execute a command and write the content of the standard output and error to
   * System.out and System.err.
   * @param cmd array with the command to execute
   * @throws IOException if an error occurs while executing the command
   */
  public static void execThreadOutput(final String[] cmd) throws IOException {

    logger.fine("execute (Thread "
        + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    final BufferedReader stdr =
        new BufferedReader(new InputStreamReader(p.getInputStream()));
    final BufferedReader errr =
        new BufferedReader(new InputStreamReader(p.getErrorStream()));

    new Thread(new ProcessThreadOutput(stdr, System.out)).run();
    new Thread(new ProcessThreadOutput(errr, System.err)).run();

    logEndTime(p, Joiner.on(' ').join(cmd), startTime);
  }

  private ProcessUtils() {
  }

}
