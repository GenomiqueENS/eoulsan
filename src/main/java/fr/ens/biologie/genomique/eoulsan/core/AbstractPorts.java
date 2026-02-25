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

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class define a basic implementation of a Ports class.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public abstract class AbstractPorts<E extends Port> implements Ports<E>, Serializable {

  private static final long serialVersionUID = -5116830881426447140L;

  private final Map<String, E> ports;

  @Override
  public Iterator<E> iterator() {

    return this.ports.values().iterator();
  }

  @Override
  public E getPort(final String name) {

    if (name == null) {
      return null;
    }

    return this.ports.get(name.trim().toLowerCase(Globals.DEFAULT_LOCALE));
  }

  @Override
  public boolean contains(final String name) {

    if (name == null) {
      return false;
    }

    return this.ports.containsKey(name.trim().toLowerCase(Globals.DEFAULT_LOCALE));
  }

  @Override
  public boolean contains(final E port) {

    if (port == null) {
      return false;
    }

    return this.ports.containsKey(port.getName());
  }

  @Override
  public Set<String> getPortNames() {

    return Collections.unmodifiableSet(this.ports.keySet());
  }

  @Override
  public int size() {

    return this.ports.size();
  }

  @Override
  public boolean isEmpty() {

    return size() == 0;
  }

  @Override
  public E getFirstPort() {

    if (size() == 0) {
      return null;
    }

    return iterator().next();
  }

  @Override
  public String toString() {

    return this.ports.toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param ports ports of the object
   */
  protected AbstractPorts(final Set<E> ports) {

    // If ports is null
    if (ports == null || ports.isEmpty()) {
      this.ports = Collections.emptyMap();
      return;
    }

    final Map<String, E> map = new HashMap<>();

    for (E port : ports) {

      if (port == null) {
        continue;
      }

      if (map.containsKey(port.getName())) {
        throw new EoulsanRuntimeException(
            "A port already exists with the same name: " + port.getName());
      }

      map.put(port.getName(), port);
    }

    switch (map.size()) {
      case 0:
        this.ports = Collections.emptyMap();
        break;

      case 1:
        final E value = map.values().iterator().next();
        this.ports = Collections.singletonMap(value.getName(), value);
        break;

      default:
        this.ports = map;
    }
  }

  @Override
  public int countDataFormat(final DataFormat format) {

    if (format == null) {
      return 0;
    }

    int count = 0;

    for (E e : this) {
      if (e.getFormat().equals(format)) {
        count++;
      }
    }

    return count;
  }

  @Override
  public List<E> getPortsWithDataFormat(final DataFormat format) {

    if (format == null) {
      return Collections.emptyList();
    }

    final List<E> result = new ArrayList<>();

    for (E e : this) {
      if (e.getFormat().equals(format)) {
        result.add(e);
      }
    }

    return Collections.unmodifiableList(result);
  }
}
