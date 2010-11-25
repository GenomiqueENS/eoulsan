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

package fr.ens.transcriptome.eoulsan.core;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Iterator;
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

/**
 * This class allow to get a Step object.
 * @author Laurent Jourdren
 */
public class StepService {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static StepService service;
  private final ServiceLoader<Step> loader;

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

  /**
   * Retrieve the singleton static instance of StepService.
   * @return A StepService instance
   */
  public static synchronized StepService getInstance() {

    if (service == null) {
      service = new StepService();
    }

    return service;
  }

  /**
   * Retrieve definitions from the first provider that contains the word.
   */
  public Step getStep(final String stepName) {

    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    final Iterator<Step> it = this.loader.iterator();

    while (it.hasNext()) {

      try {
        final Step step = it.next();

        if (step.getName().equals(stepName)) {

          return step;
        }
      } catch (ServiceConfigurationError e) {
        LOGGER.info("Class cannot be load in "
            + (hadoopMode ? "hadoop" : "local") + " mode: " + e.getClass());
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
  @SuppressWarnings("unchecked")
  private StepService() {

    final Set<Class<? extends Annotation>> autorisedAnnotations;

    if (EoulsanRuntime.getRuntime().isAmazonMode()) {
      autorisedAnnotations =
          Sets.newHashSet(HadoopOnly.class, HadoopCompatible.class);
    } else {
      autorisedAnnotations =
          Sets.newHashSet(LocalOnly.class, HadoopCompatible.class);
    }

    loader =
        ServiceLoader.load(Step.class, new ServiceClassLoader(
            autorisedAnnotations));
  }

}
