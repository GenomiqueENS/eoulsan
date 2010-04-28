/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.design;

import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.io.BioAssayFormat;

/**
 * This interface define a slide.
 * @author Laurent Jourdren
 */
public interface Sample {

  //
  // Getters
  //

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

  /**
   * Get the source of a slide.
   * @return a DataSource object
   */
  DataSource getSource();

  /**
   * Get information about the source of the slide.
   * @return information about the source of the slide
   */
  String getSourceInfo();

  //
  // Setters
  //

  /**
   * Rename the slide.
   * @param newName The new name of the slide.
   */
  void setName(final String newName);

  /**
   * Set the data source of a slide.
   * @param source The source to set
   */
  void setSource(final DataSource source);

  /**
   * Set a filename as a source of a slide.
   * @param filename The filename to set
   */
  void setSource(final String filename);

  /**
   * Get the format of the source of the slide.
   * @return The format of the source of the slide
   */
  BioAssayFormat getSourceFormat();

  /**
   * Set the format of the source fof the slide.
   * @param format The format to set
   */
  void setSourceFormat(final String format);

  /**
   * Set the format of the source fof the slide.
   * @param format The format to set
   */
  void setSourceFormat(final BioAssayFormat format);

}
