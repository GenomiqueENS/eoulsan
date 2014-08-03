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

package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define an utility on data object.
 * @since 2.0
 * @author Laurent Jourdren
 */
public final class DataUtils {

  /**
   * Change the DataFile in a Data object
   * @param data Data object to modify
   * @param dataFile new DataFile
   */
  public static void setDataFile(final Data data, final DataFile dataFile) {

    Preconditions.checkNotNull(data, "data cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException(
          "data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(dataFile);
  }

  /**
   * Change the DataFile in a Data object
   * @param data Data object to modify
   * @param fileIndex file index
   * @param dataFile new DataFile
   */
  public static void setDataFile(final Data data, final int fileIndex,
      final DataFile dataFile) {

    Preconditions.checkNotNull(data, "data cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException(
          "data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(fileIndex, dataFile);
  }

  /**
   * Get the list of the DataFile objects in a Data object.
   * @param data data object
   * @return a list of DataFile objects
   */
  public static final List<DataFile> getDataFiles(final Data data) {

    if (data.isList()) {
      return Collections.emptyList();
    }

    final List<Data> result = Lists.newArrayList();
    return ((DataElement) data).getDataFiles();
  }

  //
  // Private constructor
  //

  private DataUtils() {
  }
}
