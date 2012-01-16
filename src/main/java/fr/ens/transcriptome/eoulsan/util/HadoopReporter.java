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

package fr.ens.transcriptome.eoulsan.util;

import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import com.google.common.base.Preconditions;

/**
 * This class define a Hadoop reporter.
 * @author Laurent Jourdren
 */
@SuppressWarnings("unchecked")
public class HadoopReporter implements ReporterIncrementer {

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
  public HadoopReporter(final TaskInputOutputContext context) {

    Preconditions.checkNotNull(context, "Context is null");

    this.context = context;
  }

}
