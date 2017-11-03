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

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.data.Data;

/**
 * This class define an auto configurable data product.
 * @author Laurent Jourdren
 * @since 2.0
 */
class DefaultDataProduct implements DataProduct, Serializable {

  private static final long serialVersionUID = -4521747867897781954L;

  private static final String DATAPRODUCT_NAME = "autoconf";

  private DataProduct dataproduct;

  @Override
  public String getName() {

    return dataproduct == null ? DATAPRODUCT_NAME : this.dataproduct.getName();
  }

  @Override
  public void configure(final String conf) throws EoulsanException {

    checkState(dataproduct == null, "configure() has been already called");

    final String s = Strings.nullToEmpty(conf).trim().toLowerCase();

    switch (s) {

    case MatchDataProduct.DATAPRODUCT_NAME:
      this.dataproduct = new MatchDataProduct();
      break;

    case CrossDataProduct.DATAPRODUCT_NAME:
    case "":
      this.dataproduct = new CrossDataProduct();
      break;

    default:
      throw new EoulsanException("Unknown data product method: " + conf);
    }

    this.dataproduct.configure(conf);
  }

  @Override
  public Set<ImmutableMap<InputPort, Data>> makeProduct(
      final StepInputPorts inputPorts,
      final Multimap<InputPort, Data> inputTokens) {

    checkState(dataproduct != null, "configure() has not been called");

    return this.dataproduct.makeProduct(inputPorts, inputTokens);
  }

}
