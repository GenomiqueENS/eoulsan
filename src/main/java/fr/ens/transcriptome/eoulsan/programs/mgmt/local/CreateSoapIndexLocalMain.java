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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.MainCLI;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;

/**
 * Main class for creating soap index.
 * @author Laurent Jourdren
 */
public class CreateSoapIndexLocalMain {

  public static String PROGRAM_NAME = "createsoapindex";

  private static void makeIndex(final String designFilename)
      throws EoulsanIOException, IOException {

    DesignReader dr = new SimpleDesignReader(designFilename);
    makeIndex(dr.read());
  }

  private static void makeIndex(final Design design) throws IOException {

    if (design == null)
      return;

    final Map<String, Integer> genomesIndex = new HashMap<String, Integer>();
    int count = 0;

    for (Sample sample : design.getSamples()) {

      final String genomeFilename = sample.getMetadata().getGenome();
      if (genomeFilename == null)
        return;

      final String genomeFilenameTrimed = genomeFilename.trim();

      if (!genomesIndex.containsKey(genomeFilenameTrimed)) {

        count++;

        SOAPWrapper.makeIndex(new File(genomeFilenameTrimed), new File(
            Common.GENOME_SOAP_INDEX_DIR_PREFIX + count));

        genomesIndex.put(genomeFilenameTrimed, count);
      }

    }

  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " [options] design [genome [genome_masked [output_dir]]]", options);

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

    if (args == null || args.length != argsOptions + 1) {

      System.err.println("Invalid number of arguments.");
      System.exit(1);
    }

    final String designFilename = args[argsOptions];
    try {
      makeIndex(designFilename);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
