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

package fr.ens.transcriptome.eoulsan;


/**
 * This class is the main class. Check the environment, if Hadoop library is in
 * the classpath launch Hadoop main class else run local main class.
 * @since 1.0
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
