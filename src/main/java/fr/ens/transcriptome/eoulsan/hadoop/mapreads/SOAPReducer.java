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

package fr.ens.transcriptome.eoulsan.hadoop.mapreads;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class SOAPReducer extends Reducer<Text, Text, Text, Text> {

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

  }

  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    final String k = key.toString();

    if ("__COUNT_MORE_ONE_LOCUS__".equals(k)) {
      System.out.println("__COUNT_MORE_ONE_LOCUS__");
      int sum = 0;
      for (Text t : values)
        sum += Integer.parseInt(t.toString());

      context.write(key, new Text("" + sum));

    } else if ("__COUNT_UNMAP_READS__".equals(k)) {

      int sum = 0;
      for (Text t : values)
        sum += Integer.parseInt(t.toString());

      context.write(key, new Text("" + sum));

    } else if ("__UNMAP_FILE__".equals(k)) {

      // Nothing to do

    } else
      for (Text t : values)
        context.write(key, t);

  }
}
