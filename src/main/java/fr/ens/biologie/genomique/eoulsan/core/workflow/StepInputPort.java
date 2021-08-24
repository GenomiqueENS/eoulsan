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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import com.google.common.base.MoreObjects;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.SimpleInputPort;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.Step.StepType;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;

/**
 * This class define a workflow input port. It is like a standard InputPort but
 * it contains also the step of the port.
 * @since 2.0
 * @author Laurent Jourdren
 */
class StepInputPort extends SimpleInputPort implements Serializable {

  private static final long serialVersionUID = -3858660424325558424L;

  private final AbstractStep step;
  private StepOutputPort link;

  /**
   * Get the step related to the port.
   * @return a step object
   */
  public AbstractStep getStep() {

    return this.step;
  }

  /**
   * Get the output port linked to this input port.
   * @return the linked output port if exists or null
   */
  public StepOutputPort getLink() {
    return this.link;
  }

  /**
   * Test if the port is linked.
   * @return true if the port is linked
   */
  public boolean isLinked() {

    return this.link != null;
  }

  /**
   * Set the link for the port.
   * @param outputPort the output of the link
   */
  public void setLink(final StepOutputPort outputPort) {

    // Check if argument is null
    requireNonNull(outputPort, "outputPort argument cannot be null");

    // Check the ports are not on the same step
    checkArgument(outputPort.getStep() != this.step,
        "cannot link a step ("
            + this.step.getId() + ") to itself (input port: " + getName()
            + ", output port: " + outputPort.getName());

    // Check if a link already exists
    if (this.link != null) {
      throw new EoulsanRuntimeException("A link already exists for "
          + getStep().getId() + "." + getName() + " ("
          + this.link.getStep().getId() + "." + this.link.getName() + ")");
    }

    // Check if format are compatible
    if (!getFormat().equals(outputPort.getFormat())) {
      throw new EoulsanRuntimeException("Incompatible format: "
          + getStep().getId() + "." + getName() + " -> " + getFormat().getName()
          + " and " + outputPort.getStep().getId() + "." + outputPort.getName()
          + " <- " + outputPort.getFormat().getName());
    }

    final AbstractStep step = outputPort.getStep();

    // Check if step can be linked
    if (step.getType() != StepType.DESIGN_STEP
        && step.getType() != StepType.GENERATOR_STEP
        && step.getType() != StepType.STANDARD_STEP) {
      throw new EoulsanRuntimeException("The dependency ("
          + step.getId() + ") do not provide port (" + outputPort.getName()
          + ")");
    }

    this.link = outputPort;
  }

  @Override
  public Set<Step> getLinkedSteps() {

    if (this.link == null) {
      return emptySet();
    }

    return singleton((Step) this.link.getStep());
  }

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName())
        .add("compressionsAccepted", getCompressionsAccepted())
        .add("requiredInWorkingDirectory", isRequiredInWorkingDirectory())
        .add("step", getStep().getId()).add("link", getLink()).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step the step related to the port * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  public StepInputPort(final AbstractStep step, final String name,
      final boolean list, final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted,
      final boolean requiredInWorkingDirectory) {

    super(name, list, format, compressionsAccepted, requiredInWorkingDirectory);

    if (step == null) {
      throw new NullPointerException("Step is null");
    }

    this.step = step;
  }
}
