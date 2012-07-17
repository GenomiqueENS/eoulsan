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
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.ELIMINATED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.UNMAPPED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.TOTAL_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.EMPTY_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.AMBIGUOUS_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.NOT_ALIGNED_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.LOW_QUAL_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.NOT_UNIQUE_ALIGNMENTS_COUNTER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * Mapper for the expression estimation with htseq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCountMapper extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  // Parameters keys
  static final String STRANDED_PARAM = Globals.PARAMETER_PREFIX
      + ".expression.stranded.parameter";
  static final String OVERLAPMODE_PARAM = Globals.PARAMETER_PREFIX
      + ".expression.overlapmode.parameter";

  private GenomicArray<String> features = new GenomicArray<String>();
  private Map<String, Integer> counts = Utils.newHashMap();

  private String counterGroup;
  private String stranded;
  private String overlapMode;

  private final SAMParser parser = new SAMParser();

  private Text outKey = new Text();
  private Text outValue = new Text();

  @Override
  public void setup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of configure()");

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
      this.parser.setGenomeDescription(genomeDescription);

      // Get the "stranded" parameter
      this.stranded = conf.get(STRANDED_PARAM);

      // Get the "overlap mode" parameter
      this.overlapMode = conf.get(OVERLAPMODE_PARAM);

    } catch (IOException e) {
      LOGGER.severe("Error while loading annotation data in Mapper: "
          + e.getMessage());
    }

    LOGGER.info("End of configure()");
  }

  @Override
  public void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    context.getCounter(this.counterGroup,
        TOTAL_ALIGNMENTS_COUNTER.counterName()).increment(1);

    final String line = value.toString();

    List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();

    String[] fields = line.split("£");

    try {

      // paired-end data
      if (line.contains("£")) {
        final SAMRecord samRecord1, samRecord2;
        samRecord1 = this.parser.parseLine(fields[0]);
        samRecord2 = this.parser.parseLine(fields[1]);

        if (!samRecord1.getReadUnmappedFlag()) {
          ivSeq.addAll(addIntervals(samRecord1, stranded));
        }

        if (!samRecord2.getReadUnmappedFlag()) {
          ivSeq.addAll(addIntervals(samRecord2, stranded));
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

        ivSeq.addAll(addIntervals(samRecord, stranded));
      }

      Set<String> fs = null;

      fs = featuresOverlapped(ivSeq, this.features, overlapMode, stranded);

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
        final String id = fs.iterator().next();
        this.outKey.set(id);
        this.outValue.set("1");
        System.err.println("key : " + outKey);
        System.err.println("value : " + outValue);
        context.write(this.outKey, this.outValue);
        break;

      default:
        context.getCounter(this.counterGroup,
            AMBIGUOUS_ALIGNMENTS_COUNTER.counterName()).increment(1);
        context.getCounter(this.counterGroup,
            ELIMINATED_READS_COUNTER.counterName()).increment(1);
        break;
      }

    } catch (SAMFormatException e) {

      context.getCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
      LOGGER.info("Invalid SAM output entry: "
          + e.getMessage() + " line='" + line + "'");
      return;
    } catch (EoulsanException e) {

      context.getCounter(this.counterGroup,
          INVALID_SAM_ENTRIES_COUNTER.counterName()).increment(1);
      LOGGER.info("Invalid SAM output entry: "
          + e.getMessage() + " line='" + line + "'");
      return;
    }

  }

  @Override
  public void cleanup(final Context context) throws IOException {

    this.features.clear();
    this.counts.clear();
  }

  /**
   * Add intervals of a SAM record that are alignment matches (thanks to the
   * CIGAR code).
   * @param record the SAM record to treat.
   * @param stranded strand to consider.
   * @return the list of intervals of the SAM record.
   */
  private static List<GenomicInterval> addIntervals(SAMRecord record,
      String stranded) {

    if (record == null)
      return null;

    List<GenomicInterval> result = new ArrayList<GenomicInterval>();

    // single-end mode or first read in the paired-end mode
    if (!record.getReadPairedFlag()
        || (record.getReadPairedFlag() && record.getFirstOfPairFlag())) {

      // the read has to be mapped to the opposite strand as the feature
      if ("reverse".equals(stranded))
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '+' : '-'));

      // stranded == "yes" (so the read has to be mapped to the same strand as
      // the feature) or stranded == "no" (so the read is considered
      // overlapping with a feature regardless of whether it is mapped to the
      // same or the opposite strand as the feature)
      else
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '-' : '+'));
    }

    // second read in the paired-end mode
    else if (record.getReadPairedFlag() && !record.getFirstOfPairFlag()) {

      // the read has to be mapped to the opposite strand as the feature
      if ("reverse".equals(stranded))
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '-' : '+'));

      // stranded == "yes" (so the read has to be mapped to the same strand as
      // the feature) or stranded == "no" (so the read is considered
      // overlapping with a feature regardless of whether it is mapped to the
      // same or the opposite strand as the feature)
      else
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '+' : '-'));
    }

    return result;
  }

  /**
   * Parse a CIGAR string to have intervals of a chromosome that are alignments
   * matches.
   * @param cigar CIGAR string to parse.
   * @param chromosome chromosome that support the alignment.
   * @param start start position of the alignment.
   * @param strand strand to consider.
   * @return the list of intervals that are alignments matches.
   */
  private static final List<GenomicInterval> parseCigar(Cigar cigar,
      final String chromosome, final int start, final char strand) {

    if (cigar == null)
      return null;

    final List<GenomicInterval> result = new ArrayList<GenomicInterval>();

    int pos = start;
    for (CigarElement ce : cigar.getCigarElements()) {

      final int len = ce.getLength();

      // the CIGAR element correspond to a mapped region
      if (ce.getOperator() == CigarOperator.M) {
        result.add(new GenomicInterval(chromosome, pos, pos + len - 1, strand));
        pos += len;
      }
      // the CIGAR element did not correspond to a mapped region
      else {
        // regions coded by a 'I' (insertion) do not have to be counted
        // (are there other cases like this one ?)
        if (pos != start && ce.getOperator() != CigarOperator.I)
          pos += len;
      }
    }

    return result;
  }

  /**
   * Determine features that overlap genomic intervals.
   * @param ivList the list of genomic intervals.
   * @param features the list of features.
   * @param mode the overlap mode.
   * @return the set of features that overlap genomic intervals according to the
   *         overlap mode.
   * @throws EoulsanException
   */
  private static Set<String> featuresOverlapped(List<GenomicInterval> ivList,
      GenomicArray<String> features, String mode, String stranded)
      throws EoulsanException {

    Set<String> fs = null;
    Map<GenomicInterval, String> inter = new HashMap<GenomicInterval, String>();

    // Overlap mode "union"
    if (mode.equals("union")) {

      fs = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlap the current interval of the read
        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded.equals("yes") || stranded.equals("reverse")) {
          for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
            if (e.getKey().getStrand() == iv.getStrand())
              inter.put(e.getKey(), e.getValue());
          }
          intervals = inter;
        }

        // At least one interval is found
        if (intervals != null && intervals.size() > 0) {
          Collection<String> values = intervals.values();
          // Add all the features that overlap the current interval to the set
          if (values != null)
            fs.addAll(values);
        }
      }
    }

    // Overlap mode "intersection-nonempty"
    else if (mode.equals("intersection-nonempty")) {

      final Set<String> featureTmp = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlap the current interval of the read
        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded.equals("yes") || stranded.equals("reverse")) {
          for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
            if (e.getKey().getStrand() == iv.getStrand())
              inter.put(e.getKey(), e.getValue());
          }
          intervals = inter;
        }

        // At least one interval is found
        if (intervals != null && intervals.size() > 0) {
          Collection<String> values = intervals.values();
          if (values != null) {

            // Determine features that correspond to the overlap mode
            for (int pos = iv.getStart(); pos <= iv.getEnd(); pos++) {

              featureTmp.clear();

              for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
                if (e.getKey().include(pos, pos))
                  featureTmp.add(e.getValue());
              }

              if (featureTmp.size() > 0) {
                if (fs == null) {
                  fs = new HashSet<String>();
                  fs.addAll(featureTmp);
                } else
                  fs.retainAll(featureTmp);
              }

            }
          }
        }
      }
    }

    // Overlap mode "intersection-strict"
    else if (mode.equals("intersection-strict")) {

      final Set<String> featureTmp = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlapped the current interval of the read
        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded.equals("yes") || stranded.equals("reverse")) {
          for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
            if (e.getKey().getStrand() == iv.getStrand())
              inter.put(e.getKey(), e.getValue());
          }
          intervals = inter;
        }

        // At least one interval is found
        if (intervals != null && intervals.size() > 0) {
          Collection<String> values = intervals.values();
          if (values != null) {

            // Determine features that correspond to the overlap mode
            for (int pos = iv.getStart(); pos <= iv.getEnd(); pos++) {

              featureTmp.clear();

              for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
                if (e.getKey().include(pos, pos)) {
                  featureTmp.add(e.getValue());
                }
              }

              if (fs == null) {
                fs = new HashSet<String>();
                fs.addAll(featureTmp);
              } else
                fs.retainAll(featureTmp);
            }
          }
        }

        // no interval found
        else {
          if (fs == null)
            fs = new HashSet<String>();
          else
            fs.clear();
        }

      }
    }

    return fs;
  }

}
