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

package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import static java.util.Objects.requireNonNull;

import org.apache.hadoop.mapreduce.TaskInputOutputContext;

import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define a Hadoop reporter.
 * @since 1.0
 * @author Laurent Jourdren
 */
@SuppressWarnings("unchecked")
public class HadoopReporterIncrementer implements ReporterIncrementer {

  private final TaskInputOutputContext context;

  @Override
  public void incrCounter(final String counterGroup, final String counterName,
      final long amount) {

    this.context.getCounter(counterGroup, counterName).increment(amount);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param context context to use for counter incrementation
   */
  public HadoopReporterIncrementer(final TaskInputOutputContext context) {

    requireNonNull(context, "Context is null");

    this.context = context;
  }

}
