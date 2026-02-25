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

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import java.util.List;
import java.util.Set;

/**
 * This interface define a group of ports.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface Ports<E extends Port> extends Iterable<E> {

  /**
   * Get a port.
   *
   * @param name name of the port to get
   * @return the port if exists or null
   */
  E getPort(String name);

  /**
   * Test if a port exists.
   *
   * @param name name of the port to test
   * @return true if the port exists
   */
  boolean contains(String name);

  /**
   * Test if a port exists by testing if port name exists.
   *
   * @param port port to test
   * @return true if the port exists
   */
  boolean contains(E port);

  /**
   * Get the names of the ports
   *
   * @return a set with the names of the ports
   */
  Set<String> getPortNames();

  /**
   * Get the number of ports in the object.
   *
   * @return the number of ports
   */
  int size();

  /**
   * Test if the object is empty
   *
   * @return true if the object is null
   */
  boolean isEmpty();

  /**
   * Count the number of occurrences of a format in the port.
   *
   * @param format the format to test
   * @return the number of occurrences of the format
   */
  int countDataFormat(DataFormat format);

  /**
   * Get a list with all the ports that format is the specified format.
   *
   * @param format the format to get
   * @return always a list (even empty) of Ports
   */
  List<E> getPortsWithDataFormat(DataFormat format);

  /**
   * Get the first port.
   *
   * @return a port or null if the object does not contains ports.
   */
  E getFirstPort();
}
