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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.AMBIGUOUS_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.ELIMINATED_READS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.EMPTY_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.LOW_QUAL_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.NOT_ALIGNED_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.NOT_UNIQUE_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters.TOTAL_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.expression.hadoop.ExpressionHadoopModule.SAM_RECORD_PAIRED_END_SERPARATOR;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicArray;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval;
import fr.ens.biologie.genomique.eoulsan.bio.SAMUtils;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqUtils;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.PathUtils;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMLineParser;
import htsjdk.samtools.SAMRecord;

/**
 * Mapper for the expression estimation with htseq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCountMapper extends Mapper<Text, Text, Text, LongWritable> {

  // Parameters keys
  static final String STRANDED_PARAM =
      Globals.PARAMETER_PREFIX + ".expression.stranded.parameter";
  static final String OVERLAP_MODE_PARAM =
      Globals.PARAMETER_PREFIX + ".expression.overlapmode.parameter";
  static final String REMOVE_AMBIGUOUS_CASES =
      Globals.PARAMETER_PREFIX + ".expression.no.ambiguous.cases";

  private final GenomicArray<String> features = new GenomicArray<>();

  private String counterGroup;
  private StrandUsage stranded;
  private OverlapMode overlapMode;
  private boolean removeAmbiguousCases;

  private final SAMLineParser parser = new SAMLineParser(new SAMFileHeader());
  private final Pattern recordSplitterPattern =
      Pattern.compile("" + SAM_RECORD_PAIRED_END_SERPARATOR);

  private final Text outKey = new Text();
  private final LongWritable outValue = new LongWritable(1L);

  @Override
  public void setup(final Context context)
      throws IOException, InterruptedException {

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

      getLogger().info("Genome index compressed file (from distributed cache): "
          + localCacheFiles[0]);

      if (localCacheFiles == null || localCacheFiles.length == 0) {
        throw new IOException("Unable to retrieve annotation index");
      }

      if (localCacheFiles.length > 1) {
        throw new IOException(
            "Retrieve more than one file in distributed cache");
      }

      // Load features
      this.features.load(
          PathUtils.createInputStream(new Path(localCacheFiles[0]), conf));

      // Counter group
      this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
      if (this.counterGroup == null) {
        throw new IOException("No counter group defined");
      }

      // Get the genome description filename
      final String genomeDescFile =
          conf.get(ExpressionHadoopModule.GENOME_DESC_PATH_KEY);

      if (genomeDescFile == null) {
        throw new IOException("No genome desc file set");
      }

      // Load genome description object
      final GenomeDescription genomeDescription = GenomeDescription
          .load(PathUtils.createInputStream(new Path(genomeDescFile), conf));

      // Set the chromosomes sizes in the parser
      this.parser.getFileHeader().setSequenceDictionary(
          SAMUtils.newSAMSequenceDictionary(genomeDescription));

      // Get the "stranded" parameter
      this.stranded =
          StrandUsage.getStrandUsageFromName(conf.get(STRANDED_PARAM));

      // Get the "overlap mode" parameter
      this.overlapMode =
          OverlapMode.getOverlapModeFromName(conf.get(OVERLAP_MODE_PARAM));

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

    final String[] fields = recordSplitterPattern.split(line);
    final List<GenomicInterval> ivSeq;

    try {

      // Add intervals
      switch (fields.length) {

      // Single end data
      case 1:
        ivSeq = createSingleEndIntervals(context, fields[0]);
        break;

      // paired end data
      case 2:
        ivSeq = addPairedEndIntervals(context, fields[0], fields[1]);
        break;

      default:
        throw new EoulsanException(
            "Invalid number of SAM record(s) found in the entry: "
                + fields.length);
      }

      incrementCounter(context, TOTAL_ALIGNMENTS_COUNTER, fields.length);

      final Set<String> fs = null2empty(HTSeqUtils.featuresOverlapped(ivSeq,
          this.features, this.overlapMode, this.stranded));

      switch (fs.size()) {
      case 0:
        incrementCounter(context, EMPTY_ALIGNMENTS_COUNTER);
        incrementCounter(context, ELIMINATED_READS_COUNTER);
        break;

      case 1:
        final String id1 = fs.iterator().next();
        this.outKey.set(id1);
        context.write(this.outKey, this.outValue);
        break;

      default:

        if (this.removeAmbiguousCases) {

          // Ambiguous case will be removed

          incrementCounter(context, AMBIGUOUS_ALIGNMENTS_COUNTER);
          incrementCounter(context, ELIMINATED_READS_COUNTER);
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

      incrementCounter(context, INVALID_SAM_ENTRIES_COUNTER);
      getLogger().info("Invalid SAM output entry: "
          + e.getMessage() + " line='" + line + "'");
    }

  }

  @Override
  public void cleanup(final Context context) throws IOException {

    this.features.clear();
  }

  //
  // Intervals creation methods
  //

  /**
   * Create single end intervals.
   * @param context Hadoop context
   * @param record the SAM record
   */
  private List<GenomicInterval> createSingleEndIntervals(final Context context,
      final String record) {

    final List<GenomicInterval> ivSeq = new ArrayList<>();
    final SAMRecord samRecord = this.parser.parseLine(record);

    // unmapped read
    if (samRecord.getReadUnmappedFlag()) {

      incrementCounter(context, NOT_ALIGNED_ALIGNMENTS_COUNTER);
      incrementCounter(context, ELIMINATED_READS_COUNTER);

      return ivSeq;
    }

    // multiple alignment
    if (samRecord.getAttribute("NH") != null
        && samRecord.getIntegerAttribute("NH") > 1) {

      incrementCounter(context, NOT_UNIQUE_ALIGNMENTS_COUNTER);
      incrementCounter(context, ELIMINATED_READS_COUNTER);

      return ivSeq;
    }

    // too low quality
    if (samRecord.getMappingQuality() < 0) {

      incrementCounter(context, LOW_QUAL_ALIGNMENTS_COUNTER);
      incrementCounter(context, ELIMINATED_READS_COUNTER);

      return ivSeq;
    }

    ivSeq.addAll(HTSeqUtils.addIntervals(samRecord, this.stranded));

    return ivSeq;
  }

  /**
   * Create paired end intervals.
   * @param context Hadoop context
   * @param record1 the SAM record of the first end
   * @param record2 the SAM record of the second end
   */
  private List<GenomicInterval> addPairedEndIntervals(final Context context,
      final String record1, final String record2) {

    final List<GenomicInterval> ivSeq = new ArrayList<>();

    final SAMRecord samRecord1 = this.parser.parseLine(record1);
    final SAMRecord samRecord2 = this.parser.parseLine(record2);

    if (!samRecord1.getReadUnmappedFlag()) {
      ivSeq.addAll(HTSeqUtils.addIntervals(samRecord1, this.stranded));
    }

    if (!samRecord2.getReadUnmappedFlag()) {
      ivSeq.addAll(HTSeqUtils.addIntervals(samRecord2, this.stranded));
    }

    // unmapped read
    if (samRecord1.getReadUnmappedFlag() && samRecord2.getReadUnmappedFlag()) {

      incrementCounter(context, NOT_ALIGNED_ALIGNMENTS_COUNTER);
      incrementCounter(context, ELIMINATED_READS_COUNTER);

      return ivSeq;
    }

    // multiple alignment
    if ((samRecord1.getAttribute("NH") != null
        && samRecord1.getIntegerAttribute("NH") > 1)
        || (samRecord2.getAttribute("NH") != null
            && samRecord2.getIntegerAttribute("NH") > 1)) {

      incrementCounter(context, NOT_UNIQUE_ALIGNMENTS_COUNTER);
      incrementCounter(context, ELIMINATED_READS_COUNTER);

      return ivSeq;
    }

    // too low quality
    if (samRecord1.getMappingQuality() < 0
        || samRecord2.getMappingQuality() < 0) {

      incrementCounter(context, LOW_QUAL_ALIGNMENTS_COUNTER);
      incrementCounter(context, ELIMINATED_READS_COUNTER);

      return ivSeq;
    }

    return ivSeq;
  }

  //
  // Other methods
  //

  /**
   * Increment an expression counter with a value of 1.
   * @param context the Hadoop context
   * @param counter the expression counter
   */
  private void incrementCounter(final Context context,
      final ExpressionCounters counter) {

    incrementCounter(context, counter, 1);
  }

  /**
   * Increment an expression counter.
   * @param context the Hadoop context
   * @param counter the expression counter
   * @param increment the increment
   */
  private void incrementCounter(final Context context,
      final ExpressionCounters counter, final int increment) {

    context.getCounter(this.counterGroup, counter.counterName())
        .increment(increment);
  }

  /**
   * Return an empty set if the parameter is null.
   * @param set the set
   * @return an empty set if the parameter is null or the original set
   */
  private static <E> Set<E> null2empty(final Set<E> set) {

    if (set == null) {
      return Collections.emptySet();
    }

    return set;
  }

}
