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
 * This interface defines the design.
 * @author Xavier Bauquet
 * @since 2.0
 */
public interface Design {

  /**
   * Set the design name.
   * @param newDesignName the new design name
   */
  void setName(String newDesignName);

  /**
   * Get the name of a sample.
   * @param sampleId the sample id
   * @return a sample object
   */
  Sample getSample(String sampleId);

  /**
   * Get the list of the samples.
   * @return the list of the samples
   */
  List<Sample> getSamples();

  /**
   * Get the name of an experiment.
   * @param experimentId the experiment id
   * @return an experiment object
   */
  Experiment getExperiment(String experimentId);

  /**
   * Get the list of the experiments.
   * @return the list of the experiments
   */
  List<Experiment> getExperiments();

  /**
   * Get the design Metadata.
   * @return a designMetadata object
   */
  DesignMetadata getMetadata();

  /**
   * Get design number.
   * @return the design number
   */
  int getNumber();

  /**
   * Get design name.
   * @return the design name
   */
  String getName();

  /**
   * Remove the sample.
   * @param sampleId the sample id
   */
  void removeSample(String sampleId);

  /**
   * Remove the experiment.
   * @param experimentId the experiment id
   */
  void removeExperiment(String experimentId);

  /**
   * Test if the sample exists.
   * @param sampleId the sample id
   * @return true if the sample exists
   */
  boolean containsSample(String sampleId);

  /**
   * Test if the experiment exists.
   * @param experimentId the experiment id
   * @return true if the experiment exists
   */
  boolean containsExperiment(String experimentId);

  /**
   * Test if the sample name exists.
   * @param sampleName the sample name
   * @return true if the sample exists
   */
  boolean containsSampleName(String sampleName);

  /**
   * Test if the experiment exists.
   * @param experimentName the experiment name
   * @return true if the experiment exists
   */
  boolean containsExperimentName(String experimentName);

  /**
   * Add a sample.
   * @param sampleId the sample id
   * @return the sample object
   */
  SampleImpl addSample(String sampleId);

  /**
   * Add an experiment.
   * @param experimentId the experiment id
   * @return the experiment object
   */
  Experiment addExperiment(String experimentId);

  /**
   * Get all the experiments related to a sample.
   * @param sampleId the sample
   * @return a list with the experiments that use the sample
   */
  List<Experiment> getExperimentsUsingASample(Sample sampleId);

}
