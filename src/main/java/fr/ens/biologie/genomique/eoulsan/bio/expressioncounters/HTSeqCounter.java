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

import static fr.ens.biologie.genomique.eoulsan.util.StringUtils.join;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicArray;
import fr.ens.biologie.genomique.eoulsan.bio.GenomicInterval;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqUtils.UnknownChromosomeException;
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterCounter;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;

/**
 * This class defines a wrapper on the HTSeq-count counter.
 * @since 1.2
 * @author Claire Wallon
 */
public class HTSeqCounter extends AbstractExpressionCounter
    implements Serializable {

  private static final long serialVersionUID = 4750866178483111062L;

  /** Counter name. */
  public static final String COUNTER_NAME = "htseq-count";

  public static final String REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME =
      "remove.ambiguous.cases";
  public static final String OVERLAP_MODE_PARAMETER_NAME = "overlap.mode";
  public static final String STRANDED_PARAMETER_NAME = "stranded";
  public static final String COUNTER_PARAMETER_NAME = "counter";
  public static final String GENOMIC_TYPE_PARAMETER_NAME = "genomic.type";
  public static final String ATTRIBUTE_ID_PARAMETER_NAME = "attribute.id";
  public static final String SPLIT_ATTRIBUTE_VALUES_PARAMETER_NAME =
      "split.attribute.values";
  public static final String MINIMUM_ALIGNMENT_QUALITY_PARAMETER_NAME =
      "minimum.alignment.quality";
  public static final String REMOVE_NON_UNIQUE_ALIGNMENTS_PARAMETER_NAME =
      "remove.non.unique.alignments";
  public static final String REMOVE_SECONDARY_ALIGNMENTS_PARAMETER_NAME =
      "remove.secondary.alignments";
  public static final String REMOVE_SUPPLEMENTARY_ALIGNMENTS_PARAMETER_NAME =
      "remove.supplementary.alignments";
  public static final String REMOVE_NON_ASSIGNED_FEATURES_SAM_TAGS_PARAMETER_NAME =
      "remove.non.assigned.sam.tags";
  public static final String SAM_TAG_TO_USE_PARAMETER_NAME = "sam.tag.to.use";

  public static final String SAM_TAG_DEFAULT = "XF";

  private String genomicType = "exon";
  private String attributeId = "PARENT";
  private boolean splitAttributeValues = false;
  private StrandUsage stranded = StrandUsage.NO;
  private OverlapMode overlapMode = OverlapMode.UNION;
  private boolean removeAmbiguousCases = true;
  private int minimalQuality = 0;
  private boolean removeNonUnique = true;
  private boolean removeSecondaryAlignments = false;
  private boolean removeSupplementaryAlignments = false;
  private boolean removeNonAssignedFeatureSamTags = false;

  private String samTag = SAM_TAG_DEFAULT;

  private final GenomicArray<String> features = new GenomicArray<>();
  private boolean initialized;

  /**
   * Internal class for counters
   */
  private static class InternalCounters {

    final ReporterIncrementer reporter;
    final String counterGroup;

    private int input;
    private int empty;
    private int ambiguous;
    private int notAligned;
    private int lowQual;
    private int secondaryAlignments;
    private int supplementaryAlignments;
    private int nonUnique;
    private int missingMate;

    private void fillReporter(final HTSeqCounter counter) {

      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.TOTAL_ALIGNMENTS_COUNTER.counterName(),
          this.input);

      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.EMPTY_ALIGNMENTS_COUNTER.counterName(),
          this.empty);
      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.AMBIGUOUS_ALIGNMENTS_COUNTER.counterName(),
          this.ambiguous);
      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.LOW_QUAL_ALIGNMENTS_COUNTER.counterName(),
          this.lowQual);
      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.NOT_ALIGNED_ALIGNMENTS_COUNTER.counterName(),
          this.notAligned);
      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.NOT_UNIQUE_ALIGNMENTS_COUNTER.counterName(),
          this.nonUnique);
      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.MISSING_MATES_COUNTER.counterName(),
          this.missingMate);

      reporter.incrCounter(counterGroup,
          ExpressionCounterCounter.ELIMINATED_READS_COUNTER.counterName(),
          this.empty
              + (counter.removeAmbiguousCases ? this.ambiguous : 0)
              + this.lowQual + this.notAligned
              + (counter.removeNonUnique ? this.nonUnique : 0)
              + this.secondaryAlignments + this.supplementaryAlignments
              + this.missingMate);
    }

    private InternalCounters(final ReporterIncrementer reporter,
        final String counterGroup) {

      this.reporter = reporter;
      this.counterGroup = counterGroup;
    }

  }

  @Override
  public String getName() {

    return COUNTER_NAME;
  }

  @Override
  public String getDescription() {

    return COUNTER_NAME + " counter";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    // Set parameter if common
    if (setCommonParameter(key, value)) {
      return;
    }

    switch (key) {

    case GENOMIC_TYPE_PARAMETER_NAME:
      this.genomicType = value;
      break;

    case ATTRIBUTE_ID_PARAMETER_NAME:
      this.attributeId = value;
      break;

    case STRANDED_PARAMETER_NAME:

      this.stranded = StrandUsage.getStrandUsageFromName(value);

      if (this.stranded == null) {
        throw new EoulsanException("Unknown strand mode");
      }
      break;

    case OVERLAP_MODE_PARAMETER_NAME:

      this.overlapMode = OverlapMode.getOverlapModeFromName(value);

      if (this.overlapMode == null) {
        throw new EoulsanException("Unknown overlap mode");
      }
      break;

    case REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME:
      this.removeAmbiguousCases = Boolean.parseBoolean(value);
      break;

    case SPLIT_ATTRIBUTE_VALUES_PARAMETER_NAME:
      this.splitAttributeValues = Boolean.parseBoolean(value);
      break;

    case MINIMUM_ALIGNMENT_QUALITY_PARAMETER_NAME:
      try {
        this.minimalQuality = Integer.parseInt(value);
      } catch (NumberFormatException e) {
        throw new EoulsanException("Invalid minimal quality value: " + value);
      }
      break;

    case REMOVE_NON_UNIQUE_ALIGNMENTS_PARAMETER_NAME:
      this.removeNonUnique = Boolean.parseBoolean(value);
      break;

    case REMOVE_SECONDARY_ALIGNMENTS_PARAMETER_NAME:
      this.removeSecondaryAlignments = Boolean.parseBoolean(value);
      break;

    case REMOVE_SUPPLEMENTARY_ALIGNMENTS_PARAMETER_NAME:
      this.removeSupplementaryAlignments = Boolean.parseBoolean(value);
      break;

    case REMOVE_NON_ASSIGNED_FEATURES_SAM_TAGS_PARAMETER_NAME:
      this.removeNonAssignedFeatureSamTags = Boolean.parseBoolean(value);
      break;

    case SAM_TAG_TO_USE_PARAMETER_NAME:
      this.samTag = value.toUpperCase().trim();
      if (this.samTag.length() != 2
          || (this.samTag.charAt(0) < 'X' && this.samTag.charAt(0) > 'Z')
          || (this.samTag.charAt(1) < 'A' && this.samTag.charAt(1) > 'Z')) {
        throw new EoulsanException("Invalid SAM tag: " + value);
      }
      break;

    default:
      throw new EoulsanException("Unknown parameter: " + key);
    }
  }

  @Override
  public void checkConfiguration() throws EoulsanException {

    if (this.genomicType == null) {
      throw new EoulsanException("No parent type set");
    }

    if (this.attributeId == null) {
      throw new EoulsanException("No attribute id set");
    }

    if (this.stranded == null) {
      throw new EoulsanException("Unknown strand mode");
    }

    if (this.overlapMode == null) {
      throw new EoulsanException("Unknown overlap mode");
    }
  }

  @Override
  public void init(final GenomeDescription desc,
      final Iterable<GFFEntry> annotations) throws EoulsanException {

    if (desc == null) {
      throw new NullPointerException("the desc argument is null");
    }

    if (annotations == null) {
      throw new NullPointerException("the annotations argument is null");
    }

    if (this.initialized) {
      throw new IllegalStateException(
          "the counter has been already initialized");
    }

    // Check configuration
    checkConfiguration();

    this.features.addChromosomes(desc);

    final Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();

    // Read the annotation file
    for (final GFFEntry gff : annotations) {

      if (this.genomicType.equals(gff.getType())) {

        final String featureId = gff.getAttributeValue(attributeId);
        if (featureId == null) {

          throw new EoulsanException("Feature "
              + this.genomicType + " does not contain a " + attributeId
              + " attribute");
        }

        if ((this.stranded == StrandUsage.YES
            || this.stranded == StrandUsage.REVERSE)
            && '.' == gff.getStrand()) {

          throw new EoulsanException("Feature "
              + this.genomicType
              + " does not have strand information but you are running "
              + "htseq-count in stranded mode.");
        }

        // Addition to the list of features of a GenomicInterval object
        // corresponding to the current annotation line

        final List<String> featureIds;

        if (this.splitAttributeValues) {
          featureIds = GuavaCompatibility.splitToList(splitter, featureId);
        } else {
          featureIds = Collections.singletonList(featureId);
        }

        // Split parent if needed
        for (String f : featureIds) {
          this.features.addEntry(
              new GenomicInterval(gff, this.stranded.isSaveStrandInfo()), f);
        }
      }
    }

    if (this.features.getFeaturesIds().size() == 0) {
      throw new EoulsanException(
          "Warning: No features of type '" + this.genomicType + "' found.\n");
    }

    // The counter is now initialized
    this.initialized = true;
  }

  @Override
  public Map<String, Integer> count(final Iterable<SAMRecord> samRecords,
      final ReporterIncrementer reporter, final String counterGroup)
      throws EoulsanException {

    if (reporter == null) {
      throw new NullPointerException("the reporter argument is null");
    }

    if (counterGroup == null) {
      throw new NullPointerException("the counterGroup argument is null");
    }

    if (!this.initialized) {
      throw new IllegalStateException("the counter has not been initialized");
    }

    SAMRecord sam1 = null, sam2 = null;
    final Map<String, Integer> counts = new HashMap<>();
    final List<GenomicInterval> ivSeq = new ArrayList<>();
    final InternalCounters internalCounters =
        new InternalCounters(reporter, counterGroup);

    // Read the SAM file
    for (final SAMRecord samRecord : samRecords) {

      internalCounters.input++;

      // single-end mode
      if (!samRecord.getReadPairedFlag()) {

        sam1 = samRecord;

        if (!processSingleEnd(sam1, ivSeq, internalCounters)) {
          continue;
        }
      }

      // paired-end mode
      else {

        if (samRecord.getHeader().getSortOrder() == SortOrder.coordinate) {
          throw new EoulsanException(
              "The counter does not support SAM data sorted by coordinate in paired-end mode");
        }

        if (sam1 != null && sam2 != null) {
          sam1 = null;
          sam2 = null;
          ivSeq.clear();
        }

        if (samRecord.getFirstOfPairFlag()) {
          sam1 = samRecord;
        } else {
          sam2 = samRecord;
        }

        if (sam1 == null || sam2 == null) {
          continue;
        }

        if (!sam1.getReadName().equals(sam2.getReadName())) {
          sam1 = sam2;
          sam2 = null;
          internalCounters.missingMate++;
          continue;
        }

        if (!pairedEnd(sam1, sam2, ivSeq, internalCounters)) {
          continue;
        }
      }

      // Update counts
      updateCounts(sam1, sam2, ivSeq, counts, internalCounters);
    }

    // Set the counters in the reporter
    internalCounters.fillReporter(this);

    return counts;
  }

  //
  // Other methods
  //

  /**
   * Process single-end alignment.
   * @param samRecord SAM record
   * @param ivSeq genomic intervals
   * @param counters the counters
   * @return false if the alignment has not been processed
   */
  private boolean processSingleEnd(final SAMRecord samRecord,
      final List<GenomicInterval> ivSeq, final InternalCounters counters) {

    ivSeq.clear();

    // unmapped read
    if (samRecord.getReadUnmappedFlag()) {
      counters.notAligned++;
      assignment(samRecord, null, "__not_aligned");
      return false;
    }

    // secondary alignment
    if (this.removeSecondaryAlignments
        && samRecord.getNotPrimaryAlignmentFlag()) {
      counters.secondaryAlignments++;
      return false;
    }

    // supplementary alignment
    if (this.removeSupplementaryAlignments
        && samRecord.getSupplementaryAlignmentFlag()) {
      counters.supplementaryAlignments++;
      return false;
    }

    // multiple alignment
    if (samRecord.getAttribute("NH") != null
        && samRecord.getIntegerAttribute("NH") > 1) {
      counters.nonUnique++;
      assignment(samRecord, null, "__alignment_not_unique");
      if (this.removeNonUnique) {
        return false;
      }
    }

    // too low quality
    if (samRecord.getMappingQuality() < this.minimalQuality) {
      counters.lowQual++;
      assignment(samRecord, null, "__too_low_aQual");
      return false;
    }

    ivSeq.addAll(HTSeqUtils.addIntervals(samRecord, this.stranded));

    return true;
  }

  /**
   * Process paired-end alignment.
   * @param sam1 first SAM record
   * @param sam2 second SAM record
   * @param ivSeq genomic intervals
   * @param counters the counters
   * @return false if the alignments has not been processed
   */
  private boolean pairedEnd(final SAMRecord sam1, final SAMRecord sam2,
      final List<GenomicInterval> ivSeq, final InternalCounters counters) {

    if (!sam1.getReadUnmappedFlag()) {
      ivSeq.addAll(HTSeqUtils.addIntervals(sam1, this.stranded));
    }

    if (!sam2.getReadUnmappedFlag()) {
      ivSeq.addAll(HTSeqUtils.addIntervals(sam2, this.stranded));
    }

    // unmapped read
    if (sam1.getReadUnmappedFlag() && sam2.getReadUnmappedFlag()) {
      counters.notAligned++;
      assignment(sam1, sam2, "__not_aligned");
      return false;
    }

    // secondary alignment
    if (this.removeSecondaryAlignments) {
      if (sam1 != null && sam1.getNotPrimaryAlignmentFlag()) {
        return false;
      }
      if (sam2 != null && sam2.getNotPrimaryAlignmentFlag()) {
        return false;
      }
    }

    // supplementary alignment
    if (this.removeSupplementaryAlignments) {
      if (sam1 != null && sam1.getSupplementaryAlignmentFlag()) {
        return false;
      }
      if (sam2 != null && sam2.getSupplementaryAlignmentFlag()) {
        return false;
      }
    }

    // multiple alignment
    if ((sam1.getAttribute("NH") != null && sam1.getIntegerAttribute("NH") > 1)
        || (sam2.getAttribute("NH") != null
            && sam2.getIntegerAttribute("NH") > 1)) {
      counters.nonUnique++;
      assignment(sam1, sam2, "__alignment_not_unique");
      if (this.removeNonUnique) {
        return false;
      }
    }

    // too low quality
    if (sam1.getMappingQuality() < this.minimalQuality
        || sam2.getMappingQuality() < this.minimalQuality) {
      counters.lowQual++;
      assignment(sam1, sam2, "__too_low_aQual");
      return false;
    }

    return true;
  }

  /**
   * Update the counts.
   * @param ivSeq the genomic intervals
   * @param counts the counts
   * @param internalCounters the counters
   * @throws EoulsanException if an error occurs while counting
   */
  private void updateCounts(final SAMRecord samRecord1,
      final SAMRecord samRecord2, final List<GenomicInterval> ivSeq,
      final Map<String, Integer> counts,
      final InternalCounters internalCounters) throws EoulsanException {

    try {
      Set<String> fs = HTSeqUtils.featuresOverlapped(ivSeq, this.features,
          this.overlapMode, this.stranded);

      switch (fs.size()) {
      case 0:
        internalCounters.empty++;
        assignment(samRecord1, samRecord2, "__no_feature");
        break;

      case 1:
        String id = fs.iterator().next();
        increment(counts, id);
        assignment(samRecord1, samRecord2, id);
        break;

      default:

        internalCounters.ambiguous++;
        assignment(samRecord1, samRecord2, fs);

        if (!this.removeAmbiguousCases) {
          for (String id2 : fs) {
            increment(counts, id2);
          }
        }
        break;
      }
    } catch (UnknownChromosomeException e) {
      internalCounters.empty++;
      assignment(samRecord1, samRecord2, "__no_feature");
    }
  }

  /**
   * Increment a count.
   * @param counts the counts
   * @param key the feature to increment
   */
  private static void increment(final Map<String, Integer> counts,
      final String key) {

    if (!counts.containsKey(key)) {
      counts.put(key, 1);
    } else {
      counts.put(key, counts.get(key) + 1);
    }
  }

  /**
   * Assign a feature to SAM entries.
   * @param samRecord1 first entry
   * @param samRecord2 second entry
   * @param assignment the value of the assignment
   */
  private void assignment(final SAMRecord samRecord1,
      final SAMRecord samRecord2, final String assignment) {

    if (this.removeNonAssignedFeatureSamTags && assignment.startsWith("__")) {
      return;
    }

    if (samRecord1 != null) {
      samRecord1.setAttribute(this.samTag, assignment);
    }

    if (samRecord2 != null) {
      samRecord2.setAttribute(this.samTag, assignment);
    }
  }

  /**
   * Assign features to SAM entries.
   * @param samRecord1 first entry
   * @param samRecord2 second entry
   * @param features ambiguous features of the assignment
   */
  private void assignment(final SAMRecord samRecord1,
      final SAMRecord samRecord2, final Set<String> features) {

    // Sort the features to always have the same feature order in outputs
    List<String> list = new ArrayList<>(features);
    Collections.sort(list);

    assignment(samRecord1, samRecord2, "__ambiguous[" + join(list, "+") + ']');
  }

  @Override
  public void addZeroCountFeatures(final Map<String, Integer> counts) {

    if (counts == null) {
      throw new NullPointerException("The counts arguments cannot be null");
    }

    if (!this.initialized) {
      throw new IllegalStateException("the counter has not been initialized");
    }

    for (String feature : this.features.getFeaturesIds()) {

      if (!counts.containsKey(feature)) {
        counts.put(feature, 0);
      }
    }
  }

  @Override
  public String toString() {

    return "HTSeqCounter{genomicType="
        + this.genomicType + ", attributeId=" + this.attributeId
        + ", splitAttributeValues=" + this.splitAttributeValues + ", stranded="
        + this.stranded + ", overlapMode=" + this.overlapMode
        + ", removeAmbiguousCases=" + this.removeAmbiguousCases
        + ", removeNonUnique=" + this.removeNonUnique + ","
        + ", removeSecondaryAlignments=" + this.removeSecondaryAlignments
        + ", removeSupplementaryAlignments="
        + this.removeSupplementaryAlignments + " minAverageQuality="
        + this.minimalQuality + ", initialized=" + this.initialized + "}";
  }

}
