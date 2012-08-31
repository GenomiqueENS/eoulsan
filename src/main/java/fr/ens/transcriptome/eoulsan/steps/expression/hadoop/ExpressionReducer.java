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

import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.INVALID_CHROMOSOME_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.PARENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.PARENT_ID_NOT_FOUND_COUNTER;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.steps.expression.ExonsCoverage;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder.Transcript;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Reducer for Expression computation.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public class ExpressionReducer extends Reducer<Text, Text, Text, Text> {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private String counterGroup;
  private final TranscriptAndExonFinder tef = new TranscriptAndExonFinder();
  private final ExonsCoverage geneExpr = new ExonsCoverage();
  private final String[] fields = new String[9];
  private final Text outputValue = new Text();

  /**
   * 'key': annotation identifier of the feature (exon). 'values': a list of
   * '1', the size of this list is the number of reads found on the feature.
   */
  @Override
  public void reduce(final Text key, Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    geneExpr.clear();

    context.getCounter(this.counterGroup, PARENTS_COUNTER.counterName())
        .increment(1);
    final String parentId = key.toString();

    boolean first = true;
    String chr = null;

    int count = 0;

    final Iterator<Text> it = values.iterator();

    while (it.hasNext()) {

      count++;
      StringUtils.fastSplit(it.next().toString(), this.fields);

      final String exonChr = this.fields[0];
      final int exonStart = Integer.parseInt(this.fields[1]);
      final int exonEnd = Integer.parseInt(this.fields[2]);

      final String alignementChr = this.fields[6];
      final int alignmentStart = Integer.parseInt(this.fields[7]);
      final int alignementEnd = Integer.parseInt(this.fields[8]);

      if (first) {
        chr = exonChr;
        first = false;
      }

      if (!exonChr.equals(alignementChr) || !chr.equals(alignementChr)) {
        context.getCounter(this.counterGroup,
            INVALID_CHROMOSOME_COUNTER.counterName()).increment(1);
        continue;
      }

      geneExpr.addAlignement(exonStart, exonEnd, alignmentStart, alignementEnd,
          true);
    }

    if (count == 0)
      return;

    final Transcript transcript = tef.getTranscript(parentId);

    if (transcript == null) {
      context.getCounter(this.counterGroup,
          PARENT_ID_NOT_FOUND_COUNTER.counterName()).increment(1);

      return;
    }

    final int geneLength = transcript.getLength();
    final int notCovered = geneExpr.getNotCovered(geneLength);

    final String result = notCovered + "\t" + geneExpr.getAlignementCount();

    this.outputValue.set(result);

    context.write(key, this.outputValue);
  }

  @Override
  public void setup(final Context context) throws IOException {

    // Counter group
    this.counterGroup =
        context.getConfiguration().get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    try {

      final Path[] localCacheFiles =
          DistributedCache.getLocalCacheFiles(context.getConfiguration());

      if (localCacheFiles == null || localCacheFiles.length == 0)
        throw new IOException("Unable to retrieve genome index");

      if (localCacheFiles.length > 1)
        throw new IOException(
            "Retrieve more than one file in distributed cache");

      logger.info("Genome index compressed file (from distributed cache): "
          + localCacheFiles[0]);

      final File indexFile = new File(localCacheFiles[0].toString());
      tef.load(indexFile);

    } catch (IOException e) {

      logger.severe("Error while loading annotation data in Reducer: "
          + e.getMessage());
    }

  }

  @Override
  public void cleanup(final Context context) throws IOException {
  }

}
