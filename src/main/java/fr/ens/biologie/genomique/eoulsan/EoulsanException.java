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

package fr.ens.biologie.genomique.eoulsan;

/**
 * A nestable Eoulsan exception.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public class EoulsanException extends Exception {

  // Serialization version UID
  private static final long serialVersionUID = 7021095498629981700L;

  /** Create a new EoulsanException. */
  public EoulsanException() {

    super();
  }

  /**
   * Create a new EoulsanException with a message.
   *
   * @param message the message
   */
  public EoulsanException(final String message) {

    super(message);
  }

  /**
   * Create a new EoulsanException with a message and a cause.
   *
   * @param message the message
   * @param cause the cause
   */
  public EoulsanException(String message, Throwable cause) {

    super(message, cause);
  }

  /**
   * Create a new EoulsanException with a cause.
   *
   * @param cause the cause
   */
  public EoulsanException(Throwable cause) {

    super(cause);
  }
}
