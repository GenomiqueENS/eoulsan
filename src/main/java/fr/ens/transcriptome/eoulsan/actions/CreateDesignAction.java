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

package fr.ens.transcriptome.eoulsan.actions;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignBuilder;
import fr.ens.transcriptome.eoulsan.design.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;

/**
 * This class define an action to create design file.
 * @author Laurent Jourdren
 */
public class CreateDesignAction extends AbstractAction {

  @Override
  public String getName() {
    return "createdesign";
  }

  @Override
  public String getDescription() {
    return "create a design file from a list of files.";
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return true;
  }

  /**
   * Create soap index action.
   * @param args command line parameters for exec action
   */
  @Override
  public void action(final String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

    } catch (ParseException e) {
      Common.errorExit(e, "Error while parsing parameter file: "
          + e.getMessage());
    }

    Design design = null;

    try {

      final DesignBuilder db = new DesignBuilder(arguments);
      design = db.getDesign();

    } catch (EoulsanException e) {
      Common.errorExit(e, "Error: " + e.getMessage());
    }

    if (design.getSampleCount() == 0) {
      Common
          .showErrorMessageAndExit("Error: Nothing to create, no file found.\n"
              + "  Use the -h option to get more information.\n" + "usage: "
              + Globals.APP_NAME_LOWER_CASE + " createdesign files");

    }

    try {

      final File file = new File("design.txt");

      if (file.exists())
        throw new EoulsanIOException("Output design file "
            + file + " already exists");

      DesignWriter dw = new SimpleDesignWriter(file);

      dw.write(design);

    } catch (EoulsanIOException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    }

  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  private Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + getName() + " [options] file1 file2 ... fileN", options);

    Common.exit(0);
  }

}
