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
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.LocalExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.TerminalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.AWSMapReduceExecStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.local.ExecInfoLogStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.LocalUploadStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define an action that execute Eoulsan on AWS MapReduce.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class AWSExecAction extends AbstractAction {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final Set<Parameter> EMPTY_PARAMEMETER_SET = Collections
      .emptySet();

  @Override
  public String getName() {
    return "awsexec";
  }

  @Override
  public String getDescription() {
    return "execute eoulsan on Amazon cloud.";
  }

  @Override
  public boolean isCurrentArchCompatible() {

    return true;
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
      Common.errorExit(e,
          "Error while parsing parameter file: " + e.getMessage());
    }

    if (arguments.length != argsOptions + 3) {
      help(options);
    }

    final File paramFile = new File(arguments[argsOptions]);
    final File designFile = new File(arguments[argsOptions + 1]);
    final DataFile s3Path =
        new DataFile(StringUtils.replacePrefix(arguments[argsOptions + 2],
            "s3:/", "s3n:/"));

    // Execute program in AWS mode
    run(paramFile, designFile, s3Path, jobDescription);
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
        + ".sh " + getName()
        + " [options] param.xml design.txt s3://mybucket/test", options);

    Common.exit(0);
  }

  //
  // Execution
  //

  /**
   * Run Eoulsan in hadoop mode.
   * @param paramFile parameter file
   * @param designFile design file
   * @param s3Path path of data on S3 file system
   * @param jobDescription job description
   */
  private void run(final File paramFile, final File designFile,
      final DataFile s3Path, final String jobDescription) {

    checkNotNull(paramFile, "paramFile is null");
    checkNotNull(designFile, "designFile is null");
    checkNotNull(s3Path, "s3Path is null");

    logger.info(Globals.WELCOME_MSG + " Local mode.");
    logger.info("Parameter file: " + paramFile);
    logger.info("Design file: " + designFile);

    final String desc;

    if (jobDescription == null) {
      desc = "no job description";
    } else {
      desc = jobDescription.trim();
    }

    try {

      // Test if param file exists
      if (!paramFile.exists())
        throw new FileNotFoundException(paramFile.toString());

      // Test if design file exists
      if (!designFile.exists())
        throw new FileNotFoundException(designFile.toString());

      // Parse param file
      final ParamParser pp = new ParamParser(paramFile);
      final Command c = new Command();

      // Add execution info to log Step
      c.addStep(ExecInfoLogStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

      pp.parse(c);

      // Execute
      final Executor e = new LocalExecutor(c, designFile, paramFile, desc);
      e.execute(Lists.newArrayList((Step) new LocalUploadStep(s3Path),
          (Step) new AWSMapReduceExecStep(), (Step) new TerminalStep()), null,
          true);

    } catch (FileNotFoundException e) {
      Common.errorExit(e, "File not found: " + e.getMessage());
    } catch (EoulsanException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    } catch (EoulsanRuntimeException e) {
      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
    }

  }

}
