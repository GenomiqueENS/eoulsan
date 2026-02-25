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

import java.util.List;

/**
 * This interface defines an experiment.
 *
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface Experiment {

  /**
   * Get the design related to the experiment.
   *
   * @return the Design object related to the experiment
   */
  Design getDesign();

  /**
   * get the experiment id.
   *
   * @return the experiment id
   */
  String getId();

  /**
   * Get the experiment name.
   *
   * @return the experiment name
   */
  String getName();

  /**
   * Get the experiment number.
   *
   * @return the experiment number
   */
  int getNumber();

  /**
   * Get the experiment metadata.
   *
   * @return the experiment metadata
   */
  ExperimentMetadata getMetadata();

  /**
   * Get the samples of the experiment.
   *
   * @return a list of ExperimentSample object
   */
  List<Sample> getSamples();

  /**
   * Get experiment samples list.
   *
   * @return a list of ExperimentSample object
   */
  List<ExperimentSample> getExperimentSamples();

  /**
   * Get the experiment sample related to the sample.
   *
   * @param sample the sample
   * @return an experiment sample object if exists or null
   */
  ExperimentSample getExperimentSample(Sample sample);

  /**
   * Set the name of the experiment.
   *
   * @param newExperimentName the new experiment name
   */
  void setName(String newExperimentName);

  /**
   * Add a sample.
   *
   * @param sample the sample to add
   * @return an experiment sample object
   */
  ExperimentSample addSample(Sample sample);

  /**
   * Remove the sample.
   *
   * @param sample the sample to remove
   */
  void removeSample(Sample sample);

  /**
   * Test if the experiment contains a sample.
   *
   * @param sample the sample to test
   * @return true if the sample is the experiment
   */
  boolean containsSample(Sample sample);
}
