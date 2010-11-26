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

package fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignWriter;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This step copy design and parameter file to output directory.
 * @author Laurent Jourdren
 */
@HadoopOnly
public class CopyDesignAndParametersToOutputStep extends AbstractStep {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  /** Step name. */
  public static final String STEP_NAME = "_copy_design_params_to_output";

  private Configuration conf;

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "Copy design and parameters file to output path.";
  }
  
  @Override
  public ExecutionMode getExecutionMode() {
    
    return Step.ExecutionMode.HADOOP;
  }

  @Override
  public String getLogName() {

    return null;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    this.conf = CommonHadoop.createConfiguration(globalParameters);
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final Configuration conf = this.conf;

    final Path designPath = new Path(context.getDesignPathname());
    final Path paramPath = new Path(context.getParameterPathname());
    final Path outputPath = new Path(context.getOutputPathname());

    final Path outputDesignPath = new Path(outputPath, designPath.getName());
    final Path outputParamPath = new Path(outputPath, paramPath.getName());

    // Copy design file
    try {
      if (!PathUtils.exists(outputDesignPath, conf)) {

        final FileSystem outputDesignFs = outputDesignPath.getFileSystem(conf);

        new SimpleDesignWriter(outputDesignFs.create(outputDesignPath))
            .write(design);
      }
    } catch (IOException e) {
      logger.severe("Unable to copy design file to output path.");
    } catch (EoulsanIOException e) {
      logger.severe("Unable to copy design file to output path.");
    }

    // Copy parameter file
    try {
      if (!PathUtils.exists(outputParamPath, conf))
        PathUtils.copy(designPath, outputParamPath, conf);
    } catch (IOException e) {
      logger.severe("Unable to copy design file to output path.");
    }

    return new StepResult(this, true, "");
  }

}
