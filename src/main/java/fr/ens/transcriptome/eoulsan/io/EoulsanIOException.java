/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * A nestable nividic exception. This class came from from Biojava code.
 * @author Laurent Jourdren
 * @author Matthew Pocock
 */
public class EoulsanIOException extends EoulsanException {

  /**
   * Create a new NividicIOException with a message.
   * @param message the message
   */
  public EoulsanIOException(final String message) {
    super(message);
  }

  /**
   * Create a new NividicIOException.
   */
  public EoulsanIOException() {
    super();
  }
}