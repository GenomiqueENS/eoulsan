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

package fr.ens.biologie.genomique.eoulsan.bio.alignmentsfilters;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.log.DummyLogger;
import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;

/**
 * This class define an abstract AlignmentsFilter that contains default code for
 * some methods of AlignmentFilter.
 * @since 1.1
 * @author Laurent Jourdren
 */
public abstract class AbstractReadAlignmentsFilter
    implements ReadAlignmentsFilter {

  private GenericLogger logger = new DummyLogger();

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    throw new EoulsanException(
        "Unknown parameter for " + getName() + " alignments filter: " + key);
  }

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
  public void init() {
  }

}
