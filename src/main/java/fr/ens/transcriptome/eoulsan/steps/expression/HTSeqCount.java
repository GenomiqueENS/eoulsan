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

package fr.ens.transcriptome.eoulsan.steps.expression;

import java.io.File;
import java.io.FileWriter;
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
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class is the java implementation of the HTSeq-count program.
 * @since 1.2
 * @author Laurent Jourdren
 */
public class HTSeqCount {

  public static void countReadsInFeatures(final File samFile,
      final File gffFile, final File output, final String stranded,
      final String overlapMode, final String featureType,
      final String attributeId, final boolean quiet, final int minAverageQual,
      final File samOutFile) throws EoulsanException, IOException,
      BadBioEntryException {

    final GenomicArray<String> features = new GenomicArray<String>();
    final Map<String, Integer> counts = Utils.newHashMap();

    Writer writer = new FileWriter(output);

    boolean pairedEnd = false;

    final GFFReader gffReader = new GFFReader(gffFile);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null) {
          writer.close();
          gffReader.close();
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");
        }

        if ((stranded.equals("yes") || stranded.equals("reverse"))
            && '.' == gff.getStrand()) {
          writer.close();
          gffReader.close();
          throw new EoulsanException("Feature "
              + featureType
              + " does not have strand information but you are running "
              + "htseq-count in stranded mode.");
        }

        // Addition to the list of features of a GenomicInterval object
        // corresponding to the current annotation line
        boolean saveStrandInfo =
            "yes".equals(stranded) || "reverse".equals(stranded);
        features.addEntry(new GenomicInterval(gff, saveStrandInfo), featureId);
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

      i++;
      if (i % 1000000 == 0)
        System.out.println(i + " sam entries read.");

      // single-end mode
      if (!pairedEnd) {

        ivSeq.clear();

        // unmapped read
        if (samRecord.getReadUnmappedFlag()) {
          notaligned++;
          // System.out.println("not_aligned");
          continue;
        }

        // multiple alignment
        if (samRecord.getAttribute("NH") != null
            && samRecord.getIntegerAttribute("NH") > 1) {
          nonunique++;
          // System.out.println("non_unique");
          continue;
        }

        // too low quality
        if (samRecord.getMappingQuality() < minAverageQual) {
          lowqual++;
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
          continue;
        }

        // multiple alignment
        if ((sam1.getAttribute("NH") != null && sam1.getIntegerAttribute("NH") > 1)
            || (sam2.getAttribute("NH") != null && sam2
                .getIntegerAttribute("NH") > 1)) {
          nonunique++;
          continue;
        }

        // too low quality
        if (sam1.getMappingQuality() < minAverageQual
            || sam2.getMappingQuality() < minAverageQual) {
          lowqual++;
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
        // writer.write("no_feature\n");
        // System.out.println("no_feature");
        break;

      case 1:
        final String id = fs.iterator().next();
        counts.put(id, counts.get(id) + 1);
        // writer.write("count\n");
        // System.out.println("count");
        break;

      default:
        ambiguous++;
        // writer.write("ambiguous\n");
        // System.out.println("ambiguous");
        break;
      }

    }

    inputSam.close();

    final List<String> keysSorted = new ArrayList<String>(counts.keySet());
    Collections.sort(keysSorted);

    // Writer writer = new
    // FileWriter("/home/wallon/Bureau/TEST_HTSEQ/test-java");

    for (String key : keysSorted) {
      writer.write(key + "\t" + counts.get(key) + "\n");
    }

    writer.write("no_feature\t%" + empty + '\n');
    writer.write("ambiguous\t%d\n" + ambiguous + '\n');
    writer.write("too_low_aQual\t%d\n" + lowqual + '\n');
    writer.write("not_aligned\t%d\n" + notaligned + '\n');
    writer.write("alignment_not_unique\t%d\n" + nonunique + '\n');

