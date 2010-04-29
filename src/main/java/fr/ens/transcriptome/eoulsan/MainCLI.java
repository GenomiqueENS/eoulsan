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

import java.util.Arrays;

import fr.ens.transcriptome.eoulsan.programs.filterreads.local.FilterReadsMain;
import fr.ens.transcriptome.eoulsan.programs.mgmt.local.CreateDesignMain;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in local mode.
 * @author Laurent Jourdren
 */
public class MainCLI {

  /**
   * Show version of the application.
   */
  public static void version() {

    System.out.println(Globals.APP_NAME
        + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
        + " on " + Globals.APP_BUILD_DATE + ")");
    System.exit(0);
  }

  /**
   * Show licence information about this application.
   */
  public static void about() {

    System.out.println(Globals.ABOUT_TXT);
    System.exit(0);
  }

  /**
   * Show information about this application.
   */
  public static void license() {

    System.out.println(Globals.LICENSE_TXT);
    System.exit(0);
  }

  /**
   * Show help information.
   */
  private static void help() {

    System.out.println("Help.");
    System.out.println("TODO...");
    System.exit(0);
  }

  private static boolean testOptions(String arg, String... options) {

    if (arg == null)
      return false;

    for (String option : options)
      if (arg.equals(option))
        return true;

    return false;
  }

  /**
   * Main method for the CLI mode.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    System.out.println("MainCLI arguments: " + Arrays.toString(args));

    if (args == null || args.length == 0)
      return;

    final String program = args[0];
    final String[] arguments = StringUtils.arrayWithoutFirstElement(args);

    final String lower = program.trim().toLowerCase();

    if (testOptions(lower, "about", "-about", "--about"))
      about();

    if (testOptions(lower, "-v", "version", "-version", "--version"))
      version();

    if (testOptions(lower, "license", "-license", "--license"))
      license();

    if (testOptions(lower, "-h", "help", "-help", "--help"))
      help();

    if (CreateDesignMain.PROGRAM_NAME.equals(program))
      CreateDesignMain.main(arguments);
    else if (FilterReadsMain.PROGRAM_NAME.equals(program))
      FilterReadsMain.main(arguments);
    else
      System.err.println("Program not found: " + program);

  }

}
