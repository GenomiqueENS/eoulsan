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

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class SystemUtils {

  private static String HADOOP_CLASS_TO_TEST = "org.apache.hadoop.io.Text";
  
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

    if (className == null)
      return false;

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

  //
  // Private constructor
  //
  private SystemUtils() {
  }
}
