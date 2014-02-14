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

package fr.ens.transcriptome.eoulsan.core.workflow;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.core.SimpleOutputPort;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class define a workflow output port. It is like a standard OutputPort
 * but it contains also the step of the port.
 * @since 1.3
 * @author Laurent Jourdren
 */
class WorkflowOutputPort extends SimpleOutputPort {

  private static final long serialVersionUID = -7857426034202971843L;

  private AbstractWorkflowStep step;

  /**
   * Get the step related to the port.
   * @return a step object
   */
  public AbstractWorkflowStep getStep() {

    return this.step;
  }

  //
  // Other methods
  //

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param sample sample
   * @return the DataFile for the sample
   */
  public int getDataFileCount(final Sample sample, final boolean existingFiles) {

    return this.step.getOutputDataFileCount(getName(), sample, existingFiles);
  }

  /**
   * Get the DataFile.
   * @param sample sample
   * @param fileIndex file index for multifile data. (-1 = no file index)
   * @return the DataFile for the sample
   */
  public DataFile getDataFile(final Sample sample, final int fileIndex) {

    Preconditions.checkNotNull(sample, "Sample cannot be null");

    return this.step.getOutputDataFile(getName(), sample, fileIndex);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step the step related to the port
   * @param name name of the port
   * @param format format of the port
   * @param compression compression of the output
   */
  public WorkflowOutputPort(final AbstractWorkflowStep step, final String name,
      final DataFormat format, final CompressionType compression) {

    super(name, format, compression);

    if (step == null)
      throw new NullPointerException("Step is null");

    this.step = step;
  }

}
