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

package fr.ens.transcriptome.eoulsan.hadoop.expression;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.AlignResult;
import fr.ens.transcriptome.eoulsan.hadoop.Parameter;
import fr.ens.transcriptome.eoulsan.hadoop.expression.GeneAndExonFinder.Exon;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

@SuppressWarnings("deprecation")
public class ExpressionMapper implements Mapper<LongWritable, Text, Text, Text> {

  private final GeneAndExonFinder ef = new GeneAndExonFinder();
  private final AlignResult ar = new AlignResult();
  private final Text resultKey = new Text();
  private final Text resultValue = new Text();

  @Override
  public void map(final LongWritable key, final Text value,
      final OutputCollector<Text, Text> output, final Reporter reporter)
      throws IOException {

    ar.parseResultLine(value.toString());
    final String chr = ar.getChromosome();
    final int start = ar.getLocation();
    final int stop = start + ar.getReadLength();

    //System.out.println(chr + "\t" + start + "\t" + stop);
    final Set<Exon> exons = ef.findExons(chr, start, stop);
    //System.out.println("Found " + (exons == null ? 0 : exons.size()) + " exons.");

    reporter.incrCounter("Expression", "read total", 1);
    if (exons == null) {
      reporter.incrCounter("Expression", "reads unused", 1);
      return;
    }

    reporter.incrCounter("Expression", "reads used", 1);
    int count = 0;
    final int nbExons = exons.size();
    for (Exon e : exons) {

      this.resultKey.set(e.getParentId());
      this.resultValue.set(e.getChromosome()
          + "\t" + e.getStart() + "\t" + e.getEnd() + "\t" + e.getStart()
          + "\t" + (++count) + "\t" + nbExons + "\t" + ar.getChromosome()
          + "\t" + ar.getLocation() + "\t"
          + (ar.getLocation() + ar.getReadLength()));

      output.collect(this.resultKey, this.resultValue);
    }
  }

  @Override
  public void configure(final JobConf conf) {

    try {

      final Path indexPath =
          new Path(Parameter.getStringParameter(conf,
              ".expression.exonsindex.path", ""));
      File indexFile = FileUtils.createFileInTempDir(indexPath.getName());
      PathUtils.copyFromPathToLocalFile(indexPath, indexFile, conf);
      ef.load(indexFile);
      indexFile.delete();

    } catch (IOException e) {
      System.out.println(e);
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void close() throws IOException {
    // TODO Auto-generated method stub

  }

}
