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

package fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop;

import java.io.IOException;
import java.util.Iterator;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Reducer for the expression estimation.
 *
 * @since 1.2
 * @author Claire Wallon
 */
public class ExpressionReducer extends Reducer<Text, LongWritable, Text, LongWritable> {

  final LongWritable outValue = new LongWritable();

  /**
   * This method allow to sum of the values of an Iterable of longs.
   *
   * @param values values to sum
   * @return the sum of the values
   */
  private static long sum(final Iterable<LongWritable> values) {

    final Iterator<LongWritable> it = values.iterator();
    long result = 0L;

    while (it.hasNext()) {
      result += it.next().get();
    }

    return result;
  }

  /**
   * 'key': annotation identifier of the feature (gene, mRNA, exon...). 'values': a list of '1', the
   * size of this list is the number of reads found on the feature.
   */
  @Override
  protected void reduce(final Text key, final Iterable<LongWritable> values, final Context context)
      throws IOException, InterruptedException {

    this.outValue.set(sum(values));
    context.write(key, this.outValue);
  }
}
