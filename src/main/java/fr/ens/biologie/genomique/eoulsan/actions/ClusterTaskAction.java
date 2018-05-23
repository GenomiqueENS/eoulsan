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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.LocalEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskSerializationUtils;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class define a action to launch a task on a cluster.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterTaskAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "clustertask";

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  public String getDescription() {

    return "Execute a cluster task";
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options,
          arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.size() != argsOptions + 1) {
      help(options);
    }

    final DataFile contextFile = new DataFile(arguments.get(0));

    // Execute task
    run(contextFile);
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
        + ".sh " + getName() + " [options] stepcontext.context", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Execute the task.
   * @param taskContextFile context file
   */
  private static void run(final DataFile taskContextFile) {

    checkNotNull(taskContextFile, "contextFile is null");

    // Get Eoulsan runtime
    final LocalEoulsanRuntime localRuntime =
        (LocalEoulsanRuntime) EoulsanRuntime.getRuntime();

    // Set the cluster task mode
    localRuntime.setMode(EoulsanExecMode.CLUSTER_TASK);

    try {

      // Execute the task
      TaskSerializationUtils.execute(taskContextFile);

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      Common.errorExit(e, "IOException: " + e.getMessage());
    } catch (EoulsanRuntimeException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

}
