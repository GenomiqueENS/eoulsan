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

package fr.ens.transcriptome.eoulsan.design2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import org.python.google.common.base.Objects;

/**
 * This class defines a sample.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class Sample implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = 3674095532228721218L;

  private static int instanceCount;

  private final Design design;
  private final String sampleId;
  private final int sampleNumber = ++instanceCount;
  private String sampleName = "Sample" + sampleNumber;
  private final SampleMetadata sampleMetadata = new SampleMetadata();

  //
  // Getters
  //
  /**
   * Get the sample id.
   * @return the sample id
   */
  public String getId() {

    return this.sampleId;
  }

  /**
   * Get the sample number.
   * @return the sample number
   */
  public int getNumber() {

    return this.sampleNumber;
  }

  /**
   * Get the sample name.
   * @return the sample name
   */
  public String getName() {

    return this.sampleName;
  }

  /**
   * Get the sample metadata.
   * @return an object SampleMetadata
   */
  public SampleMetadata getMetadata() {

    return this.sampleMetadata;
  }

  /**
   * Get the design.
   * @return the design
   */
  Design getDesign() {

    return this.design;
  }

  //
  // Setter
  //

  /**
   * Set the sample name.
   * @param newSampleName the new sample name
   */
  public void setName(String newSampleName) {

    checkNotNull(newSampleName, "newSampleName argument cannot be null");

    final String name = newSampleName.trim();

    checkArgument(!this.design.containsSampleName(name),
        "The sample name already exists in the design: " + name);

    this.sampleName = name;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("sampleId", this.sampleId)
        .add("sampleNumber", this.sampleNumber)
        .add("sampleName", this.sampleName)
        .add("sampleMetadata", this.sampleMetadata).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hashCode(this.sampleId, this.sampleNumber, this.sampleName,
        this.sampleMetadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof Sample)) {
      return false;
    }

    final Sample that = (Sample) o;

    return Objects.equal(this.sampleId, that.sampleId)
        && Objects.equal(this.sampleNumber, that.sampleNumber)
        && Objects.equal(this.sampleName, that.sampleName)
        && Objects.equal(this.sampleMetadata, that.sampleMetadata);
  }

  //
  // Constructor
  //

  /**
   * @param design the design object
   * @param sampleId the sample id
   */
  Sample(final Design design, final String sampleId) {

    checkNotNull(design, "design argument cannot be null");
    checkNotNull(sampleId, "sampleId argument cannot be null");

    this.design = design;
    this.sampleId = sampleId.trim();
  }
}
