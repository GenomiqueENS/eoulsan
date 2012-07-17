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

package fr.ens.transcriptome.eoulsan.util;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.rosuda.REngine.REngineException;

public interface RSConnection {

  /**
   * Write a file to the RServer
   * @param outputFilename the filename
   * @param value The content of the file
   * @throws REngineException if an error occurs while writing the file
   */
  void writeStringAsFile(final String outputFilename, final String value)
      throws REngineException;

  /**
   * Create an inputStream on a file on RServer.
   * @param filename Name of the file on RServer to load
   * @return an inputStream
   * @throws REngineException if an exception occurs while reading file
   */
  public InputStream getFileInputStream(final String filename)
      throws REngineException;

  /**
   * Create an outputStream on a file on RServer.
   * @param filename Name of the file on RServer to write
   * @return an outputStream
   * @throws REngineException if an exception occurs while reading file
   */
  OutputStream getFileOutputStream(final String filename)
      throws REngineException;

  /**
   * Put a file from the RServer.
   * @param rServeFilename filename of the file to put
   * @param outputfile output file of the file to put
   * @throws REngineException if an error occurs while downloading the file
   */
  void putFile(final File inputFile, final String rServeFilename)
      throws REngineException;

  /**
   * Get a file from the RServer.
   * @param rServeFilename filename of the file to retrieve
   * @param outputfile output file of the file to retrieve
   * @throws REngineException if an error occurs while downloading the file
   */
  void getFile(final String rServeFilename, final File outputfile)
      throws REngineException;

  /**
   * Get a list of files from the RServer.
   * @param rServeFilenames list of filenames of the files to retrieve
   * @param zipFile zip output file for the files to retrieve
   * @throws REngineException if an error occurs while downloading the file
   */
  void getFilesIntoZip(final List<String> rServeFilenames, final File zipFile)
      throws REngineException;

  /**
   * Remove a file on the RServer
   * @param filename File to remove
   */
  void removeFile(final String filename) throws REngineException;

  /**
   * Execute a R code.
   * @param source code to execute
   * @throws REngineException if an error while executing the code
   */
  void executeRCode(final String source) throws REngineException;

  /**
   * Destroy the connection to the Rserve server
   * @throws REngineException if an error occurs while deleting to Rserve
   */
  void disConnect();

}