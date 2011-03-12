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

package fr.ens.transcriptome.eoulsan.steps;

import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define an abstract class useful for process samples in a Step with
 * multithreading.
 * @author Laurent Jourdren
 */
public abstract class ProcessSample {

  /**
   * This class is a wrapper for Exceptions throwed by processSample method.
   * @author Laurent Jourdren
   */
  public static final class ProcessSampleException extends Exception {

    private Exception e;
    private String msg;

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
     * Private constructor.
     * @param e exception to wrap
     * @param msg message for the exception
     */
    private ProcessSampleException(final Exception e, final String msg) {

      this.e = e;
      this.msg = msg;
    }

  }

  /**
   * This method allow to throw an exception and a message about the exception
   * in processSample() method.
   * @param e exception to throw
   * @param message message about the exception
   * @throws ProcessSampleException Exception that wrap the original exception
   */
  public final void throwException(final Exception e, final String message)
      throws ProcessSampleException {

    throw new ProcessSampleException(e, message);
  }

  /**
   * Process a Sample.
   * @param context Eoulsan context of execution
   * @param sample Sample to process
   * @return a String with the log message about the process of the sample
   * @throws ProcessSampleException if error occurs while processing sample
   */
  public abstract String processSample(Context context, Sample sample)
      throws ProcessSampleException;

}
