package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

import java.io.File;

import org.apache.commons.lang.NullArgumentException;

import fr.ens.biologie.genomique.eoulsan.Main;

/**
 * This class define an abstract scheduler that use a bpipe script bundled in
 * Eoulsan distribution.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class BundledScriptBpipeTaskScheduler
    extends BpipeTaskScheduler {

  private final String schedulerName;
  private final File commandWrapperFile;

  @Override
  public String getSchedulerName() {

    return this.schedulerName;
  }

  @Override
  protected File getBpipeCommandWrapper() {

    return this.commandWrapperFile;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param schedulerName scheduler name
   * @param commandWrapperScript command wrapper script name
   */
  protected BundledScriptBpipeTaskScheduler(final String schedulerName,
      final String commandWrapperScript) {

    if (schedulerName == null) {
      throw new NullArgumentException("schedulerName argument cannot be null");
    }

    if (commandWrapperScript == null) {
      throw new NullArgumentException(
          "commandWrapperScript argument cannot be null");
    }

    this.schedulerName = schedulerName;

    final File eoulsanScript =
        new File(Main.getInstance().getEoulsanScriptPath());

    final File binDir = new File(eoulsanScript.getParent(), "bin");

    this.commandWrapperFile = new File(binDir, commandWrapperScript);
  }
}
