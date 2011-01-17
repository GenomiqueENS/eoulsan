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

import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import com.google.common.base.Preconditions;

public class HadoopReporterIncrementer implements ReporterIncrementer {

  private final TaskInputOutputContext context;

  @Override
  public void incrCounter(String counterGroup, String counterName, long amount) {

    context.getCounter(counterGroup, counterName).increment(amount);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param context context to use for counter incrementation
   */
  public HadoopReporterIncrementer(final TaskInputOutputContext context) {

    Preconditions.checkNotNull(context, "Context is null");

    this.context = context;
  }

}
