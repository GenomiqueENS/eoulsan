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

import fr.ens.transcriptome.eoulsan.NividicException;

/**
 * A nestable nividic exception. This class came from from Biojava code.
 * @author Laurent Jourdren
 * @author Matthew Pocock
 */
public class EoulsanIOException extends NividicException {

  /**
   * Create a new NividicIOException with a message.
   * @param message the message
   */
  public EoulsanIOException(final String message) {
    super(message);
  }

  /**
   * Create a new NividicIOException with a cause.
   * @param ex the Throwable that caused this NividicIOException
   */
  public EoulsanIOException(final Throwable ex) {
    super(ex);
  }

  /**
   * Create a new NividicIOException with a cause and a message.
   * @param ex the Throwable that caused this NividicIOException
   * @param message the message
   * @deprecated use new NividicIOException(message, ex) instead
   */
  public EoulsanIOException(final Throwable ex, final String message) {
    this(message, ex);
  }

  /**
   * Create a new NividicIOException with a cause and a message.
   * @param message the message
   * @param ex the Throwable that caused this NividicIOException
   */
  public EoulsanIOException(final String message, final Throwable ex) {
    super(message, ex);
  }

  /**
   * Create a new NividicIOException.
   */
  public EoulsanIOException() {
    super();
  }
}