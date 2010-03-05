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

package fr.ens.transcriptome.eoulsan.hadoop.gmorse;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * This class implements the reducer for the coverage phase. It is base on
 * Gmorse source code.
 * @author Laurent Jourdren
 * @author Jean-Marc Aury
 * @author France Denoeud
 */
public class CoverageReducer extends
    Reducer<Text, IntWritable, Text, IntWritable> {

  private IntWritable outValue = new IntWritable();

  @Override
  protected void reduce(final Text key, final Iterable<IntWritable> values,
      final Context context) throws IOException, InterruptedException {

    int count = 0;

    for (IntWritable v : values)
      count += v.get();

    outValue.set(count);
    context.write(key, outValue);

  }
}
