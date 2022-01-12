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

package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.log.DummyLogger;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;

/**
 * This class define an abstract ReadFilter that allow simple Pair-end and
 * Mate-pair filter handling.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractReadFilter implements ReadFilter {

  private GenericLogger logger = new DummyLogger();

  @Override
  public void setLogger(GenericLogger logger) {

    requireNonNull(logger);
    this.logger = logger;
  }

  @Override
  public GenericLogger getLogger() {

    return this.logger;
  }

  @Override
  public boolean accept(final ReadSequence read1, final ReadSequence read2) {

    return accept(read1) && accept(read2);
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    throw new EoulsanException(
        "Unknown parameter for " + getName() + " read filter: " + key);
  }

  @Override
  public void init() throws EoulsanException {
  }

}
