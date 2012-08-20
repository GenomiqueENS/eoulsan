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

package fr.ens.transcriptome.eoulsan.io;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * A nestable Eoulsan exception. This class came from from Biojava code.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Matthew Pocock
 */
public class EoulsanIOException extends EoulsanException {

  private static final long serialVersionUID = 2347936040194578705L;

  /**
   * Create a new EoulsanRuntimeException with a message.
   * @param message the message
   */
  public EoulsanIOException(final String message) {
    super(message);
  }

  /**
   * Create a new EoulsanIOException.
   */
  public EoulsanIOException() {
    super();
  }
}