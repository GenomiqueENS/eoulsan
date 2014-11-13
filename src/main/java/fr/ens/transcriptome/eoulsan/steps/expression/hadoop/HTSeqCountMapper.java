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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.AMBIGUOUS_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.ELIMINATED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.EMPTY_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.LOW_QUAL_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.NOT_ALIGNED_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.NOT_UNIQUE_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.TOTAL_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.UNMAPPED_READS_COUNTER;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMLineParser;
import net.sf.samtools.SAMRecord;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.SAMUtils;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.HTSeqUtils;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.util.Utils;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * Mapper for the expression estimation with htseq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCountMapper extends Mapper<LongWritable, Text, Text, Long> {

  // Parameters keys
  static final String STRANDED_PARAM = Globals.PARAMETER_PREFIX
      + ".expression.stranded.parameter";
  static final String OVERLAPMODE_PARAM = Globals.PARAMETER_PREFIX
      + ".expression.overlapmode.parameter";
  static final String REMOVE_AMBIGUOUS_CASES = Globals.PARAMETER_PREFIX
      + ".expression.no.ambiguous.cases";

  private GenomicArray<String> features = new GenomicArray<String>();
  private Map<String, Integer> counts = Utils.newHashMap();

  private String counterGroup;
  private StrandUsage stranded;
  private OverlapMode overlapMode;
  private boolean removeAmbiguousCases;

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());

  private Text outKey = new Text();

  @Override
  public void setup(final Context context) throws IOException,
      InterruptedException {

    getLogger().info("Start of configure()");

    try {

      final Configuration conf = context.getConfiguration();

      final URI[] localCacheFiles = context.getCacheFiles();

      if (localCacheFiles == null || localCacheFiles.length == 0)
        throw new IOException("Unable to retrieve genome index");

      if (localCacheFiles.length > 1)
        throw new IOException(
            "Retrieve more than one file in distributed cache");

      getLogger().info(
          "Genome index compressed file (from distributed cache): "
              + localCacheFiles[0]);

      final File indexFile = new File(localCacheFiles[0]);
      this.features.load(indexFile);

      // Counter group
      this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
      if (this.counterGroup == null) {
        throw new IOException("No counter group defined");
      }

      // Get the genome description filename
      final String genomeDescFile =
          conf.get(ExpressionMapper.GENOME_DESC_PATH_KEY);

      if (genomeDescFile == null) {
        throw new IOException("No genome desc file set");
      }

      // Load genome description object
      final GenomeDescription genomeDescription =
          GenomeDescription.load(PathUtils.createInputStream(new Path(
              genomeDescFile), conf));

      // Set the chromosomes sizes in the parser
      this.parser.getFileHeader().setSequenceDictionary(
          SAMUtils.newSAMSequenceDictionary(genomeDescription));

      // Get the "stranded" parameter
      this.stranded =
          StrandUsage.getStrandUsageFromName(conf.get(STRANDED_PARAM));

      // Get the "overlap mode" parameter
      this.overlapMode =
          OverlapMode.getOverlapModeFromName(conf.get(OVERLAPMODE_PARAM));

      // Get the "no ambiguous cases" parameter
      this.removeAmbiguousCases = conf.getBoolean(REMOVE_AMBIGUOUS_CASES, true);

    } catch (IOException e) {
      getLogger().severe(
          "Error while loading annotation data in Mapper: " + e.getMessage());
    }

    getLogger().info("End of configure()");
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the
   * alignment file. 'value': the SAM record, if data are in paired-end mode,
   * 'value' contains the two paired alignments separated by a '£' (TSAM
   * format).
   */
  @Override
  public void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    final String line = value.toString();

    // Discard SAM headers
    if (line.length() > 0 && line.charAt(0) == '@')
      return;

    context.getCounter(this.counterGroup,
        TOTAL_ALIGNMENTS_COUNTER.counterName()).increment(1);

    List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();

    String[] fields = line.split("£");

    try {

      // paired-end data
      if (line.contains("£")) {
        final SAMRecord samRecord1, samRecord2;
        samRecord1 = this.parser.parseLine(fields[0]);
        samRecord2 = this.parser.parseLine(fields[1]);

        if (!samRecord1.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(samRecord1, stranded));
        }

        if (!samRecord2.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(samRecord2, stranded));
        }

        // unmapped read
        if (samRecord1.getReadUnmappedFlag()
            && samRecord2.getReadUnmappedFlag()) {
          context.getCounter(this.counterGroup,
              NOT_ALIGNED_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
          return;
        }

        // multiple alignment
        if ((samRecord1.getAttribute("NH") != null && samRecord1
            .getIntegerAttribute("NH") > 1)
            || (samRecord2.getAttribute("NH") != null && samRecord2
                .getIntegerAttribute("NH") > 1)) {
          context.getCounter(this.counterGroup,
              NOT_UNIQUE_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
          return;
        }

        // too low quality
        if (samRecord1.getMappingQuality() < 0
            || samRecord2.getMappingQuality() < 0) {
          context.getCounter(this.counterGroup,
              LOW_QUAL_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
          return;
        }

      }
      // single-end data
      else {
        final SAMRecord samRecord;
        samRecord = this.parser.parseLine(line);

        // unmapped read
        if (samRecord.getReadUnmappedFlag()) {
          context.getCounter(this.counterGroup,
              NOT_ALIGNED_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
          return;
        }

        // multiple alignment
        if (samRecord.getAttribute("NH") != null
            && samRecord.getIntegerAttribute("NH") > 1) {
          context.getCounter(this.counterGroup,
              NOT_UNIQUE_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
          return;
        }

        // too low quality
        if (samRecord.getMappingQuality() < 0) {
          context.getCounter(this.counterGroup,
              LOW_QUAL_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
          return;
        }

        ivSeq.addAll(HTSeqUtils.addIntervals(samRecord, stranded));
      }

      Set<String> fs = null;

      fs =
          HTSeqUtils.featuresOverlapped(ivSeq, this.features, this.overlapMode,
              this.stranded);

      if (fs == null)
        fs = new HashSet<String>();

      switch (fs.size()) {
      case 0:
        context.getCounter(this.counterGroup,
            EMPTY_ALIGNMENTS_COUNTER.counterName()).increment(1);
        context.getCounter(this.counterGroup,
            UNMAPPED_READS_COUNTER.counterName()).increment(1);
        break;

      case 1:
        final String id1 = fs.iterator().next();
        this.outKey.set(id1);
        context.write(this.outKey, 1L);
        break;

      default:

        if (this.removeAmbiguousCases) {

          // Ambiguous case will be removed

          context.getCounter(this.counterGroup,
              AMBIGUOUS_ALIGNMENTS_COUNTER.counterName()).increment(1);
          context.getCounter(this.counterGroup,
              ELIMINATED_READS_COUNTER.counterName()).increment(1);
        } else {

          // Ambiguous case will be used in the count

          for (String id2 : fs) {
            this.outKey.set(id2);
            context.write(this.outKey, 1L);
          }
        }
        break;
      }

    } catch (SAMFormatException e) {

      context.getCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
      getLogger().info(
          "Invalid SAM output entry: "
              + e.getMessage() + " line='" + line + "'");
      return;
    } catch (EoulsanException e) {

      context.getCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
      getLogger().info(
          "Invalid SAM output entry: "
              + e.getMessage() + " line='" + line + "'");
      return;
    }

  }

  @Override
  public void cleanup(final Context context) throws IOException {

    this.features.clear();
    this.counts.clear();
  }

}
