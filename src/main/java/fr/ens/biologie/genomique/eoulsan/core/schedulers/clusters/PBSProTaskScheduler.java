package fr.ens.biologie.genomique.eoulsan.core.schedulers.clusters;

/**
 * This class define a PBSPro cluster scheduler using a Bpipe script.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class PBSProTaskScheduler extends BundledScriptBpipeTaskScheduler {

  public static final String SCHEDULER_NAME = "pbspro";
  private static final String COMMAND_WRAPPER_SCRIPT = "bpipe-pbspro.sh";

  //
  // Constructor
  //

  /** Constructor. */
  public PBSProTaskScheduler() {
    super(SCHEDULER_NAME, COMMAND_WRAPPER_SCRIPT);
  }
}
