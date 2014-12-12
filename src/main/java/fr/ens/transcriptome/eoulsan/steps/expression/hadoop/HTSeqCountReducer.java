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

package fr.ens.transcriptome.eoulsan.steps.expression.hadoop;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Reducer for the expression estimation with htseq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCountReducer extends Reducer<Text, Long, Text, Long> {

  /**
   * This method allow to sum of the values of an Iterable of longs.
   * @param values values to sum
   * @return the sum of the values
   */
  private static long sum(final Iterable<Long> values) {

    final Iterator<Long> it = values.iterator();
    long result = 0L;

    while (it.hasNext()) {
      result += it.next();
    }

    return result;
  }

  /**
   * 'key': annotation identifier of the feature (gene, mRNA, exon...).
   * 'values': a list of '1', the size of this list is the number of reads found
   * on the feature.
   */
  @Override
  protected void reduce(final Text key, final Iterable<Long> values,
      final Context context) throws IOException, InterruptedException {

    context.write(key, sum(values));
  }

}
