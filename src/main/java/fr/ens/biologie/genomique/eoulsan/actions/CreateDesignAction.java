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

package fr.ens.biologie.genomique.eoulsan.actions;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignBuilder;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.io.DesignWriter;
import fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignWriter;
import fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

/**
 * This class define an action to create design file.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class CreateDesignAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "createdesign";

  private static final int DEFAULT_DESIGN_FORMAT = 2;

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "create a design file from a list of files.";
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return true;
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new DefaultParser();
    String filename = "design.txt";
    int argsOptions = 0;
    boolean pairedEndMode = false;
    final List<String> sampleSheetPaths = new ArrayList<>();
    String samplesProjectName = null;
    boolean symlinks = false;
    int formatVersion = DEFAULT_DESIGN_FORMAT;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments.toArray(new String[0]), true);

      // Pair-end option
      if (line.hasOption("paired-end")) {
        pairedEndMode = true;
        argsOptions += 1;
      }

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      // Output option
      if (line.hasOption("o")) {

        filename = line.getOptionValue("o");
        argsOptions += 2;
      }

      // Casava design option
      if (line.hasOption("s")) {

        String[] sampleSheets = line.getOptionValues("s");
        sampleSheetPaths.addAll(Arrays.asList(sampleSheets));
        argsOptions += sampleSheets.length * 2;
      }

      // Casava project option
      if (line.hasOption("n")) {

        samplesProjectName = line.getOptionValue("n");
        argsOptions += 2;
      }

      // Symbolic links option
      if (line.hasOption("symlinks")) {

        symlinks = true;
        argsOptions++;
      }

      // Eoulsan design format version option
      if (line.hasOption("f")) {

        try {
          formatVersion = Integer.parseInt(line.getOptionValue("f").trim());
        } catch (NumberFormatException e) {
          Common.errorExit(e, "Invalid Eoulsan design format version: " + e.getMessage());
        }
        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e, "Error while parsing command line arguments: " + e.getMessage());
    }

    // Write log entries
    Main.getInstance().flushLog();

    Design design = null;
    final DataFile designFile = new DataFile(filename);

    try {

      final List<String> newArgs = arguments.subList(argsOptions, arguments.size());

      final DesignBuilder db = new DesignBuilder();

      // Add all the files of a Casava design if Casava design path is defined
      for (String sampleSheetPath : sampleSheetPaths) {
        db.addBcl2FastqSamplesheetProject(Path.of(sampleSheetPath), samplesProjectName);
      }

      // Add files in the command line
      db.addFiles(newArgs);

      design = db.getDesign(pairedEndMode);

      if (symlinks) {
        DesignUtils.replaceLocalPathBySymlinks(design, designFile.getParent());
      }

    } catch (EoulsanException | IOException e) {
      Common.errorExit(e, "Error: " + e.getMessage());
    }

    if (design.getSamples().isEmpty()) {
      Common.showErrorMessageAndExit(
          "Error: Nothing to create, no file found.\n"
              + "  Use the -h option to get more information.\n"
              + "usage: "
              + Globals.APP_NAME_LOWER_CASE
              + " createdesign files");
    }

    try {

      if (designFile.exists()) {
        throw new IOException("Output design file " + designFile + " already exists");
      }

      final DesignWriter dw;

      switch (formatVersion) {
        case 1:
          dw = new Eoulsan1DesignWriter(designFile.create());
          break;

        case 2:
          dw = new Eoulsan2DesignWriter(designFile.create());
          break;

        default:
          Common.showErrorMessageAndExit("Unknown Eoulsan design format version: " + formatVersion);
          return;
      }

      dw.write(design);

    } catch (IOException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    }
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   *
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Pair end mode
    options.addOption("p", "paired-end", false, "Paired-end mode");

    // Help option
    options.addOption("h", "help", false, "Display this help");

    // Bcl2fastq samplesheet path option
    options.addOption(
        Option.builder("s")
            .argName("file")
            .hasArg()
            .desc("Illumina samplesheet file")
            .longOpt("samplesheet")
            .get());

    // Bcl2fastq project option
    options.addOption(
        Option.builder("n")
            .argName("name")
            .hasArg()
            .desc("Illumina project name")
            .longOpt("project-name")
            .get());

    // Create symbolic links
    options.addOption("l", "symlinks", false, "Create symbolic links in design file directory");

    // Output option
    options.addOption(
        Option.builder("o").argName("file").hasArg().desc("Output file").longOpt("output").get());

    // Eoulsan design format version
    options.addOption(
        Option.builder("f")
            .argName("version")
            .hasArg()
            .desc("Eoulsan design format version")
            .longOpt("format-version")
            .get());

    return options;
  }

  /**
   * Show command line help.
   *
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = HelpFormatter.builder().setShowSince(false).get();
    try {
      formatter.printHelp(
          Globals.APP_NAME_LOWER_CASE + ".sh " + ACTION_NAME + " [options] file1 file2 ... fileN",
          "",
          options,
          "",
          false);
    } catch (IOException e) {
      Common.errorExit(e, "Error while creating help message.");
    }

    Common.exit(0);
  }
}
