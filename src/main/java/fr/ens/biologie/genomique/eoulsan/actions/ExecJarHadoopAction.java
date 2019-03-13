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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.core.workflow.Executor;
import fr.ens.biologie.genomique.eoulsan.core.workflow.ExecutorArguments;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define an action that allow to execute a jar on an Hadoop cluster.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ExecJarHadoopAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "execjarhadoop";

  private static class HadoopExecutorArguments extends ExecutorArguments {

    public HadoopExecutorArguments(final long millisSinceEpoch,
        final Path paramPath, final Path designPath, final Path destPath) {
      super(millisSinceEpoch);

      final Path outputPath = designPath.getParent();

      final Path jobPath = new Path(outputPath, getJobId());
      final Path workingPath = new Path(jobPath, "working");
      final Path taskPath = new Path(jobPath, "tasks");
      final Path tmpDir = new Path(jobPath, "tmp");
      final Path dataPath =
          new Path(outputPath, Globals.APP_NAME_LOWER_CASE + "-data");

      // Set log pathname
      setJobPathname(jobPath.toString());

      // Set output pathname
      setOutputPathname(outputPath.toString());

      // Set design file pathname
      setDesignPathname(designPath.toString());

      // Set workflow file pathname
      setWorkflowPathname(paramPath.toString());

      // Set the output path
      setLocalWorkingPathname(workingPath.toString());

      // Set the tasks path
      setTaskPathname(taskPath.toString());

      // Set Hadoop working pathname
      setHadoopWorkingPathname(destPath.toString());

      // Set the data path
      setDataPathname(dataPath.toString());

      // Set the temporary directory
      setTemporaryPathname(tmpDir.toString());
    }

  }

  @Override
  public String getName() {

    return ACTION_NAME;
  }

  @Override
  public String getDescription() {

    return "Execute " + Globals.APP_NAME + " in Hadoop jar mode ";
  }

  @Override
  public boolean isHadoopJarMode() {

    return true;
  }

  @Override
  public void action(final List<String> arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String jobDescription = null;
    String jobEnvironment = null;
    long millisSinceEpoch = System.currentTimeMillis();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options,
          arguments.toArray(new String[0]), true);

      // Help option
      if (line.hasOption("help")) {
        help(options);
      }

      if (line.hasOption("d")) {

        jobDescription = line.getOptionValue("d");
        argsOptions += 2;
      }

      if (line.hasOption("e")) {

        jobEnvironment = line.getOptionValue("e");
        argsOptions += 2;
      }

      if (line.hasOption("p")) {

        try {
          millisSinceEpoch = Long.parseLong(line.getOptionValue("p").trim());
        } catch (NumberFormatException e) {
        }
        argsOptions += 2;
      }

    } catch (ParseException e) {
      Common.errorExit(e,
          "Error while parsing command line arguments: " + e.getMessage());
    }

    if (arguments.size() != argsOptions + 3) {
      help(options);
    }

    // Get the command line arguments
    final String paramPathname = convertS3URL(arguments.get(argsOptions));
    final String designPathname = convertS3URL(arguments.get(argsOptions + 1));
    final String destPathname = convertS3URL(arguments.get(argsOptions + 2));

    // Execute program in hadoop mode
    run(paramPathname, designPathname, destPathname, jobDescription,
        jobEnvironment, millisSinceEpoch);

  }

  /**
   * Convert a s3:// URL to a s3n:// URL
   * @param url input URL
   * @return converted URL
   */
  private static String convertS3URL(final String url) {

    return StringUtils.replacePrefix(url, "s3:/", "s3n:/");
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
        .withDescription("job description").withLongOpt("jobdesc").create('d'));

    // Environment option
    options.addOption(OptionBuilder.withArgName("environment").hasArg()
        .withDescription("environment description").withLongOpt("envdesc")
        .create('e'));

    // UploadOnly option
    options.addOption("upload", false, "upload only");

    // Parent job creation time
    options.addOption(OptionBuilder.withArgName("parent-job-time").hasArg()
        .withDescription("parent job time").withLongOpt("parent-job")
        .create('p'));

    return options;
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("hadoop -jar "
        + Globals.APP_NAME_LOWER_CASE + ".jar  [options] " + ACTION_NAME
        + " workflow.xml design.txt hdfs://server/path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode
   * @param workflowPathname workflow file path
   * @param designPathname design file path
   * @param destPathname data path
   * @param jobDescription job description
   * @param jobEnvironment job environment
   * @param millisSinceEpoch milliseconds since epoch
   */
  private static void run(final String workflowPathname,
      final String designPathname, final String destPathname,
      final String jobDescription, final String jobEnvironment,
      final long millisSinceEpoch) {

    requireNonNull(workflowPathname, "workflowPathname is null");
    requireNonNull(designPathname, "designPathname is null");
    requireNonNull(destPathname, "destPathname is null");

    final String desc;
    final String env;

    if (jobDescription == null) {
      desc = "no job description";
    } else {
      desc = jobDescription;
    }

    if (jobEnvironment == null) {
      env = "no environment description";
    } else {
      env = jobEnvironment;
    }

    try {

      // Get the Hadoop configuration object
      final Configuration conf =
          ((HadoopEoulsanRuntime) EoulsanRuntime.getRuntime())
              .getConfiguration();

      // Define parameter URI
      final URI workflowURI;
      if (workflowPathname.contains("://")) {
        workflowURI = new URI(workflowPathname);
      } else {
        workflowURI = new File(workflowPathname).getAbsoluteFile().toURI();
      }

      // Define design URI
      final URI designURI;
      if (designPathname.contains("://")) {
        designURI = new URI(designPathname);
      } else {
        designURI = new File(designPathname).getAbsoluteFile().toURI();
      }

      // Define destination URI
      final URI destURI = new URI(destPathname);

      final Path workflowPath = new Path(workflowURI.toString());
      final Path designPath = new Path(designURI.toString());
      final Path destPath = new Path(destURI.toString());

      // Test if workflow file exists
      FileSystem workflowFs = workflowPath.getFileSystem(conf);
      if (!workflowFs.exists(workflowPath)) {
        throw new FileNotFoundException(workflowPath.toString());
      }

      // Test if design file exists
      FileSystem designFs = designPath.getFileSystem(conf);
      if (!designFs.exists(designPath)) {
        throw new FileNotFoundException(designPath.toString());
      }

      // Create ExecutionArgument object
      final ExecutorArguments arguments = new HadoopExecutorArguments(
          millisSinceEpoch, workflowPath, designPath, destPath);
      arguments.setJobDescription(desc);
      arguments.setJobEnvironment(env);

      // Create the log Files
      Main.getInstance().createLogFiles(arguments.logPath(Globals.LOG_FILENAME),
          arguments.logPath(Globals.OTHER_LOG_FILENAME));

      // Create executor
      final Executor e = new Executor(arguments);

      // Launch executor
      e.execute();

    } catch (FileNotFoundException e) {

      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (IOException | URISyntaxException e) {

      Common.errorExit(e, "Error: " + e.getMessage());
    } catch (Throwable e) {

      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
