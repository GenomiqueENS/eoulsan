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

package fr.ens.biologie.genomique.eoulsan.data;

import java.util.Set;

import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;

/**
 * This interface define metadata of data objects.
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface DataMetadata {

  String PAIRED_END_KEY = "pairedend";
  String FASTQ_FORMAT_KEY = SampleMetadata.FASTQ_FORMAT_KEY;
  String SAMPLE_ID_KEY = Sample.SAMPLE_ID_FIELD;
  String SAMPLE_NAME_KEY = Sample.SAMPLE_NAME_FIELD;
  String SAMPLE_NUMBER_KEY = Sample.SAMPLE_NUMBER_FIELD;

  //
  // Low level methods
  //

  /**
   * Get the value of metadata entry.
   * @param key the key
   * @return the value related to key or null if the key does not exists
   */
  String get(String key);

  /**
   * Set a metadata entry.
   * @param key the key
   * @param value the value
   */
  void set(String key, String value);

  /**
   * Test if a key exists.
   * @param key the key to test
   * @return true if the key exists
   */
  boolean containsKey(String key);

  /**
   * Remove a entry.
   * @param key the key of the entry to remove
   * @return true if the entry has been removed
   */
  boolean removeKey(String key);

  /**
   * The the entries of the metadata with the entries of another metadata
   * object.
   * @param metadata the entries to add
   */
  void set(DataMetadata metadata);

  /**
   * Clear the entries of the object.
   */
  void clear();

  /**
   * Get the keys of the entries.
   * @return a set with the keys of the entries
   */
  Set<String> keySet();

  //
  // Predefined methods for common entries
  //

  /**
   * Test if the data is paired end data.
   * @return true if the data is paired end data
   */
  boolean isPairedEnd();

  /**
   * Set single-end/paired-end data type.
   * @param pairedEnd true if data is paired-end data
   */
  void setPairedEnd(boolean pairedEnd);

  /**
   * Get the FastqFormat. If value not set, the default format is fastq-sanger.
   * @return the fastq format
   */
  FastqFormat getFastqFormat();

  /**
   * Get the FastqFormat.
   * @param defaultValue the default value
   * @return the fastq format
   */
  FastqFormat getFastqFormat(FastqFormat defaultValue);

  /**
   * Set the FASTQ format of the data.
   * @param fastqFormat the FASTQ format
   */
  void setFastqFormat(FastqFormat fastqFormat);

  /**
   * Get the sample name related to the data.
   * @return a String with the sample name related to the data
   */
  String getSampleName();

  /**
   * Set the sample name related to the data.
   * @param sampleName the sample name
   */
  void setSampleName(String sampleName);

  /**
   * Get the sample id related to the data.
   * @return the sample id or null if the value is not set
   */
  String getSampleId();

  /**
   * Set the sample id related to the data
   * @param sampleId the sample id
   */
  void setSampleId(String sampleId);

  /**
   * Get the sample number related to the data.
   * @return the sample number or -1 if the value is not set
   */
  int getSampleNumber();

  /**
   * Set the sample number related to the data
   * @param sampleNumber the sample number
   */
  void setSampleNumber(int sampleNumber);

}
