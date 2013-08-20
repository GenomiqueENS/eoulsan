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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
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

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.ExecutorArguments;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.TerminalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.CopyDesignAndWorkflowFilesToOutputStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataDownloadStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HadoopUploadStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

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
        final Path designPath, final Path paramPath) {
      super(millisSinceEpoch);

      // Set base pathname
      setHadoopWorkingPathname(designPath.getParent().toString());

      final Path logPath =
          new Path(designPath.getParent().toString() + "/" + getJobId());

      final Path outputPath =
          new Path(designPath.getParent().toString() + "/" + getJobId());

      // Set log pathname
      setLogPathname(logPath.toString());

      // Set output pathname
      setOutputPathname(outputPath.toString());

      // Set design file pathname
      setDesignPathname(designPath.toString());

      // Set workflow file pathname
      setWorkflowPathname(paramPath.toString());
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
    boolean uploadOnly = false;
    long millisSinceEpoch = System.currentTimeMillis();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line =
          parser.parse(options, arguments.toArray(new String[0]), true);

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

      if (line.hasOption("upload")) {

        uploadOnly = true;
        argsOptions += 1;
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
        jobEnvironment, uploadOnly, millisSinceEpoch);

  }

  /**
   * Convert a s3:// URL to a s3n:// URL
   * @param url input URL
   * @return converted URL
   */
  private static final String convertS3URL(final String url) {

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
  private static final Options makeOptions() {

    // create Options object
    final Options options = new Options();

    // Help option
    options.addOption("h", "help", false, "display this help");

    // Description option
    options.addOption(OptionBuilder.withArgName("description").hasArg()
        .withDescription("job description").withLongOpt("desc").create('d'));

    // Environment option
    options.addOption(OptionBuilder.withArgName("environment").hasArg()
        .withDescription("environment description").withLongOpt("desc")
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
  private static final void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("hadoop -jar "
        + Globals.APP_NAME_LOWER_CASE + ".jar  [options] " + ACTION_NAME
        + "workflow.xml design.txt hdfs://server/path", options);

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
   * @param uploadOnly true if execution must end after upload
   */
  private static final void run(final String workflowPathname,
      final String designPathname, final String destPathname,
      final String jobDescription, final String jobEnvironment,
      final boolean uploadOnly, final long millisSinceEpoch) {

    checkNotNull(workflowPathname, "paramPathname is null");
    checkNotNull(designPathname, "designPathname is null");
    checkNotNull(destPathname, "destPathname is null");

    final String desc;
    final String env;

    if (jobDescription == null) {
      desc = "no job description";
    } else {
      desc = jobDescription;
    }

    if (jobEnvironment == null) {
      env = "no enviromnent description";
    } else {
      env = jobEnvironment;
    }

    try {

      // Get the Hadoop configuration object
      final Configuration conf =
          ((HadoopEoulsanRuntime) EoulsanRuntime.getRuntime())
              .getConfiguration();

      // Define parameter URI
      final URI paramURI;
      if (workflowPathname.indexOf("://") != -1)
        paramURI = new URI(workflowPathname);
      else
        paramURI = new File(workflowPathname).getAbsoluteFile().toURI();

      // Define design URI
      final URI designURI;
      if (designPathname.indexOf("://") != -1)
        designURI = new URI(designPathname);
      else
        designURI = new File(designPathname).getAbsoluteFile().toURI();

      // Define destination URI
      final URI destURI = new URI(destPathname);

      final Path paramPath = new Path(paramURI.toString());
      final Path designPath = new Path(designURI.toString());

      // Test if param file exists
      FileSystem paramFs = paramPath.getFileSystem(conf);
      if (!paramFs.exists(paramPath))
        throw new FileNotFoundException(paramPath.toString());

      // Test if design file exists
      FileSystem designFs = designPath.getFileSystem(conf);
      if (!designFs.exists(designPath))
        throw new FileNotFoundException(designPath.toString());

      // Create ExecutionArgument object
      final ExecutorArguments arguments =
          new HadoopExecutorArguments(millisSinceEpoch, paramPath, designPath);
      arguments.setJobDescription(desc);
      arguments.setJobEnvironment(env);

      // Create the log File
      Main.getInstance().createLogFileAndFlushLog(
          arguments.getLogPathname() + File.separator + "eoulsan.log");

      // Create executor
      final Executor e = new Executor(arguments);

      // Create upload step
      final Step uploadStep =
          new HadoopUploadStep(new DataFile(destURI.toString()), conf);

      // Add init global logger Step
      // Add Copy design and workflow file Step
      // Add terminal step if upload only
      final List<Step> firstSteps;
      if (uploadOnly) {
        firstSteps = Arrays.asList(new Step[] {uploadStep, new TerminalStep(),

        new CopyDesignAndWorkflowFilesToOutputStep()});
      } else {
        firstSteps =
            Arrays.asList(uploadStep,
                new CopyDesignAndWorkflowFilesToOutputStep());
      }

      // Add download Step
      final List<Step> lastSteps =
          Collections.singletonList((Step) new HDFSDataDownloadStep());

      // Launch executor
      e.execute(firstSteps, lastSteps);

    } catch (FileNotFoundException e) {

      Common.errorExit(e, "File not found: " + e.getMessage());

    } catch (EoulsanException e) {

      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());

    } catch (EoulsanRuntimeException e) {

      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());

    } catch (IOException e) {

      Common.errorExit(e, "Error: " + e.getMessage());

    } catch (URISyntaxException e) {

      Common.errorExit(e, "Error: " + e.getMessage());
    }

  }

}
