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

package fr.ens.transcriptome.eoulsan.core;

/**
 * This class is a wrapper for Exceptions thrown by processSample method.
 * @author Laurent Jourdren
 */
public class SampleStepException extends Exception {

  private static final long serialVersionUID = -8346518620581583517L;

  private final Exception e;
  private final String msg;

  /**
   * Get the wrapped exception
   * @return the wrapped exception
   */
  public Exception getException() {

    return this.e;
  }

  /**
   * Get the message of the exception
   * @return the message of the exception
   */
  public String getMessage() {

    return this.msg;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param e exception to wrap
   * @param msg message for the exception
   */
  public SampleStepException(final Exception e, final String msg) {

    this.e = e;
    this.msg = msg;
  }

  /**
   * Wrap an existing exception in a SampleStepException.
   * @param e exception to wrap
   * @param msg message for the exception
   * @throws SampleStepException allways
   */
  public static final void reThrow(final Exception e, final String msg)
      throws SampleStepException {

    throw new SampleStepException(e, msg);
  }

}
