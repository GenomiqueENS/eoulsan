package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import fr.ens.biologie.genomique.kenetre.util.ServiceNameLoader;

/**
 * This class define a service to retrieve a InputPreprocessor.
 *
 * @since 2.2
 * @author Laurent Jourdren
 */
public class InputPreprocessorService extends ServiceNameLoader<InputPreprocessor> {

  private static InputPreprocessorService service;

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of an AlignmentsFilter.
   *
   * @return A ActionService instance
   */
  public static synchronized InputPreprocessorService getInstance() {

    if (service == null) {
      service = new InputPreprocessorService();
    }

    return service;
  }

  //
  // Protected methods
  //

  @Override
  protected boolean accept(final Class<?> clazz) {

    return true;
  }

  @Override
  protected String getMethodName() {

    return "getReportName";
  }

  //
  // Instance methods
  //

  //
  // Constructor
  //

  /** Private constructor. */
  private InputPreprocessorService() {
    super(InputPreprocessor.class);
  }
}
