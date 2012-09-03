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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;

/**
 * This class groups HTSeq functions that are used in both local and distributed
 * modes.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqUtils {

  public static void storeAnnotation(GenomicArray<String> features,
      final InputStream gffFile, final String featureType,
      final StrandUsage stranded, final String attributeId,
      final Map<String, Integer> counts) throws IOException, EoulsanException,
      BadBioEntryException {

    final GFFReader gffReader = new GFFReader(gffFile);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null) {
          gffReader.close();
          // writer.close();
          // samout.close();
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");
        }

        if ((stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE)
            && '.' == gff.getStrand()) {
          gffReader.close();
          // writer.close();
          // samout.close();
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
  }

  public static void storeAnnotation(GenomicArray<String> features,
      final File gffFile, final String featureType, final StrandUsage stranded,
      final String attributeId, final Map<String, Integer> counts)
      throws IOException, EoulsanException, BadBioEntryException {

    final GFFReader gffReader = new GFFReader(gffFile);

    // Read the annotation file
    for (final GFFEntry gff : gffReader) {

      if (featureType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null) {
          gffReader.close();
          // writer.close();
          // samout.close();
          throw new EoulsanException("Feature "
              + featureType + " does not contain a " + attributeId
              + " attribute");
        }

        if ((stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE)
            && '.' == gff.getStrand()) {
          gffReader.close();
          // writer.close();
          // samout.close();
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
  }

  /**
   * Add intervals of a SAM record that are alignment matches (thanks to the
   * CIGAR code).
   * @param record the SAM record to treat.
   * @param stranded strand to consider.
   * @return the list of intervals of the SAM record.
   */
  public static List<GenomicInterval> addIntervals(final SAMRecord record,
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

    else
      return null;

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
  public static final List<GenomicInterval> parseCigar(Cigar cigar,
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
  public static Set<String> featuresOverlapped(List<GenomicInterval> ivList,
      GenomicArray<String> features, OverlapMode mode, StrandUsage stranded)
      throws EoulsanException, IOException {

    Set<String> fs = null;
    Map<GenomicInterval, Set<String>> inter =
        new HashMap<GenomicInterval, Set<String>>();

    // Overlap mode "union"
    if (mode == OverlapMode.UNION) {

      fs = new HashSet<String>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlap the current interval of the read
        Map<GenomicInterval, Set<String>> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE) {
          for (Map.Entry<GenomicInterval, Set<String>> i : intervals.entrySet()) {
            if (i.getKey().getStrand() == iv.getStrand())
              inter.put(i.getKey(), i.getValue());
          }
          intervals = inter;
        }

        // At least one interval is found
        if (intervals != null && intervals.size() > 0) {
          for (Map.Entry<GenomicInterval, Set<String>> e : intervals.entrySet()) {
            if (e.getValue() != null)
              fs.addAll(e.getValue());
          }
        }

      }
    }

    // Overlap modes : "intersection-nonempty" or "intersection-strict"
    else if (mode == OverlapMode.INTERSECTION_NONEMPTY
        || mode == OverlapMode.INTERSECTION_STRICT) {

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr))
          throw new EoulsanException("Unknown chromosome: " + chr);

        // Get features that overlap the current interval of the read
        Map<GenomicInterval, Set<String>> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        if (stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE) {
          for (Map.Entry<GenomicInterval, Set<String>> e : intervals.entrySet()) {
            if (e.getKey().getStrand() == iv.getStrand())
              inter.put(e.getKey(), e.getValue());
          }
          intervals = inter;
        }

        if (intervals == null || intervals.size() == 0) {
          fs = new HashSet<String>();
        }
        // At least one interval is found
        if (intervals != null && intervals.size() > 0) {
          for (Map.Entry<GenomicInterval, Set<String>> i : intervals.entrySet()) {
            if (i.getValue().size() > 0
                || mode == OverlapMode.INTERSECTION_STRICT) {
              if (fs == null) {
                fs = new HashSet<String>();
                fs.addAll(i.getValue());
              } else
                fs.retainAll(i.getValue());
            }
          }
        }
      }
    }

    else
      throw new EoulsanException("Error : illegal overlap mode.");

    return fs;
  }
}
