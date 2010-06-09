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

package fr.ens.transcriptome.eoulsan.programs.mgmt.local;

import java.io.FileNotFoundException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.MainCLI;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.programs.mgmt.DesignBuilder;

/**
 * Main class for Create Design program.
 * @author Laurent Jourdren
 */
public class CreateDesignLocalMain {

  public static String PROGRAM_NAME = "createdesign";

  private static boolean stdout;
  private static String outputFilename;

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " " + PROGRAM_NAME + " [options] files", options);

    System.exit(0);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    options.addOption("version", false, "show version of the software");
    options
        .addOption("about", false, "display information about this software");
    options.addOption("h", "help", false, "display this help");
    options.addOption("license", false,
        "display information about the license of this software");

    options.addOption("stdout", false,
        "display use stdout as output rather than a file");

    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("output file").create("output"));

    return options;
  }

  /**
   * Parse the options of the command line
   * @param args command line arguments
   * @return the number of optional arguments
   */
  private static int parseCommandLine(final String args[]) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("help"))
        help(options);

      if (line.hasOption("about"))
        MainCLI.about();

      if (line.hasOption("version"))
        MainCLI.version();

      if (line.hasOption("license"))
        MainCLI.license();

      if (line.hasOption("stdout"))
        stdout = true;

      if (line.hasOption("output")) {
        outputFilename = line.getOptionValue("output");
        argsOptions += 2;
      }

    } catch (ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return argsOptions;
  }

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    if (args == null || args.length <= argsOptions) {

      System.err
          .println("Invalid number of arguments. Use the -h option to get more information.");

      System.exit(1);
    }

    DesignBuilder db = new DesignBuilder(args);

    Design design = db.getDesign();

    if (design.getSampleCount() == 0) {
      System.err.println("Error: Nothing to create, no file found.  Use the -h option to get more information.");
      System.err.println("usage: "
          + Globals.APP_NAME_LOWER_CASE + " " + PROGRAM_NAME
          + " [options] files");
      System.exit(1);
    }

    try {
      DesignWriter dw;

      if (!stdout)
        dw =
            new SimpleDesignWriter(outputFilename == null
                ? "design.txt" : outputFilename);
      else
        dw = new SimpleDesignWriter(System.out);
      dw.write(design);

    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      System.exit(1);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }

  }

}
