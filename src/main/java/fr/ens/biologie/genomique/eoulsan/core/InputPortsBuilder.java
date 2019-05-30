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

package fr.ens.biologie.genomique.eoulsan.core;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;

/**
 * This class allow to easily create input ports for a step.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class InputPortsBuilder {

  /** Default single input port name. */
  public static final String DEFAULT_SINGLE_INPUT_PORT_NAME = "input";

  private final Set<InputPort> result = new HashSet<>();

  /**
   * Add an input port.
   * @param name name of the port
   * @param format format of the port
   */
  public InputPortsBuilder addPort(final String name, final DataFormat format) {

    return addPort(new SimpleInputPort(name, format));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   */
  public InputPortsBuilder addPort(final String name, final boolean list,
      final DataFormat format) {

    return addPort(new SimpleInputPort(name, list, format));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   */
  public InputPortsBuilder addPort(final String name, final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted) {

    return addPort(new SimpleInputPort(name, format, compressionsAccepted));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   */
  public InputPortsBuilder addPort(final String name, final boolean list,
      final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted) {

    return addPort(
        new SimpleInputPort(name, list, format, compressionsAccepted));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param format format of the port
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  public InputPortsBuilder addPort(final String name, final DataFormat format,
      final boolean requiredInWorkingDirectory) {

    return addPort(
        new SimpleInputPort(name, format, requiredInWorkingDirectory));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  public InputPortsBuilder addPort(final String name, final boolean list,
      final DataFormat format, final boolean requiredInWorkingDirectory) {

    return addPort(
        new SimpleInputPort(name, list, format, requiredInWorkingDirectory));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  public InputPortsBuilder addPort(final String name, final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted,
      final boolean requiredInWorkingDirectory) {

    return addPort(new SimpleInputPort(name, format, compressionsAccepted,
        requiredInWorkingDirectory));
  }

  /**
   * Add an input port.
   * @param name name of the port
   * @param list true if a list is excepted as port value
   * @param format format of the port
   * @param compressionsAccepted compression accepted
   * @param requiredInWorkingDirectory if data is required in working directory
   */
  public InputPortsBuilder addPort(final String name, final boolean list,
      final DataFormat format,
      final EnumSet<CompressionType> compressionsAccepted,
      final boolean requiredInWorkingDirectory) {

    return addPort(new SimpleInputPort(name, list, format, compressionsAccepted,
        requiredInWorkingDirectory));
  }

  /**
   * Create the ports.
   * @return a new InputPorts object
   */
  public InputPorts create() {

    return new SimpleInputPorts(this.result);
  }

  //
  // Private instance methods
  //

  private InputPortsBuilder addPort(final InputPort port) {

    this.result.add(port);

    return this;
  }

  //
  // Static methods
  //

  /**
   * Create the ports with no ports.
   * @return a new InputPorts object
   */
  public static InputPorts noInputPort() {

    return new SimpleInputPorts(null);
  }

  /**
   * Convenient method to create the ports with only one port.
   * @return a new InputPorts object
   */
  public static InputPorts singleInputPort(final DataFormat format) {

    return new InputPortsBuilder()
        .addPort(DEFAULT_SINGLE_INPUT_PORT_NAME, format).create();
  }

  /**
   * Convenient method to create the ports with only one port.
   * @return a new InputPorts object
   */
  public static InputPorts singleInputPort(final String name,
                                           final DataFormat format) {

    return new InputPortsBuilder().addPort(name, format).create();
  }

  /**
   * Set all ports of an existing input ports to be required in working
   * directory.
   * @param inputPorts original input ports
   * @return a new InputPorts object that ports data are required in working
   *         directory
   */
  public static InputPorts allPortsRequiredInWorkingDirectory(
      final InputPorts inputPorts) {

    if (inputPorts == null) {
      return null;
    }

    final InputPortsBuilder builder = new InputPortsBuilder();

    for (InputPort port : inputPorts) {
      builder.addPort(port.getName(), port.getFormat(), true);
    }

    return builder.create();
  }

  /**
   * Convenient method to create a defensive copy of an InputPorts object.
   * @param ports an existing OutputPorts object
   * @return a new InputPorts object or null if the ports parameter is null
   */
  public static InputPorts copy(final InputPorts ports) {

    if (ports == null) {
      return null;
    }

    final InputPortsBuilder builder = new InputPortsBuilder();
    for (InputPort port : ports) {
      builder.addPort(port);
    }

    return builder.create();
  }

}
