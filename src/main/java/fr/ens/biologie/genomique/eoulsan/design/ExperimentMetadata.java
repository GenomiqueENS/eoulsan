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

/**
 * This class defines the experiment metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class ExperimentMetadata extends AbstractMetadata
    implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -625223839967520050L;

  // constants
  public static final String SKIP_KEY = "skip";
  public static final String REFERENCE_KEY = "reference";
  public static final String MODEL_KEY = "model";
  public static final String CONTRAST_KEY = "contrast";
  public static final String BUILD_CONTRAST_KEY = "buildContrast";
  public static final String DESIGN_FILE_KEY = "designFile";
  public static final String COMPARISONS_KEY = "comparisons";
  public static final String CONTRAST_FILE_KEY = "contrastFile";

  //
  // Getters
  //

  /**
   * Get the skip option.
   * @return the skip option
   */
  public boolean isSkip() {
    return getAsBoolean(SKIP_KEY);
  }

  /**
   * Get the reference option.
   * @return the reference option
   */
  public String getReference() {
    return getTrimmed(REFERENCE_KEY);
  }

  /**
   * Get the model DEseq2 option.
   * @return the model DEseq2 option
   */
  public String getModel() {
    return getTrimmed(MODEL_KEY);
  }

  /**
   * Get the contrast DEseq2 option.
   * @return the contrast DEseq2 option
   */
  public boolean isContrast() {
    return getAsBoolean(CONTRAST_KEY);
  }

  /**
   * Get the buildContrast DEseq2 option.
   * @return the buildContrast DEseq2 option
   */
  public boolean isBuildContrast() {
    return getAsBoolean(BUILD_CONTRAST_KEY);
  }

  /**
   * Get the designFile DEseq2 option.
   * @return the designFile DEseq2 option
   */
  public String getDesignFile() {
    return get(DESIGN_FILE_KEY);
  }

  /**
   * Get the comparisons DEseq2 option.
   * @return the comparisons DEseq2 option
   */
  public String getComparisons() {
    return get(COMPARISONS_KEY);
  }

  /**
   * Get the contrastFile DEseq2 option.
   * @return the contrastFile DEseq2 option
   */
  public String getContrastFile() {
    return get(CONTRAST_FILE_KEY);
  }

  //
  // Setters
  //

  /**
   * Set the skip option.
   * @param newSkip the new skip option
   */
  public void setSkip(boolean newSkip) {
    set(SKIP_KEY, "" + newSkip);
  }

  /**
   * Set the reference option.
   * @param newReference the new reference option
   */
  public void setReference(String newReference) {
    set(REFERENCE_KEY, newReference);
  }

  /**
   * Set the model DEseq2 option.
   * @param newModel the new model DEseq2 option
   */
  public void setModel(String newModel) {
    set(MODEL_KEY, newModel);
  }

  /**
   * Set the contrast DEseq2 option.
   * @param newContrast the new contrast DEseq2 option
   */
  public void setContrast(boolean newContrast) {
    set(CONTRAST_KEY, "" + newContrast);
  }

  /**
   * Set the buildContrast DEseq2 option.
   * @param newBuildContrast the new buildContrast DEseq2 option
   */
  public void setBuildContrast(boolean newBuildContrast) {
    set(BUILD_CONTRAST_KEY, "" + newBuildContrast);
  }

  /**
   * Set the designFile DEseq2 option.
   * @param newDesignFile the new designFile DEseq2 option
   */
  public void setDesignFile(String newDesignFile) {
    set(DESIGN_FILE_KEY, newDesignFile);
  }

  /**
   * Set the comparisons DEseq2 option.
   * @param newComparisons the new comparisons DEseq2 option
   */
  public void setComparisons(String newComparisons) {
    set(COMPARISONS_KEY, newComparisons);
  }

  /**
   * Set the contrastFile DEseq2 option.
   * @param newContrastFile the new contrastFile DEseq2 option
   */
  public void setContrastFile(String newContrastFile) {
    set(CONTRAST_FILE_KEY, newContrastFile);
  }

  //
  // Contains
  //

  /**
   * Test if the skip option exists.
   * @return true if the skip option exists
   */
  public boolean containsSkip() {
    return contains(SKIP_KEY);
  }

  /**
   * Test if the reference option exists.
   * @return true if the reference option exists
   */
  public boolean containsReference() {
    return contains(REFERENCE_KEY);
  }

  /**
   * Test if the model option exists.
   * @return true if the model option exists
   */
  public boolean containsModel() {
    return contains(MODEL_KEY);
  }

  /**
   * Test if the contrast option exists.
   * @return true if the contrast option exists
   */
  public boolean containsContrast() {
    return contains(CONTRAST_KEY);
  }

  /**
   * Test if the buildContrast option exists.
   * @return true if the buildContrast option exists
   */
  public boolean containsBuildContrast() {
    return contains(BUILD_CONTRAST_KEY);
  }

  /**
   * Test if the designFile option exists.
   * @return true if the designFile option exists
   */
  public boolean containsDesignFile() {
    return contains(DESIGN_FILE_KEY);
  }

  /**
   * Test if the comparisons option exists.
   * @return true if the comparisons option exists
   */
  public boolean containsComparisons() {
    return contains(COMPARISONS_KEY);
  }

  /**
   * Test if the contrastFile option exists.
   * @return true if the contrastFile option exists
   */
  public boolean containsContrastFile() {
    return contains(CONTRAST_FILE_KEY);
  }

  //
  // Constructor
  //
  public ExperimentMetadata() {

  }

}
