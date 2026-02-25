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

package fr.ens.biologie.genomique.eoulsan.data;

/**
 * This class define source metadata
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface DataFileMetadata {

  /**
   * Get the content length of the file.
   *
   * @return the content length or -1 if unavailable
   */
  long getContentLength();

  /**
   * Get the content type of the file.
   *
   * @return the content type or null if unavailable
   */
  String getContentType();

  /**
   * Get the content type of the file.
   *
   * @return the content type or null if unavailable
   */
  String getContentEncoding();

  /**
   * Get the content MD5 of the file.
   *
   * @return the content MD5 or null if unavailable
   */
  String getContentMD5();

  /**
   * Get the date of the last modification of the file.
   *
   * @return the last modified date in seconds since epoch of -1 if unavailable
   */
  long getLastModified();

  /**
   * Test if the file is a directory.
   *
   * @return true if the file is a directory
   */
  boolean isDir();

  /**
   * Test if the file is a symbolic link.
   *
   * @return true if the file is symbolic link
   */
  boolean isSymbolicLink();

  /**
   * Get the DataFormat of the file.
   *
   * @return the DataFormat of the source
   */
  DataFormat getDataFormat();

  /**
   * Get the symbolic link target.
   *
   * @return The target of the symbolic link as DataFile
   */
  DataFile getLinkTarget();
}
