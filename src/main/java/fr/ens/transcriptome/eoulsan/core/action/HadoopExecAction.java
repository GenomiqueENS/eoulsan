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

package fr.ens.transcriptome.eoulsan.core.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.HadoopAnalysisExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.CopyDesignAndParametersToOutputStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.InitGlobalLoggerStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataDownloadStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataUploadStep;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define the exec action in hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopExecAction implements Action {

  private static final Set<Parameter> EMPTY_PARAMEMETER_SET =
      Collections.emptySet();

  /**
   * Main method. This method is called by MainHadoop.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    new HadoopExecAction().action(args);
  }

  @Override
  public void action(final String[] args) {

    if (args.length != 3)
      Common.showErrorMessageAndExit("Invalid number of arguments.\n"
          + "usage: " + Globals.APP_NAME_LOWER_CASE
          + " exec param.xml design.txt");

    try {

      // Initialize The application
      final Configuration conf = init();

      // Get the command line argumnts
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

      final Path basePath = new Path(destURI.toString());
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

      // Add upload Step
      c.addStep(HDFSDataUploadStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

      // Add Copy design and parameter file Step
      c.addStep(CopyDesignAndParametersToOutputStep.STEP_NAME,
          EMPTY_PARAMEMETER_SET);

      // Parse param file
      final ParamParser pp =
          new ParamParser(PathUtils.createInputStream(paramPath, conf));

      pp.parse(c);

      // Add download Step
      c.addStep(HDFSDataDownloadStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

      // Execute
      final Executor e =
          new HadoopAnalysisExecutor(conf, c, design, designPath, paramPath,
              basePath);
      e.execute();

    } catch (FileNotFoundException e) {

      Common.errorExit(e, "File not found: " + e.getMessage());

    } catch (EoulsanException e) {

      Common.errorExit(e, "Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());

    } catch (IOException e) {

      Common.errorExit(e, "Error: " + e.getMessage());

    } catch (URISyntaxException e) {

      Common.errorExit(e, "Error: " + e.getMessage());
    }

  }

  /**
   * Configure the application.
   * @return a Hadoop configuration object
   * @throws EoulsanException if an error occurs while reading settings
   */
  private Configuration init() throws EoulsanException {

    try {
      // Create and load settings
      final Settings settings = new Settings();

      // Create Hadoop configuration object
      final Configuration conf =
          CommonHadoop.createConfigurationFromSettings(settings);

      // Initialize runtime
      HadoopEoulsanRuntime.init(settings, conf);

      return conf;
    } catch (IOException e) {
      throw new EoulsanException("Error while reading settings: "
          + e.getMessage());
    }
  }

}
