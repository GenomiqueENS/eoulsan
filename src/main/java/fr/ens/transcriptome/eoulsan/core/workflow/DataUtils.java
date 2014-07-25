package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define an utility on data object.
 * @since 1.3
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
      throw new IllegalArgumentException("data list are not handled by this method");
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
  public static void setDataFile(final Data data, final int fileIndex, final DataFile dataFile) {

    Preconditions.checkNotNull(data, "data cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException("data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(fileIndex,  dataFile);
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

  private DataUtils() {}
}
