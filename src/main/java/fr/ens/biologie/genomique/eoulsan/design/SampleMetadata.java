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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.design;

import static org.python.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;

/**
 * This class defines the sample metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class SampleMetadata extends AbstractMetadata implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -6298102513903455973L;

  // constants
  public static final String READS_KEY = "Reads";
  public static final String DESCRIPTION_KEY = "Description";
  public static final String OPERATOR_KEY = "Operator";
  public static final String COMMENT_KEY = "Comment";
  public static final String DATE_KEY = "Date";
  public static final String SERIAL_NUMBER_KEY = "SerialNumber";
  public static final String UUID_KEY = "UUID";
  public static final String REP_TECH_GROUP_KEY = "RepTechGroup";
  public static final String REFERENCE_KEY = "Reference";
  public static final String FASTQ_FORMAT_KEY = "FastqFormat";
  public static final String CONDITION_KEY = "Condition";

  //
  // Getters
  //

  /**
   * Get the reads as a list.
   * @return the list of reads
   */
  public List<String> getReads() {
    return getAsList(READS_KEY);
  }

  /**
   * Get the description.
   * @return the description
   */
  public String getDescription() {
    return get(DESCRIPTION_KEY);
  }

  /**
   * Get the operator.
   * @return the operator
   */
  public String getOperator() {
    return get(OPERATOR_KEY);
  }

  /**
   * Get the comment.
   * @return the comment
   */
  public String getComment() {
    return get(COMMENT_KEY);
  }

  /**
   * Get the date.
   * @return the date
   */
  public String getDate() {
    return get(DATE_KEY);
  }

  /**
   * Get the serial number.
   * @return the serial number
   */
  public String getSerialNumber() {
    return get(SERIAL_NUMBER_KEY);
  }

  /**
   * Get the UUID.
   * @return the UUID
   */
  public String getUUID() {
    return get(UUID_KEY);
  }

  /**
   * Get the RepTechGroup.
   * @return the RepTechGroup
   */
  public String getRepTechGroup() {
    return get(REP_TECH_GROUP_KEY);
  }

  /**
   * Get the reference.
   * @return the reference
   */
  public boolean isReference() {

    String value = get(REFERENCE_KEY);

    if (value == null) {
      return false;
    }

    value = value.trim().toLowerCase();

    return "t".equals(value)
        || "true".equals(value) || "y".equals(value) || "yes".equals(value);
  }

  /**
   * Get the fastq format.
   * @return the fastq format
   */
  public FastqFormat getFastqFormat() {

    return FastqFormat.getFormatFromName(get(FASTQ_FORMAT_KEY));
  }

  /**
   * Get the condition.
   * @return the condition
   */
  public String getCondition() {
    return get(CONDITION_KEY);
  }

  //
  // Setters
  //

  /**
   * Set the reads.
   * @param newReads the new reads
   */
  public void setReads(List<String> newReads) {
    set(READS_KEY, newReads);
  }

  /**
   * Set the description.
   * @param newDescription the new description
   */
  public void setDescription(String newDescription) {
    set(DESCRIPTION_KEY, newDescription);
  }

  /**
   * Set the operator.
   * @param newOperator the new operator
   */
  public void setOperator(String newOperator) {
    set(OPERATOR_KEY, newOperator);
  }

  /**
   * Set the comment.
   * @param newComment the new comment
   */
  public void setComment(String newComment) {
    set(COMMENT_KEY, newComment);
  }

  /**
   * Set the date.
   * @param newDate the new date
   */
  public void setDate(String newDate) {
    set(DATE_KEY, newDate);
  }

  /**
   * Set the serial number.
   * @param newSerialNumber the new serial number
   */
  public void setSerialNumber(String newSerialNumber) {
    set(SERIAL_NUMBER_KEY, newSerialNumber);
  }

  /**
   * Set the UUID.
   * @param newUUID the new UUID
   */
  public void setUUID(String newUUID) {
    set(UUID_KEY, newUUID);
  }

  /**
   * Set the ReptechGroup.
   * @param newReptechGroup the new ReptechGroup
   */
  public void setRepTechGroup(String newReptechGroup) {
    set(REP_TECH_GROUP_KEY, newReptechGroup);
  }

  /**
   * Set the reference.
   * @param newReference the new reference
   */
  public void setReference(String newReference) {
    set(REFERENCE_KEY, newReference);
  }

  /**
   * Set the fastq format.
   * @param newfastqFormat the new fastq format
   */
  public void setFastqFormat(FastqFormat newfastqFormat) {

    checkNotNull(newfastqFormat, "FastqFormat is null");

    set(FASTQ_FORMAT_KEY, newfastqFormat.getName());
  }

  /**
   * Set the condition.
   * @param newCondition the new condition
   */
  public void setCondition(String newCondition) {
    set(CONDITION_KEY, newCondition);
  }

  //
  // Contains
  //

  /**
   * Test if the reads field exists.
   * @return true if the reads field exists
   */
  public boolean containsReads() {
    return contains(READS_KEY);
  }

  /**
   * Test if the description field exists.
   * @return true if the description field exists
   */
  public boolean containsDescription() {
    return contains(DESCRIPTION_KEY);
  }

  /**
   * Test if the operator field exists.
   * @return true if the operator field exists
   */
  public boolean containsOperator() {
    return contains(OPERATOR_KEY);
  }

  /**
   * Test if the comment field exists.
   * @return true if the comment field exists
   */
  public boolean containsComment() {
    return contains(COMMENT_KEY);
  }

  /**
   * Test if the date field exists.
   * @return true if the date field exists
   */
  public boolean containsDate() {
    return contains(DATE_KEY);
  }

  /**
   * Test if the serial number field exists.
   * @return true if the serial number field exists
   */
  public boolean containsSerialNumber() {
    return contains(SERIAL_NUMBER_KEY);
  }

  /**
   * Test if the UUID field exists.
   * @return true if the UUID field exists
   */
  public boolean containsUUID() {
    return contains(UUID_KEY);
  }

  /**
   * Test if the RepTechGroup field exists.
   * @return true if the RepTechGroup field exists
   */
  public boolean containsRepTechGroup() {
    return contains(REP_TECH_GROUP_KEY);
  }

  /**
   * Test if the reference field exists.
   * @return true if the reference field exists
   */
  public boolean containsReference() {
    return contains(REFERENCE_KEY);
  }

  /**
   * Test if the fastq format field exists.
   * @return true if the fastq format field exists
   */
  public boolean containsFastqFormat() {
    return contains(FASTQ_FORMAT_KEY);
  }

  /**
   * Test if the condition field exists.
   * @return true if the condition field exists
   */
  public boolean containsCondition() {
    return contains(CONDITION_KEY);
  }

  //
  // Constructor
  //
  public SampleMetadata() {

  }

}
