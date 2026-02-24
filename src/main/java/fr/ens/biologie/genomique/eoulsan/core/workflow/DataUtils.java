package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static java.util.Objects.requireNonNull;

/**
 * This class define useful methods to handle Data object.
 * @since 2.4
 * @author Laurent Jourdren
 */
class DataUtils {

  /**
   * Copy a Data object
   * @param data data to copy
   * @return the copied data
   */
  static AbstractData copy(final AbstractData data) {

    requireNonNull(data, "data argument cannot be null");

    // The data is a DataElement
    if (data instanceof DataElement) {
      return new DataElement((DataElement) data);
    }

    // The data is a DataList
    if (data instanceof DataList) {
      return new DataList((DataList) data);
    }

    throw new IllegalArgumentException(
        "This method cannot handle unmodifiable Data objects");
  }

}
