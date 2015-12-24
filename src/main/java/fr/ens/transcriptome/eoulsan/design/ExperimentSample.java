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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import org.python.google.common.base.MoreObjects;
import org.python.google.common.base.Objects;

/**
 * This class defines the experiment sample.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class ExperimentSample implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = -3180171254543892681L;

  private final Sample sample;
  private final ExperimentSampleMetadata metadata =
      new ExperimentSampleMetadata();

  //
  // Getters
  //

  /**
   * Get the experiment sample name.
   * @return the experiment sample name
   */
  public Sample getSample() {
    return this.sample;
  }

  /**
   * Get the experiment sample metadata.
   * @return an ExperimentSampleMetadata object
   */
  public ExperimentSampleMetadata getMetadata() {
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

    return Objects.hashCode(this.sample, this.metadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof ExperimentSample)) {
      return false;
    }

    final ExperimentSample that = (ExperimentSample) o;

    return Objects.equal(this.sample, that.sample)
        && Objects.equal(this.metadata, that.metadata);
  }

  //
  // Constructor
  //
  /**
   * @param sample the experiment sample
   */
  ExperimentSample(final Sample sample) {

    checkNotNull(sample, "The sample argument cannot be null");

    this.sample = sample;
  }

}
