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
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.AlignResult;

/**
 * This class implements the mapper for the coverage phase. It is base on Gmorse
 * source code.
 * @author Laurent Jourdren
 * @author Jean-Marc Aury
 * @author France Denoeud
 */
public class CoverageMapper extends
    Mapper<LongWritable, Text, Text, IntWritable> {

  private final AlignResult soapResult = new AlignResult();

  private Text outKey = new Text();
  private IntWritable outValue = new IntWritable(1);

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    final String s = value.toString();

    if (s.startsWith("__"))
      return;

    // Fill the ReadSequence object
    soapResult.parseResultLine(s);

    // Location of the read
    final int location = soapResult.getLocation();

    // Length of the read
    // final int seqLen = soapResult.getReadLength();
    final String seq = soapResult.getSequence();
    final int seqLen = seq == null ? 0 : seq.length();

    // Chromosome
    final String chr = soapResult.getChromosome();

    int i = location;
    while (i < location + seqLen) {

      outKey.set(chr + " " + i);
      context.write(this.outKey, outValue);
      i++;
    }

  }

}
