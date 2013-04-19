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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class defines a wrapper on the HTSeq-count counter.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCounter extends AbstractExpressionCounter {

  /** Counter name. */
  public static final String COUNTER_NAME = "htseq-count";

  @Override
  public String getCounterName() {

    return COUNTER_NAME;
  }

  @Override
  protected void internalCount(final DataFile alignmentFile,
      final DataFile annotationFile, final DataFile expressionFile,
      final DataFile genomeDescFile, Reporter reporter, String counterGroup)
      throws IOException, EoulsanException, BadBioEntryException {

    countReadsInFeatures(alignmentFile, annotationFile, expressionFile,
        getStranded(), getOverlapMode(), isRemoveAmbiguousCases(),
        getGenomicType(), getAttributeId(), false, 0, null, genomeDescFile,
        reporter, counterGroup);

  }

  /**
   * Count the number of alignments for all the features of the annotation file.
   * @param samFile SAM file that contains alignments.
   * @param gffFile annotation file.
   * @param outFile output file.
   * @param stranded strand to consider.
   * @param overlapMode overlap mode to consider.
   * @param removeAmbiguousCases if true : ambiguous cases will be removed.
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
  private static void countReadsInFeatures(final DataFile samFile,
      final DataFile gffFile, final DataFile outFile,
      final StrandUsage stranded, final OverlapMode overlapMode,
      final boolean removeAmbiguousCases, final String featureType,
      final String attributeId, final boolean quiet, final int minAverageQual,
      final DataFile samOutFile, final DataFile genomeDescFile,
      Reporter reporter, String counterGroup) throws EoulsanException,
      IOException, BadBioEntryException {

    final GenomicArray<String> features =
        new GenomicArray<String>(GenomeDescription.load(genomeDescFile.open()));

    final Map<String, Integer> counts = Utils.newHashMap();

    final Writer writer = FileUtils.createBufferedWriter(outFile.create());

    boolean pairedEnd = false;

    // read and store in 'features' the annotation file
    HTSeqUtils.storeAnnotation(features, gffFile.open(), featureType, stranded,
        attributeId, counts);

    if (counts.size() == 0) {
      writer.close();
      throw new EoulsanException("Warning: No features of type '"
          + featureType + "' found.\n");
    }

    List<GenomicInterval> ivSeq = new ArrayList<GenomicInterval>();

    final SAMFileReader inputSam = new SAMFileReader(samFile.open());

    // paired-end mode ?
    final SAMFileReader input = new SAMFileReader(samFile.open());
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
    SAMRecord sam1 = null, sam2 = null;

    // Read the SAM file
    for (final SAMRecord samRecord : inputSam) {

      reporter.incrCounter(counterGroup,
          ExpressionCounters.TOTAL_ALIGNMENTS_COUNTER.counterName(), 1);

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

        ivSeq.addAll(HTSeqUtils.addIntervals(samRecord, stranded));

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
          ivSeq.addAll(HTSeqUtils.addIntervals(sam1, stranded));
        }

        if (sam2 != null && !sam2.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(sam2, stranded));
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

      fs =
          HTSeqUtils.featuresOverlapped(ivSeq, features, overlapMode, stranded);

      if (fs == null)
        fs = Collections.emptySet();

      switch (fs.size()) {
      case 0:
        empty++;
        break;

      case 1:
        final String id1 = fs.iterator().next();
        counts.put(id1, counts.get(id1) + 1);
        break;

      default:

        if (removeAmbiguousCases) {
          ambiguous++;
        } else {
          for (String id2 : fs)
            counts.put(id2, counts.get(id2) + 1);
        }
        break;
      }

    }

    inputSam.close();

    // Write results
    final List<String> keysSorted = new ArrayList<String>(counts.keySet());
    Collections.sort(keysSorted);

    writer.write("Id\tCount\n");
    for (String key : keysSorted) {
      writer.write(key + "\t" + counts.get(key) + "\n");
    }

    reporter.incrCounter(counterGroup,
        ExpressionCounters.EMPTY_ALIGNMENTS_COUNTER.counterName(), empty);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.AMBIGUOUS_ALIGNMENTS_COUNTER.counterName(),
        ambiguous);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.LOW_QUAL_ALIGNMENTS_COUNTER.counterName(), lowqual);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.NOT_ALIGNED_ALIGNMENTS_COUNTER.counterName(),
        notaligned);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.NOT_UNIQUE_ALIGNMENTS_COUNTER.counterName(),
        nonunique);

    reporter.incrCounter(counterGroup,
        ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(), empty
            + ambiguous + lowqual + notaligned + nonunique);

    writer.close();
  }

}
