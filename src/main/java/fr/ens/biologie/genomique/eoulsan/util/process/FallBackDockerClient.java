package fr.ens.biologie.genomique.eoulsan.util.process;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getGenericLogger;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;

/**
 * This class define a Docker client using the Docker command line.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FallBackDockerClient implements DockerClient {

  private final GenericLogger logger;

  @Override
  public void initialize(URI dockerConnectionURI) {
    // Nothing to do
  }

  @Override
  public DockerImageInstance createConnection(String dockerImage) {

    return new FallBackDockerImageInstance(dockerImage, this.logger);
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public Set<String> listImageTags() throws IOException {

    Set<String> result = new HashSet<>();
    String output = ProcessUtils.execToString("docker images");

    Splitter lineSplitter = Splitter.on('\n');
    Splitter fieldSplitter =
        com.google.common.base.Splitter.on(' ').omitEmptyStrings();

    boolean first = true;

    for (String line : lineSplitter.split(output)) {

      if (first) {
        first = false;
        continue;
      }

      List<String> fields = Lists.newArrayList(fieldSplitter.split(line));

      if (fields.size() >= 2) {
        String tagName = fields.get(0) + ':' + fields.get(1);
        if (!"<none>:<none>".equals(tagName)) {
          result.add(tagName);
        }
      }
    }

    return result;
  }

  //
  // Constructors
  //

  /**
   * Constructor.
   */
  public FallBackDockerClient() {

    this(null);
  }

  /**
   * Constructor.
   * @param logger logger to use
   */
  public FallBackDockerClient(GenericLogger logger) {

    this.logger = logger == null ? getGenericLogger() : logger;
  }

}
