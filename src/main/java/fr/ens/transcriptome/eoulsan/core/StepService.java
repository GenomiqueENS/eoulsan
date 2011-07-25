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
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.steps.Step;

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
   * @param hadoopMode true if this service must return hadoopCompatible Steps
   * @return A StepService instance
   */
  public static synchronized StepService getInstance(final boolean hadoopMode) {

    if (service == null) {
      service = new StepService(hadoopMode);
    }

    return service;
  }

  /**
   * Get a Step object from its name.
   * @param stepName name of the step to retrieve
   */
  public Step getStep(final String stepName) {

    if (stepName == null) {
      return null;
    }

    final String stepNameLower = stepName.toLowerCase().trim();

    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    final Iterator<Step> it = this.loader.iterator();

    while (it.hasNext()) {

      try {
        final Step step = it.next();

        if (step.getName().equals(stepNameLower)) {

          return step;
        }
      } catch (ServiceConfigurationError e) {
        LOGGER.fine("Class for step cannot be load in "
            + (hadoopMode ? "hadoop" : "local") + " mode: " + e.getMessage());
      }

    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param hadoopMode true if this service must return hadoopCompatible Steps
   */
  @SuppressWarnings("unchecked")
  private StepService(final boolean hadoopMode) {

    final Set<Class<? extends Annotation>> autorisedAnnotations;

    if (hadoopMode) {
      autorisedAnnotations =
          Sets.newHashSet(HadoopOnly.class, HadoopCompatible.class);
    } else {
      autorisedAnnotations =
          Sets.newHashSet(LocalOnly.class, HadoopCompatible.class);
    }

    loader =
        ServiceLoader.load(Step.class, new ServiceClassLoader(
            autorisedAnnotations));

    // Log available steps
    final Iterator<Step> it = this.loader.iterator();

    while (it.hasNext()) {

      try {
        final Step step = it.next();

        final String stepName = step.getName();

        if (stepName == null || !stepName.toLowerCase().trim().equals(stepName)) {
          throw new EoulsanRuntimeException("Invalid name of step: "
              + stepName + " (Uppercase and spaces are forbidden).");
        }

        LOGGER.config("found step: "
            + stepName + " (" + step.getClass().getName() + ")");

      } catch (ServiceConfigurationError e) {
        LOGGER.fine("Class for step cannot be load in "
            + (hadoopMode ? "hadoop" : "local") + " mode: " + e.getMessage());
      }
    }
  }
}
