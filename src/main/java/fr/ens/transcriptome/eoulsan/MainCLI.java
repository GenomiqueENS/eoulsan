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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.LocalAnalysisExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.SimpleExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignBuilder;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.design.io.DesignWriter;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.FakeS3ProtocolFactory;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.S3DataUploadStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in local mode.
 * @author Laurent Jourdren
 */
public class MainCLI {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /**
   * Show version of the application.
   */
  public static void version() {

    System.out.println(Globals.APP_NAME
        + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
        + " on " + Globals.APP_BUILD_DATE + ")");
    System.exit(0);
  }

  /**
   * Show licence information about this application.
   */
  public static void about() {

    System.out.println(Globals.ABOUT_TXT);
    System.exit(0);
  }

  /**
   * Show information about this application.
   */
  public static void license() {

    System.out.println(Globals.LICENSE_TXT);
    System.exit(0);
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE + " [options] design",
        options);

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

    options.addOption(OptionBuilder.withArgName("file").hasArg()
        .withDescription("external log file").create("log"));

    options.addOption(OptionBuilder.withArgName("level").hasArg()
        .withDescription("log level").create("loglevel"));

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

      // Set Log file
      if (line.hasOption("log")) {

        argsOptions += 2;
        try {
          Handler fh = new FileHandler(line.getOptionValue("log"));
          fh.setFormatter(Globals.LOG_FORMATTER);
          logger.setUseParentHandlers(false);

          logger.addHandler(fh);
        } catch (IOException e) {
          logger.severe("Error while creating log file: " + e.getMessage());
          System.exit(1);
        }
      }

      // Set log level
      if (line.hasOption("loglevel")) {

        argsOptions += 2;
        try {
          logger.setLevel(Level.parse(line.getOptionValue("loglevel")
              .toUpperCase()));
        } catch (IllegalArgumentException e) {

          logger
              .warning("Unknown log level ("
                  + line.getOptionValue("loglevel")
                  + "). Accepted values are [SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST].");

        }
      }

    } catch (ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return argsOptions;
  }

  //
  // Action methods
  //

  /**
   * Exec action.
   * @param args command line parameters for exec action
   */
  private static void execAction(final String[] args) {

    if (args.length != 2) {

      System.err.println("Invalid number of arguments.");
      System.err.println("usage: "
          + Globals.APP_NAME_LOWER_CASE + " exec param.xml design.txt");

      System.exit(1);

    }

    final File paramFile = new File(args[0]);
    final File designFile = new File(args[1]);

    logger.info(Globals.APP_NAME
        + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
        + " on " + Globals.APP_BUILD_DATE + ")");
    logger.info("Parameter file: " + paramFile);
    logger.info("Design file: " + designFile);

    try {

      // Test if param file exists
      if (!paramFile.exists())
        throw new FileNotFoundException(paramFile.toString());

      // Test if design file exists
      if (!designFile.exists())
        throw new FileNotFoundException(designFile.toString());

      // Parse param file
      final ParamParser pp = new ParamParser(paramFile);
      final Command c = pp.parse();

      // Execute
      final Executor e = new LocalAnalysisExecutor(c, designFile);
      e.execute();

    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      System.exit(1);
    } catch (EoulsanException e) {

      e.printStackTrace();

      System.err.println("Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
      System.exit(1);
    }

  }

  /**
   * Create soap index action.
   * @param args command line parameters for exec action
   */
  private static void createDesignAction(final String[] args) {

    DesignBuilder db = new DesignBuilder(args);

    Design design = db.getDesign();

    if (design.getSampleCount() == 0) {
      System.err
          .println("Error: Nothing to create, no file found.  Use the -h option to get more information.");
      System.err.println("usage: "
          + Globals.APP_NAME_LOWER_CASE + " createdesign files");
      System.exit(1);
    }

    try {
      DesignWriter dw = new SimpleDesignWriter("design.txt");

      dw.write(design);

    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      System.exit(1);
    } catch (EoulsanIOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }

  }

  /**
   * Exec action.
   * @param args command line parameters for exec action
   */
  private static void uploadS3Action(final String[] args) {

    final File userDir = new File(System.getProperty("user.dir"));

    try {

      URL.setURLStreamHandlerFactory(new FakeS3ProtocolFactory());

      // DataUploadStep du = new S3DataUploadStep(new File(userDir,
      // ".credentials"));

      final String paramPathname = args[0];
      final String designPathname = args[1];

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
      final URI destURI = new URI(args[2]);

      logger.info(Globals.APP_NAME
          + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
          + " on " + Globals.APP_BUILD_DATE + ")");
      logger.info("Parameter file: " + paramURI);
      logger.info("Design file: " + designURI);
      logger.info("Destination : " + destURI);

      // Read design file
      final Design design =
          DesignUtils.readAndCheckDesign(FileUtils.createInputStream(new File(
              designURI)));

      // Add upload Step
      final Set<Parameter> uploadParameters = new HashSet<Parameter>();
      uploadParameters.add(new Parameter("basepath", destURI.toString()));
      uploadParameters.add(new Parameter("parampath", paramURI.toString()));
      uploadParameters.add(new Parameter("designpath", destURI.toString()));

      final S3DataUploadStep step =
          new S3DataUploadStep(new File(userDir, ".credentials"));
      step.configure(uploadParameters, new HashSet<Parameter>());

      // Create Executor information
      final ExecutorInfo info = new SimpleExecutorInfo();

      final StepResult result = step.execute(design, info);

      if (result.getException() != null) {
        System.err.println("Error: " + result.getException().getMessage());
        System.exit(1);
      }

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    } catch (EoulsanException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    } catch (URISyntaxException e) {
      System.err.println("Error: " + e.getMessage());
      System.exit(1);
    }

  }

  //
  // Main method
  //

  /**
   * Main method for the CLI mode.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Set log level
    logger.setLevel(Globals.LOG_LEVEL);
    logger.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    if (args == null || args.length == argsOptions) {

      System.err
          .println("This program needs one argument. Use the -h option to get more information.");
      System.err.println("usage:\n\t"
          + Globals.APP_NAME_LOWER_CASE
          + " createdesign fastq_files fasta_file gff_gfile");
      System.err.println("usage:\n\t"
          + Globals.APP_NAME_LOWER_CASE
          + " createdesign exec param.xml design.txt");
      System.err.println("usage:\n\t"
          + Globals.APP_NAME_LOWER_CASE + " uploads3 param.xml design.txt");
      System.exit(1);
    }

    final String action = args[argsOptions].trim().toLowerCase();
    final String[] arguments =
        StringUtils.arrayWithoutFirstsElement(args, argsOptions + 1);

    if ("exec".equals(action))
      execAction(arguments);
    else if ("createdesign".equals(action))
      createDesignAction(arguments);
    else if ("uploads3".equals(action))
      uploadS3Action(arguments);

  }
}
