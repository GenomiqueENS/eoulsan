package fr.ens.transcriptome.eoulsan.core.workflow;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * Created by jourdren on 22/07/14.
 */
public final class DataUtils {


  public static void setDataFile(final Data data, final DataFile dataFile) {

    Preconditions.checkNotNull(data, "data cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException("data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(dataFile);
  }

  public static void setDataFile(final Data data, final int fileIndex, final DataFile dataFile) {

    Preconditions.checkNotNull(data, "data cannot be null");

    if (data.isList()) {
      throw new IllegalArgumentException("data list are not handled by this method");
    }

    final DataElement modifiableData = (DataElement) data;
    modifiableData.setDataFile(fileIndex,  dataFile);
  }

  //
  // Private constructor
  //

  private DataUtils() {}
}
