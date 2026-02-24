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

import static java.util.Objects.requireNonNull;

import java.util.List;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;

/**
 * This class defines the default implementation of the sample metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */
class SampleMetadataImpl extends AbstractMetadata
    implements SampleMetadata {

  /** Serialization version UID. */
  private static final long serialVersionUID = -6298102513903455973L;

  //
  // Getters
  //

  @Override
  public List<String> getReads() {
    return getAsList(READS_KEY);
  }

  @Override
  public String getDescription() {
    return getTrimmed(DESCRIPTION_KEY);
  }

  @Override
  public String getOperator() {
    return getTrimmed(OPERATOR_KEY);
  }

  @Override
  public String getComment() {
    return getTrimmed(COMMENT_KEY);
  }

  @Override
  public String getDate() {
    return getTrimmed(DATE_KEY);
  }

  @Override
  public String getSerialNumber() {
    return getTrimmed(SERIAL_NUMBER_KEY);
  }

  @Override
  public String getUUID() {
    return getTrimmed(UUID_KEY);
  }

  @Override
  public String getRepTechGroup() {
    return getTrimmed(REP_TECH_GROUP_KEY);
  }

  @Override
  public String getReference() {
    return getTrimmed(REFERENCE_KEY);
  }

  @Override
  public boolean isReference() {

    String value = getReference();

    if (value == null) {
      return false;
    }

    value = value.trim().toLowerCase(Globals.DEFAULT_LOCALE);

    return "t".equals(value)
        || "true".equals(value) || "y".equals(value) || "yes".equals(value);
  }

  @Override
  public FastqFormat getFastqFormat() {

    return FastqFormat.getFormatFromName(get(FASTQ_FORMAT_KEY));
  }

  @Override
  public String getCondition() {
    return get(CONDITION_KEY);
  }

  //
  // Setters
  //

  @Override
  public void setReads(List<String> newReads) {
    set(READS_KEY, newReads);
  }

  @Override
  public void setDescription(String newDescription) {
    set(DESCRIPTION_KEY, newDescription);
  }

  @Override
  public void setOperator(String newOperator) {
    set(OPERATOR_KEY, newOperator);
  }

  @Override
  public void setComment(String newComment) {
    set(COMMENT_KEY, newComment);
  }

  @Override
  public void setDate(String newDate) {
    set(DATE_KEY, newDate);
  }

  @Override
  public void setSerialNumber(String newSerialNumber) {
    set(SERIAL_NUMBER_KEY, newSerialNumber);
  }

  @Override
  public void setUUID(String newUUID) {
    set(UUID_KEY, newUUID);
  }

  @Override
  public void setRepTechGroup(String newReptechGroup) {
    set(REP_TECH_GROUP_KEY, newReptechGroup);
  }

  @Override
  public void setReference(String newReference) {
    set(REFERENCE_KEY, newReference);
  }

  @Override
  public void setFastqFormat(FastqFormat newfastqFormat) {

    requireNonNull(newfastqFormat, "FastqFormat is null");

    set(FASTQ_FORMAT_KEY, newfastqFormat.getName());
  }

  @Override
  public void setCondition(String newCondition) {
    set(CONDITION_KEY, newCondition);
  }

  //
  // Contains
  //

  @Override
  public boolean containsReads() {
    return contains(READS_KEY);
  }

  @Override
  public boolean containsDescription() {
    return contains(DESCRIPTION_KEY);
  }

  @Override
  public boolean containsOperator() {
    return contains(OPERATOR_KEY);
  }

  @Override
  public boolean containsComment() {
    return contains(COMMENT_KEY);
  }

  @Override
  public boolean containsDate() {
    return contains(DATE_KEY);
  }

  @Override
  public boolean containsSerialNumber() {
    return contains(SERIAL_NUMBER_KEY);
  }

  @Override
  public boolean containsUUID() {
    return contains(UUID_KEY);
  }

  @Override
  public boolean containsRepTechGroup() {
    return contains(REP_TECH_GROUP_KEY);
  }

  @Override
  public boolean containsReference() {
    return contains(REFERENCE_KEY);
  }

  @Override
  public boolean containsFastqFormat() {
    return contains(FASTQ_FORMAT_KEY);
  }

  @Override
  public boolean containsCondition() {
    return contains(CONDITION_KEY);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  SampleMetadataImpl() {
  }

}
