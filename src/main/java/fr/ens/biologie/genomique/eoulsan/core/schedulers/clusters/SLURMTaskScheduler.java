package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

/**
 * This class define a SLURM cluster scheduler using a Bpipe script.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SLURMTaskScheduler extends BundledScriptBpipeTaskScheduler {

  public static final String SCHEDULER_NAME = "slurm";
  private static final String COMMAND_WRAPPER_SCRIPT = "bpipe-slurm.sh";

  //
  // Constructor
  //

  /** Constructor. */
  public SLURMTaskScheduler() {
    super(SCHEDULER_NAME, COMMAND_WRAPPER_SCRIPT);
  }
}
