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

package fr.ens.transcriptome.eoulsan.steps;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ProgressDetail;
import com.spotify.docker.client.messages.ProgressMessage;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.DockerManager;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.util.Version;
import jersey.repackaged.com.google.common.base.Splitter;

/**
 * This class define a step that fetches Docker images.
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
public class DockerImagesFetcherStep extends AbstractStep {

  public static final String STEP_NAME = "dockerfetcher";

  public static final String IMAGES_TO_FETCH_PARAMETER_NAME =
      "docker.images.to.fetch";

  private final Set<String> images = new HashSet<>();

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case IMAGES_TO_FETCH_PARAMETER_NAME:

        for (String dockerImage : Splitter.on('\t').trimResults()
            .split(p.getValue())) {

          this.images.add(dockerImage);
        }

        break;

      default:
        throw new EoulsanException(
            "Unknown parameter for step " + getName() + ": " + p.getName());
      }

    }

  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    final DockerClient dockerClient = DockerManager.getInstance().getClient();

    if (dockerClient == null) {
      return status.createStepResult(
          new EoulsanException("cannot instanciate docker client"));
    }

    try {

      for (String image : this.images) {
        context.getLogger().info("Docker image to fetch: " + image);
      }

      final Set<String> imagesToDownload =
          filterDownloadedImages(dockerClient, this.images);

      final double count = imagesToDownload.size();

      int i = 0;

      for (String dockerImageName : imagesToDownload) {

        context.getLogger().info("Download Docker image: " + dockerImageName);

        final double done = i;
        dockerClient.pull(dockerImageName, new ProgressHandler() {

          private long lastTime = System.currentTimeMillis();

          @Override
          public void progress(final ProgressMessage msg)
              throws DockerException {

            final long currentTime = System.currentTimeMillis();

            if (currentTime - lastTime > 1000) {

              final ProgressDetail pg = msg.progressDetail();
              double percent = pg.current() / pg.total();

              status.setProgress((done + percent) / count);
              lastTime = currentTime;
            }
          }
        });

        i++;
      }

    } catch (DockerException | InterruptedException e) {

      return status.createStepResult(e);
    }

    return status.createStepResult();
  }

  private static final Set<String> filterDownloadedImages(
      final DockerClient dockerClient, final Set<String> imagesToFetch)
          throws DockerException, InterruptedException {

    checkNotNull(dockerClient, "dockerClient argument cannot be null");
    checkNotNull(imagesToFetch, "imagesToFetch argument cannot be null");

    final Set<String> result = new HashSet<String>(imagesToFetch);

    List<Image> images = dockerClient.listImages();

    for (String imageToFetch : imagesToFetch) {

      for (Image image : images) {
        for (String tag : image.repoTags()) {
          if (imageToFetch.equals(tag)) {
            result.remove(imageToFetch);
            getLogger().info(
                "Docker image has been already downloaded: " + imageToFetch);
          }
        }
      }
    }

    return Collections.unmodifiableSet(result);
  }

}
