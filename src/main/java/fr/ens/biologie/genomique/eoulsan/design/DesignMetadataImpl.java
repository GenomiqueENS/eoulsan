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
 * This class defines the default implementation of the design metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */
public class DesignMetadataImpl extends AbstractMetadata
    implements DesignMetadata {

  /** Serialization version UID. */
  private static final long serialVersionUID = -2481571646937449716L;

  //
  // Getters
  //

  @Override
  public String getGenomeFile() {
    return getTrimmed(GENOME_FILE_KEY);
  }

  @Override
  public String getGffFile() {
    return getTrimmed(GFF_FILE_KEY);
  }

  @Override
  public String getGtfFile() {
    return getTrimmed(GTF_FILE_KEY);
  }

  @Override
  public String getAdditionalAnnotationFile() {
    return getTrimmed(ADDITIONAL_ANNOTATION_FILE_KEY);
  }

  //
  // Setters
  //

  @Override
  public void setGenomeFile(String newGenomeFile) {
    set(GENOME_FILE_KEY, newGenomeFile);
  }

  @Override
  public void setGffFile(String newGffFile) {
    set(GFF_FILE_KEY, newGffFile);
  }

  @Override
  public void setGtfFile(String newGtfFile) {
    set(GTF_FILE_KEY, newGtfFile);
  }

  @Override
  public void setAdditionalAnnotationFile(String newAdditionalAnotationFile) {
    set(ADDITIONAL_ANNOTATION_FILE_KEY, newAdditionalAnotationFile);
  }

  //
  // Contains
  //

  @Override
  public boolean containsGenomeFile() {
    return contains(GENOME_FILE_KEY);
  }

  @Override
  public boolean containsGffFile() {
    return contains(GFF_FILE_KEY);
  }

  @Override
  public boolean containsGtfFile() {
    return contains(GTF_FILE_KEY);
  }

  @Override
  public boolean containsAdditionalAnnotationFile() {
    return contains(ADDITIONAL_ANNOTATION_FILE_KEY);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  DesignMetadataImpl() {
  }

}
