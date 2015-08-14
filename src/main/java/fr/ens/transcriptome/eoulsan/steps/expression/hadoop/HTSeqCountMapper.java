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
import static fr.ens.transcriptome.eoulsan.steps.expression.hadoop.ExpressionHadoopStep.SAM_RECORD_PAIRED_END_SERPARATOR;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.SAMUtils;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.HTSeqUtils;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * Mapper for the expression estimation with htseq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCountMapper extends Mapper<Text, Text, Text, LongWritable> {

  // Parameters keys
  static final String STRANDED_PARAM = Globals.PARAMETER_PREFIX
      + ".expression.stranded.parameter";
  static final String OVERLAPMODE_PARAM = Globals.PARAMETER_PREFIX
      + ".expression.overlapmode.parameter";
  static final String REMOVE_AMBIGUOUS_CASES = Globals.PARAMETER_PREFIX
      + ".expression.no.ambiguous.cases";

  private final GenomicArray<String> features = new GenomicArray<>();
  private final Map<String, Integer> counts = new HashMap<>();

  private String counterGroup;
  private StrandUsage stranded;
  private OverlapMode overlapMode;
  private boolean removeAmbiguousCases;

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());

  private final Text outKey = new Text();
  private final LongWritable outValue = new LongWritable(1L);

  @Override
  public void setup(final Context context) throws IOException,
      InterruptedException {

    EoulsanLogger.initConsoleHandler();
    getLogger().info("Start of setup()");

    try {

      final Configuration conf = context.getConfiguration();

      final URI[] localCacheFiles = context.getCacheFiles();

      if (localCacheFiles == null || localCacheFiles.length == 0) {
        throw new IOException("Unable to retrieve genome index");
      }

      if (localCacheFiles.length > 1) {
        throw new IOException(
            "Retrieve more than one file in distributed cache");
      }

      getLogger().info(
          "Genome index compressed file (from distributed cache): "
              + localCacheFiles[0]);

      if (localCacheFiles == null || localCacheFiles.length == 0) {
        throw new IOException("Unable to retrieve annotation index");
      }

      if (localCacheFiles.length > 1) {
        throw new IOException(
            "Retrieve more than one file in distributed cache");
      }

      // Load features
      this.features.load(PathUtils.createInputStream(new Path(
          localCacheFiles[0]), conf));

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

    getLogger().info("End of setup()");
  }

  /**
   * 'key': offset of the beginning of the line from the beginning of the
   * alignment file. 'value': the SAM record, if data are in paired-end mode,
   * 'value' contains the two paired alignments separated by a '£' (TSAM
   * format).
   */
  @Override
  public void map(final Text key, final Text value, final Context context)
      throws IOException, InterruptedException {

    final String line = value.toString();

    // Discard SAM headers
    if (line.length() > 0 && line.charAt(0) == '@') {
      return;
    }

    context.getCounter(this.counterGroup,
        TOTAL_ALIGNMENTS_COUNTER.counterName()).increment(1);

    List<GenomicInterval> ivSeq = new ArrayList<>();

    final String[] fields = line.split("" + SAM_RECORD_PAIRED_END_SERPARATOR);
    System.out.println("fields length: " + fields.length);
    try {

      // paired-end data
      if (fields.length > 1) {
        final SAMRecord samRecord1, samRecord2;
        samRecord1 = this.parser.parseLine(fields[0]);
        samRecord2 = this.parser.parseLine(fields[1]);

        if (!samRecord1.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(samRecord1, this.stranded));
        }

        if (!samRecord2.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(samRecord2, this.stranded));
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

        ivSeq.addAll(HTSeqUtils.addIntervals(samRecord, this.stranded));
      }

      Set<String> fs = null;

      fs =
          HTSeqUtils.featuresOverlapped(ivSeq, this.features, this.overlapMode,
              this.stranded);

      if (fs == null) {
        fs = new HashSet<>();
      }

      switch (fs.size()) {
      case 0:
        context.getCounter(this.counterGroup,
            EMPTY_ALIGNMENTS_COUNTER.counterName()).increment(1);
        context.getCounter(this.counterGroup,
            ELIMINATED_READS_COUNTER.counterName()).increment(1);
        break;

      case 1:
        final String id1 = fs.iterator().next();
        this.outKey.set(id1);
        context.write(this.outKey, this.outValue);
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
            context.write(this.outKey, this.outValue);
          }
        }
        break;
      }

    } catch (SAMFormatException | EoulsanException e) {

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
