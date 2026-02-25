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
 * This interface defines the design metadata.
 *
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface DesignMetadata extends Metadata {

  // Constants
  String GENOME_FILE_KEY = "GenomeFile";
  String GFF_FILE_KEY = "GffFile";
  String GTF_FILE_KEY = "GtfFile";
  String ADDITIONAL_ANNOTATION_FILE_KEY = "AdditionalAnnotationFile";

  /**
   * Get the genome file.
   *
   * @return the genome file
   */
  String getGenomeFile();

  /**
   * Get the GFF file.
   *
   * @return the GFF file
   */
  String getGffFile();

  /**
   * Get the GTF file.
   *
   * @return the GTF file
   */
  String getGtfFile();

  /**
   * Get the additional annotation file.
   *
   * @return the additional annotation file
   */
  String getAdditionalAnnotationFile();

  /**
   * Set the genome file.
   *
   * @param newGenomeFile the new genome file
   */
  void setGenomeFile(String newGenomeFile);

  /**
   * Set the gff file.
   *
   * @param newGffFile the new GFF file
   */
  void setGffFile(String newGffFile);

  /**
   * Set the gff file.
   *
   * @param newGtfFile the new GTF file
   */
  void setGtfFile(String newGtfFile);

  /**
   * Set the additional annotation file.
   *
   * @param newAdditionalAnotationFile the new additional annotation file
   */
  void setAdditionalAnnotationFile(String newAdditionalAnotationFile);

  /**
   * Test if the genomeFile field exists.
   *
   * @return true if the genomeFile field exists
   */
  boolean containsGenomeFile();

  /**
   * Test if the gffFile field exists.
   *
   * @return the gffFile field exists
   */
  boolean containsGffFile();

  /**
   * Test if the gtfFile field exists.
   *
   * @return the gtfFile field exists
   */
  boolean containsGtfFile();

  /**
   * Test if the additional annotation file field exists.
   *
   * @return the additional annotation file field exists
   */
  boolean containsAdditionalAnnotationFile();
}
