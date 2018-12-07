package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.io.Files;

import fr.ens.biologie.genomique.eoulsan.io.FileCharsets;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define a Docker client using the Singularity command line.
 * @author Laurent Jourdren
 * @since 2.3
 */
public class SingularityDockerClient implements DockerClient {

  private File imageDirectory = new File("singularity");

  @Override
  public void initialize(URI dockerConnectionURI) throws IOException {

    // Create a directory for singularity images if not exists
    if (!this.imageDirectory.exists()) {
      if (!this.imageDirectory.mkdirs()) {
        throw new IOException(
            "Unable to create directory for singularity images: "
                + this.imageDirectory);
      }
    }

  }

  @Override
  public DockerImageInstance createConnection(String dockerImage) {

    return new SingularityDockerImageInstance(dockerImage, this.imageDirectory);
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public Set<String> listImageTags() throws IOException {

    return loadImageList(this.imageDirectory).keySet();
  }

  /**
   * Load the list of downloaded images
   * @param imageDirectory image directory
   * @return a map with the names of downloaded images
   * @throws IOException if an error occurs while reading the list of images
   */
  static Map<String, String> loadImageList(final File imageDirectory)
      throws IOException {

    File f = new File(imageDirectory, "image.list");
    final Map<String, String> result = new HashMap<>();

    if (!f.exists()) {
      return result;
    }

    Splitter splitter = Splitter.on('\t');

    for (String line : Files.readLines(f, FileCharsets.UTF8_CHARSET)) {

      line = line.trim();
      if (line.startsWith("#") || line.isEmpty()) {
        continue;
      }

      List<String> fields = GuavaCompatibility.splitToList(splitter, line);

      if (fields.size() == 2) {
        result.put(fields.get(0), fields.get(1));
      }

    }

    return result;
  }

}
