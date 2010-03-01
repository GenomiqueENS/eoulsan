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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class SOAPCombiner extends Reducer<Text, Text, Text, Text> {

  private String unmapFilePath;

  @Override
  protected void setup(Context context) throws IOException,
      InterruptedException {

    // Get SOAP path
    this.unmapFilePath =
        context.getConfiguration().get(
            Globals.PARAMETER_PREFIX + ".soap.unmap.path");

    if (this.unmapFilePath == null)
      throw new IOException("No path for unmap file defined");

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
      System.out.println("__COUNT_UNMAP_READS__");
      int sum = 0;
      for (Text t : values)
        sum += Integer.parseInt(t.toString());

      context.write(key, new Text("" + sum));

    } else
      for (Text t : values)
        context.write(key, t);

  }
}
