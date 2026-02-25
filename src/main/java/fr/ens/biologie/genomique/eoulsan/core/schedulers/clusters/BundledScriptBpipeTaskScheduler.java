package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

import fr.ens.biologie.genomique.eoulsan.Main;
import java.io.File;
import java.nio.file.Path;

/**
 * This class define an abstract scheduler that use a bpipe script bundled in Eoulsan distribution.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class BundledScriptBpipeTaskScheduler extends BpipeTaskScheduler {

  private final String schedulerName;
  private final Path commandWrapperFile;

  @Override
  public String getSchedulerName() {

    return this.schedulerName;
  }

  @Override
  protected File getBpipeCommandWrapper() {

    return this.commandWrapperFile.toFile();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param schedulerName scheduler name
   * @param commandWrapperScript command wrapper script name
   */
  protected BundledScriptBpipeTaskScheduler(
      final String schedulerName, final String commandWrapperScript) {

    if (schedulerName == null) {
      throw new NullPointerException("schedulerName argument cannot be null");
    }

    if (commandWrapperScript == null) {
      throw new NullPointerException("commandWrapperScript argument cannot be null");
    }

    this.schedulerName = schedulerName;

    final Path binDir = Main.getInstance().getEoulsanDirectory().toPath().resolve("bin");

    this.commandWrapperFile = binDir.resolve(commandWrapperScript);
  }
}
