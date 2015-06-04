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

package fr.ens.transcriptome.eoulsan.design2;

import java.io.Serializable;

/**
 * This class defines the experiment metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class ExperimentMetadata extends AbstractMetadata implements
    Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -625223839967520050L;
  
  // constants
  public static final String SKIP_KEY = "skip";
  public static final String REFERENCE_KEY = "Reference";
  public static final String MODEL_KEY = "model";
  public static final String CONTRAST_KEY = "contrast";
  public static final String BUILD_CONTRAST_KEY = "buildContrast";
  public static final String DESIGN_FILE_KEY = "designFile";
  public static final String COMPARISON_FILE_KEY = "comparisonFile";
  public static final String CONTRAST_FILE_KEY = "contrastFile";

  //
  // Getters
  //

  /**
   * Get the skip option.
   * @return the skip option
   */
  public String getSkip() {
    return get(SKIP_KEY);
  }

  /**
   * Get the reference option.
   * @return the reference option
   */
  public String getReference() {
    return get(REFERENCE_KEY);
  }

  /**
   * Get the model DEseq2 option.
   * @return the model DEseq2 option
   */
  public String getModel() {
    return get(MODEL_KEY);
  }

  /**
   * Get the contrast DEseq2 option.
   * @return the contrast DEseq2 option
   */
  public String getContrast() {
    return get(CONTRAST_KEY);
  }

  /**
   * Get the buildContrast DEseq2 option.
   * @return the buildContrast DEseq2 option
   */
  public String getBuildContrast() {
    return get(BUILD_CONTRAST_KEY);
  }

  /**
   * Get the designFile DEseq2 option.
   * @return the designFile DEseq2 option
   */
  public String getDesignFile() {
    return get(DESIGN_FILE_KEY);
  }

  /**
   * Get the comparisonFile DEseq2 option.
   * @return the comparisonFile DEseq2 option
   */
  public String getComparisonFile() {
    return get(COMPARISON_FILE_KEY);
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
  public void setSkip(String newSkip) {
    set(SKIP_KEY, newSkip);
  }

  /**
   * Set the reference option.
   * @param newReptechGroup the new reference option
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
  public void setContrast(String newContrast) {
    set(CONTRAST_KEY, newContrast);
  }

  /**
   * Set the buildContrast DEseq2 option.
   * @param newbuildContrast the new buildContrast DEseq2 option
   */
  public void setBuildContrast(String newBuildContrast) {
    set(BUILD_CONTRAST_KEY, newBuildContrast);
  }

  /**
   * Set the designFile DEseq2 option.
   * @param newDesignFile the new designFile DEseq2 option
   */
  public void setDesignFile(String newDesignFile) {
    set(DESIGN_FILE_KEY, newDesignFile);
  }

  /**
   * Set the comparisonFile DEseq2 option.
   * @param newComparisonFile the new comparisonFile DEseq2 option
   */
  public void setComparisonFile(String newComparisonFile) {
    set(COMPARISON_FILE_KEY, newComparisonFile);
  }

  /**
   * Set the contrastFile DEseq2 option.
   * @param newcontrastFile the new contrastFile DEseq2 option
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
   * Test if the comparisonFile option exists.
   * @return true if the comparisonFile option exists
   */
  public boolean containsComparisonFile() {
    return contains(COMPARISON_FILE_KEY);
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
