package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

/**
 * This class define a TORQUE cluster scheduler using a Bpipe script.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class TORQUETaskScheduler extends BundledScriptBpipeTaskScheduler {

  public static final String SCHEDULER_NAME = "torque";
  private static final String COMMAND_WRAPPER_SCRIPT = "bpipe-torque.sh";

  //
  // Constructor
  //

  /** Constructor. */
  public TORQUETaskScheduler() {
    super(SCHEDULER_NAME, COMMAND_WRAPPER_SCRIPT);
  }
}
