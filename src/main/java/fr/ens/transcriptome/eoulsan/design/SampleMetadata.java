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

package fr.ens.transcriptome.eoulsan.design;

import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;

/**
 * This interface define the description of a slide.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface SampleMetadata {

  /** Description field. */
  String DESCRIPTION_FIELD = "Description";
  /** Reads field. */
  String READS_FIELD = "Reads";
  /** Genome field. */
  String GENOME_FIELD = "Genome";
  /** Annotation field. */
  String ANNOTATION_FIELD = "Annotation";
  /** Additional annotation field. */
  String ADDITIONAL_ANNOTATION_FIELD = "AdditionalAnnotation";
  /** Comment field. */
  String COMMENT_FIELD = "Comment";
  /** Date field. */
  String DATE_FIELD = "Date";
  /** Serial Number field. */
  String SERIAL_NUMBER_FIELD = "SerialNumber";
  /** Operator field. */
  String OPERATOR_FIELD = "Operator";
  /** Condition field. */
  String CONDITION_FIELD = "Condition";
  /** UUID field. */
  String UUID_TYPE_FIELD = "UUID";
  /** Fastq format field. */
  String FASTQ_FORMAT_FIELD = "FastqFormat";
  /** RepTechGroup field. */
  String REP_TECH_GROUP_FIELD = "RepTechGroup";
  /** Experiment name field */
  String EXPERIMENT_FIELD = "Experiment";
  /** Sample reference */
  String REFERENCE_FIELD = "Reference";

  //
  // Getters
  //

  /**
   * Get a field value.
   * @param field Field of the description to get
   * @return a String with the value
   */
  String getField(final String field);

  /**
   * Get a field value as a list.
   * @param field Field of the description to get
   * @return a list with the values
   */
  List<String> getFieldAsList(final String field);

  /**
   * Get the fields of the descriptions.
   * @return a list of strings with the descriptions fields
   */
  List<String> getFields();

  /**
   * Get the description about the sample.
   * @return Returns the comment
   */
  String getDescription();

  /**
   * Get the comment about the sample.
   * @return Returns the comment
   */
  String getComment();

  /**
   * Get the reads file relative to the sample.
   * @return Returns the reads file
   */
  List<String> getReads();

  /**
   * Get the genome file relative to the sample.
   * @return Returns the genome file
   */
  String getGenome();

  /**
   * Get the annotation relative to the sample.
   * @return Returns the annotation
   */
  String getAnnotation();

  /**
   * Get the additional annotation relative to the sample.
   * @return Returns the additional annotation
   */
  String getAdditionalAnnotation();

  /**
   * Get the date of the sample.
   * @return Returns the date
   */
  String getDate();

  /**
   * Get the name of the operator.
   * @return Returns the operator
   */
  String getOperator();

  /**
   * Get the serial number of the sample.
   * @return Returns the serialNumber
   */
  String getSerialNumber();

  /**
   * Get the condition of the sample.
   * @return Returns the condition
   */
  String getCondition();

  /**
   * Get UUID.
   * @return Returns the UUID
   */
  String getUUID();

  /**
   * Get the fastq format.
   * @return the fastq format.
   */
  FastqFormat getFastqFormat();

  /**
   * Get RepTechGroup
   * @return The repTechGroup
   */
  String getRepTechGroup();

  /**
   * Test if the sample is the reference for the experiment.
   * @return true if the sample is the reference
   */
  boolean isReference();

  /**
   * Get Reference
   * @return the Reference
   */
  String getExperiment();

  //
  // Setters
  //

  /**
   * Set a field of the metadata.
   * @param field Field to set
   * @param value value to set
   */
  void setField(String field, String value);

  /**
   * Set a field of the metadata.
   * @param field Field to set
   * @param values values to set
   */
  void setField(String field, List<String> values);

  /**
   * Set the description.
   * @param description The description to set
   */
  void setDescription(String description);

  /**
   * Set the comment.
   * @param comment The comment to set
   */
  void setComment(String comment);

  /**
   * Set the reads file relative to the sample.
   * @param reads file to set
   */
  void setReads(List<String> reads);

  /**
   * Set the genome file relative to the sample.
   * @param genome file to set
   */
  void setGenome(String genome);

  /**
   * Set the project name
   * @param experiment
   */
  void setExperiment(String experiment);

  /**
   * Set the annotation file relative to the sample.
   * @param annotation file to set
   */
  void setAnnotation(String annotation);

  /**
   * Set the additional annotation file relative to the sample.
   * @param additionalAnnotation file to set
   */
  void setAdditionalAnnotation(String additionalAnnotation);

  /**
   * Set the hybridation date
   * @param date The date to set
   */
  void setDate(String date);

  /**
   * Set the name of the operator.
   * @param operator The operator to set
   */
  void setOperator(String operator);

  /**
   * Set the serial number of the sample.
   * @param serialNumber The serialNumber to set
   */
  void setSerialNumber(String serialNumber);

  /**
   * Set the condition of the sample.
   * @param condition The condition to set
   */
  void setCondition(String condition);

  /**
   * Set the UUID of the sample.
   * @param uuid
   */
  void setUUID(final String uuid);

  /**
   * Set the fastq format.
   * @param fastqFormat the fastq format to set
   */
  void setFastqFormat(final FastqFormat fastqFormat);

  /**
   * Set the repTechGroup.
   * @param repTechGroup the technical replicate group to set
   */
  void setRepTechGroup(final String repTechGroup);

  /**
   * Set if the sample is the reference of the experiment.
   * @param reference true if the sample is a reference
   */
  void setReference(final boolean reference);

  //
  // Fields tests
  //

  /**
   * Test if a field exists.
   * @param field The field to test
   * @return true if the field exists
   */
  boolean isField(final String field);

  /**
   * Test if the comment field exists.
   * @return true if the field exists
   */
  boolean isCommentField();

  /**
   * Test if the reads field exists.
   * @return true if the field exists
   */
  boolean isReadsField();

  /**
   * Test if the genome field exists.
   * @return true if the field exists
   */
  boolean isGenomeField();

  /**
   * Test id the experiment field exists
   * @return true if the field exists
   */
  boolean isExperiment();

  /**
   * Test if the annotation field exists.
   * @return true if the field exists
   */
  boolean isAnnotationField();

  /**
   * Test if the additional annotation field exists.
   * @return true if the field exists
   */
  boolean isAdditionalAnnotationField();

  /**
   * Test if the description field exists.
   * @return true if the field exists
   */
  boolean isDescriptionField();

  /**
   * Test if the date field exists.
   * @return true if the field exists
   */
  boolean isDateField();

  /**
   * Test if the operator field exists.
   * @return true if the field exists
   */
  boolean isOperatorField();

  /**
   * Test if the serial number field exists.
   * @return true if the field exists
   */
  boolean isSerialNumberField();

  /**
   * Test if the condition field exists.
   * @return true if the field exists
   */
  boolean isConditionField();

  /**
   * Test if the UUID field exists.
   * @return true if the field exists
   */
  boolean isUUIDField();

  /**
   * Test if the FastqFormat field exists.
   * @return true if the field exists
   */
  boolean isFastqFormatField();

  /**
   * Test if the experiment field exists.
   * @return true if the field exists
   */
  boolean isExperimentField();

  /**
   * Test if the technical replicates group field exists.
   * @return true if the field exists
   */
  boolean isRepTechGroupField();

  /**
   * Test if the Reference exists
   * @return true if the reference exists
   */
  boolean isReferenceField();

}
