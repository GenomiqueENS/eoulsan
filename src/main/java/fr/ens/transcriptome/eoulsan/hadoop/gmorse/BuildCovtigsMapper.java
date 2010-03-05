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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.hadoop.Parameter;

/**
 * This class implements the mapper for the build covtigs phase. It is base on
 * Gmorse source code.
 * @author Laurent Jourdren
 * @author Jean-Marc Aury
 * @author France Denoeud
 */
public class BuildCovtigsMapper extends Mapper<LongWritable, Text, Text, Text> {

  private int threshold;
  private Text outKey = new Text();
  private Text outValue = new Text();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    final Configuration conf = context.getConfiguration();

    this.threshold =
        Parameter.getInt(conf, ".gmorse.buildcovtigs.threshold",
            "Invalid or not found buildcovtigs threshold");
  }

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    // Parse key
    final String s = value.toString();
    final int spacePos = s.indexOf(' ');
    final int tabPos = s.indexOf('\t');

    // Get chromosome
    final String chr = s.substring(0, spacePos);

    // Get position
    final int pos = Integer.parseInt(s.substring(spacePos + 1, tabPos));

    // Get coverage
    final int cov = Integer.parseInt(s.substring(tabPos + 1));

    // Filter coverage
    if (cov >= this.threshold) {

      outKey.set(chr + " " + pos);
      outValue.set(pos + " " + cov);
      context.write(outKey, outValue);
    }

  }

}
