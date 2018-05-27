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
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopJarRepackager;

/**
 * This class launch Eoulsan in hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class HadoopExecAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "hadoopexec";

  private static final String HADOOP_CLIENT_OPTS_ENV = "HADOOP_CLIENT_OPTS";

  @Override
  public String getName() {
    return ACTION_NAME;
  }

  @Override
  public String getDescription() {
    return "execute " + Globals.APP_NAME + " on local hadoop cluster.";
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String jobDescription = null;

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options,
          arguments.toArray(new String[arguments.size()]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      if (line.hasOption("d")) {

        jobDescription = line.getOptionValue("d");
        argsOptions += 2;
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
    final String hdfsPath = arguments.get(argsOptions + 2);

    // Execute program in hadoop mode
    run(paramFile, designFile, hdfsPath, jobDescription);
  }

  //
  // Command line parsing
  //

  /**
   * Create options for command line
   * @return an Options object
   */
  @SuppressWarnings("static-access")
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Description option
    options.addOption(OptionBuilder.withArgName("description").hasArg()
        .withDescription("job description").withLongOpt("desc").create('d'));

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
        + " [options] workflow.xml design.txt hdfs://server/path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Get the JVM arguments as a string.
   * @return a String with the JVM arguments
   */
  private static String getJVMArgs() {

    final List<String> result = new ArrayList<>();

    for (String a : Main.getInstance().getJVMArgs()) {

      if (a.startsWith("-Xm")) {
        result.add(a);
      }
    }

    final Main main = Main.getInstance();

    if (main.getEoulsanScriptPath() != null) {
      result
          .add("-D" + Main.EOULSAN_SCRIPT + "=" + main.getEoulsanScriptPath());
      result.add("-D" + Main.EOULSAN_PATH + "=" + main.getEoulsanDirectory());
    }

    if (main.getClassPath() != null) {
      result.add("-D"
          + Main.EOULSAN_CLASSPATH_JVM_ARG + "=" + main.getClassPath());
    }

    return Joiner.on(' ').join(result);
  }

  /**
   * Run Eoulsan in hadoop mode.
   * @param workflowFile workflow file
   * @param designFile design file
   * @param hdfsPath path of data on hadoop file system
   * @param jobDescription job description
   */
  private static void run(final File workflowFile, final File designFile,
      final String hdfsPath, final String jobDescription) {

    checkNotNull(workflowFile, "paramFile is null");
    checkNotNull(designFile, "designFile is null");
    checkNotNull(hdfsPath, "hdfsPath is null");

    // Write log entries
    Main.getInstance().flushLog();

    // Repackage application for Hadoop
    System.out.println("Package " + Globals.APP_NAME + " for hadoop mode...");
    final File repackagedJarFile;
    try {
      repackagedJarFile = HadoopJarRepackager.repack();

    } catch (IOException e) {
      Common.errorExit(e, "Error while repackaging "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());

      // Never called
      return;
    }

    getLogger().info("Launch Eoulsan in Hadoop mode.");

    // Create command line
    final List<String> argsList = new ArrayList<>();

    argsList.add("hadoop");
    argsList.add("jar");
    argsList.add(repackagedJarFile.getAbsolutePath());

    final Main main = Main.getInstance();

    if (main.getLogLevelArgument() != null) {
      argsList.add("-loglevel");
      argsList.add(main.getLogLevelArgument());
    }

    if (main.getConfigurationFileArgument() != null) {
      argsList.add("-conf");
      argsList.add(main.getConfigurationFileArgument());
    }

    for (String setting : main.getCommandLineSettings()) {
      argsList.add("-s");
      argsList.add(setting);
    }

    argsList.add(ExecJarHadoopAction.ACTION_NAME);

    if (jobDescription != null) {
      argsList.add("-d");
      argsList.add(jobDescription.trim());
    }

    argsList.add("-e");
    argsList.add("local hadoop cluster");
    argsList.add(workflowFile.toString());
    argsList.add(designFile.toString());
    argsList.add(hdfsPath);

    // execute Hadoop
    System.out.println("Launch " + Globals.APP_NAME + " in hadoop mode...");

    try {

      // Create the process builder the the command line
      final ProcessBuilder builder = new ProcessBuilder(argsList).inheritIO();

      // Set the JVM arguments for Hadoop in the process builder
      builder.environment().put(HADOOP_CLIENT_OPTS_ENV, getJVMArgs());

      // Execute the hadoop jar command
      final int exitCode = builder.start().waitFor();

      // Exit with the same exit of the child process
      System.exit(exitCode);

    } catch (IOException | InterruptedException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }
}
