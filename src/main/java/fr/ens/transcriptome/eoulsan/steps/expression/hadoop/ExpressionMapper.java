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

import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.TOTAL_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.UNUSED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.USED_READS_COUNTER;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder.Exon;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * Mapper for Expression computation.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public class ExpressionMapper extends Mapper<LongWritable, Text, Text, Text> {

  // Parameters keys
  static final String GENOME_DESC_PATH_KEY = Globals.PARAMETER_PREFIX
      + ".expression.genome.desc.file";

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private String counterGroup;

  private final TranscriptAndExonFinder tef = new TranscriptAndExonFinder();

  private final SAMParser parser = new SAMParser();

  private final Text resultKey = new Text();
  private final Text resultValue = new Text();
  private final Map<String, Exon> oneExonByParentId =
      new HashMap<String, Exon>();

  /**
   * 'key': offset of the beginning of the line from the beginning of the
   * alignment file. 'value': the SAM record.
   */
  @Override
  public void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    final String line = value.toString();

    // Discard SAM headers
    if (line.length() > 0 && line.charAt(0) == '@')
      return;

    final SAMRecord samRecord;

    try {
      samRecord = this.parser.parseLine(line);

    } catch (SAMFormatException e) {

      context.getCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
      LOGGER.info("Invalid SAM output entry: "
          + e.getMessage() + " line='" + line + "'");
      return;
    }

    final String chr = samRecord.getReferenceName();
    final int start = samRecord.getAlignmentStart();
    final int stop = samRecord.getAlignmentEnd();

    final Set<Exon> exons = tef.findExons(chr, start, stop);

    context.getCounter(this.counterGroup, TOTAL_READS_COUNTER.counterName())
        .increment(1);
    if (exons == null) {
      context.getCounter(this.counterGroup, UNUSED_READS_COUNTER.counterName())
          .increment(1);
      return;
    }

    context.getCounter(this.counterGroup, USED_READS_COUNTER.counterName())
        .increment(1);
    int count = 1;
    final int nbExons = exons.size();

    this.oneExonByParentId.clear();

    // Sort the exon
    final List<Exon> exonSorted = Lists.newArrayList(exons);
    Collections.sort(exonSorted);

    for (Exon e : exonSorted)
      oneExonByParentId.put(e.getParentId(), e);

    for (Map.Entry<String, Exon> entry : oneExonByParentId.entrySet()) {

      final Exon e = entry.getValue();

      this.resultKey.set(e.getParentId());
      this.resultValue.set(e.getChromosome()
          + "\t" + e.getStart() + "\t" + e.getEnd() + "\t" + e.getStrand()
          + "\t" + (count++) + "\t" + nbExons + "\t" + chr + "\t" + start
          + "\t" + stop);

      context.write(this.resultKey, this.resultValue);
    }
  }

  @Override
  public void setup(final Context context) {

    try {

      final Configuration conf = context.getConfiguration();

      final Path[] localCacheFiles = DistributedCache.getLocalCacheFiles(conf);

      if (localCacheFiles == null || localCacheFiles.length == 0)
        throw new IOException("Unable to retrieve genome index");

      if (localCacheFiles.length > 1)
        throw new IOException(
            "Retrieve more than one file in distributed cache");

      LOGGER.info("Genome index compressed file (from distributed cache): "
          + localCacheFiles[0]);

      final File indexFile = new File(localCacheFiles[0].toString());
      tef.load(indexFile);

      // Counter group
      this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
      if (this.counterGroup == null) {
        throw new IOException("No counter group defined");
      }

      // Get the genome description filename
      final String genomeDescFile = conf.get(GENOME_DESC_PATH_KEY);

      if (genomeDescFile == null) {
        throw new IOException("No genome desc file set");
      }

      // Load genome description object
      final GenomeDescription genomeDescription =
          GenomeDescription.load(PathUtils.createInputStream(new Path(
              genomeDescFile), conf));

      // Set the chromosomes sizes in the parser
      this.parser.setGenomeDescription(genomeDescription);

    } catch (IOException e) {
      LOGGER.severe("Error while loading annotation data in Mapper: "
          + e.getMessage());
    }

  }

  @Override
  public void cleanup(final Context context) throws IOException {

    this.tef.clear();
    this.oneExonByParentId.clear();
  }

}
