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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Set;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.SimpleOutputPort;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class define a workflow output port. It is like a standard OutputPort
 * but it contains also the step of the port.
 * @since 1.3
 * @author Laurent Jourdren
 */
class WorkflowOutputPort extends SimpleOutputPort {

  private static final long serialVersionUID = -7857426034202971843L;

  private final AbstractWorkflowStep step;
  private Set<WorkflowInputPort> links = Sets.newHashSet();

  /**
   * Get the step related to the port.
   * @return a step object
   */
  public AbstractWorkflowStep getStep() {

    return this.step;
  }

  /**
   * Get the output port linked to this input port.
   * @return the linked output port if exists or null
   */
  public Set<WorkflowInputPort> getLinks() {
    return Collections.unmodifiableSet(this.links);
  }

  /**
   * Test if the port is linked.
   * @return true if the port is linked
   */
  public boolean isLinked() {

    return this.links.size() > 0;
  }

  /**
   * Set the link for the port.
   * @param inputPort the output of the link
   */
  public void addLink(final WorkflowInputPort inputPort) {

    // Check if argument is null
    checkNotNull(inputPort, "inputPort argument cannot be null");

    // Check the ports are not on the same step
    checkArgument(inputPort.getStep() != this.step, "cannot link a step ("
        + this.step.getId() + ") to itself (input port: " + inputPort.getName()
        + ", output port: " + getName());

    // Check if a link already exists
    if (this.links.contains(inputPort))
      return;

    // Check if format are compatible
    if (!getFormat().equals(inputPort.getFormat()))
      throw new EoulsanRuntimeException("Incompatible format: "
          + inputPort.getStep().getId() + "." + inputPort.getName() + " -> "
          + inputPort.getFormat().getName() + " and " + getStep().getId() + "."
          + getName() + " <- " + getFormat().getName());

    this.links.add(inputPort);
  }

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", getName())
        .add("format", getFormat().getName())
        .add("compression", getCompression()).add("step", getStep().getId())
        .toString();
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
      final boolean list, final DataFormat format,
      final CompressionType compression) {

    super(name, list, format, compression);

    if (step == null)
      throw new NullPointerException("Step is null");

    this.step = step;
  }

}
