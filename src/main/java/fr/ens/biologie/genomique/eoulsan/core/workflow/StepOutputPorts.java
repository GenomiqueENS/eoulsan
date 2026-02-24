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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.core.AbstractPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPort;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;

/**
 * This class define a group of Workflow output ports.
 * @since 2.0
 * @author Laurent Jourdren
 */
class StepOutputPorts extends AbstractPorts<StepOutputPort> {

  private static final long serialVersionUID = 183816706502235237L;

  /**
   * Get the steps linked to the ports.
   * @return a set with the steps related to the ports
   */
  public Set<AbstractStep> getLinkedSteps() {

    final Set<AbstractStep> result = new HashSet<>();

    for (StepOutputPort sop : this) {
      for (StepInputPort sip : sop.getLinks()) {
        result.add(sip.getStep());
      }
    }

    return Collections.unmodifiableSet(result);
  }

  //
  // Static methods
  //

  /**
   * Return a WorkflowOutputPorts with no ports.
   * @return a WorkflowOutputPorts with no ports
   */
  static StepOutputPorts noOutputPort() {

    final Set<StepOutputPort> ports = Collections.emptySet();
    return new StepOutputPorts(ports);
  }

  /**
   * Convert an OutputPorts object to a set of WorkflowOutputPort
   * @param step step related to the WorkflowOutputPort objects
   * @param ports ports to convert
   * @return a new set
   */
  private static Set<StepOutputPort> convert(final AbstractStep step,
      final OutputPorts ports) {

    if (ports == null) {
      throw new NullPointerException("Ports is null");
    }

    final Set<StepOutputPort> result = new HashSet<>();

    for (OutputPort port : ports) {
      if (port != null) {
        result.add(new StepOutputPort(step, port.getName(), port.isList(),
            port.getFormat(), port.getCompression()));
      }

    }

    return result;
  }

  //
  // Constructor.
  //

  /**
   * Constructor.
   * @param ports ports to add.
   */
  StepOutputPorts(final Set<StepOutputPort> ports) {
    super(ports);
  }

  /**
   * Constructor.
   * @param step step related to the WorkflowOutputPort objects
   * @param ports port to convert
   */
  StepOutputPorts(final AbstractStep step, final OutputPorts ports) {

    super(convert(step, ports));
  }

}
