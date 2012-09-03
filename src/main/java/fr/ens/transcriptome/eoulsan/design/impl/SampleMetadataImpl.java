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

package fr.ens.transcriptome.eoulsan.design.impl;

import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class is the implementation of SampleMetadata.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SampleMetadataImpl implements SampleMetadata {

  private DesignImpl design;
  private int slideId;

  @Override
  public String getField(final String fieldName) {

    final String sampleName = this.design.getSampleName(this.slideId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    return this.design.getMetadata(sampleName, fieldName);
  }

  @Override
  public List<String> getFieldAsList(final String fieldName) {

    return StringUtils.deserializeStringArray(getField(fieldName));
  }

  @Override
  public List<String> getFields() {

    return this.design.getMetadataFieldsNames();
  }

  @Override
  public String getComment() {

    return getField(COMMENT_FIELD);
  }

  @Override
  public String getExperiment() {

    return getField(EXPERIMENT_FIELD);
  }

  @Override
  public String getDescription() {

    return getField(DESCRIPTION_FIELD);
  }

  @Override
  public String getDate() {

    return getField(DATE_FIELD);
  }

  @Override
  public String getOperator() {

    return getField(OPERATOR_FIELD);
  }

  @Override
  public String getSerialNumber() {

    return getField(SERIAL_NUMBER_FIELD);
  }

  @Override
  public String getAnnotation() {

    return getField(ANNOTATION_FIELD);
  }

  @Override
  public String getGenome() {

    return getField(GENOME_FIELD);
  }

  @Override
  public List<String> getReads() {

    return getFieldAsList(READS_FIELD);
  }

  @Override
  public String getCondition() {

    return getField(CONDITION_FIELD);
  }

  @Override
  public String getUUID() {

    return getField(UUID_TYPE_FIELD);
  }

  @Override
  public FastqFormat getFastqFormat() {

    final String value;

    // Get the value from metadata, if field does not exist return default fastq
    // format
    // offset value
    try {

      value = getField(FASTQ_FORMAT_FIELD);

    } catch (EoulsanRuntimeException e) {
      return EoulsanRuntime.getSettings().getDefaultFastqFormat();
    }

    final FastqFormat result = FastqFormat.getFormatFromName(value);

    if (result == null)
      return EoulsanRuntime.getSettings().getDefaultFastqFormat();

    return result;
  }

  @Override
  public String getRepTechGroup() {

    return getField(REP_TECH_GROUP_FIELD);
  }

  @Override
  public boolean isReference() {

    String value = getField(REFERENCE_FIELD);

    if (value == null)
      return false;

    value = value.trim().toLowerCase();

    return "t".equals(value)
        || "true".equals(value) || "y".equals(value) || "yes".equals(value);
  }

  //
  // Setters
  //

  @Override
  public void setField(final String field, final String value) {

    final String sampleName = this.design.getSampleName(this.slideId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    this.design.setMetadata(sampleName, field, value);
  }

  @Override
  public void setField(final String field, final List<String> value) {

    if (value == null) {
      setField(field, (String) null);
      return;
    }

    switch (value.size()) {

    case 0:
      setField(field, "");
      break;

    case 1:
      setField(field, value.get(0));
      break;

    default:
      setField(field, StringUtils.serializeStringArray(value));
    }
  }

  @Override
  public void setComment(final String comment) {

    setField(COMMENT_FIELD, comment);
  }

  @Override
  public void setDescription(final String description) {

    setField(DESCRIPTION_FIELD, description);
  }

  @Override
  public void setDate(final String date) {

    setField(DATE_FIELD, date);
  }

  @Override
  public void setOperator(final String operator) {

    setField(OPERATOR_FIELD, operator);
  }

  @Override
  public void setSerialNumber(final String serialNumber) {

    setField(SERIAL_NUMBER_FIELD, serialNumber);
  }

  @Override
  public void setAnnotation(final String annotation) {

    setField(ANNOTATION_FIELD, annotation);
  }

  @Override
  public void setGenome(final String genome) {

    setField(GENOME_FIELD, genome);
  }

  @Override
  public void setExperiment(String experiment) {

    setField(EXPERIMENT_FIELD, experiment);
  }

  @Override
  public void setReads(final List<String> reads) {

    setField(READS_FIELD, reads);

  }

  @Override
  public void setCondition(final String condition) {

    setField(CONDITION_FIELD, condition);
  }

  @Override
  public void setUUID(final String uuid) {

    setField(UUID_TYPE_FIELD, uuid);
  }

  @Override
  public void setFastqFormat(final FastqFormat fastqFormat) {

    if (fastqFormat == null)
      throw new NullPointerException("FastqFormat is null");

    setField(FASTQ_FORMAT_FIELD, fastqFormat.getName());
  }

  @Override
  public void setRepTechGroup(String repTechGroup) {

    setField(REP_TECH_GROUP_FIELD, repTechGroup);
  }

  @Override
  public void setReference(boolean reference) {

    setField(REFERENCE_FIELD, Boolean.toString(reference));
  }

  //
  // Fields tester
  //

  @Override
  public boolean isField(final String fieldName) {

    final String slideName = this.design.getSampleName(this.slideId);

    if (slideName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    return this.design.isMetadataField(fieldName);
  }

  @Override
  public boolean isCommentField() {

    return isField(COMMENT_FIELD);
  }

  @Override
  public boolean isDescriptionField() {

    return isField(COMMENT_FIELD);
  }

  @Override
  public boolean isDateField() {

    return isField(DATE_FIELD);
  }

  @Override
  public boolean isOperatorField() {

    return isField(OPERATOR_FIELD);
  }

  @Override
  public boolean isAnnotationField() {

    return isField(ANNOTATION_FIELD);
  }

  @Override
  public boolean isGenomeField() {

    return isField(GENOME_FIELD);
  }

  @Override
  public boolean isExperiment() {

    return isField(EXPERIMENT_FIELD);
  }

  @Override
  public boolean isReadsField() {

    return isField(READS_FIELD);
  }

  @Override
  public boolean isSerialNumberField() {

    return isField(SERIAL_NUMBER_FIELD);
  }

  @Override
  public boolean isConditionField() {

    return isField(CONDITION_FIELD);
  }

  @Override
  public boolean isUUIDField() {

    return isField(UUID_TYPE_FIELD);
  }

  @Override
  public boolean isFastqFormatField() {

    return isField(FASTQ_FORMAT_FIELD);
  }

  @Override
  public boolean isRepTechGroupField() {

    return isField(REP_TECH_GROUP_FIELD);
  }

  @Override
  public boolean isReferenceField() {

    return isField(REFERENCE_FIELD);
  }

  @Override
  public boolean isExperimentField() {

    return isField(EXPERIMENT_FIELD);
  }

  //
  // Constructor
  //

  SampleMetadataImpl(final DesignImpl design, final int slideId) {

    this.design = design;
    this.slideId = slideId;
  }

}
