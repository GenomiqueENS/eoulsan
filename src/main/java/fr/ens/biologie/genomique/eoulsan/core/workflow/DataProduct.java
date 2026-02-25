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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import java.util.Set;

/**
 * This interface define a data product.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
interface DataProduct {

  /**
   * Get the name of the DataProduct.
   *
   * @return the name of the data product
   */
  String getName();

  /**
   * Configure the DataProduct.
   *
   * @param conf configuration string
   * @throws EoulsanException if an error occurs while configuring the data product
   */
  void configure(String conf) throws EoulsanException;

  /**
   * Make the product.
   *
   * @param inputPorts input ports
   * @param inputTokens input token
   * @return a set of map with the relation between the port and the data
   */
  Set<ImmutableMap<InputPort, Data>> makeProduct(
      StepInputPorts inputPorts, Multimap<InputPort, Data> inputTokens);
}
