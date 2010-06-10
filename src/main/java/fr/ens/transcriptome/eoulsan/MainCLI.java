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

import fr.ens.transcriptome.eoulsan.programs.anadiff.local.AnaDiffLocalMain;
import fr.ens.transcriptome.eoulsan.programs.expression.local.ExpressionLocalMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.local.FilterReadsLocalMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.local.FilterSamplesLocalMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.local.SoapMapReadsLocalMain;
import fr.ens.transcriptome.eoulsan.programs.mgmt.local.CreateDesignLocalMain;
import fr.ens.transcriptome.eoulsan.programs.mgmt.local.CreateSoapIndexLocalMain;
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

    System.out.println(Globals.APP_NAME + " version " + Globals.APP_VERSION);

    System.out
        .println("usage: eoulsan.sh command [options] [arguments]\nAvailable commands:");

    System.out
        .println(" - "
            + CreateDesignLocalMain.PROGRAM_NAME
            + "          Create a design file");
    System.out.println(" - "
        + CreateSoapIndexLocalMain.PROGRAM_NAME + "       Create SOAP index");
    System.out.println(" - "
        + FilterReadsLocalMain.PROGRAM_NAME + "           Filter reads");
    System.out.println(" - "
        + SoapMapReadsLocalMain.PROGRAM_NAME
        + "            Map read on genome using SOAP");
    System.out.println(" - "
        + FilterSamplesLocalMain.PROGRAM_NAME
        + "         Filter samples with low reads alignment");
    System.out.println(" - "
        + ExpressionLocalMain.PROGRAM_NAME
        + "            Compute expression of genes");
    System.out.println(" - "
        + AnaDiffLocalMain.PROGRAM_NAME
        + "               Compute differential analysis");

    System.out.println();
    System.out.println(" - about                 About Eoulsan");
    System.out
        .println(" - version               Show information about the version");
    System.out
        .println(" - license               Show the license of the software");
    System.out.println(" - help                  This help");

    System.out
        .println("\n With eoulsan.sh command -help you can get help about each command.");

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

    if (args == null || args.length == 0)
      help();

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

    if (CreateDesignLocalMain.PROGRAM_NAME.equals(program))
      CreateDesignLocalMain.main(arguments);
    else if (CreateSoapIndexLocalMain.PROGRAM_NAME.equals(program))
      CreateSoapIndexLocalMain.main(arguments);
    else if (FilterReadsLocalMain.PROGRAM_NAME.equals(program))
      FilterReadsLocalMain.main(arguments);
    else if (SoapMapReadsLocalMain.PROGRAM_NAME.equals(program))
      SoapMapReadsLocalMain.main(arguments);
    else if (FilterSamplesLocalMain.PROGRAM_NAME.equals(program))
      FilterSamplesLocalMain.main(arguments);
    else if (ExpressionLocalMain.PROGRAM_NAME.equals(program))
      ExpressionLocalMain.main(arguments);
    else if (AnaDiffLocalMain.PROGRAM_NAME.equals(program))
      AnaDiffLocalMain.main(arguments);
    else
      System.err.println("Program not found: " + program);

  }

}
