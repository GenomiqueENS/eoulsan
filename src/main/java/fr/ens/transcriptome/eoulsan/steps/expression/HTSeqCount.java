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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
      final File gffFile, final String stranded, final String overlapMode,
      final String featureType, final String attributeId, final boolean quiet,
      final int minAverageQual, final File samOutFile) throws EoulsanException,
      IOException, BadBioEntryException {

    final GenomicArray<String> features = new GenomicArray<String>();
    final Map<String, Integer> counts = Utils.newHashMap();

    // !!!!!!!!!!!!!!!!! à supprimer
    Writer writer = new FileWriter("/home/wallon/Bureau/TEST_HTSEQ/EOULSAN/test-apres");

    boolean pairedEnd = false;

    final GFFReader gffReader = new GFFReader(gffFile);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null)
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");

        if (stranded.equals("yes") && '.' == gff.getStrand())
          throw new EoulsanException("Feature "
              + featureType
              + " does not have strand information but you are running "
              + "htseq-count in stranded mode.");

        // Addition to the list of features of a GenomicInterval object
        // corresponding to the current annotation line
        features.addEntry(new GenomicInterval(gff, stranded), featureId);
        counts.put(featureId, 0);
      }
    }
    gffReader.throwException();
    gffReader.close();

    if (counts.size() == 0)
      throw new EoulsanException("Warning: No features of type '"
          + featureType + "' found.\n");

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

//        // the read has to be mapped to the opposite strand as the feature
//        if ("reverse".equals(stranded)) {
//          ivSeq.addAll(parseCigar(samRecord.getCigar(),
//              samRecord.getReferenceName(), samRecord.getAlignmentStart(),
//              samRecord.getReadNegativeStrandFlag() ? '+' : '-'));
//        }
//
//        // stranded == "true" (so the read has to be mapped to the same strand
//        // as
//        // the feature) or stranded == "false" (so the read is considered
//        // overlapping with a feature regardless of whether it is mapped to the
//        // same or the opposite strand as the feature)
//        else {
//          ivSeq.addAll(parseCigar(samRecord.getCigar(),
//              samRecord.getReferenceName(), samRecord.getAlignmentStart(),
//              samRecord.getReadNegativeStrandFlag() ? '-' : '+'));
//        }

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
          // System.out.println("sam1");
