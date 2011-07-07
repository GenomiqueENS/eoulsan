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

import java.util.List;

/**
 * This interface define a Design.
 * @author Laurent Jourdren
 */
public interface Design {

  /**
   * Test if a sample exists.
   * @param sampleName Name of the sample to test
   * @return true if the sample exists
   */
  boolean isSample(final String sampleName);

  /**
   * Add a sample.
   * @param sampleName Name of the sample to add
   */
  void addSample(final String sampleName);

  /**
   * Rename a sample.
   * @param oldSampleName Old name of the sample
   * @param newSampleName New name of the sample
   */
  void renameSample(final String oldSampleName, final String newSampleName);

  /**
   * Get the name of the samples
   * @return a Set with the name of the samples
   */
  List<String> getSamplesNames();

  /**
   * Remove a sample.
   * @param sampleName Name of the sample to remove
   */
  void removeSample(final String sampleName);

  /**
   * Get the number of sample in the design.
   * @return The number of samples in the design
   */
  int getSampleCount();

  /**
   * Get the metadata for a sample.
   * @param sampleName Name of the slide
   * @return the metadata of the sample
   */
  SampleMetadata getSampleMetadata(final String sampleName);

  /**
   * Extract a sample object from the design.
   * @param index Index of the sample in the design
   * @return a slide object
   */
  Sample getSample(final int index);

  /**
   * Extract a sample object from the design.
   * @param sampleName The name of the slide to extract
   * @return a sample object
   */
  Sample getSample(final String sampleName);

  /**
   * Get a list of the samples of the design
   * @return a unmodifiable list of the samples
   */
  List<Sample> getSamples();

  /**
   * Test if the metadata field is already set.
   * @param fieldName Name of the metadata field to test
   * @return true if the field exists
   */
  boolean isMetadataField(final String fieldName);

  /**
   * Add a metadata field.
   * @param fieldName Name of the label to add
   */
  void addMetadataField(final String fieldName);

  /**
   * Rename a metadata field.
   * @param oldMetadataFieldName Old name of the metadata field
   * @param newMetadataFieldName New name of the metadata field
   */
  void renameMetadataField(final String oldMetadataFieldName,
      final String newMetadataFieldName);

  /**
   * Get the names of the metadata fields.
   * @return A List with the name of the metadata fields
   */
  List<String> getMetadataFieldsNames();

  /**
   * Remove a metadata field.
   * @param fieldName Name of the metadata field to remove
   */
  void removeMetadataField(final String fieldName);

  /**
   * Get the number of metadata fields
   * @return The number of metadata fields
   */
  int getMetadataFieldCount();

  /**
   * Set a metadata field for a sample.
   * @param sampleName Sample name
   * @param fieldName metadata field
   * @param value of the description to set
   */
  void setMetadata(final String sampleName, final String fieldName,
      final String value);

  /**
   * Get a metadata
   * @param sampleName Sample name
   * @param fieldName The metadata field
   * @return The value of the metadata
   */
  String getMetadata(final String sampleName, final String fieldName);

}
