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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.util.locker;

import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import java.util.Set;

/**
 * Utility class for the locker classes.
 *
 * @since 1.1
 * @author Laurent Jourdren
 */
public final class LockerUtils {

  /**
   * Return a set withs pid of existing JVMs.
   *
   * @return a set of integers with pid of existing JVMs
   */
  public static Set<Integer> getJVMsPIDs() {

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
