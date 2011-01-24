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

package fr.ens.transcriptome.eoulsan.actions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.HadoopJarRepackager;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * This class launch Eoulsan in hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopExecAction implements Action {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String HADOOP_CMD = "hadoop jar ";

  @Override
  public String getName() {
    return "hadoopexec";
  }

  @Override
  public String getDescription() {
    return "execute " + Globals.APP_NAME + " on local hadoop cluster.";
  }

  @Override
  public void action(final String[] arguments) {
    
    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    String jobDescription = null;

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

    } catch (ParseException e) {
      Common.errorExit(e, "Error while parsing parameter file: "
          + e.getMessage());
    }

    if (arguments.length != argsOptions + 2) {
      help(options);
    }

    final File paramFile = new File(arguments[argsOptions]);
    final File designFile = new File(arguments[argsOptions + 1]);
    final String hdfsPath = arguments[argsOptions + 2];

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
  private Options makeOptions() {

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
  private void help(final Options options) {

    // Show help message
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + ".sh " + getName() + " [options] param.xml design.txt hdfs://server/path",
        options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode.
   * @param paramFile parameter file
   * @param designFile design file
   * @param hdfsPath path of data on hadoop file system
   * @param jobDescription job description
   */
  private void run(final File paramFile, final File designFile,
      final String hdfsPath, final String jobDescription) {

    checkNotNull(paramFile, "paramFile is null");
    checkNotNull(designFile, "designFile is null");
    checkNotNull(hdfsPath, "hdfsPath is null");
    
    try {

      File repackagedJarFile = HadoopJarRepackager.repack();

      LOGGER.info("Launch Eoulsan in Hadoop mode.");

      // Create command line
      final StringBuilder sb = new StringBuilder();
      sb.append(HADOOP_CMD);
      sb.append(repackagedJarFile.getCanonicalPath());
      sb.append(" exec ");

      if (jobDescription != null) {
        sb.append("-d \"");
        sb.append(jobDescription.trim());
        sb.append("\" ");
      }

      sb.append("-e \"local hadoop cluster\" ");
      sb.append(paramFile);
      sb.append(" ");
      sb.append(designFile);
      sb.append(" ");
      sb.append(hdfsPath);

      // execute hadoop
      ProcessUtils.execThreadOutput(sb.toString());

    } catch (IOException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }
}