    writer.close();
  }

  public static void countReadsInFeatures(final InputStream samFile,
      final File gffFile, final File output, final String stranded,
      final String overlapMode, final String featureType,
      final String attributeId, final boolean quiet, final int minAverageQual,
      final File samOutFile) throws EoulsanException, IOException,
      BadBioEntryException {

    final GenomicArray<String> features = new GenomicArray<String>();
    final Map<String, Integer> counts = Utils.newHashMap();

    Writer writer = new FileWriter(output);

    boolean pairedEnd = false;

    final GFFReader gffReader = new GFFReader(gffFile);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null) {
          writer.close();
          gffReader.close();
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");
        }

        if (stranded.equals("yes") && '.' == gff.getStrand()) {
          writer.close();
          gffReader.close();
          throw new EoulsanException("Feature "
              + featureType
              + " does not have strand information but you are running "
              + "htseq-count in stranded mode.");
        }

        // Addition to the list of features of a GenomicInterval object
        // corresponding to the current annotation line
        boolean saveStrandInfo =
            "yes".equals(stranded) || "reverse".equals(stranded);
        features.addEntry(new GenomicInterval(gff, saveStrandInfo), featureId);
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
    SAMRecord firstRecord;
    if (samIterator.hasNext())
      firstRecord = samIterator.next();
    else {
      writer.close();
      input.close();
      inputSam.close();
      throw new EoulsanException("The SAM file is empty.");
    }
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

      i++;
      if (i % 1000000 == 0)
        System.out.println(i + " sam entries read.");

      // single-end mode
      if (!pairedEnd) {

        ivSeq.clear();

        // unmapped read
        if (samRecord.getReadUnmappedFlag()) {
          notaligned++;
          continue;
        }

        // multiple alignment
        if (samRecord.getAttribute("NH") != null
            && samRecord.getIntegerAttribute("NH") > 1) {
          nonunique++;
          continue;
        }

        // too low quality
        if (samRecord.getMappingQuality() < minAverageQual) {
          lowqual++;
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
          continue;
        }

        // multiple alignment
        if ((sam1.getAttribute("NH") != null && sam1.getIntegerAttribute("NH") > 1)
            || (sam2.getAttribute("NH") != null && sam2
                .getIntegerAttribute("NH") > 1)) {
          nonunique++;
          continue;
        }

        // too low quality
        if (sam1.getMappingQuality() < minAverageQual
            || sam2.getMappingQuality() < minAverageQual) {
          lowqual++;
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
        break;

      case 1:
        final String id = fs.iterator().next();
        counts.put(id, counts.get(id) + 1);
        break;

      default:
        ambiguous++;
        break;
      }

    }

    inputSam.close();

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

        Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded.equals("yes") || stranded.equals("reverse")) {
          for (Map.Entry<GenomicInterval, String> e : intervals.entrySet()) {
            if (e.getKey().getStrand() == iv.getStrand())
              inter.put(e.getKey(), e.getValue());
          }
          intervals = inter;
        }

        if (intervals != null) {
          Collection<String> values = intervals.values();
          if (values != null) {
            fs.addAll(values);
          }
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
        if (intervals != null) {
          Collection<String> values = intervals.values();
          if (values != null) {

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
    else if ("intersection-strict".equals(mode)) {

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
        if (intervals != null) {
          Collection<String> values = intervals.values();
          if (values != null) {

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

  private static List<GenomicInterval> addIntervals(SAMRecord record,
      String stranded) {

    if (record == null)
      return null;

    List<GenomicInterval> result = new ArrayList<GenomicInterval>();

    if (!record.getReadPairedFlag()
        || (record.getReadPairedFlag() && record.getFirstOfPairFlag())) {
      if ("reverse".equals(stranded))
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '+' : '-'));
      else
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '-' : '+'));
    }

    else if (record.getReadPairedFlag() && !record.getFirstOfPairFlag()) {
      if ("reverse".equals(stranded))
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '-' : '+'));
      else
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(), record.getReadNegativeStrandFlag()
                ? '+' : '-'));
    }

    return result;
  }

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

  public static void main(String[] args) throws EoulsanException, IOException,
      BadBioEntryException {

    final File dir = new File("/home/wallon/Bureau/TEST_HTSEQ/EOULSAN");
    // final File samFile = new File(dir, "filtered_mapper_results_1.sam");
    // final File samFile = new
    // File("/home/wallon/Bureau/GSNAP/PE/500head.sam");
    final File samFile = new File(dir, "filtered_mapper_results_1.sam");
    final File gffFile = new File("/home/wallon/Bureau/DATA/annotation.gff");
    // final File gffFile = new File("/home/wallon/Bureau/GSNAP/PE/mouse.gff");
    final File output = new File(dir, "test-java-strict-yes");

    final long startTime = System.currentTimeMillis();
    System.out.println("start.");
    countReadsInFeatures(samFile, gffFile, output, "yes",
        "intersection-strict", "exon", "ID", false, 0, null);
    System.out.println("end.");
    System.out.println("Duration: "
        + (System.currentTimeMillis() - startTime) + " ms.");

    // Python
    // real 19m51.548s
    // user 19m9.692s
    // sys 0m13.385s

  }
}