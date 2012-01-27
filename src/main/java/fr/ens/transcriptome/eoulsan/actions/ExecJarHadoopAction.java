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
import java.util.Set;

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
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.HadoopExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.TerminalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.CopyDesignAndParametersToOutputStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.InitGlobalLoggerStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataDownloadStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HadoopUploadStep;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class ExecJarHadoopAction extends AbstractAction {

  /** Name of this action. */
  public static final String ACTION_NAME = "execjarhadoop";

  private static final Set<Parameter> EMPTY_PARAMEMETER_SET = Collections
      .emptySet();

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
  public void action(String[] arguments) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String jobDescription = null;
    String jobEnvironment = null;
    boolean uploadOnly = false;
    long millisSinceEpoch = System.currentTimeMillis();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      final CommandLine line = parser.parse(options, arguments, true);

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
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions + 3) {
      help(options);
    }

    // Get the command line arguments
    final String paramPathname = convertS3URL(arguments[argsOptions]);
    final String designPathname = convertS3URL(arguments[argsOptions + 1]);
    final String destPathname = convertS3URL(arguments[argsOptions + 2]);

    // Execute program in hadoop mode
    run(paramPathname, designPathname, destPathname, jobDescription,
        jobEnvironment, uploadOnly, millisSinceEpoch);

  }

  /**
   * Convert a s3:// URL to a s3n:// URL
   * @param url input URL
   * @return converted URL
   */
  private String convertS3URL(final String url) {

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
  private static void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("hadoop -jar "
        + Globals.APP_NAME_LOWER_CASE
        + ".jar  [options] param.xml design.txt hdfs://server/path", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode
   * @param paramPathname parameter file
   * @param designPathname design file
   * @param destPathname data path
   * @param jobDescription job description
   * @param jobEnvironment job environment
   * @param millisSinceEpoch milliseconds since epoch
   * @param uploadOnly true if execution must end after upload
   */
  private static void run(final String paramPathname,
      final String designPathname, final String destPathname,
      final String jobDescription, final String jobEnvironment,
      final boolean uploadOnly, final long millisSinceEpoch) {

    checkNotNull(paramPathname, "paramPathname is null");
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
      if (paramPathname.indexOf("://") != -1)
        paramURI = new URI(paramPathname);
      else
        paramURI = new File(paramPathname).getAbsoluteFile().toURI();

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

      // Read design file
      final Design design =
          DesignUtils.readAndCheckDesign(designFs.open(designPath));

      // Create command object
      final Command c = new Command();

      // Add init global logger Step
      c.addStep(InitGlobalLoggerStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

      // Add Copy design and parameter file Step
      c.addStep(CopyDesignAndParametersToOutputStep.STEP_NAME,
          EMPTY_PARAMEMETER_SET);

      // Parse param file
      final ParamParser pp =
          new ParamParser(PathUtils.createInputStream(paramPath, conf));

      pp.parse(c);

      // Add download Step
      c.addStep(HDFSDataDownloadStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

      // Create executor
      final Executor e =
          new HadoopExecutor(conf, c, design, designPath, paramPath, desc, env,
              millisSinceEpoch);

      // Create upload step
      final Step uploadStep =
          new HadoopUploadStep(new DataFile(destURI.toString()), conf);

      // Add terminal step if upload only
      final List<Step> firstSteps;
      if (uploadOnly) {
        firstSteps = Arrays.asList(new Step[] {uploadStep, new TerminalStep()});
      } else {
        firstSteps = Collections.singletonList(uploadStep);
      }

      // Execute
      e.execute(firstSteps, null);

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
