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

package fr.ens.transcriptome.eoulsan.steps.expression.hadoop;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder.Exon;

/**
 * Mapper for Expression computation
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@SuppressWarnings("deprecation")
public class ExpressionMapper implements Mapper<LongWritable, Text, Text, Text> {

  public static final String COUNTER_GROUP = "Expression";

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private final TranscriptAndExonFinder tef = new TranscriptAndExonFinder();
  private final AlignResult ar = new AlignResult();
  private final Text resultKey = new Text();
  private final Text resultValue = new Text();
  private final Map<String, Exon> oneExonByParentId =
      new HashMap<String, Exon>();

  @Override
  public void map(final LongWritable key, final Text value,
      final OutputCollector<Text, Text> output, final Reporter reporter)
      throws IOException {

    ar.parseResultLine(value.toString());
    final String chr = ar.getChromosome();
    final int start = ar.getLocation();
    final int stop = start + ar.getReadLength();

    final Set<Exon> exons = tef.findExons(chr, start, stop);

    reporter.incrCounter(COUNTER_GROUP, "read total", 1);
    if (exons == null) {
      reporter.incrCounter(COUNTER_GROUP, "reads unused", 1);
      return;
    }

    reporter.incrCounter(COUNTER_GROUP, "reads used", 1);
    int count = 1;
    final int nbExons = exons.size();

    this.oneExonByParentId.clear();

    for (Exon e : exons)
      oneExonByParentId.put(e.getParentId(), e);

    for (Map.Entry<String, Exon> entry : oneExonByParentId.entrySet()) {

      final Exon e = entry.getValue();

      this.resultKey.set(e.getParentId());
      this.resultValue.set(e.getChromosome()
          + "\t" + e.getStart() + "\t" + e.getEnd() + "\t" + e.getStrand()
          + "\t" + (count++) + "\t" + nbExons + "\t" + ar.getChromosome()
          + "\t" + ar.getLocation() + "\t"
          + (ar.getLocation() + ar.getReadLength()));

      output.collect(this.resultKey, this.resultValue);
    }
  }

  @Override
  public void configure(final JobConf conf) {

    try {

      final Path[] localCacheFiles = DistributedCache.getLocalCacheFiles(conf);

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
      System.err.println(e);
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Override
  public void close() throws IOException {

    this.tef.clear();
    this.oneExonByParentId.clear();
  }

}
