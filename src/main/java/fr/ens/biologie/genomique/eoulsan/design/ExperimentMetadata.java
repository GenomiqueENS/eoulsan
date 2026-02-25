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
 * This interface defines the experiment metadata.
 *
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface ExperimentMetadata extends Metadata {

  // constants
  String SKIP_KEY = "skip";
  String REFERENCE_KEY = "reference";
  String MODEL_KEY = "model";
  String CONTRAST_KEY = "contrast";
  String BUILD_CONTRAST_KEY = "buildContrast";
  String DESIGN_FILE_KEY = "designFile";
  String COMPARISONS_KEY = "comparisons";
  String CONTRAST_FILE_KEY = "contrastFile";

  /**
   * Get the skip option.
   *
   * @return the skip option
   */
  boolean isSkip();

  /**
   * Get the reference option.
   *
   * @return the reference option
   */
  String getReference();

  /**
   * Get the model DEseq2 option.
   *
   * @return the model DEseq2 option
   */
  String getModel();

  /**
   * Get the contrast DEseq2 option.
   *
   * @return the contrast DEseq2 option
   */
  boolean isContrast();

  /**
   * Get the buildContrast DEseq2 option.
   *
   * @return the buildContrast DEseq2 option
   */
  boolean isBuildContrast();

  /**
   * Get the designFile DEseq2 option.
   *
   * @return the designFile DEseq2 option
   */
  String getDesignFile();

  /**
   * Get the comparisons DEseq2 option.
   *
   * @return the comparisons DEseq2 option
   */
  String getComparisons();

  /**
   * Get the contrastFile DEseq2 option.
   *
   * @return the contrastFile DEseq2 option
   */
  String getContrastFile();

  /**
   * Set the skip option.
   *
   * @param newSkip the new skip option
   */
  void setSkip(boolean newSkip);

  /**
   * Set the reference option.
   *
   * @param newReference the new reference option
   */
  void setReference(String newReference);

  /**
   * Set the model DEseq2 option.
   *
   * @param newModel the new model DEseq2 option
   */
  void setModel(String newModel);

  /**
   * Set the contrast DEseq2 option.
   *
   * @param newContrast the new contrast DEseq2 option
   */
  void setContrast(boolean newContrast);

  /**
   * Set the buildContrast DEseq2 option.
   *
   * @param newBuildContrast the new buildContrast DEseq2 option
   */
  void setBuildContrast(boolean newBuildContrast);

  /**
   * Set the designFile DEseq2 option.
   *
   * @param newDesignFile the new designFile DEseq2 option
   */
  void setDesignFile(String newDesignFile);

  /**
   * Set the comparisons DEseq2 option.
   *
   * @param newComparisons the new comparisons DEseq2 option
   */
  void setComparisons(String newComparisons);

  /**
   * Set the contrastFile DEseq2 option.
   *
   * @param newContrastFile the new contrastFile DEseq2 option
   */
  void setContrastFile(String newContrastFile);

  /**
   * Test if the skip option exists.
   *
   * @return true if the skip option exists
   */
  boolean containsSkip();

  /**
   * Test if the reference option exists.
   *
   * @return true if the reference option exists
   */
  boolean containsReference();

  /**
   * Test if the model option exists.
   *
   * @return true if the model option exists
   */
  boolean containsModel();

  /**
   * Test if the contrast option exists.
   *
   * @return true if the contrast option exists
   */
  boolean containsContrast();

  /**
   * Test if the buildContrast option exists.
   *
   * @return true if the buildContrast option exists
   */
  boolean containsBuildContrast();

  /**
   * Test if the designFile option exists.
   *
   * @return true if the designFile option exists
   */
  boolean containsDesignFile();

  /**
   * Test if the comparisons option exists.
   *
   * @return true if the comparisons option exists
   */
  boolean containsComparisons();

  /**
   * Test if the contrastFile option exists.
   *
   * @return true if the contrastFile option exists
   */
  boolean containsContrastFile();
}
