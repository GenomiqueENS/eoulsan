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

package fr.ens.transcriptome.eoulsan.design;

/**
 * This interface define a slide.
 * @author Laurent Jourdren
 */
public interface Sample {

  //
  // Getters
  //

  /**
   * Get the id of the sample
   * @return the id of the sample
   */
  int getId();

  /**
   * Get the name of the slide.
   * @return The name of the slide
   */
  String getName();

  /**
   * Get the description of the slide.
   * @return a SlideDescriptionImpl Object
   */
  SampleMetadata getMetadata();

  //
  // Setters
  //

  /**
   * Set the id of the sample.
   * @param id the id of the sample
   */
  void setId(final int id);

  /**
   * Rename the slide.
   * @param newName The new name of the slide.
   */
  void setName(final String newName);

}
