package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * This class define a service to retrieve a AlignentsFilter.
 * @author Laurent Jourdren
 */
public class ReadAlignmentsFilterService {

  private static ReadAlignmentsFilterService service;
  private final ServiceLoader<ReadAlignmentsFilter> loader;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an AlignmentsFilter.
   * @return A ActionService instance
   */
  public static synchronized ReadAlignmentsFilterService getInstance() {

    if (service == null) {
      service = new ReadAlignmentsFilterService();
    }

    return service;
  }

  //
  // Instance methods
  //

  /**
   * Get a alignmentsFilter object.
   * @param alignmentsFilterName name of the filter to get
   * @return an Action
   */
  public ReadAlignmentsFilter getAlignmentsFilter(final String alignmentsFilterName) {

    if (alignmentsFilterName == null) {
      return null;
    }

    final String actionNameLower = alignmentsFilterName.toLowerCase();

    final Iterator<ReadAlignmentsFilter> it = this.loader.iterator();

    while (it.hasNext()) {

      final ReadAlignmentsFilter filter = it.next();

      if (actionNameLower.equals(filter.getName().toLowerCase())) {
        return filter;
      }
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private ReadAlignmentsFilterService() {

    loader = ServiceLoader.load(ReadAlignmentsFilter.class);
  }

}
