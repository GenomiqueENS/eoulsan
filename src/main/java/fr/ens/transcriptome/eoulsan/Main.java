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


/**
 * This class is the main class. Check the environment, if Hadoop library is in
 * the classpath launch Hadoop main class else run local main class.
 * @author Laurent Jourdren
 */
public final class Main {

  /**
   * Main method of the program.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Set the default local for all the application
    Globals.setDefaultLocale();

    // Select the application execution mode
    final String eoulsanMode = System.getProperty(Globals.LAUNCH_MODE_PROPERTY);

    if (eoulsanMode != null && eoulsanMode.equals("local")) {
      MainCLI.main(args);
    } else {
      MainHadoop.main(args);
    }
  }

  //
  // Constructor
  //

  private Main() {

    throw new IllegalStateException();
  }

}
