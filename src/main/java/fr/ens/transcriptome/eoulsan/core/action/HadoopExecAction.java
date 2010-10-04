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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Executor;
import fr.ens.transcriptome.eoulsan.core.HadoopAnalysisExecutor;
import fr.ens.transcriptome.eoulsan.core.ParamParser;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.CopyDesignAndParametersToOutputStep;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.HDFSDataDownloadStep;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.HDFSDataUploadStep;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define the exec action in hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopExecAction implements Action {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  // Configure URL handler for hdfs protocol
  // static {
  // URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
  // }

  private static final Set<Parameter> EMPTY_PARAMEMETER_SET = new HashSet<Parameter>();
  
  @Override
  public void action(final String[] args) {

    if (args.length != 3) {

      System.err.println("Invalid number of arguments.");
      System.err.println("usage: "
          + Globals.APP_NAME_LOWER_CASE + " exec param.xml design.txt");

      System.exit(1);

    }

    final Configuration conf = new Configuration();
    conf.set("fs.s3n.awsAccessKeyId", "AKIAJPXBAOLESJ2TOABA");
    conf.set("fs.s3n.awsSecretAccessKey",
        "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");

    try {

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

      logger.info(Globals.APP_NAME
          + " version " + Globals.APP_VERSION + " (" + Globals.APP_BUILD_NUMBER
          + " on " + Globals.APP_BUILD_DATE + ")");
      logger.info("Hadoop base dir: " + basePath);
      logger.info("Parameter file: " + paramPath);
      logger.info("Design file: " + designPath);

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

      // Add Copy design and parameter file Step
      c.addStep(CopyDesignAndParametersToOutputStep.STEP_NAME,
          EMPTY_PARAMEMETER_SET);

      // Add upload Step
      c.addStep(HDFSDataUploadStep.STEP_NAME, EMPTY_PARAMEMETER_SET);

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

      System.err.println("File not found: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);

    } catch (EoulsanException e) {

      System.err.println("Error while executing "
          + Globals.APP_NAME_LOWER_CASE + ": " + e.getMessage());
      e.printStackTrace();
      System.exit(1);

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);

    } catch (URISyntaxException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }

  }

  public static void main(final String[] args) {

    new HadoopExecAction().action(args);
  }

}
