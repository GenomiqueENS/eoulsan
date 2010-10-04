/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.design;

import java.util.List;

/**
 * This interface define the description of a slide
 * @author Laurent Jourdren
 */
public interface SampleMetadata {

  /** Description field. */
  String DESCRIPTION_FIELD = "Description";
  /** Genome field. */
  String GENOME_FIELD = "Genome";
  /** Annotation field. */
  String ANNOTATION_FIELD = "Annotation";
  /** Comment field. */
  String COMMENT_FIELD = "Comment";
  /** Date field. */
  String DATE_FIELD = "Date";
  /** Slide Number field. */
  String SLIDE_NUMBER_FIELD = "SildeNumber";
  /** Serial Number field. */
  String SERIAL_NUMBER_FIELD = "Serial number";
  /** Operator field. */
  String OPERATOR_FIELD = "Operator";
  /** Operator field. */
  String CONDITION_FIELD = "Condition";
  /** Operator field. */
  String REPLICAT_TYPE_FIELD = "ReplicateType";

  /**
   * Get a description.
   * @param field Field of the description to get
   * @return a String with the description
   */
  String get(final String field);

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
   * Get the genome file relative to the sample.
   * @return Returns the genome file
   */
  String getGenome();

  /**
   * Get the annotation relative to the sample.
   * @return Returns the annaotation
   */
  String getAnnotation();

  /**
   * Get the date of the hybridation
   * @return Returns the date
   */
  String getDate();

  /**
   * Get the name of the operator
   * @return Returns the operator
   */
  String getOperator();

  /**
   * Get the slide number of the sample
   * @return Returns the slide number
   */
  String getSampleNumber();

  /**
   * Get the serial number of the sample
   * @return Returns the serialNumber
   */
  String getSerialNumber();

  /**
   * Get the condition of the sample
   * @return Returns the condition
   */
  String getCondition();

  /**
   * Get replicat type of the sample
   * @return Returns the replicat
   */
  String getReplicatType();

  /**
   * Set a field of the metadata.
   * @param field Field to set
   * @param value value to set
   */
  void set(final String field, final String value);

  /**
   * Set the description.
   * @param description The description to set
   */
  void setDescription(final String description);

  /**
   * Set the comment.
   * @param comment The comment to set
   */
  void setComment(final String comment);

  /**
   * Set the genome file relative to the sample.
   * @param genome file to set
   */
  void setGenome(final String genome);

  /**
   * Set the annotation file relative to the sample.
   * @param annotation file to set
   */
  void setAnnotation(final String annotation);

  /**
   * Set the hybridation date
   * @param date The date to set
   */
  void setDate(final String date);

  /**
   * Set the name of the operator
   * @param operator The operator to set
   */
  void setOperator(final String operator);

  /**
   * Set the slide number of the sample
   * @param slideNumber The slideNumber to set
   */
  void setSlideNumber(final int slideNumber);

  /**
   * Set the serial number of the sample
   * @param serialNumber The serialNumber to set
   */
  void setSerialNumber(final String serialNumber);

  /**
   * Set the condition of the sample
   * @param condition The condition to set
   */
  void setCondition(final String condition);

  /**
   * Set the replicat type of the sample
   * @param replicatType The replicat type to set
   */
  void setReplicatType(final String replicatType);

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
   * Test if the genome field exists.
   * @return true if the field exists
   */
  boolean isGenomeField();

  /**
   * Test if the annoatation field exists.
   * @return true if the field exists
   */
  boolean isAnnotationField();

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
  boolean isCondition();

  /**
   * Test if the replicat type field exists.
   * @return true if the field exists
   */
  boolean isReplicatType();

}
