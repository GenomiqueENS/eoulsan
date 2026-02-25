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

package fr.ens.biologie.genomique.eoulsan.util;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.kenetre.util.Utils.silentSleep;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import fr.ens.biologie.genomique.eoulsan.io.FileCharsets;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Utility class for launching external process.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class ProcessUtils {

  /* Default Charset. */
  private static final Charset CHARSET = Charset.forName(System.getProperty("file.encoding"));

  private static Random random;

  /**
   * Execute a command.
   *
   * @param cmd command to execute
   * @return the exit error of the program
   * @throws IOException if an error occurs while executing the command
   */
  public static int system(final String cmd) throws IOException {

    getLogger().fine("execute (Thread " + Thread.currentThread().getId() + "): " + cmd);

    final Process p = Runtime.getRuntime().exec(cmd);

    try {
      return p.waitFor();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  /**
   * Execute a command.
   *
   * @param cmd command to execute
   * @return the exit error of the program
   * @throws IOException if an error occurs while executing the command
   */
  public static int sh(final List<String> cmd) throws IOException {

    return sh(cmd, null);
  }

  /**
   * Execute a command.
   *
   * @param cmd command to execute
   * @param temporaryDirectory temporary where create the temporary shell file
   * @return the exit error of the program
   * @throws IOException if an error occurs while executing the command
   */
  public static int sh(final List<String> cmd, final File temporaryDirectory) throws IOException {

    ProcessBuilder pb;
    Process p = null;
    int exitValue = Integer.MAX_VALUE;

    try {
      pb = new ProcessBuilder(cmd);

      if (!(temporaryDirectory == null)) {
        pb.directory(temporaryDirectory);
      }

      getLogger().fine("Execute command: " + cmd.toString());

      p = pb.start();

      final Thread terr = new Thread(new ProcessThreadErrOutput(p.getErrorStream()));
      terr.start();

      terr.join();
      exitValue = p.waitFor();
      getLogger().fine("Command exit value: " + exitValue);

    } catch (InterruptedException e) {
      getLogger().warning("Process interrupted : " + e.getMessage());
    }
    return exitValue;
  }

  /**
   * Execute a command with the OS.
   *
   * @param cmd Command to execute
   * @param stdOutput don't show the result of the command on the standard output
   * @throws IOException if an error occurs while running the process
   */
  public static void exec(final String cmd, final boolean stdOutput) throws IOException {

    getLogger().fine("execute (Thread " + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    InputStream std = p.getInputStream();
    BufferedReader stdr = new BufferedReader(new InputStreamReader(std, CHARSET));

    String l = null;

    while ((l = stdr.readLine()) != null) {
      if (stdOutput) {
        System.out.println(l);
      }
    }

    InputStream err = p.getInputStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err, CHARSET));

    String l2 = null;

    while ((l2 = errr.readLine()) != null) {
      System.err.println(l2);
    }

    stdr.close();
    errr.close();

    logEndTime(p, cmd, startTime);
  }

  /**
   * Execute a command with the OS and save the output in file.
   *
   * @param cmd Command to execute
   * @param outputFile The output file
   * @throws IOException if an error occurs while running the process
   */
  public static void execWriteOutput(final String cmd, final File outputFile) throws IOException {

    getLogger().fine("execute (Thread " + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    InputStream std = p.getInputStream();

    final OutputStream fos = FileUtils.createOutputStream(outputFile);

    FileUtils.copy(std, fos);

    InputStream err = p.getInputStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err, CHARSET));

    String l2 = null;

    while ((l2 = errr.readLine()) != null) {
      System.err.println(l2);
    }

    fos.close();
    errr.close();

    logEndTime(p, cmd, startTime);
  }

  /**
   * Execute a command with the OS and return the output in a string.
   *
   * @param cmd Command to execute
   * @return a string with the output the command
   * @throws IOException if an error occurs while running the process
   */
  public static String execToString(final String cmd) throws IOException {

    return execToString(cmd, false, true);
  }

  /**
   * Execute a command with the OS and return the output in a string.
   *
   * @param cmd Command to execute
   * @param addStdErr add the output of stderr in the result
   * @param checkExitCode if true exit code will be checked
   * @return a string with the output the command
   * @throws IOException if an error occurs while running the process
   */
  public static String execToString(
      final String cmd, final boolean addStdErr, final boolean checkExitCode) throws IOException {

    getLogger().fine("execute (Thread " + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    final Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});

    final InputStream std = p.getInputStream();

    final BufferedReader stdr = new BufferedReader(new InputStreamReader(std, CHARSET));

    final StringBuilder sb = new StringBuilder();
    String l1 = null;

    while ((l1 = stdr.readLine()) != null) {
      sb.append(l1);
      sb.append('\n');
    }

    InputStream err = p.getErrorStream();
    BufferedReader errr = new BufferedReader(new InputStreamReader(err, CHARSET));

    String l2 = null;

    while ((l2 = errr.readLine()) != null) {
      if (addStdErr) {
        sb.append(l2);
        sb.append('\n');
      } else {
        System.err.println(l2);
      }
    }

    stdr.close();
    errr.close();

    if (checkExitCode) {
      logEndTime(p, cmd, startTime);
    }

    return sb.toString();
  }

  /**
   * Log the time of execution of a process.
   *
   * @param p Process to log
   * @param cmd Command of the process
   * @param startTime Start time in ms
   * @throws IOException if an error occurs at the end of the process
   */
  public static void logEndTime(final Process p, final String cmd, final long startTime)
      throws IOException {

    try {

      final int exitValue = p.waitFor();
      final long endTime = System.currentTimeMillis();

      throwExitCodeException(exitValue, cmd);

      getLogger()
          .fine(
              "Done (Thread "
                  + Thread.currentThread().getId()
                  + ", exit code: "
                  + exitValue
                  + ") in "
                  + (endTime - startTime)
                  + " ms.");
    } catch (InterruptedException e) {

      getLogger().severe("Interrupted exception: " + e.getMessage());
    }
  }

  /**
   * Throw an IOException if the exit code of a process is not equals to 0.
   *
   * @param exitCode the exit code
   * @param command the executed command
   * @throws IOException if the exit code if not 0
   */
  public static void throwExitCodeException(final int exitCode, final String command)
      throws IOException {

    switch (exitCode) {
      case 0:
        return;

      case 126:
        throw new IOException("Command invoked cannot execute: " + command);

      case 127:
        throw new IOException("Command not found: " + command);

      case 134:
        throw new IOException("Abort: " + command);

      case 139:
        throw new IOException("Segmentation fault: " + command);

      default:
        throw new IOException("Error while executing (exit code " + exitCode + "): " + command);
    }
  }

  /**
   * Return a set withs pid of existing executable.
   *
   * @param executableName executable name
   * @return a set of integers with pid of existing executable
   */
  public static Set<Integer> getExecutablePids(final String executableName) {

    if (executableName == null) {
      return null;
    }

    Set<Integer> result = new HashSet<>();

    try {
      final String s = ProcessUtils.execToString("pgrep -x " + executableName.trim());

      for (String line : Splitter.on('\n').split(s)) {
        try {
          result.add(Integer.parseInt(line));
        } catch (NumberFormatException e) {
          // Do nothing
        }
      }

    } catch (IOException e) {
      return result;
    }

    return result;
  }

  /**
   * Wait the end of the execution of all the instance of an executable.
   *
   * @param executableName name of the executable
   */
  public static void waitUntilExecutableRunning(final String executableName) {

    if (executableName == null) {
      return;
    }

    while (true) {

      final Set<Integer> pids = getExecutablePids(executableName);

      if (pids.size() == 0) {
        return;
      }

      silentSleep(5000);
    }
  }

  /**
   * Wait a random number of milliseconds.
   *
   * @param maxMilliseconds the maximum number of milliseconds to wait
   */
  public static void waitRandom(final int maxMilliseconds) {

    if (maxMilliseconds <= 0) {
      return;
    }

    if (random == null) {
      random = new Random(System.currentTimeMillis());
    }

    silentSleep(random.nextInt(maxMilliseconds));
  }

  /** This class allow to fetch standard error of the process, without printing */
  public static final class ProcessThreadErrOutput implements Runnable {
    final InputStream err;
    public String exceptionMessage;
    BufferedReader buff;

    @Override
    public void run() {
      try {
        while (new BufferedReader(new InputStreamReader(this.err, FileCharsets.LATIN1_CHARSET))
                .readLine()
            != null) {}

        this.err.close();
      } catch (IOException e) {
        this.exceptionMessage = e.getMessage();
      }
    }

    public ProcessThreadErrOutput(final InputStream err) {
      this.err = err;
    }
  } // ProcessThreadErrOutput

  /**
   * This class allow to write on a PrintStream the content of a BufferedReader
   *
   * @author Laurent Jourdren
   */
  @SuppressWarnings("CatchAndPrintStackTrace")
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
        // Do not handle interruption exception
        e.printStackTrace();
      }
    }

    ProcessThreadOutput(final BufferedReader reader, final PrintStream pw) {
      this.reader = reader;
      this.pw = pw;
    }
  }

  /**
   * Execute a command and write the content of the standard output and error to System.out and
   * System.err.
   *
   * @param cmd Command to execute
   * @throws IOException if an error occurs while executing the command
   */
  public static void execThreadOutput(final String cmd) throws IOException {

    getLogger().fine("execute (Thread " + Thread.currentThread().getId() + "): " + cmd);

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    final BufferedReader stdr =
        new BufferedReader(new InputStreamReader(p.getInputStream(), CHARSET));
    final BufferedReader errr =
        new BufferedReader(new InputStreamReader(p.getErrorStream(), CHARSET));

    new Thread(new ProcessThreadOutput(stdr, System.out)).start();
    new Thread(new ProcessThreadOutput(errr, System.err)).start();

    logEndTime(p, cmd, startTime);
  }

  /**
   * Execute a command and write the content of the standard output and error to System.out and
   * System.err.
   *
   * @param cmd array with the command to execute
   * @throws IOException if an error occurs while executing the command
   */
  public static void execThreadOutput(final String[] cmd) throws IOException {

    getLogger()
        .fine("execute (Thread " + Thread.currentThread().getId() + "): " + Arrays.toString(cmd));

    final long startTime = System.currentTimeMillis();

    Process p = Runtime.getRuntime().exec(cmd);

    final BufferedReader stdr =
        new BufferedReader(new InputStreamReader(p.getInputStream(), CHARSET));
    final BufferedReader errr =
        new BufferedReader(new InputStreamReader(p.getErrorStream(), CHARSET));

    new Thread(new ProcessThreadOutput(stdr, System.out)).start();
    new Thread(new ProcessThreadOutput(errr, System.err)).start();

    logEndTime(p, Joiner.on(' ').join(cmd), startTime);
  }

  private ProcessUtils() {}
}
