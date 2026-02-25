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
 * This class define a runtime exception for integration tests.
 *
 * @author Sandrine Perrin
 * @since 2.0
 */
public class EoulsanITRuntimeException extends EoulsanRuntimeException {

  private static final long serialVersionUID = -1897738767737351557L;

  /** Create a new EoulsanITRuntimeException. */
  public EoulsanITRuntimeException() {

    super();
  }

  /**
   * Create a new EoulsanITRuntimeException with a message.
   *
   * @param message Exception message
   */
  public EoulsanITRuntimeException(final String message) {

    super(message);
  }

  /**
   * Create a new EoulsanITRuntimeException with a message and a cause.
   *
   * @param message the message
   * @param cause the cause
   */
  public EoulsanITRuntimeException(String message, Throwable cause) {

    super(message, cause);
  }

  /**
   * Create a new EoulsanITRuntimeException with a cause.
   *
   * @param cause the cause
   */
  public EoulsanITRuntimeException(Throwable cause) {

    super(cause);
  }
}
