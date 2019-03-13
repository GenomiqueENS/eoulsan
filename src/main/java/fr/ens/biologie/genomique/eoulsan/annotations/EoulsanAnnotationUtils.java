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

package fr.ens.biologie.genomique.eoulsan.annotations;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;

import fr.ens.biologie.genomique.eoulsan.core.Module;

/**
 * This class contains annotation utilities.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EoulsanAnnotationUtils {

  /**
   * Test if a module contain the @Generator annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isGenerator(final Module module) {

    return isAnnotation(module, Generator.class);
  }

  /**
   * Test if a module contain the @ReuseStepInstance annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isReuseStepInstance(final Module module) {

    return isAnnotation(module, ReuseModuleInstance.class);
  }

  /**
   * Test if a step contain the @Terminal annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isTerminal(final Module module) {

    return isAnnotation(module, Terminal.class);
  }

  /**
   * Test if a module contain the @NoLog annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isNoLog(final Module module) {

    return isAnnotation(module, NoLog.class);
  }

  /**
   * Test if a module contain the @RequiresPreviousStep annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isRequiresPreviousStep(final Module module) {

    return isAnnotation(module, RequiresPreviousStep.class);
  }

  /**
   * Test if a module contain the @RequiresPreviousStep annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isRequiresAllPreviousSteps(final Module module) {

    return isAnnotation(module, RequiresAllPreviousSteps.class);
  }

  /**
   * Test if a module contain the @NoOutputDirectory annotation.
   * @param module the module to test
   * @return true if the module contains the annotation
   */
  public static boolean isNoOutputDirectory(final Module module) {

    return isAnnotation(module, NoOutputDirectory.class);
  }

  /**
   * Test if a module contains an annotation.
   * @param module the module
   * @param clazz the annotation to test
   * @return true if the module contains the annotation
   */
  private static boolean isAnnotation(final Module module,
      Class<? extends Annotation> clazz) {

    requireNonNull(module, "module argument cannot be null");
    requireNonNull(clazz, "clazz argument cannot be null");

    return module.getClass().getAnnotation(clazz) != null;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private EoulsanAnnotationUtils() {

    throw new IllegalStateException();
  }

}
