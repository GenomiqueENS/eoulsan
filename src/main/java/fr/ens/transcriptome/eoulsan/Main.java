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

package fr.ens.transcriptome.eoulsan;

import fr.ens.transcriptome.eoulsan.util.SystemUtils;

/**
 * This class is the main class. Check the environment, if Hadoop library is in
 * the classpath launch Hadoop main class else run local main class.
 * @author Laurent Jourdren
 */
public class Main {

  /**
   * Get in a string with all arch
   * @return a string with
   */
  public static String availableArchsToString() {

    final StringBuilder sb = new StringBuilder();

    boolean first = true;

    for (String osArch : Globals.AVAILABLE_BINARY_ARCH) {
      if (first)
        first = false;
      else
        sb.append(", ");
      sb.append(osArch.replace('\t', '/'));
    }

    return sb.toString();
  }

  /**
   * Main method of the program.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Test if the application can run with current platform
    if (!SystemUtils.isApplicationAvailableForCurrentArch())
      Common.showErrorMessageAndExit(Globals.WELCOME_MSG
          + "\n" + Globals.APP_NAME
          + " is not available for your platform. Required platforms: "
          + availableArchsToString() + ".");

    // Set the default local for all the application
    Globals.setDefaultLocale();

    // Select the application execution mode
    if (SystemUtils.isHadoop())
      MainHadoop.main(args);
    else
      MainCLI.main(args);
  }

}