//          if ("reverse".equals(stranded))
//            ivSeq.addAll(parseCigar(sam1.getCigar(), sam1.getReferenceName(),
//                sam1.getAlignmentStart(), sam1.getReadNegativeStrandFlag()
//                    ? '+' : '-'));
//          else
//            ivSeq.addAll(parseCigar(sam1.getCigar(), sam1.getReferenceName(),
//                sam1.getAlignmentStart(), sam1.getReadNegativeStrandFlag()
//                    ? '-' : '+'));
        }

        if (sam2 != null && !sam2.getReadUnmappedFlag()) {
          ivSeq.addAll(addIntervals(sam2, stranded));
          // System.out.println("sam2");
//          if ("reverse".equals(stranded))
//            ivSeq.addAll(parseCigar(sam2.getCigar(), sam2.getReferenceName(),
//                sam2.getAlignmentStart(), sam2.getReadNegativeStrandFlag()
//                    ? '-' : '+'));
//          else
//            ivSeq.addAll(parseCigar(sam2.getCigar(), sam2.getReferenceName(),
//                sam2.getAlignmentStart(), sam2.getReadNegativeStrandFlag()
//                    ? '+' : '-'));
        }

        // unmapped read
        if (sam1.getReadUnmappedFlag() && sam2.getReadUnmappedFlag()) {
          notaligned++;
          // System.out.println("not_aligned");
          continue;
        }

        // multiple alignment
        if ((sam1.getAttribute("NH") != null && sam1.getIntegerAttribute("NH") > 1)
            || (sam2.getAttribute("NH") != null && sam2
                .getIntegerAttribute("NH") > 1)) {
          nonunique++;
          // System.out.println("non_unique");
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
      
      fs = featuresOverlapped(ivSeq, features, overlapMode);

//      // Overlap mode "union"
//      if (overlapMode.equals("union")) {
//
//        fs = new HashSet<String>();
//
//        for (final GenomicInterval iv : ivSeq) {
//
//          // writer.write(iv.getChromosome()
//          // + ":[" + (iv.getStart() - 1) + "," + iv.getEnd() + ")/"
//          // + iv.getStrand() + "\n");
//
//          final String chr = iv.getChromosome();
//
//          if (!features.containsChromosome(chr))
//            throw new EoulsanException("Unknown chromosome: " + chr);
//
//          final Map<GenomicInterval, String> intervals =
//              features.getEntries(chr, iv.getStart(), iv.getEnd());
//
//          if (intervals != null) {
//            Collection<String> values = intervals.values();
//            if (values != null)
//              fs.addAll(values);
//          }
//        }
//      }
//
//      // Overlap mode "intersection-nonempty"
//      else if (overlapMode.equals("intersection-nonempty")) {
//
//        final Set<String> featureTmp = new HashSet<String>();
//
//        for (final GenomicInterval iv : ivSeq) {
//
//          // writer.write(iv.getChromosome()
//          // + ":[" + (iv.getStart() - 1) + "," + iv.getEnd() + ")/"
//          // + iv.getStrand() + "\n");
//
//          final String chr = iv.getChromosome();
//
//          if (!features.containsChromosome(chr))
//            throw new EoulsanException("Unknown chromosome: " + chr);
//
//          // Get features that overlapped the current interval of the read
//          final Map<GenomicInterval, String> intervals =
//              features.getEntries(chr, iv.getStart(), iv.getEnd());
//
//          // At least one interval is found
//          if (intervals != null) {
//            Collection<String> values = intervals.values();
//            if (values != null) {
//
//              for (int pos = iv.getStart(); pos <= iv.getEnd(); pos++) {
//
//                featureTmp.clear();
//
//                for (Map.Entry<GenomicInterval, String> inter : intervals
//                    .entrySet()) {
//                  if (inter.getKey().include(pos, pos))
//                    featureTmp.add(inter.getValue());
//                }
//
//                if (featureTmp.size() > 0) {
//                  if (fs == null) {
//                    fs = new HashSet<String>();
//                    fs.addAll(featureTmp);
//                  } else
//                    fs.retainAll(featureTmp);
//                }
//
//              }
//            }
//          }
//        }
//      }
//
//      else if (overlapMode == "intersection-strict") {
//
//        final Set<String> featureTmp = new HashSet<String>();
//
//        for (final GenomicInterval iv : ivSeq) {
//
//          // writer.write(iv.getChromosome()
//          // + ":[" + (iv.getStart() - 1) + "," + iv.getEnd() + ")/"
//          // + iv.getStrand() + "\n");
//
//          final String chr = iv.getChromosome();
//
//          if (!features.containsChromosome(chr))
//            throw new EoulsanException("Unknown chromosome: " + chr);
//
//          final Map<GenomicInterval, String> intervals =
//              features.getEntries(chr, iv.getStart(), iv.getEnd());
//
//          // At least one interval is found
//          if (intervals != null) {
//            Collection<String> values = intervals.values();
//            if (values != null) {
//
//              for (int pos = iv.getStart(); pos <= iv.getEnd(); pos++) {
//
//                featureTmp.clear();
//
//                for (Map.Entry<GenomicInterval, String> inter : intervals
//                    .entrySet()) {
//                  if (inter.getKey().include(pos, pos)) {
//                    featureTmp.add(inter.getValue());
//                  }
//                }
//
//                if (fs == null) {
//                  fs = new HashSet<String>();
//                  fs.addAll(featureTmp);
//                } else
//                  fs.retainAll(featureTmp);
//              }
//            }
//          }
//
//          // no interval found
//          else {
//            if (fs == null)
//              fs = new HashSet<String>();
//            else
//              fs.clear();
//          }
//
//        }
//      }
//
//      // TODO : exceptions
//      else {
//        System.err.println("Illegal overlap mode.");
//      }

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

    writer.write(String.format("no_feature\t%d\n", empty));
    writer.write(String.format("ambiguous\t%d\n", ambiguous));
    writer.write(String.format("too_low_aQual\t%d\n", lowqual));
    writer.write(String.format("not_aligned\t%d\n", notaligned));
    writer.write(String.format("alignment_not_unique\t%d\n", nonunique));

    writer.close();
  }

  private static Set<String> featuresOverlapped(List<GenomicInterval> ivList,
      GenomicArray<String> features, String mode) throws EoulsanException {

    Set<String> fs = null;

    // Overlap mode "union"
    if (mode.equals("union")) {

      fs = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        final Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (intervals != null) {
          Collection<String> values = intervals.values();
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

        // Get features that overlapped the current interval of the read
        final Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        // At least one interval is found
        if (intervals != null) {
          Collection<String> values = intervals.values();
          if (values != null) {

            for (int pos = iv.getStart(); pos <= iv.getEnd(); pos++) {

              featureTmp.clear();

              for (Map.Entry<GenomicInterval, String> inter : intervals
                  .entrySet()) {
                if (inter.getKey().include(pos, pos))
                  featureTmp.add(inter.getValue());
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
    else if (mode == "intersection-strict") {

      final Set<String> featureTmp = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlapped the current interval of the read
        final Map<GenomicInterval, String> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        // At least one interval is found
        if (intervals != null) {
          Collection<String> values = intervals.values();
          if (values != null) {

            for (int pos = iv.getStart(); pos <= iv.getEnd(); pos++) {

              featureTmp.clear();

              for (Map.Entry<GenomicInterval, String> inter : intervals
                  .entrySet()) {
                if (inter.getKey().include(pos, pos)) {
                  featureTmp.add(inter.getValue());
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
//    final File samFile = new File(dir, "filtered_mapper_results_1.sam");
    final File samFile = new File("/home/wallon/Bureau/GSNAP/PE/500head.sam");
//    final File samFile = new File(dir, "500head.sam");
//    final File gffFile = new File("/home/wallon/Bureau/TEST_EOULSAN/TEST_modifications/annotation.gff");
    final File gffFile = new File("/home/wallon/Bureau/GSNAP/PE/mouse.gff");

    final long startTime = System.currentTimeMillis();
    System.out.println("start.");
    countReadsInFeatures(samFile, gffFile, "no", "union", "exon", "ID", false,
        0, null);
    System.out.println("end.");
    System.out.println("Duration: "
        + (System.currentTimeMillis() - startTime) + " ms.");

    // Python
    // real 19m51.548s
    // user 19m9.692s
    // sys 0m13.385s

  }

}