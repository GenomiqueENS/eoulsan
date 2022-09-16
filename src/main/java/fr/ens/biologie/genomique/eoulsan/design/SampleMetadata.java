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

package fr.ens.biologie.genomique.eoulsan.design;

import java.util.List;

import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;

/**
 * This interface defines the sample metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface SampleMetadata extends Metadata {

  // constants
  String READS_KEY = "Reads";
  String DESCRIPTION_KEY = "Description";
  String OPERATOR_KEY = "Operator";
  String COMMENT_KEY = "Comment";
  String DATE_KEY = "Date";
  String SERIAL_NUMBER_KEY = "SerialNumber";
  String UUID_KEY = "UUID";
  String REP_TECH_GROUP_KEY = "RepTechGroup";
  String REFERENCE_KEY = "Reference";
  String FASTQ_FORMAT_KEY = "FastqFormat";
  String CONDITION_KEY = "Condition";

  /**
   * Get the reads as a list.
   * @return the list of reads
   */
  List<String> getReads();

  /**
   * Get the description.
   * @return the description
   */
  String getDescription();

  /**
   * Get the operator.
   * @return the operator
   */
  String getOperator();

  /**
   * Get the comment.
   * @return the comment
   */
  String getComment();

  /**
   * Get the date.
   * @return the date
   */
  String getDate();

  /**
   * Get the serial number.
   * @return the serial number
   */
  String getSerialNumber();

  /**
   * Get the UUID.
   * @return the UUID
   */
  String getUUID();

  /**
   * Get the RepTechGroup.
   * @return the RepTechGroup
   */
  String getRepTechGroup();

  /**
   * Get the reference.
   * @return the reference
   */
  String getReference();

  /**
   * Get the reference.
   * @return the reference
   */
  boolean isReference();

  /**
   * Get the fastq format.
   * @return the fastq format
   */
  FastqFormat getFastqFormat();

  /**
   * Get the condition.
   * @return the condition
   */
  String getCondition();

  /**
   * Set the reads.
   * @param newReads the new reads
   */
  void setReads(List<String> newReads);

  /**
   * Set the description.
   * @param newDescription the new description
   */
  void setDescription(String newDescription);

  /**
   * Set the operator.
   * @param newOperator the new operator
   */
  void setOperator(String newOperator);

  /**
   * Set the comment.
   * @param newComment the new comment
   */
  void setComment(String newComment);

  /**
   * Set the date.
   * @param newDate the new date
   */
  void setDate(String newDate);

  /**
   * Set the serial number.
   * @param newSerialNumber the new serial number
   */
  void setSerialNumber(String newSerialNumber);

  /**
   * Set the UUID.
   * @param newUUID the new UUID
   */
  void setUUID(String newUUID);

  /**
   * Set the ReptechGroup.
   * @param newReptechGroup the new ReptechGroup
   */
  void setRepTechGroup(String newReptechGroup);

  /**
   * Set the reference.
   * @param newReference the new reference
   */
  void setReference(String newReference);

  /**
   * Set the fastq format.
   * @param newfastqFormat the new fastq format
   */
  void setFastqFormat(FastqFormat newfastqFormat);

  /**
   * Set the condition.
   * @param newCondition the new condition
   */
  void setCondition(String newCondition);

  /**
   * Test if the reads field exists.
   * @return true if the reads field exists
   */
  boolean containsReads();

  /**
   * Test if the description field exists.
   * @return true if the description field exists
   */
  boolean containsDescription();

  /**
   * Test if the operator field exists.
   * @return true if the operator field exists
   */
  boolean containsOperator();

  /**
   * Test if the comment field exists.
   * @return true if the comment field exists
   */
  boolean containsComment();

  /**
   * Test if the date field exists.
   * @return true if the date field exists
   */
  boolean containsDate();

  /**
   * Test if the serial number field exists.
   * @return true if the serial number field exists
   */
  boolean containsSerialNumber();

  /**
   * Test if the UUID field exists.
   * @return true if the UUID field exists
   */
  boolean containsUUID();

  /**
   * Test if the RepTechGroup field exists.
   * @return true if the RepTechGroup field exists
   */
  boolean containsRepTechGroup();

  /**
   * Test if the reference field exists.
   * @return true if the reference field exists
   */
  boolean containsReference();

  /**
   * Test if the fastq format field exists.
   * @return true if the fastq format field exists
   */
  boolean containsFastqFormat();

  /**
   * Test if the condition field exists.
   * @return true if the condition field exists
   */
  boolean containsCondition();

}
