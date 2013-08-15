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

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;

/**
 * This interface define a protocol.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface DataProtocol {

  /**
   * Get Protocol name.
   * @return the name of the protocol
   */
  String getName();

  /**
   * Get the name of the filename that correspond to the source.
   * @param source the source
   * @return a String with the filename
   */
  String getSourceFilename(String source);

  /**
   * Get the parent source of the source.
   * @param src source to use
   * @return a String with the source of the parent or null if there is parent
   */
  DataFile getDataFileParent(DataFile src);

  /**
   * Create an InputStream from the source.
   * @param src source to use
   * @return an InputStream
   * @throws IOException if an error occurs while creating the InputStream
   */
  InputStream getData(DataFile src) throws IOException;

  /**
   * Create an OutputStream from the source.
   * @param dest destination to use
   * @return an OutputStream
   * @throws IOException if an error occurs while creating the OutputStream
   */
  OutputStream putData(DataFile dest) throws IOException;

  /**
   * Create an OutputStream from the source.
   * @param dest source to use
   * @param md metadata for the stream to write
   * @return an OutputStream
   * @throws IOException if an error occurs while creating the OutputStream
   */
  OutputStream putData(DataFile dest, DataFileMetadata md) throws IOException;

  /**
   * Copy data from a source to a destination source
   * @param src source source
   * @param dest destination source
   * @throws IOException if an error occurs while copying data
   */
  void putData(DataFile src, DataFile dest) throws IOException;

  /**
   * Test a source exists.
   * @param src source to use
   * @return true if the source exists
   */
  boolean exists(DataFile src);

  /**
   * Create a directory.
   * @param dir directory to create
   * @throws IOException if an error occurs while creating the directory
   */
  void mkdir(DataFile dir) throws IOException;

  /**
   * Create a directory and its parents if not exists.
   * @param dir directory to create
   * @throws IOException if an error occurs while creating the directory
   */
  void mkdirs(DataFile dir) throws IOException;

  /**
   * Create a symbolic link.
   * @param target target file
   * @param link symbolic link file
   * @throws IOException if an error occurs while creating the symbolic link
   */
  void symlink(DataFile target, DataFile link) throws IOException;

  /**
   * Delete a file.
   * @param file file to delete
   * @throws IOException if an error occurs while deleting the file
   */
  void delete(DataFile file) throws IOException;

  /**
   * Get the metadata for the source.
   * @param src source to use
   * @return always a metadataObject
   * @throws IOException if an error occurs while getting metadata
   */
  DataFileMetadata getMetadata(DataFile src) throws IOException;

  /**
   * Test if source is readable with this protocol.
   * @return true if the source is readable
   */
  boolean canRead();

  /**
   * Test if source is writable with this protocol.
   * @return true if the source is writable
   */
  boolean canWrite();

  /**
   * Test if the mkdir() and mkdirs() methods are available with this protocol.
   * @return true if mkdir() and mkdirs() are available
   */
  boolean canMkdir();

  /**
   * Test if the symlink() method is available with this protocol.
   * @return true if symlink() is available
   */
  boolean canSymlink();

  /**
   * Test if the delete() method is available with this protocol.
   * @return true if delete() is available
   */
  boolean canDelete();

  /**
   * Get the underlying File object for the DataFile if the protocol allow it.
   * @return a File object or null if the protocol does not allow it
   */
  File getSourceAsFile(DataFile src);

}
