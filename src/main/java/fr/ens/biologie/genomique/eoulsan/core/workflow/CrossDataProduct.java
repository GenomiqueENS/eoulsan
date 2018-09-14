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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.data.Data;

/**
 * This class define a cross data product.
 * @author Laurent Jourdren
 * @since 2.0
 */
class CrossDataProduct implements DataProduct, Serializable {

  private static final long serialVersionUID = 5268549880105535524L;

  public static final String DATAPRODUCT_NAME = "cross";

  @Override
  public String getName() {
    return DATAPRODUCT_NAME;
  }

  /**
   * Class needed for cartesian product computation.
   */
  private static class CartesianProductEntry {
    final StepInputPort port;
    final Data data;

    CartesianProductEntry(final StepInputPort port, final Data data) {
      this.port = port;
      this.data = data;
    }
  }

  @Override
  public void configure(final String conf) {
    // Nothing to do
  }

  @Override
  public Set<ImmutableMap<InputPort, Data>> makeProduct(
      final StepInputPorts inputPorts,
      final Multimap<InputPort, Data> inputTokens) {

    requireNonNull(inputPorts, "inputPorts argument cannot be null");
    requireNonNull(inputTokens, "inputTokens argument cannot be null");

    final Set<ImmutableMap<InputPort, Data>> result = new HashSet<>();
    final List<StepInputPort> portsList =
        Lists.newArrayList(inputPorts.iterator());

    // First create the lists for Sets.cartesianProduct()
    final List<Set<CartesianProductEntry>> sets = new ArrayList<>();
    for (StepInputPort port : portsList) {
      final Set<CartesianProductEntry> s = new HashSet<>();
      for (Data d : inputTokens.get(port)) {
        s.add(new CartesianProductEntry(port, d));
      }
      sets.add(s);
    }

    // Compute cartesian product
    final Set<List<CartesianProductEntry>> cartesianProduct =
        Sets.cartesianProduct(sets);

    // Now convert result of cartesianProduct() to final result
    for (List<CartesianProductEntry> l : cartesianProduct) {

      final ImmutableMap.Builder<InputPort, Data> imb = ImmutableMap.builder();

      for (CartesianProductEntry e : l) {
        imb.put(e.port, e.data);
      }

      result.add(imb.build());
    }

    return result;
  }

}
