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

package fr.ens.biologie.genomique.eoulsan.core;

import fr.ens.biologie.genomique.eoulsan.core.workflow.WorkflowDataUtils;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import java.util.Collection;
import java.util.List;

/**
 * This class define an utility on data object.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public final class DataUtils {

  /**
   * Get the list of the DataFile objects in a Data object.
   *
   * @param data data object
   * @return a list of DataFile objects
   */
  public static List<DataFile> getDataFiles(final Data data) {

    return WorkflowDataUtils.getDataFiles(data);
  }

  /**
   * Change the DataFile in a Data object
   *
   * @param data Data object to modify
   * @param dataFile new DataFile
   */
  public static void setDataFile(final Data data, final DataFile dataFile) {

    WorkflowDataUtils.setDataFile(data, dataFile);
  }

  /**
   * Change the DataFiles in a Data object
   *
   * @param data Data object to modify
   * @param dataFiles DataFiles to set
   */
  public static void setDataFiles(final Data data, final List<DataFile> dataFiles) {

    WorkflowDataUtils.setDataFiles(data, dataFiles);
  }

  /**
   * Set the metadata of a data object from the information of another data object.
   *
   * @param data the data object
   * @param dataSourceOfMetadata data source of metadata
   */
  public static void setDataMetadata(final Data data, final Collection<Data> dataSourceOfMetadata) {

    WorkflowDataUtils.setDataMetadata(data, dataSourceOfMetadata);
  }

  /**
   * Set the metadata of a data object from the information of a Sample object from a Design.
   *
   * @param data the data object
   * @param sample the sample
   */
  public static void setDataMetaData(final Data data, final Sample sample) {

    WorkflowDataUtils.setDataMetaData(data, sample);
  }

  //
  // Private constructor
  //

  private DataUtils() {}
}
