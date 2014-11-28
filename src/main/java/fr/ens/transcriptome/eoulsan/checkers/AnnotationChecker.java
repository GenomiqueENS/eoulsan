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

package fr.ens.transcriptome.eoulsan.checkers;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep.ATTRIBUTE_ID_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep.COUNTER_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep.GENOMIC_TYPE_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep.OVERLAPMODE_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep.REMOVEAMBIGUOUSCASES_PARAMETER_NAME;
import static fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep.STRANDED_PARAMETER_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.bio.GenomicInterval;
import fr.ens.transcriptome.eoulsan.bio.io.GFFReader;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.generators.GenomeDescriptionCreator;

/**
 * This class define a Checker on GFF annotation.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class AnnotationChecker implements Checker {

  private String genomicType;
  private String attributeId;
  private boolean stranded = true;

  @Override
  public String getName() {

    return "annotation_checker";
  }

  @Override
  public DataFormat getFormat() {
    return ANNOTATION_GFF;
  }

  @Override
  public Set<DataFormat> getCheckersRequiered() {
    return Sets.newHashSet(DataFormats.GENOME_FASTA);
  }

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {

    // TODO the parsing of the parameter must be shared with
    // AbstractExpressionStep

    for (Parameter p : stepParameters) {

      if (GENOMIC_TYPE_PARAMETER_NAME.equals(p.getName())) {
        this.genomicType = p.getStringValue();
      } else if (ATTRIBUTE_ID_PARAMETER_NAME.equals(p.getName())) {
        this.attributeId = p.getStringValue();
      } else if (STRANDED_PARAMETER_NAME.equals(p.getName())) {
        this.stranded =
            "yes".equals(p.getStringValue())
                || "reverse".equals(p.getStringValue());
      } else if (!COUNTER_PARAMETER_NAME.equals(p.getName())
          && !OVERLAPMODE_PARAMETER_NAME.equals(p.getName())
          && !REMOVEAMBIGUOUSCASES_PARAMETER_NAME.equals(p.getName())) {
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());
      }
    }
  }

  @Override
  public boolean check(final Data data, final CheckStore checkInfo)
      throws EoulsanException {

    if (data == null)
      throw new NullPointerException("The data is null");

    if (checkInfo == null)
      throw new NullPointerException("The check info info is null");

    try {
      final DataFile annotationFile = data.getDataFile();

      if (!annotationFile.exists())
        return true;

      if (this.genomicType == null)
        return true;

      final GenomeDescription desc =
          getGenomeDescription(annotationFile, checkInfo);

      validationAnnotation(annotationFile, desc, this.genomicType,
          this.attributeId, this.stranded);

    } catch (IOException e) {
      throw new EoulsanException(
          "Annotation Check: Error while reading annotation file for checking: "
              + e.getMessage());
    } catch (BadBioEntryException e) {
      throw new EoulsanException("Annotation Check: "
          + e.getMessage() + " in line \"" + e.getEntry() + "\"");
    }
    return false;
  }

  private static void validationAnnotation(final DataFile file,
      final GenomeDescription desc, final String featureType,
      final String attributeId, final boolean stranded) throws IOException,
      BadBioEntryException, EoulsanException {

    InputStream is = file.open();
    final GFFReader gffReader = new GFFReader(is);

    final GenomicArray<String> features = new GenomicArray<String>();
    Map<String, long[]> sequenceRegions = null;
    final Map<String, Long> sequenceLengths = getSequencesLengths(desc);
    boolean featuresFound = false;
    boolean first = true;

    long[] interval = null;
    long sequenceLength = -1;
    String lastSequenceName = null;

    for (final GFFEntry e : gffReader) {

      if (first) {
        sequenceRegions = checkSequenceRegions(e, desc);
        first = false;
      }

      if (!featureType.equals(e.getType()))
        continue;

      final String sequenceName = e.getSeqId();
      final int start = e.getStart();
      final int end = e.getEnd();

      if (sequenceRegions != null) {

        if (!sequenceName.equals(lastSequenceName)) {
          interval = sequenceRegions.get(sequenceName);

          if (interval == null) {
            gffReader.close();
            throw new BadBioEntryException("GFF entry with id ("
                + sequenceName + ") not found in sequence region", e.toString());
          }
        }

        if (Math.min(start, end) < interval[0]) {
          gffReader.close();
          throw new BadBioEntryException("GFF entry with start position ("
              + Math.min(start, end)
              + ") lower than the start of sequence region" + sequenceName
              + " (" + interval[0] + ")", e.toString());
        }

        if (Math.max(start, end) > interval[1]) {
          gffReader.close();
          throw new BadBioEntryException("GFF entry with end position ("
              + Math.max(start, end)
              + ") greater than the end of sequence region " + sequenceName
              + " (" + interval[1] + ")", e.toString());
        }
      }

      if (sequenceLengths != null) {

        if (!sequenceName.equals(lastSequenceName)) {

          if (!sequenceLengths.containsKey(sequenceName)) {
            gffReader.close();
            throw new BadBioEntryException("GFF entry with id ("
                + sequenceName + ") not found in genome", e.toString());
          }

          sequenceLength = sequenceLengths.get(sequenceName);
        }

        if (Math.min(start, end) < 1) {
          gffReader.close();
          throw new BadBioEntryException("GFF entry with start position ("
              + Math.min(start, end) + ") lower than 1 in sequence "
              + sequenceName, e.toString());
        }

        if (Math.max(start, end) - 1 > sequenceLength) {
          gffReader.close();
          throw new BadBioEntryException("GFF entry with end position ("
              + Math.max(start, end)
              + ") greater than the the length of sequence " + sequenceName
              + " (" + sequenceLength + ")", e.toString());
        }
      }

      final String featureId = e.getAttributeValue(attributeId);

      if (attributeId != null && featureId == null) {
        throw new BadBioEntryException(
            "Feature "
                + featureType + " does not contain a " + attributeId
                + " attribute", e.toString());
      }

      if (featureId != null) {

        features.addEntry(new GenomicInterval(e, stranded), featureId);
        featuresFound = true;
      }

      lastSequenceName = sequenceName;
    }

    gffReader.throwException();
    gffReader.close();

    if (featureType != null && !featuresFound)
      throw new EoulsanException("No feature \""
          + featureType + "\" with attribute \"" + attributeId
          + "\" in annotation.");

  }

  private static Map<String, Long> getSequencesLengths(
      final GenomeDescription desc) {

    if (desc == null)
      return null;

    final Map<String, Long> result = new HashMap<>();

    for (String sequenceName : desc.getSequencesNames())
      result.put(sequenceName, desc.getSequenceLength(sequenceName));

    return result;
  }

  private static Map<String, long[]> checkSequenceRegions(final GFFEntry entry,
      final GenomeDescription desc) throws BadBioEntryException {

    if (entry == null || desc == null)
      return null;

    final Map<String, long[]> result = new HashMap<>();

    final List<String> sequenceRegions =
        entry.getMetadataEntryValues("sequence-region");

    if (sequenceRegions == null)
      return null;

    for (String sequenceRegion : sequenceRegions) {

      if (sequenceRegion == null)
        continue;

      final String[] fields = sequenceRegion.trim().split(" ");

      if (fields.length != 3)
        throw new BadBioEntryException("Invalid GFF metadata",
            "##sequence-region " + sequenceRegion);

      try {
        final String sequenceName = fields[0].trim();
        final long start = Integer.parseInt(fields[1]);
        final long end = Integer.parseInt(fields[2]);

        result.put(sequenceName, new long[] {start, end});
        final long len = desc.getSequenceLength(sequenceName);

        if (len == -1)
          throw new BadBioEntryException(
              "Unknown sequence found in GFF metadata", "##sequence-region "
                  + sequenceRegion);

        // Don't check the start position because it can
        // be < 1

        // TODO Why len+2 for dmel annotation ?
        if (end > len + 2)
          throw new BadBioEntryException(
              "Invalid GFF metadata, the end position ("
                  + end + ") is greater than the length of the sequence ("
                  + (len + 2) + ")", "##sequence-region " + sequenceRegion);

      } catch (NumberFormatException e) {
        throw new BadBioEntryException("Invalid GFF metadata",
            "##sequence-region " + sequenceRegion);
      }
    }

    return result;
  }

  private GenomeDescription getGenomeDescription(final DataFile annotationFile,
      final CheckStore checkInfo) throws EoulsanException,
      BadBioEntryException, IOException {

    GenomeDescription result =
        (GenomeDescription) checkInfo.get(GenomeChecker.GENOME_DESCRIPTION);

    if (result != null)
      return result;

    result =
        new GenomeDescriptionCreator()
            .createGenomeDescriptionFromAnnotation(annotationFile);

    return result;
  }

}
