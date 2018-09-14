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
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;

/**
 * This class defines the default implementation of a sample.
 * @author Xavier Bauquet
 * @since 2.0
 */
class SampleImpl implements Serializable, Sample {

  /** Serialization version UID. */
  private static final long serialVersionUID = 3674095532228721218L;

  private static AtomicInteger instanceCount = new AtomicInteger(0);

  private final Design design;
  private final String sampleId;
  private final int sampleNumber = instanceCount.incrementAndGet();
  private String sampleName = "Sample" + sampleNumber;
  private final SampleMetadataImpl sampleMetadata = new SampleMetadataImpl();

  //
  // Getters
  //

  @Override
  public Design getDesign() {

    return this.design;
  }

  @Override
  public String getId() {

    return this.sampleId;
  }

  @Override
  public int getNumber() {

    return this.sampleNumber;
  }

  @Override
  public String getName() {

    return this.sampleName;
  }

  @Override
  public SampleMetadataImpl getMetadata() {

    return this.sampleMetadata;
  }

  //
  // Setter
  //

  @Override
  public void setName(String newSampleName) {

    requireNonNull(newSampleName, "newSampleName argument cannot be null");

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

    return com.google.common.base.Objects.toStringHelper(this)
        .add("sampleId", this.sampleId).add("sampleNumber", this.sampleNumber)
        .add("sampleName", this.sampleName)
        .add("sampleMetadata", this.sampleMetadata).toString();
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.sampleId, this.sampleNumber, this.sampleName,
        this.sampleMetadata);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (!(o instanceof SampleImpl)) {
      return false;
    }

    final SampleImpl that = (SampleImpl) o;

    return Objects.equals(this.sampleId, that.sampleId)
        && Objects.equals(this.sampleName, that.sampleName)
        && Objects.equals(this.sampleMetadata, that.sampleMetadata);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param design the design object
   * @param sampleId the sample id
   */
  SampleImpl(final Design design, final String sampleId) {

    requireNonNull(design, "design argument cannot be null");
    requireNonNull(sampleId, "sampleId argument cannot be null");
    checkArgument(FileNaming.isDataNameValid(sampleId),
        "The id of a sample can only contains letters and digit: " + sampleId);

    this.design = design;
    this.sampleId = sampleId.trim();
  }

}
