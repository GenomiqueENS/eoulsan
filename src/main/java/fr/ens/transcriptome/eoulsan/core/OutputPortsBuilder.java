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

package fr.ens.transcriptome.eoulsan.core;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class allow to easily create input ports for a step.
 * @since 1.3
 * @author Laurent Jourdren
 */
public class OutputPortsBuilder {

  private Set<OutputPort> result = Sets.newHashSet();
  private Set<String> portNames = Sets.newHashSet();

  /**
   * Add an output port.
   * @param name name of the port
   * @param format format of the port
   */
  public OutputPortsBuilder addPort(final String name, final DataFormat format) {

    return addPort(new SimpleOutputPort(name, format));
  }

  /**
   * Add an output port.
   * @param name name of the port
   * @param format format of the port
   * @param compression compression of the output
   */
  public OutputPortsBuilder addPort(final String name, final DataFormat format,
      final CompressionType compression) {

    return addPort(new SimpleOutputPort(name, format, compression));
  }

  /**
   * Create the ports.
   * @return a set with the ports
   */
  public Set<OutputPort> create() {

    return Collections.unmodifiableSet(Sets.newHashSet(this.result));
  }

  /**
   * Create the ports with no ports.
   * @return a set with the ports
   */
  public static final Set<OutputPort> noInputPort() {

    return Collections.emptySet();
  }

  /**
   * Convenient method to create the ports with only one port.
   * @return a set with the ports
   */
  public static final Set<OutputPort> singleOutputPort(final DataFormat format) {

    return new OutputPortsBuilder().addPort("output", format).create();
  }

  /**
   * Convenient method to create the ports with only one port.
   * @return a set with the ports
   */
  public static final Set<OutputPort> singleOutputPort(final String name,
      final DataFormat format) {

    return new OutputPortsBuilder().addPort(name, format).create();
  }

  //
  // Other method
  //

  private OutputPortsBuilder addPort(final OutputPort port) {

    if (this.portNames.contains(port.getName()))
      throw new EoulsanRuntimeException("Two output ports had the same name: "
          + port.getName());

    this.result.add(port);
    this.portNames.add(port.getName());

    return this;
  }

}
