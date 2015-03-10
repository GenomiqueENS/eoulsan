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

package fr.ens.transcriptome.eoulsan.annotations;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;

import fr.ens.transcriptome.eoulsan.core.Step;

/**
 * This class contains annotation utilities.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EoulsanAnnotationUtils {

  /**
   * Test if a step contain the Generator annotation.
   * @param step the step to test
   * @return true if the step contains the annotation
   */
  public static boolean isGenerator(final Step step) {

    return isAnnotation(step, Generator.class);
  }

  /**
   * Test if a step contain the @ReuseStepInstance annotation.
   * @param step the step to test
   * @return true if the step contains the annotation
   */
  public static boolean isReuseStepInstance(final Step step) {

    return isAnnotation(step, ReuseStepInstance.class);
  }

  /**
   * Test if a step contains an annotation.
   * @param step the step
   * @param clazz the annotation to test
   * @return true if the step contains the annotation
   */
  private static boolean isAnnotation(final Step step,
      Class<? extends Annotation> clazz) {

    checkNotNull(step, "step argument cannot be null");
    checkNotNull(clazz, "clazz argument cannot be null");

    return step.getClass().getAnnotation(clazz) != null;
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
