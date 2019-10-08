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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.workflow.Executor;
import fr.ens.biologie.genomique.eoulsan.core.workflow.ExecutorArguments;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.TerminalModule;
import fr.ens.biologie.genomique.eoulsan.modules.mgmt.upload.LocalUploadModule;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define the Local Upload S3 Action.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class UploadS3Action extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "s3upload";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "upload data on Amazon S3.";
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return true;
  }

  @Override
  public void action(final List<String> arguments) {

    System.err.println("WARNING: the action \""
        + getName()
        + "\" is currently under development for the next version of "
        + Globals.APP_NAME + " and may actually not work.");

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }

    if (arguments.size() != argsOptions + 3) {
      help(options);
    }

    final File paramFile = new File(arguments.get(argsOptions));
    final File designFile = new File(arguments.get(argsOptions + 1));
    final DataFile s3Path = new DataFile(StringUtils
        .replacePrefix(arguments.get(argsOptions + 2), "s3:/", "s3n:/"));
    final String jobDescription = "Upload data to " + s3Path;

    // Upload data
    run(paramFile, designFile, s3Path, jobDescription);
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  private static Options makeOptions() {

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
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + ACTION_NAME
        + " [options] workflow.xml design.txt s3://mybucket/test", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode.
   * @param workflowFile workflow file
   * @param designFile design file
   * @param s3Path path of data on S3 file system
   * @param jobDescription job description
   */
  private static void run(final File workflowFile, final File designFile,
      final DataFile s3Path, final String jobDescription) {

    requireNonNull(workflowFile, "paramFile is null");
    requireNonNull(designFile, "designFile is null");
    requireNonNull(s3Path, "s3Path is null");

    getLogger().info("Parameter file: " + workflowFile);
    getLogger().info("Design file: " + designFile);

    try {

      // Test if worklflow file exists
      if (!workflowFile.exists()) {
        throw new FileNotFoundException(workflowFile.toString());
      }

      // Test if design file exists
      if (!designFile.exists()) {
        throw new FileNotFoundException(designFile.toString());
      }

      // Create ExecutionArgument object
      final ExecutorArguments arguments =
          new ExecutorArguments(workflowFile, designFile);
      arguments.setJobDescription(jobDescription);

      // Create the log Files
      Main.getInstance().createLogFiles(arguments.logPath(Globals.LOG_FILENAME),
          arguments.logPath(Globals.OTHER_LOG_FILENAME));

      // Create the executor
      final Executor e = new Executor(arguments);

      // Launch executor
      e.execute(Lists.newArrayList((Module) new LocalUploadModule(s3Path),
          new TerminalModule()), null);

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (Throwable e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
