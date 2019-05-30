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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class define some utility methods for the underlying operating system.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class SystemUtils {

  private static final String HADOOP_CLASS_TO_TEST =
      "org.apache.hadoop.io.Text";

  /**
   * Get the name of the host.
   * @return The name of the host
   */
  public static String getHostName() {

    try {
      InetAddress addr = InetAddress.getLocalHost();

      // Get hostname
      return addr.getHostName();
    } catch (UnknownHostException e) {

      return null;
    }
  }

  /**
   * Get IP of the host
   * @return The IP of the host in textual form
   */
  public static String getIPAddr() {

    try {
      InetAddress addr = InetAddress.getLocalHost();

      // Get IP
      return addr.getHostAddress();
    } catch (UnknownHostException e) {

      return null;
    }
  }

  /**
   * Test if the system is Mac OS X.
   * @return true if the system is Mac OS X
   */
  public static boolean isMacOsX() {
    return System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  }

  /**
   * Test if the system is Unix.
   * @return true if the operating system is Linux.
   */
  public static boolean isLinux() {

    return System.getProperty("os.name").toLowerCase().startsWith("linux");
  }

  /**
   * Test if the system is Windows.
   * @return true if the operating system is Windows
   */
  public static boolean isWindows() {

    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }

  /**
   * Test if the system is an *nix.
   * @return true if the operating system is an *nix
   */
  public static boolean isUnix() {

    return !isWindows();
  }

  /**
   * Test if a class is present is the classpath
   * @param className the class to test
   * @return true if the class is present in the classpath
   */
  public static boolean isClass(final String className) {

    if (className == null) {
      return false;
    }

    try {
      SystemUtils.class.getClassLoader().loadClass(className);

      return true;

    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Test if Eoulsan is in Hadoop mode
   * @return true if Eoulsan is in Hadoop mode
   */
  public static boolean isHadoop() {

    return isClass(HADOOP_CLASS_TO_TEST);
  }

  /**
   * Check if the application is available for current platform.
   * @return true if the application is available for current platform
   */
  public static boolean isApplicationAvailableForCurrentArch() {

    final String os = System.getProperty("os.name").toLowerCase();
    final String arch = System.getProperty("os.arch").toLowerCase();

    return Globals.AVAILABLE_BINARY_ARCH.contains(os + "\t" + arch);
  }

  /**
   * Get the Java version.
   * @return the java version
   */
  public static int getJavaVersion() {

    final String version = System.getProperty("java.version");

    final int pos1 = version.indexOf('.') + 1;

    // Java >= 9
    int major = Integer.parseInt(version.substring(0, pos1 - 1));
    if (major > 1) {
      return major;
    }

    // Java < 9
    final int pos2 = version.indexOf('.', pos1);
    return Integer.parseInt(version.substring(pos1, pos2));
  }

  /**
   * Get user UID.
   * @return the user UID or -1 if UID cannot be found
   */
  public static int uid() {

    try {
      return Integer
          .parseInt(execToString("/usr/bin/id", "-u").replace("\n", "").trim());
    } catch (NumberFormatException | IOException e) {
      return -1;
    }
  }

  /**
   * Get user GID.
   * @return the user GID or -1 if GID cannot be found
   */
  public static int gid() {

    try {
      return Integer
          .parseInt(execToString("/usr/bin/id", "-g").replace("\n", "").trim());
    } catch (NumberFormatException | IOException e) {
      return -1;
    }
  }

  //
  // Private methods
  //

  /**
   * Execute a system command and return the output.
   * @param args the argument of the command line
   * @return a String with the stdout of the command
   * @throws IOException if an error occurs while executing the command
   */
  private static String execToString(final String... args) throws IOException {

    ProcessBuilder pb = new ProcessBuilder(args);
    Process p = pb.start();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(p.getInputStream()))) {

      final StringBuilder result = new StringBuilder();
      String line = null;

      while ((line = reader.readLine()) != null) {
        result.append(line);
        result.append('\n');
      }

      reader.close();

      return result.toString();
    }
  }

  /**
   * Search an executable in the system PATH.
   * @param executableName the name of the interpreter
   * @return a File with the interpreter path or null if the executable path has
   *         not been defined in the system PATH
   */
  public static File searchExecutableInPATH(final String executableName) {

    if (executableName == null) {
      throw new NullPointerException("the executableName cannot be null");
    }

    final String pathEnv = System.getenv().get("PATH");

    if (pathEnv == null) {
      return null;
    }

    for (String dirname : Splitter.on(File.pathSeparatorChar).split(pathEnv)) {

      final File dir = new File(dirname);

      final File file = new File(dir, executableName);

      if (dir.isDirectory() && file.exists() && file.canExecute()) {
        return file;
      }

    }

    return null;
  }

  //
  // Private constructor
  //

  private SystemUtils() {
  }

}
