package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Joiner;
import com.spotify.docker.client.DockerException;

import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerProcess;

/**
 * This class define a Docker RExecutor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DockerRExecutor extends ProcessRExecutor {

  private final String dockerImage;

  @Override
  protected void executeRScript(final File rScriptFile, final boolean sweave)
      throws IOException {

    final DockerProcess process =
        new DockerProcess(this.dockerImage, getTemporaryDirectory());

    final List<String> commandLine = createCommand(rScriptFile, sweave);

    final File stdoutFile = changeFileExtension(rScriptFile, ".out");
    final File stderrFile = changeFileExtension(rScriptFile, ".err");

    try {

      final int exitValue = process.execute(commandLine, getOutputDirectory(),
          getTemporaryDirectory(), stdoutFile, stderrFile);

      ProcessUtils.throwExitCodeException(exitValue,
          Joiner.on(' ').join(commandLine));

    } catch (DockerException | InterruptedException e) {
      throw new IOException(e);
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param outputDirectory the output directory
   * @param temporaryDirectory the temporary directory
   * @param dockerImage docker image to use
   * @throws IOException if an error occurs while creating the object
   */
  public DockerRExecutor(final File outputDirectory,
      final File temporaryDirectory, final String dockerImage)
          throws IOException {
    super(outputDirectory, temporaryDirectory);

    if (dockerImage == null) {
      throw new NullPointerException("dockerImage argument cannot be null");
    }

    this.dockerImage = dockerImage;
  }

}
