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
 * This interface defines a sample.
 *
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface Sample {

  /** Sample Id field. */
  String SAMPLE_ID_FIELD = "SampleId";

  /** Sample Name field. */
  String SAMPLE_NAME_FIELD = "SampleName";

  /** Sample Name field. */
  String SAMPLE_NUMBER_FIELD = "SampleNumber";

  /**
   * Get the design related to the sample.
   *
   * @return the Design object related to the sample
   */
  Design getDesign();

  /**
   * Get the sample id.
   *
   * @return the sample id
   */
  String getId();

  /**
   * Get the sample number.
   *
   * @return the sample number
   */
  int getNumber();

  /**
   * Get the sample name.
   *
   * @return the sample name
   */
  String getName();

  /**
   * Get the sample metadata.
   *
   * @return an object SampleMetadata
   */
  SampleMetadata getMetadata();

  /**
   * Set the sample name.
   *
   * @param newSampleName the new sample name
   */
  void setName(String newSampleName);
}
