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

import java.io.Serializable;

import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class defines the default implementation of the experiment sample
 * metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */
public class ExperimentSampleMetadataImpl extends AbstractMetadata
    implements Serializable, ExperimentSampleMetadata {

  /** Serialization version UID. */
  private static final long serialVersionUID = 4079804296437126108L;

  //
  // Getters
  //

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
  public String getCondition() {
    return get(CONDITION_KEY);
  }

  //
  // Setters
  //

  @Override
  public void setRepTechGroup(final String newReptechGroup) {
    set(REP_TECH_GROUP_KEY, newReptechGroup);
  }

  @Override
  public void setReference(final boolean newReference) {
    setReference(Boolean.toString(newReference));
  }

  @Override
  public void setReference(final String newReference) {
    set(REFERENCE_KEY, newReference);
  }

  @Override
  public void setCondition(final String newCondition) {
    set(CONDITION_KEY, newCondition);
  }

  //
  // Contains
  //

  @Override
  public boolean containsRepTechGroup() {
    return contains(REP_TECH_GROUP_KEY);
  }

  @Override
  public boolean containsReference() {
    return contains(REFERENCE_KEY);
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
  ExperimentSampleMetadataImpl() {
  }

}
