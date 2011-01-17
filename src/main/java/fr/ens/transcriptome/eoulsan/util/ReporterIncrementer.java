/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.util;

/**
 * This interface allow to increment a counter in hadoop or local mode.
 * @author Laurent Jourdren
 */
public interface ReporterIncrementer {

  /**
   * Increment a counter.
   * @param counterGroup group of the counter
   * @param counter the counter to increment
   * @param amount amount to add to the counter
   */
  public void incrCounter(String counterGroup, String counterName, long amount);

}
