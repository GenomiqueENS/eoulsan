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

import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This interface define data used by ports.
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface Data {

  /**
   * Get data name.
   * @return the name of the data
   */
  String getName();

  /**
   * Get the data format of the data.
   * @return a DataFormat object
   */
  DataFormat getFormat();

  /**
   * Get metadata about the data.
   * @return a map with the metadata entries
   */
  Map<String, String> getMetadata();

  //
  // List methods
  //

  /**
   * Test if the data is a list.
   * @return true if the data is a list
   */
  boolean isList();

  /**
   * Get the the list of data.
   * @return a list even if the data is not a list
   */
  List<Data> getListElements();

  /**
   * Add a data to the list of data.
   * @param name name of the data
   * @return the data object added to the list
   */
  Data addDataToList(String name);

  //
  // Files methods
  //

  /**
   * Get the pathname for the data.
   * @return a String with the pathname
   */
  String getDataFilename();

  /**
   * Get the pathname for the data. This method works only for a multifile
   * DataFormat.
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throws fr.ens.transcriptome.eoulsan.EoulsanRuntimeException if the
   *           DataFormat is not multifile
   */
  String getDataFilename(int fileIndex);

  /**
   * Get the DataFile for an input DataType and a Sample.
   * @return a new DataFile object
   */
  DataFile getDataFile();

  /**
   * Get the DataFile for an input DataType and a Sample. This method works only
   * for a multifile DataFormat.
   * @param fileIndex file index for multifile data
   * @return a new DataFile object
   * @throws fr.ens.transcriptome.eoulsan.EoulsanRuntimeException if the
   *           DataFormat is not multifile
   */
  DataFile getDataFile(int fileIndex);

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @return the number of multifile for the DataFormat and the sample
   * @throws fr.ens.transcriptome.eoulsan.EoulsanRuntimeException if the
   *           DataFormat is not multifile
   */
  int getDataFileCount();

  /**
   * Count the number for DataFile available for a multifile DataFormat and a
   * Sample. This method works only for a multifile DataFormat.
   * @param existingFiles if true return the number of files that really exists
   * @return the number of multifile for the DataFormat and the sample
   * @throws fr.ens.transcriptome.eoulsan.EoulsanRuntimeException if the
   *           DataFormat is not multifile
   */
  int getDataFileCount(boolean existingFiles);

}
