package fr.ens.transcriptome.eoulsan.util.locker;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * Utility class for the locker classes.
 * @author Laurent Jourdren
 */
public final class LockerUtils {

  /**
   * Return a set withs pid of existing JVMs.
   * @return a set of integers with pid of existing JVMs
   */
  public static Set<Integer> getJVMsPids() {

    return ProcessUtils.getExecutablePids("java");
  }

  // public static Set<Integer> getJVMsPids2() {
  //
  // try {
  //
  // MonitoredHost monitoredHost =
  // MonitoredHost.getMonitoredHost((String) null);
  //
  // if (monitoredHost==null)
  // return null;
  //
  // // get the set active JVMs on the specified host.
  // return monitoredHost.activeVms();
  //
  // } catch (URISyntaxException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // } catch (MonitorException e) {
  // // TODO Auto-generated catch block
  // e.printStackTrace();
  // }
  //
  // return null;
  // }

}
