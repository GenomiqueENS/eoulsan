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

package fr.ens.transcriptome.eoulsan.design.impl;

import com.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.design.SampleMetadata;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class implements the <code>Sample</code> interface.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SampleImpl implements Sample {

  private DesignImpl design;
  private int sampleId;

  //
  // Getters
  //

  @Override
  public int getId() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    return this.design.getSampleId(sampleName);
  }

  @Override
  public String getName() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    return sampleName;
  }

  @Override
  public SampleMetadata getMetadata() {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    return this.design.getSampleMetadata(sampleName);
  }

  //
  // Setters
  //

  /**
   * Set the identfier of the sample
   * @param id the identifier to set
   */
  public void setId(final int id) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    if (this.design.getSampleId(sampleName) == id)
      return;

    this.design.setSampleId(sampleName, id);
  }

  @Override
  public void setName(final String newName) {

    final String sampleName = this.design.getSampleName(this.sampleId);

    if (sampleName == null)
      throw new EoulsanRuntimeException("The sample doesn't exists");

    this.design.renameSample(sampleName, newName);
  }

  //
  // Other methods
  //

  @Override
  public int hashCode() {

    return Utils.hashCode(this.design, this.sampleId);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (!(o instanceof SampleImpl))
      return false;

    final SampleImpl that = (SampleImpl) o;

    return that.design.equals(this.design) && that.sampleId == this.sampleId;
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("id", getId()).toString();
  }

  //
  // Constructor
  //

  SampleImpl(final DesignImpl design, final int slideId) {

    this.design = design;
    this.sampleId = slideId;
  }

}
