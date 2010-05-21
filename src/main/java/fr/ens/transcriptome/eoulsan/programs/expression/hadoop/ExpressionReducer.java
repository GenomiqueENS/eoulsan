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

package fr.ens.transcriptome.eoulsan.programs.expression.hadoop;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.programs.expression.ExonsCoverage;
import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder.Transcript;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Reducer for Expression computation
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@SuppressWarnings("deprecation")
public class ExpressionReducer implements Reducer<Text, Text, Text, Text> {

  public static final String COUNTER_GROUP = "Expression";
  
  private final TranscriptAndExonFinder tef = new TranscriptAndExonFinder();
  private final ExonsCoverage geneExpr = new ExonsCoverage();
  private final String[] fields = new String[9];
  private final Text outputValue = new Text();

  @Override
  public void reduce(final Text key, Iterator<Text> values,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    geneExpr.clear();

    reporter.incrCounter(COUNTER_GROUP, "parent", 1);
    final String parentId = key.toString();

    boolean first = true;
    String chr = null;

    int count = 0;

    while (values.hasNext()) {

      count++;
      StringUtils.fastSplit(values.next().toString(), this.fields);

      final String exonChr = this.fields[0];
      final int exonStart = Integer.parseInt(this.fields[1]);
      final int exonEnd = Integer.parseInt(this.fields[2]);
      // codingStrand = Boolean.parseBoolean(this.fields[3]);

      // final int exonNumber = Integer.parseInt(this.fields[4]);
      // final int exonTotal = Integer.parseInt(this.fields[5]);

      final String alignementChr = this.fields[6];
      final int alignmentStart = Integer.parseInt(this.fields[7]);
      final int alignementEnd = Integer.parseInt(this.fields[8]);

      if (first) {
        chr = exonChr;
        first = false;
      }

      if (!exonChr.equals(alignementChr) || !chr.equals(alignementChr)) {
        reporter.incrCounter(COUNTER_GROUP, "invalid chromosome", 1);
        continue;
      }

      geneExpr.addAlignement(exonStart, exonEnd, alignmentStart, alignementEnd,
          true);
    }

    if (count == 0)
      return;

    final Transcript transcript = tef.getTranscript(parentId);

    if (transcript == null) {
      reporter
          .incrCounter(COUNTER_GROUP, "Parent Id not found in exon range", 1);
      return;
    }

    final int geneLength = transcript.getLength();
    final int notCovered = geneExpr.getNotCovered(geneLength);

    final String result = notCovered + "\t" + geneExpr.getAlignementCount();

    this.outputValue.set(result);

    collector.collect(key, this.outputValue);
  }

  @Override
  public void configure(final JobConf conf) {

    try {

      final Path indexPath =
          new Path(Parameter.getStringParameter(conf,
              ".expression.exonsindex.path", ""));
      final FileSystem fs = PathUtils.getFileSystem(indexPath, conf);
      tef.load(fs.open(indexPath));

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
