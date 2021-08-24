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

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * This class defines the default implementation of the experiment sample.
 * @author Xavier Bauquet
 * @since 2.0
 */
public class ExperimentSampleImpl implements Serializable, ExperimentSample {

  /** Serialization version UID. */
  private static final long serialVersionUID = -3180171254543892681L;

  private final Sample sample;
  private final ExperimentSampleMetadataImpl metadata =
      new ExperimentSampleMetadataImpl();

  //
  // Getters
  //

  @Override
  public Sample getSample() {
    return this.sample;
  }

  @Override
  public ExperimentSampleMetadataImpl getMetadata() {
    return this.metadata;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("experimentSampleName", this.sample)
        .add("experimentSampleMetadata", this.metadata).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.sample, this.metadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof ExperimentSampleImpl)) {
      return false;
    }

    final ExperimentSampleImpl that = (ExperimentSampleImpl) o;

    return Objects.equals(this.sample, that.sample)
        && Objects.equals(this.metadata, that.metadata);
  }

  //
  // Constructor
  //
  /**
   * Constructor.
   * @param sample the experiment sample
   */
  ExperimentSampleImpl(final Sample sample) {

    requireNonNull(sample, "The sample argument cannot be null");

    this.sample = sample;
  }

}
