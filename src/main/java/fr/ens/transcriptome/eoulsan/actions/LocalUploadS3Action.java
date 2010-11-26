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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.SimpleContext;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.DesignUtils;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.FakeS3ProtocolFactory;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.S3DataUploadStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define the Local Upload S3 Action.
 * @author Laurent Jourdren
 */
public class LocalUploadS3Action implements Action {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  @Override
  public void action(final String[] args) {

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
          + " version " + Globals.APP_VERSION_STRING + " ("
          + Globals.APP_BUILD_NUMBER + " on " + Globals.APP_BUILD_DATE + ") Local mode.");
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

      // Create Execution context
      final Context context = new SimpleContext();

      final StepResult result = step.execute(design, context);

      if (result.getException() != null)
        Common.errorExit(result.getException(), "Error: "
            + result.getException().getMessage());

    } catch (IOException e) {
      Common.errorExit(e, "Error: " + e.getMessage());
    } catch (EoulsanException e) {
      Common.errorExit(e, "Error: " + e.getMessage());
    } catch (URISyntaxException e) {
      Common.errorExit(e, "Error: " + e.getMessage());
    }

  }

}
