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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This class define a basic implementation of a Ports class.
 * @since 1.3
 * @author Laurent Jourdren
 */
public abstract class AbstractPorts<E extends Port> implements Ports<E> {

  private final Map<String, E> ports;

  @Override
  public Iterator<E> iterator() {

    return this.ports.values().iterator();
  }

  @Override
  public E getPort(final String name) {

    return this.ports.get(name);
  }

  @Override
  public boolean contains(final String name) {

    return this.ports.containsKey(name);
  }

  @Override
  public boolean contains(final E port) {

    if (port == null)
      return false;

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

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param ports ports of the object
   */
  protected AbstractPorts(final Set<E> ports) {

    // If ports is null
    if (ports == null) {
      this.ports = Collections.emptyMap();
      return;
    }

    final Map<String, E> map = Maps.newHashMap();

    for (E port : ports) {

      if (port == null)
        continue;

      if (map.containsKey(port.getName()))
        throw new EoulsanRuntimeException(
            "A port already exists with the same name: " + port.getName());

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

    if (format == null)
      return 0;

    int count = 0;

    for (E e : this)
      if (e.getFormat().equals(format))
        count++;

    return count;
  }

  @Override
  public List<E> getPortsWithDataFormat(DataFormat format) {

    if (format == null)
      return Collections.emptyList();

    final List<E> result = new ArrayList<E>();

    for (E e : this)
      if (e.getFormat().equals(format))
        result.add(e);

    return Collections.unmodifiableList(result);
  }

}
