/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;

public class DataProtocolService {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static DataProtocolService service;
  private final ServiceLoader<DataProtocol> loader;

  private Map<String, DataProtocol> protocols =
      new HashMap<String, DataProtocol>();

  private String defaultProtocolName;

  /**
   * This class loader allow to reject Steps without correct annotation to
   * prevent instantiation of classes that use Hadoop API.
   * @author Laurent Jourdren
   */
  private final static class ServiceClassLoader extends ClassLoader {

    /** The set if the authorized annotations. */
    private final Set<Class<? extends Annotation>> authorizedAnnotations;

    @Override
    public Class<?> loadClass(final String arg0) throws ClassNotFoundException {

      final Class<?> result = super.loadClass(arg0);

      if (result == null) {
        return null;
      }

      if (testClass(result)) {
        return result;
      }

      return null;
    }

    /**
     * Test if a class has correct annotation
     * @param clazz the class to test
     * @return true if the class has correct annotation
     */
    private boolean testClass(Class<?> clazz) {

      for (Class<? extends Annotation> annot : authorizedAnnotations) {
        if (clazz.getAnnotation(annot) != null) {
          return true;
        }
      }

      return false;
    }

    /**
     * Constructor.
     * @param authorizedAnnotations set with authorized annotation
     */
    public ServiceClassLoader(
        Set<Class<? extends Annotation>> autorisedAnnotations) {

      super(ServiceClassLoader.class.getClassLoader());

      if (autorisedAnnotations == null) {
        throw new NullPointerException("The autorized annotation is null.");
      }

      this.authorizedAnnotations =
          new HashSet<Class<? extends Annotation>>(autorisedAnnotations);
    }

  }

  //
  // Static method
  //

  /**
   * Retrieve the singleton static instance of StepService.
   * @return A StepService instance
   */
  public static synchronized DataProtocolService getInstance() {

    if (service == null) {
      service = new DataProtocolService();
    }

    return service;
  }

  //
  // Instance methods
  //

  /**
   * Get the default protocol.
   * @return the default DataProtocol
   */
  public FileDataProtocol getDefaultProtocol() {

    return (FileDataProtocol) getProtocol(this.defaultProtocolName);
  }

  /**
   * Get DataProtocol.
   * @param protocolName name of the protocol to get
   * @return a DataProtocol
   */
  public DataProtocol getProtocol(final String protocolName) {

    return this.protocols.get(protocolName);
  }

  /**
   * Test if a protocol exists.
   * @param protocolName name of the protocol to test
   * @return true if the protocol exists
   */
  public boolean isProtocol(final String protocolName) {

    return this.protocols.containsKey(protocolName);
  }

  /**
   * Reload the protocols.
   */
  public void reload() {

    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    final Iterator<DataProtocol> it = this.loader.iterator();

    while (it.hasNext()) {

      try {
        final DataProtocol protocol = it.next();

        if (!this.defaultProtocolName.equals(protocol.getName()))
          this.protocols.put(protocol.getName(), protocol);

      } catch (ServiceConfigurationError e) {
        LOGGER.fine("Class for DataProtocol cannot be load in "
            + (hadoopMode ? "hadoop" : "local") + " mode: " + e.getMessage());
      }
    }
  }

  //
  // Constructor
  //

  /**
   * Private protocol.
   */
  @SuppressWarnings("unchecked")
  private DataProtocolService() {

    final Set<Class<? extends Annotation>> autorisedAnnotations;

    if (EoulsanRuntime.getRuntime().isHadoopMode()) {
      autorisedAnnotations =
          Sets.newHashSet(HadoopOnly.class, HadoopCompatible.class);
    } else {
      autorisedAnnotations =
          Sets.newHashSet(LocalOnly.class, HadoopCompatible.class);
    }

    loader =
        ServiceLoader.load(DataProtocol.class, new ServiceClassLoader(
            autorisedAnnotations));

    final DataProtocol defaultProtocol = new FileDataProtocol();
    this.defaultProtocolName = defaultProtocol.getName();
    this.protocols.put(this.defaultProtocolName, defaultProtocol);

    reload();
  }

}
