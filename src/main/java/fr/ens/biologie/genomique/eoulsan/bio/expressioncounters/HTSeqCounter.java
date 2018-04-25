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
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounters;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;
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

  private static final String GENOMIC_TYPE_DEFAULT = "exon";
  private static final String ATTRIBUTE_ID_DEFAULT = "PARENT";
  private static final int MIN_AVERAGE_QUALITY_DEFAULT = 0;

  private String genomicType = GENOMIC_TYPE_DEFAULT;
  private String attributeId = ATTRIBUTE_ID_DEFAULT;
  private boolean splitAttributeValues;
  private StrandUsage stranded;
  private OverlapMode overlapMode;
  private boolean removeAmbiguousCases;
  private int minAverageQuality = MIN_AVERAGE_QUALITY_DEFAULT;
  private GenomicArray<String> features;

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

    if (key == null) {
      throw new NullPointerException("the key argument is null");
    }

    if (value == null) {
      throw new NullPointerException("the value argument is null");
    }

    // TODO handle minimal quality parameter

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

    if (this.features != null) {
      throw new IllegalStateException(
          "the counter has been already initialized");
    }

    // Check configuration
    checkConfiguration();

    this.features = new GenomicArray<>(desc);

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

    if (this.features == null) {
      throw new IllegalStateException("the counter has not been initialized");
    }

    int empty = 0;
    int ambiguous = 0;
    int notAligned = 0;
    int lowQual = 0;
    int nonUnique = 0;
    int missingMate = 0;

    SAMRecord sam1 = null, sam2 = null;
    final List<GenomicInterval> ivSeq = new ArrayList<>();
    final Map<String, Integer> counts = new HashMap<>();

    // Read the SAM file
    for (final SAMRecord samRecord : samRecords) {

      reporter.incrCounter(counterGroup,
          ExpressionCounters.TOTAL_ALIGNMENTS_COUNTER.counterName(), 1);

      // single-end mode
      if (!samRecord.getReadPairedFlag()) {

        ivSeq.clear();

        // unmapped read
        if (samRecord.getReadUnmappedFlag()) {
          notAligned++;
          continue;
        }

        // multiple alignment
        if (samRecord.getAttribute("NH") != null
            && samRecord.getIntegerAttribute("NH") > 1) {
          nonUnique++;
          continue;
        }

        // too low quality
        if (samRecord.getMappingQuality() < minAverageQuality) {
          lowQual++;
          continue;
        }

        ivSeq.addAll(HTSeqUtils.addIntervals(samRecord, this.stranded));
      }

      // paired-end mode
      else {

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
          missingMate++;
          continue;
        }

        if (!sam1.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(sam1, this.stranded));
        }

        if (!sam2.getReadUnmappedFlag()) {
          ivSeq.addAll(HTSeqUtils.addIntervals(sam2, this.stranded));
        }

        // unmapped read
        if (sam1.getReadUnmappedFlag() && sam2.getReadUnmappedFlag()) {
          notAligned++;
          continue;
        }

        // multiple alignment
        if ((sam1.getAttribute("NH") != null
            && sam1.getIntegerAttribute("NH") > 1)
            || (sam2.getAttribute("NH") != null
                && sam2.getIntegerAttribute("NH") > 1)) {
          nonUnique++;
          continue;
        }

        // too low quality
        if (sam1.getMappingQuality() < minAverageQuality
            || sam2.getMappingQuality() < minAverageQuality) {
          lowQual++;
          continue;
        }

      }

      Set<String> fs = HTSeqUtils.featuresOverlapped(ivSeq, this.features,
          this.overlapMode, this.stranded);

      switch (fs.size()) {
      case 0:
        empty++;
        break;

      case 1:
        final String id1 = fs.iterator().next();
        if (!counts.containsKey(id1)) {
          counts.put(id1, 1);
        } else {
          counts.put(id1, counts.get(id1) + 1);
        }
        break;

      default:

        if (this.removeAmbiguousCases) {
          ambiguous++;
        } else {
          for (String id2 : fs) {
            if (!counts.containsKey(id2)) {
              counts.put(id2, 1);
            } else {
              counts.put(id2, counts.get(id2) + 1);
            }
          }
        }
        break;
      }

    }

    reporter.incrCounter(counterGroup,
        ExpressionCounters.EMPTY_ALIGNMENTS_COUNTER.counterName(), empty);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.AMBIGUOUS_ALIGNMENTS_COUNTER.counterName(),
        ambiguous);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.LOW_QUAL_ALIGNMENTS_COUNTER.counterName(), lowQual);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.NOT_ALIGNED_ALIGNMENTS_COUNTER.counterName(),
        notAligned);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.NOT_UNIQUE_ALIGNMENTS_COUNTER.counterName(),
        nonUnique);
    reporter.incrCounter(counterGroup,
        ExpressionCounters.MISSING_MATES_COUNTER.counterName(), missingMate);

    reporter.incrCounter(counterGroup,
        ExpressionCounters.ELIMINATED_READS_COUNTER.counterName(),
        empty + ambiguous + lowQual + notAligned + nonUnique);

    return counts;
  }

  @Override
  public void addZeroCountFeatures(final Map<String, Integer> counts) {

    if (counts == null) {
      throw new NullPointerException("The counts arguments cannot be null");
    }

    if (this.features == null) {
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
        + ", minAverageQuality=" + this.minAverageQuality + "}";
  }

}
