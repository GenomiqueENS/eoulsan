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

package fr.ens.transcriptome.eoulsan.programs.mgmt.hadoop;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This class initialize the global logger
 * @author Laurent Jourdren
 */
public class InitGlobalLoggerStep implements Step {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static final String STEP_NAME = "_init_global_logger";

  //
  // Step methods
  //

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    final Configuration conf = new Configuration();
    final Path loggerPath =
        new Path(info.getLogPathname(), Globals.APP_NAME_LOWER_CASE + ".log");

    try {

      final FileSystem loggerFs = loggerPath.getFileSystem(conf);
      logger.addHandler(new StreamHandler(loggerFs.create(loggerPath),
          Globals.LOG_FORMATTER));
      logger.setLevel(Globals.LOG_LEVEL);

      logger.info(Globals.APP_NAME
          + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
          + " on " + Globals.APP_BUILD_DATE + ")");
      logger.info("Hadoop base dir: " + info.getBasePathname());
      logger.info("Parameter file: " + info.getParameterPathname());
      logger.info("Design file: " + info.getDesignPathname());

    } catch (IOException e) {
      logger.severe("Unable to configure global logger: " + loggerPath);
    }

    return new StepResult(this, true, "");
  }

  @Override
  public String getDescription() {

    return "Initialize global logger";
  }

  @Override
  public String getLogName() {

    return null;
  }

  @Override
  public String getName() {

    return STEP_NAME;
  }

}
