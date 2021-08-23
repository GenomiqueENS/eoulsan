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

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This interface define a mapper executors that allow to execute system
 * processes against the mapper execution mode (bundled binaries, binaries in
 * PATH or execute binaries in a Docker container).
 * @author Laurent Jourdren
 */
interface MapperExecutor {

  /**
   * This interface define a result of a call of MapperExecutor.execute().
   * @author Laurent Jourdren
   */
  interface Result {

    /**
     * Get the stdout input stream of the process
     * @return an InputStream
     * @throws IOException if an error occurs while creating the input stream
     */
    InputStream getInputStream() throws IOException;

    /**
     * Wait the end of the process.
     * @return the exit code of the process
     * @throws IOException if an error occurs while waiting the process
     */
    int waitFor() throws IOException;
  }

  /**
   * Get the logger to use for the mapping.
   * @return the logger to use for the mapping
   */
  MapperLogger getLogger();

  /**
   * Test if an executable exists.
   * @param executable executable to test
   * @return true if the executable exists
   * @throws IOException if an error occurs while testing if the executable
   *           exists
   */
  boolean isExecutable(String executable) throws IOException;

  /**
   * Install an executable
   * @param executable the executable to install
   * @return the path of the executable
   * @throws IOException if an error occurs while installing the executable
   */
  String install(final String executable) throws IOException;

  /**
   * Execute a command
   * @param command the command to execute
   * @param executionDirectory the execution directory
   * @param stdout true if stdout will be used
   * @param stdErrFile standard error file
   * @param redirectStderr redirect stderr into stdout
   * @param filesUsed files used by the process
   * @return a MapperExecutor.Result object
   * @throws IOException if an error occurs while starting the execution of the
   *           command
   */
  Result execute(List<String> command, File executionDirectory, boolean stdout,
      File stdErrFile, boolean redirectStderr, File... filesUsed)
      throws IOException;

}
