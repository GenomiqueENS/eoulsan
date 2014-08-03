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

import java.io.IOException;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define the result of a step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface StepResult {

  //
  // Getter
  //

  /**
   * Get the duration of the step.
   * @return duration in milliseconds
   */
  long getDuration();

  /**
   * Test the result of the step is successful.
   * @return Returns the success
   */
  boolean isSuccess();

  /**
   * Get the exception.
   * @return Returns the exception
   */
  Throwable getException();

  /**
   * Get the error message.
   * @return Returns the errorMessage
   */
  String getErrorMessage();

  //
  // I/O
  //

  /**
   * Write the result in a file.
   * @param file output file
   * @throws IOException if an error occurs while writing the result
   */
  void write(final DataFile file) throws IOException;

  /**
   * Write the result in a file.
   * @param out output stream
   * @throws IOException if an error occurs while writing the result
   */
  void write(final OutputStream out) throws IOException;

}
