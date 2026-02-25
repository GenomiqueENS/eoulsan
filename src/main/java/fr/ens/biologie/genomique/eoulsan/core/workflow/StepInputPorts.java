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

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.core.AbstractPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class define a group of Workflow input ports.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
class StepInputPorts extends AbstractPorts<StepInputPort> {

  private static final long serialVersionUID = -746211786359434112L;

  /**
   * Get the steps linked to the ports.
   *
   * @return a set with the steps related to the ports
   */
  public Set<AbstractStep> getLinkedSteps() {

    final Set<AbstractStep> result = new HashSet<>();

    for (StepInputPort sip : this) {
      result.add(sip.getLink().getStep());
    }

    return Collections.unmodifiableSet(result);
  }

  //
  // Static methods
  //

  static StepInputPorts noInputPort() {

    final Set<StepInputPort> ports = Collections.emptySet();
    return new StepInputPorts(ports);
  }

  /**
   * Convert an OutputPorts object to a set of WorkflowOutputPort
   *
   * @param step step related to the WorkflowOutputPort objects
   * @param ports ports to convert
   * @return a new set
   */
  private static Set<StepInputPort> convert(final AbstractStep step, final InputPorts ports) {

    if (ports == null) {
      throw new NullPointerException("Ports is null");
    }

    final boolean hadoopMode =
        EoulsanRuntime.getRuntime().getMode().isHadoopMode()
            && step.getEoulsanMode().isHadoopCompatible();

    final Set<StepInputPort> result = new HashSet<>();

    for (InputPort port : ports) {
      if (port != null) {
        result.add(
            new StepInputPort(
                step,
                port.getName(),
                port.isList(),
                port.getFormat(),
                port.getCompressionsAccepted(),
                hadoopMode ? true : port.isRequiredInWorkingDirectory()));
      }
    }

    return result;
  }

  //
  // Constructor.
  //

  /**
   * Constructor.
   *
   * @param ports ports to add.
   */
  StepInputPorts(final Set<StepInputPort> ports) {
    super(ports);
  }

  /**
   * Constructor.
   *
   * @param step step related to the WorkflowOutputPort objects
   * @param ports port to convert
   */
  StepInputPorts(final AbstractStep step, final InputPorts ports) {

    super(convert(step, ports));
  }
}
