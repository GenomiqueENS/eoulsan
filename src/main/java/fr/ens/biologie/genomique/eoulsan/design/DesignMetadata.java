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
 * This class defines the design metadata.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class DesignMetadata extends AbstractMetadata implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -2481571646937449716L;

  // constants
  public static final String GENOME_FILE_KEY = "GenomeFile";
  public static final String GFF_FILE_KEY = "GffFile";
  public static final String GTF_FILE_KEY = "GtfFile";
  public static final String ADDITIONAL_ANNOTATION_FILE_KEY =
      "AdditionalAnnotationFile";

  //
  // Getters
  //

  /**
   * Get the genome file.
   * @return the genome file
   */
  public String getGenomeFile() {
    return getTrimmed(GENOME_FILE_KEY);
  }

  /**
   * Get the GFF file.
   * @return the GFF file
   */
  public String getGffFile() {
    return getTrimmed(GFF_FILE_KEY);
  }

  /**
   * Get the GTF file.
   * @return the GTF file
   */
  public String getGtfFile() {
    return getTrimmed(GTF_FILE_KEY);
  }

  /**
   * Get the additional annotation file.
   * @return the additional annotation file
   */
  public String getAdditionalAnnotationFile() {
    return getTrimmed(ADDITIONAL_ANNOTATION_FILE_KEY);
  }

  //
  // Setters
  //

  /**
   * Set the genome file.
   * @param newGenomeFile the new genome file
   */
  public void setGenomeFile(String newGenomeFile) {
    set(GENOME_FILE_KEY, newGenomeFile);
  }

  /**
   * Set the gff file.
   * @param newGffFile the new GFF file
   */
  public void setGffFile(String newGffFile) {
    set(GFF_FILE_KEY, newGffFile);
  }

  /**
   * Set the gff file.
   * @param newGtfFile the new GTF file
   */
  public void setGtfFile(String newGtfFile) {
    set(GTF_FILE_KEY, newGtfFile);
  }

  /**
   * Set the additional annotation file.
   * @param newAdditionalAnotationFile the new additional annotation file
   */
  public void setAdditionalAnnotationFile(String newAdditionalAnotationFile) {
    set(ADDITIONAL_ANNOTATION_FILE_KEY, newAdditionalAnotationFile);
  }

  //
  // Contains
  //

  /**
   * Test if the genomeFile field exists.
   * @return true if the genomeFile field exists
   */
  public boolean containsGenomeFile() {
    return contains(GENOME_FILE_KEY);
  }

  /**
   * Test if the gffFile field exists.
   * @return the gffFile field exists
   */
  public boolean containsGffFile() {
    return contains(GFF_FILE_KEY);
  }

  /**
   * Test if the gtfFile field exists.
   * @return the gtfFile field exists
   */
  public boolean containsGtfFile() {
    return contains(GTF_FILE_KEY);
  }

  /**
   * Test if the additional annotation file field exists.
   * @return the additional annotation file field exists
   */
  public boolean containsAdditionalAnnotationFile() {
    return contains(ADDITIONAL_ANNOTATION_FILE_KEY);
  }

  //
  // Constructor
  //
  public DesignMetadata() {
  }
}
