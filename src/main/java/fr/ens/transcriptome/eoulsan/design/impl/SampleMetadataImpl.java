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
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;

/**
 * This class is the implementation of SampleMetadata.
 * @author Laurent Jourdren
 */
public class SampleMetadataImpl implements SampleMetadata {

  private DesignImpl design;
  private int slideId;

  @Override
  public String get(final String fieldName) {

    final String sampleName = this.design.getSampleName(this.slideId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    return this.design.getMetadata(sampleName, fieldName);
  }

  @Override
  public List<String> getFields() {

    return this.design.getMetadataFieldsNames();
  }

  @Override
  public String getComment() {

    return get(COMMENT_FIELD);
  }

  @Override
  public String getDescription() {

    return get(DESCRIPTION_FIELD);
  }

  @Override
  public String getDate() {

    return get(DATE_FIELD);
  }

  @Override
  public String getOperator() {

    return get(OPERATOR_FIELD);
  }

  @Override
  public String getSerialNumber() {

    return get(SERIAL_NUMBER_FIELD);
  }

  @Override
  public String getAnnotation() {

    return get(ANNOTATION_FIELD);
  }

  @Override
  public String getGenome() {

    return get(GENOME_FIELD);
  }

  @Override
  public String getReads() {

    return get(READS_FIELD);
  }

  @Override
  public String getCondition() {

    return get(CONDITION_FIELD);
  }

  @Override
  public String getReplicatType() {

    return get(REPLICAT_TYPE_FIELD);
  }

  @Override
  public String getUUID() {

    return get(UUID_TYPE_FIELD);
  }

  @Override
  public int getPhredOffset() {

    final String value;

    // Get the value from metadata, if field does not exist return default PHRED
    // offset value
    try {
      value = get(PHRED_OFFSET_FIELD);
    } catch (EoulsanRuntimeException e) {
      return EoulsanRuntime.getSettings().getPhredOffsetDefault();
    }

    final int intValue;
    try {
      intValue = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new EoulsanRuntimeException("PHRED offset value is not an integer");
    }

    checkPhredValue(intValue);

    return intValue;
  }

  //
  // Setters
  //

  @Override
  public void set(final String field, final String value) {

    final String sampleName = this.design.getSampleName(this.slideId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    this.design.setMetadata(sampleName, field, value);
  }

  @Override
  public void setComment(final String comment) {

    set(COMMENT_FIELD, comment);
  }

  @Override
  public void setDescription(final String description) {

    set(DESCRIPTION_FIELD, description);
  }

  @Override
  public void setDate(final String date) {

    set(DATE_FIELD, date);
  }

  @Override
  public void setOperator(final String operator) {

    set(OPERATOR_FIELD, operator);
  }

  @Override
  public void setSerialNumber(final String serialNumber) {

    set(SERIAL_NUMBER_FIELD, serialNumber);
  }

  @Override
  public void setAnnotation(final String annotation) {

    set(ANNOTATION_FIELD, annotation);
  }

  @Override
  public void setGenome(final String genome) {

    set(GENOME_FIELD, genome);

  }

  @Override
  public void setReads(final String reads) {

    set(READS_FIELD, reads);

  }

  @Override
  public void setCondition(final String condition) {

    set(CONDITION_FIELD, condition);
  }

  @Override
  public void setReplicatType(final String replicatType) {

    set(REPLICAT_TYPE_FIELD, replicatType);
  }

  @Override
  public void setUUID(final String uuid) {

    set(UUID_TYPE_FIELD, uuid);
  }

  @Override
  public void setPhredOffset(final int phredOffset) {

    set(PHRED_OFFSET_FIELD, Integer.toString(phredOffset));
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
  public boolean isReplicatTypeField() {

    return isField(REPLICAT_TYPE_FIELD);
  }

  @Override
  public boolean isUUIDField() {

    return isField(UUID_TYPE_FIELD);
  }

  @Override
  public boolean isPhredOffset() {

    return isField(PHRED_OFFSET_FIELD);
  }

  //
  // Other methods
  //

  private void checkPhredValue(final int phredOffset) {

    if (phredOffset != 33 && phredOffset != 64)
      throw new EoulsanRuntimeException(
          "Invalid PHRED offset value (only 33 or 64 are allowed): "
              + phredOffset);
  }

  //
  // Constructor
  //

  SampleMetadataImpl(final DesignImpl design, final int slideId) {

    this.design = design;
    this.slideId = slideId;
  }

}
