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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import org.python.google.common.base.MoreObjects;
import org.python.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;

/**
 * This class defines a sample.
 * @author Xavier Bauquet
 * @since 2.0
 */

public class Sample implements Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = 3674095532228721218L;

  /** Sample Id field. */
  public static final String SAMPLE_ID_FIELD = "SampleId";

  /** Sample Name field. */
  public static final String SAMPLE_NAME_FIELD = "SampleName";

  /** Sample Name field. */
  public static final String SAMPLE_NUMBER_FIELD = "SampleNumber";

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
   * Get the design related to the sample.
   * @return the Design object related to the sample
   */
  public Design getDesign() {

    return this.design;
  }

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

    // Do nothing if the new name is the old name
    if (name.equals(this.sampleName)) {
      return;
    }

    checkArgument(!this.design.containsSampleName(name),
        "The sample name already exists in the design: " + name);

    this.sampleName = name;
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).add("sampleId", this.sampleId)
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
    checkArgument(FileNaming.isDataNameValid(sampleId),
        "The id of a sample can only contains letters and digit: " + sampleId);

    this.design = design;
    this.sampleId = sampleId.trim();
  }
}
