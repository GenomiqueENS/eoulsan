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

package fr.ens.biologie.genomique.eoulsan.bio.expressioncounters;

import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode.INTERSECTION_NONEMPTY;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode.INTERSECTION_STRICT;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode.UNION;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage.REVERSE;
import static fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage.YES;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicArray;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval;
import fr.ens.biologie.genomique.eoulsan.bio.io.GFFReader;
import fr.ens.biologie.genomique.eoulsan.bio.io.GTFReader;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;
import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

/**
 * This class groups HTSeq functions that are used in both local and distributed
 * modes.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqUtils {

  /**
   * This class define a unknown chromosome exception.
   */
  public static class UnknownChromosomeException extends EoulsanException {

    private static final long serialVersionUID = -7074516037283735042L;

    public UnknownChromosomeException(String chromosome) {
      super("Unknown chromosome: " + chromosome);
    }
  }

  public static void storeAnnotation(final GenomicArray<String> features,
      final InputStream annotationIs, final boolean gtfFormat,
      final String featureType, final StrandUsage stranded,
      final String attributeId, final boolean splitAttributeValues,
      final Map<String, Integer> counts)
      throws IOException, EoulsanException, BadBioEntryException {

    final Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();

    // Splitter for parents string
    try (final GFFReader gffReader =
        gtfFormat ? new GTFReader(annotationIs) : new GFFReader(annotationIs)) {

      // Read the annotation file
      for (final GFFEntry gff : gffReader) {

        if (featureType.equals(gff.getType())) {

          final String featureId = gff.getAttributeValue(attributeId);
          if (featureId == null) {

            throw new EoulsanException("Feature "
                + featureType + " does not contain a " + attributeId
                + " attribute");
          }

          if ((stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE)
              && '.' == gff.getStrand()) {

            throw new EoulsanException("Feature "
                + featureType
                + " does not have strand information but you are running "
                + "htseq-count in stranded mode.");
          }

          // Addition to the list of features of a GenomicInterval object
          // corresponding to the current annotation line

          final List<String> featureIds;

          if (splitAttributeValues) {
            featureIds = GuavaCompatibility.splitToList(splitter, featureId);
          } else {
            featureIds = Collections.singletonList(featureId);
          }

          // Split parent if needed
          for (String f : featureIds) {
            features.addEntry(
                new GenomicInterval(gff, stranded.isSaveStrandInfo()), f);
            counts.put(f, 0);
          }
        }
      }
      gffReader.throwException();
    }
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

    if (record == null) {
      return null;
    }

    List<GenomicInterval> result = new ArrayList<>();

    // single-end mode or first read in the paired-end mode
    if (!record.getReadPairedFlag()
        || (record.getReadPairedFlag() && record.getFirstOfPairFlag())) {

      // the read has to be mapped to the opposite strand as the feature
      if (stranded == REVERSE) {
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(),
            record.getReadNegativeStrandFlag() ? '+' : '-'));
      }
      // stranded == "yes" (so the read has to be mapped to the same strand as
      // the feature) or stranded == "no" (so the read is considered
      // overlapping with a feature regardless of whether it is mapped to the
      // same or the opposite strand as the feature)
      else {
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(),
            record.getReadNegativeStrandFlag() ? '-' : '+'));
      }
    }

    // second read in the paired-end mode
    else if (record.getReadPairedFlag() && !record.getFirstOfPairFlag()) {

      // the read has to be mapped to the opposite strand as the feature
      if (stranded == StrandUsage.REVERSE) {
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(),
            record.getReadNegativeStrandFlag() ? '-' : '+'));
      }
      // stranded == "yes" (so the read has to be mapped to the same strand as
      // the feature) or stranded == "no" (so the read is considered
      // overlapping with a feature regardless of whether it is mapped to the
      // same or the opposite strand as the feature)
      else {
        result.addAll(parseCigar(record.getCigar(), record.getReferenceName(),
            record.getAlignmentStart(),
            record.getReadNegativeStrandFlag() ? '+' : '-'));
      }
    } else {
      return null;
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
  public static final List<GenomicInterval> parseCigar(final Cigar cigar,
      final String chromosome, final int start, final char strand) {

    if (cigar == null) {
      return null;
    }

    final List<GenomicInterval> result = new ArrayList<>();

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
        if (pos != start && ce.getOperator() != CigarOperator.I) {
          pos += len;
        }
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
   * @throws EoulsanException if an error occurs while getting overlapped features
   */
  public static Set<String> featuresOverlapped(
      final List<GenomicInterval> ivList, final GenomicArray<String> features,
      final OverlapMode mode, final StrandUsage stranded)
      throws EoulsanException {

    Set<String> fs = null;

    // Overlap mode "union"
    if (mode == UNION) {

      fs = new HashSet<>();

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr)) {
          throw new UnknownChromosomeException(chr);
        }

        // Get features that overlap the current interval of the read
        final Map<GenomicInterval, Set<String>> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        // Filter intervals if necessary
        if (stranded == YES || stranded == REVERSE) {
          filterIntervalsStrands(intervals, iv.getStrand());
        }

        // At least one interval is found
        if (intervals != null && intervals.size() > 0) {
          for (Map.Entry<GenomicInterval, Set<String>> e : intervals
              .entrySet()) {

            if (e.getValue() != null) {
              fs.addAll(e.getValue());
            }
          }
        }

      }
    }

    // Overlap modes : "intersection-nonempty" or "intersection-strict"
    else if (mode == INTERSECTION_NONEMPTY || mode == INTERSECTION_STRICT) {

      for (final GenomicInterval iv : ivList) {

        final String chr = iv.getChromosome();

        if (!features.containsChromosome(chr)) {
          throw new EoulsanException("Unknown chromosome: " + chr);
        }

        // Get features that overlap the current interval of the read
        final Map<GenomicInterval, Set<String>> intervals =
            features.getEntries(chr, iv.getStart(), iv.getEnd());

        // Filter intervals if necessary
        if (stranded == StrandUsage.YES || stranded == StrandUsage.REVERSE) {
          filterIntervalsStrands(intervals, iv.getStrand());
        }

        // If internal is empty, add an entry with requested iv as key and an
        // empty set as value (HTSeq compatibility)
        if (intervals.isEmpty()) {
          final Set<String> emptySet = Collections.emptySet();
          intervals.put(iv, emptySet);
        }

        // At least one interval is found
        if (intervals.size() > 0) {
          for (Map.Entry<GenomicInterval, Set<String>> i : intervals
              .entrySet()) {

            final Set<String> fs2 = i.getValue();

            if (fs2.size() > 0 || mode == INTERSECTION_STRICT) {
              if (fs == null) {
                fs = new HashSet<>(fs2);
              } else {
                fs.retainAll(fs2);
              }

            }
          }
        }
      }
    } else {
      throw new EoulsanException("Error : illegal overlap mode.");
    }

    // Do not return null
    if (fs == null) {
      return Collections.emptySet();
    }

    return fs;
  }

  /**
   * Filter the output of GenomicArray.getEntries() by keeping only features on
   * a strand
   * @param intervals intervals to filter
   * @param strand strand to keep
   */
  private static void filterIntervalsStrands(
      final Map<GenomicInterval, Set<String>> intervals, final char strand) {

    if (intervals == null) {
      return;
    }

    final Set<GenomicInterval> toRemove = new HashSet<>();

    for (Map.Entry<GenomicInterval, Set<String>> e : intervals.entrySet()) {
      if (e.getKey().getStrand() != strand) {
        toRemove.add(e.getKey());
      }
    }

    for (GenomicInterval iv : toRemove) {
      intervals.remove(iv);
    }
  }

}
