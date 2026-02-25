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

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class define a data product that return data/port couples with only data with the same name.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
class MatchDataProduct implements DataProduct, Serializable {

  private static final long serialVersionUID = -3335375092653861359L;

  public static final String DATAPRODUCT_NAME = "match";

  @Override
  public String getName() {
    return DATAPRODUCT_NAME;
  }

  @Override
  public void configure(String conf) {
    // Nothing to do
  }

  @Override
  public Set<ImmutableMap<InputPort, Data>> makeProduct(
      final StepInputPorts inputPorts, final Multimap<InputPort, Data> inputTokens) {

    requireNonNull(inputPorts, "inputPorts argument cannot be null");
    requireNonNull(inputTokens, "inputTokens argument cannot be null");

    final Set<ImmutableMap<InputPort, Data>> result = new HashSet<>();
    final int portCount = inputPorts.size();

    final Map<String, Map<InputPort, Data>> map = new HashMap<>();

    // Put tokens data in a multimap
    for (Map.Entry<InputPort, Data> e : inputTokens.entries()) {

      final String dataName = e.getValue().getName();
      final Map<InputPort, Data> portDataMap;

      if (map.containsKey(dataName)) {
        portDataMap = map.get(dataName);
      } else {
        portDataMap = new HashMap<>();
        map.put(dataName, portDataMap);
      }

      portDataMap.put(e.getKey(), e.getValue());
    }

    // Create the result object
    for (Map.Entry<String, Map<InputPort, Data>> e : map.entrySet()) {

      final Map<InputPort, Data> portDataMap = e.getValue();

      // Do not handle case where data for ports are missing
      if (portDataMap.size() != portCount) {
        continue;
      }

      final ImmutableMap.Builder<InputPort, Data> imb = ImmutableMap.builder();

      for (Map.Entry<InputPort, Data> e2 : portDataMap.entrySet()) {
        imb.put(e2);
      }

      result.add(imb.build());
    }

    return result;
  }
}
