package fr.ens.transcriptome.eoulsan.bio.alignmentsfilters;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * This class define a service to retrieve a AlignentsFilter.
 * @author Laurent Jourdren
 */
public class AlignmentsFilterService {

  private static AlignmentsFilterService service;
  private final ServiceLoader<AlignmentsFilter> loader;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an AlignmentsFilter.
   * @return A ActionService instance
   */
  public static synchronized AlignmentsFilterService getInstance() {

    if (service == null) {
      service = new AlignmentsFilterService();
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
  public AlignmentsFilter getAlignmentsFilter(final String alignmentsFilterName) {

    if (alignmentsFilterName == null) {
      return null;
    }

    final String actionNameLower = alignmentsFilterName.toLowerCase();

    final Iterator<AlignmentsFilter> it = this.loader.iterator();

    while (it.hasNext()) {

      final AlignmentsFilter filter = it.next();

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
  private AlignmentsFilterService() {

    loader = ServiceLoader.load(AlignmentsFilter.class);
  }

}
