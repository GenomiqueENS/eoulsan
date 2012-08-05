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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a wrapper on the HTSeq-count counter.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCounter extends AbstractExpressionCounter {

  private static final String COUNTER_NAME = "htseq-count";

  @Override
  public String getCounterName() {

    return COUNTER_NAME;
  }

  @Override
  protected void internalCount(File alignmentFile, DataFile annotationFile,
      File expressionFile, final DataFile GenomeDescFile, Reporter reporter,
      String counterGroup) throws IOException {

    try {

      countReadsInFeatures(alignmentFile, annotationFile.open(),
          expressionFile, getStranded(), getOverlapMode(), getGenomicType(),
          "ID", false, 0, null, reporter, counterGroup);

    } catch (EoulsanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (BadBioEntryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * Count the number of alignments for all the features of the annotation file.
   * @param samFile SAM file that contains alignments.
   * @param gffFile annotation file.
   * @param outFile output file.
   * @param stranded strand to consider.
   * @param overlapMode overlap mode to consider.
   * @param featureType annotation feature type to consider.
   * @param attributeId annotation attribute id to consider.
   * @param quiet if true : suppress progress report and warnings.
   * @param minAverageQual minimum value for alignment quality.
   * @param samOutFile output SAM file annotating each line with its assignment
   *          to a feature or a special counter (as an optional field with tag
   *          'XF').
   * @param reporter Reporter object.
   * @param counterGroup counter group for the Reporter object.
   * @throws EoulsanException
   * @throws IOException
   * @throws BadBioEntryException
   */
  private static void countReadsInFeatures(final File samFile,
      final InputStream gffFile, final File outFile,
      final StrandUsage stranded, final OverlapMode overlapMode,
      final String featureType, final String attributeId, final boolean quiet,
      final int minAverageQual, final File samOutFile, Reporter reporter,
      String counterGroup) throws EoulsanException, IOException,
      BadBioEntryException {

    final GenomicArray<String> features = new GenomicArray<String>();
    final Map<String, Integer> counts = Utils.newHashMap();

    final Writer writer = FileUtils.createBufferedWriter(outFile);

    boolean pairedEnd = false;

    // features = readAnnotation(gffFile, stranded, featureType, attributeId);

    final GFFReader gffReader = new GFFReader(gffFile);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null) {
          gffReader.close();
          writer.close();
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");
        }

        if ((stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE)
            && '.' == gff.getStrand()) {
          gffReader.close();
          writer.close();
          throw new EoulsanException("Feature "
              + featureType
              + " does not have strand information but you are running "
              + "htseq-count in stranded mode.");
        }

        // Addition to the list of features of a GenomicInterval object
        // corresponding to the current annotation line
        features.addEntry(
            new GenomicInterval(gff, stranded.isSaveStrandInfo()), featureId);
        counts.put(featureId, 0);
      }
    }
    gffReader.throwException();
    gffReader.close();

    if (counts.size() == 0) {
      writer.close();
      throw new EoulsanException("Warning: No features of type '"
          + featureType + "' found.\n");
    }

    List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();

    final SAMFileReader inputSam = new SAMFileReader(samFile);

    // paired-end mode ?
    final SAMFileReader input = new SAMFileReader(samFile);
    SAMRecordIterator samIterator = input.iterator();
    SAMRecord firstRecord = samIterator.next();
    if (firstRecord.getReadPairedFlag())
      pairedEnd = true;
    input.close();

    int empty = 0;
    int ambiguous = 0;
    int notaligned = 0;
    int lowqual = 0;
    int nonunique = 0;
    int i = 0;
    SAMRecord sam1 = null, sam2 = null;

    // Read the SAM file
    for (final SAMRecord samRecord : inputSam) {

      reporter.incrCounter(counterGroup,
          ExpressionCounters.TOTAL_ALIGNMENTS_COUNTER.counterName(), 1);

      i++;
      if (i % 1000000 == 0)
        System.out.println(i + " sam entries read.");

      // single-end mode
      if (!pairedEnd) {

        ivSeq.clear();

        // unmapped read
        if (samRecord.getReadUnmappedFlag()) {
          notaligned++;
          reporter.incrCounter(counterGroup,
              ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
          continue;
        }

        // multiple alignment
        if (samRecord.getAttribute("NH") != null
            && samRecord.getIntegerAttribute("NH") > 1) {
          nonunique++;
          reporter.incrCounter(counterGroup,
              ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
          continue;
        }

        // too low quality
        if (samRecord.getMappingQuality() < minAverageQual) {
          lowqual++;
          reporter.incrCounter(counterGroup,
              ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
          continue;
        }

        ivSeq.addAll(addIntervals(samRecord, stranded));

      }

      // paired-end mode
      else {

        if (sam1 != null && sam2 != null) {
          sam1 = null;
          sam2 = null;
          ivSeq.clear();
        }

        if (samRecord.getFirstOfPairFlag())
          sam1 = samRecord;
        else
          sam2 = samRecord;

        if (sam1 == null || sam2 == null)
          continue;

        if (!sam1.getReadName().equals(sam2.getReadName())) {
          sam1 = sam2;
          sam2 = null;
          continue;
        }

        if (sam1 != null && !sam1.getReadUnmappedFlag()) {
          ivSeq.addAll(addIntervals(sam1, stranded));
        }

        if (sam2 != null && !sam2.getReadUnmappedFlag()) {
          ivSeq.addAll(addIntervals(sam2, stranded));
        }

        // unmapped read
        if (sam1.getReadUnmappedFlag() && sam2.getReadUnmappedFlag()) {
          notaligned++;
          reporter.incrCounter(counterGroup,
              ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
          continue;
        }

        // multiple alignment
        if ((sam1.getAttribute("NH") != null && sam1.getIntegerAttribute("NH") > 1)
            || (sam2.getAttribute("NH") != null && sam2
                .getIntegerAttribute("NH") > 1)) {
          nonunique++;
          reporter.incrCounter(counterGroup,
              ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
          continue;
        }

        // too low quality
        if (sam1.getMappingQuality() < minAverageQual
            || sam2.getMappingQuality() < minAverageQual) {
          lowqual++;
          reporter.incrCounter(counterGroup,
              ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
          continue;
        }

      }

      Set<String> fs = null;

      fs = featuresOverlapped(ivSeq, features, overlapMode, stranded);

      if (fs == null)
        fs = new HashSet<String>();

      switch (fs.size()) {
      case 0:
        empty++;
        reporter.incrCounter(counterGroup,
            ExpressionCounters.UNMAPPED_READS_COUNTER.counterName(), 1);
        break;

      case 1:
        final String id = fs.iterator().next();
        counts.put(id, counts.get(id) + 1);
        break;

      default:
        ambiguous++;
        reporter.incrCounter(counterGroup,
            ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), 1);
        break;
      }

    }

    inputSam.close();

    // Write results
    final List<String> keysSorted = new ArrayList<String>(counts.keySet());
    Collections.sort(keysSorted);

    for (String key : keysSorted) {
      writer.write(key + "\t" + counts.get(key) + "\n");
    }

    writer.write("no_feature\t" + empty + '\n');
    writer.write("ambiguous\t" + ambiguous + '\n');
    writer.write("too_low_aQual\t" + lowqual + '\n');
    writer.write("not_aligned\t" + notaligned + '\n');
    writer.write("alignment_not_unique\t" + nonunique + '\n');

    writer.close();
  }

  // private static final GenomicArray<String> readAnnotation(
  // final InputStream gffFile, final String stranded,
  // final String featureType, final String attributeId) {
  //
  // final GenomicArray<String> features = new GenomicArray<String>();
  // final GFFReader gffReader = new GFFReader(gffFile);
  //
  // // Read the annotation file
  // for (final GFFEntry gff : gffReader) {
  //
  // if (featureType.equals(gff.getType())) {
  //
  // final String featureId = gff.getAttributeValue(attributeId);
  // if (featureId == null)
  // throw new EoulsanException("Feature "
  // + featureType + " does not contain a " + attributeId
  // + " attribute");
  //
  // if (stranded.equals("yes") && '.' == gff.getStrand())
  // throw new EoulsanException("Feature "
  // + featureType
  // + " does not have strand information but you are running "
  // + "htseq-count in stranded mode.");
  //
  // // Addition to the list of features of a GenomicInterval object
  // // corresponding to the current annotation line
  // features.addEntry(new GenomicInterval(gff, stranded), featureId);
  // counts.put(featureId, 0);
  // }
  // }
  // gffReader.throwException();
  // gffReader.close();
  //
  // return features;
  // }

  /**
   * Add intervals of a SAM record that are alignment matches.
   * @param record the SAM record to treat.
   * @param stranded strand to consider.
   * @return the list of intervals of the SAM record.
   */
  private static List<GenomicInterval> addIntervals(final SAMRecord record,
      final StrandUsage stranded) {

    if (record == null)
      return null;

    List<GenomicInterval> result = new ArrayList<GenomicInterval>();

    // single-end mode or first read in the paired-end mode
    if (!record.getReadPairedFlag()
        || (record.getReadPairedFlag() && record.getFirstOfPairFlag())) {

      // the read has to be mapped to the opposite strand as the feature
      if (stranded == StrandUsage.REVERSE)
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
      if (stranded == StrandUsage.REVERSE)
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
      GenomicArray<String> features, OverlapMode mode, StrandUsage stranded)
      throws EoulsanException {

    Set<String> fs = null;
    Map<GenomicInterval, String> inter = new HashMap<GenomicInterval, String>();

    // Overlap mode "union"
    if (mode == OverlapMode.UNION) {

      fs = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlap the current interval of the read
        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE) {
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
    else if (mode == OverlapMode.INTERSECTION_NONEMPTY) {

      final Set<String> featureTmp = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlap the current interval of the read
        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE) {
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
    else if (mode == OverlapMode.INTERSECTION_STRICT) {

      final Set<String> featureTmp = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlapped the current interval of the read
        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        // LOGGER.info("intervals size before : "+intervals.size());

        if (stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE) {
          for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
            if (e.getKey().getStrand() == iv.getStrand())
              inter.put(e.getKey(), e.getValue());
          }
          intervals = inter;
        }

        // LOGGER.info("intervals size after : "+intervals.size());

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

  public static void main(String[] args) throws EoulsanException, IOException,
      BadBioEntryException {

    final File dir = new File("/home/wallon/Bureau/TEST_HTSEQ/EOULSAN");
    // final File samFile = new File(dir, "filtered_mapper_results_1.sam");
    // final File samFile = new
    // File("/home/wallon/Bureau/GSNAP/PE/500head.sam");
    final File samFile = new File(dir, "filtered_mapper_results_1.sam");
    final FileInputStream gffFile =
        new FileInputStream(new File("/home/wallon/Bureau/DATA/annotation.gff"));
    // final File gffFile = new File("/home/wallon/Bureau/GSNAP/PE/mouse.gff");
    final File output = new File(dir, "counter-test-java-strict-yes");
    Reporter reporter = new Reporter();
    String counterGroup = "expression-test";

    final long startTime = System.currentTimeMillis();
    System.out.println("start.");
    countReadsInFeatures(samFile, gffFile, output, StrandUsage.YES,
        OverlapMode.INTERSECTION_STRICT, "exon", "ID", false, 0, null,
        reporter, counterGroup);
    System.out.println("end.");
    System.out.println("Duration: "
        + (System.currentTimeMillis() - startTime) + " ms.");

  }

}
