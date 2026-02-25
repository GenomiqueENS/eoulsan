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

/**
 * This class defines the default implementation of the experiment metadata.
 *
 * @author Xavier Bauquet
 * @since 2.0
 */
class ExperimentMetadataImpl extends AbstractMetadata implements ExperimentMetadata {

  /** Serialization version UID. */
  private static final long serialVersionUID = -625223839967520050L;

  //
  // Getters
  //

  @Override
  public boolean isSkip() {
    return getAsBoolean(SKIP_KEY);
  }

  @Override
  public String getReference() {
    return getTrimmed(REFERENCE_KEY);
  }

  @Override
  public String getModel() {
    return getTrimmed(MODEL_KEY);
  }

  @Override
  public boolean isContrast() {
    return getAsBoolean(CONTRAST_KEY);
  }

  @Override
  public boolean isBuildContrast() {
    return getAsBoolean(BUILD_CONTRAST_KEY);
  }

  @Override
  public String getDesignFile() {
    return get(DESIGN_FILE_KEY);
  }

  @Override
  public String getComparisons() {
    return get(COMPARISONS_KEY);
  }

  @Override
  public String getContrastFile() {
    return get(CONTRAST_FILE_KEY);
  }

  //
  // Setters
  //

  @Override
  public void setSkip(boolean newSkip) {
    set(SKIP_KEY, "" + newSkip);
  }

  @Override
  public void setReference(String newReference) {
    set(REFERENCE_KEY, newReference);
  }

  @Override
  public void setModel(String newModel) {
    set(MODEL_KEY, newModel);
  }

  @Override
  public void setContrast(boolean newContrast) {
    set(CONTRAST_KEY, "" + newContrast);
  }

  @Override
  public void setBuildContrast(boolean newBuildContrast) {
    set(BUILD_CONTRAST_KEY, "" + newBuildContrast);
  }

  @Override
  public void setDesignFile(String newDesignFile) {
    set(DESIGN_FILE_KEY, newDesignFile);
  }

  @Override
  public void setComparisons(String newComparisons) {
    set(COMPARISONS_KEY, newComparisons);
  }

  @Override
  public void setContrastFile(String newContrastFile) {
    set(CONTRAST_FILE_KEY, newContrastFile);
  }

  //
  // Contains
  //

  @Override
  public boolean containsSkip() {
    return contains(SKIP_KEY);
  }

  @Override
  public boolean containsReference() {
    return contains(REFERENCE_KEY);
  }

  @Override
  public boolean containsModel() {
    return contains(MODEL_KEY);
  }

  @Override
  public boolean containsContrast() {
    return contains(CONTRAST_KEY);
  }

  @Override
  public boolean containsBuildContrast() {
    return contains(BUILD_CONTRAST_KEY);
  }

  @Override
  public boolean containsDesignFile() {
    return contains(DESIGN_FILE_KEY);
  }

  @Override
  public boolean containsComparisons() {
    return contains(COMPARISONS_KEY);
  }

  @Override
  public boolean containsContrastFile() {
    return contains(CONTRAST_FILE_KEY);
  }

  //
  // Constructor
  //

  /** Constructor. */
  ExperimentMetadataImpl() {}
}
