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

import java.lang.annotation.Annotation;

/**
 * This class define an enum for the execution mode of Eoulsan.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public enum ExecutionMode {
  NONE,
  LOCAL_ONLY,
  HADOOP_COMPATIBLE,
  HADOOP_ONLY;

  /**
   * Get the Eoulsan annotation class that corresponds to the Eoulsan mode.
   *
   * @return an annotation class
   */
  public Class<? extends Annotation> getAnnotationClass() {

    switch (this) {
      case LOCAL_ONLY:
        return LocalOnly.class;

      case HADOOP_COMPATIBLE:
        return HadoopCompatible.class;

      case HADOOP_ONLY:
        return HadoopOnly.class;

      case NONE:
      default:
        return null;
    }
  }

  /**
   * Test if the excution mode is compatible with local mode.
   *
   * @return true if the mode is compatible with local mode
   */
  public boolean isLocalCompatible() {

    switch (this) {
      case LOCAL_ONLY:
      case HADOOP_COMPATIBLE:
        return true;

      case HADOOP_ONLY:
      case NONE:
      default:
        return false;
    }
  }

  /**
   * Test if the execution mode is compatible with Hadoop mode.
   *
   * @return true if the mode is compatible with Hadoop mode
   */
  public boolean isHadoopCompatible() {

    switch (this) {
      case HADOOP_COMPATIBLE:
      case HADOOP_ONLY:
        return true;

      case LOCAL_ONLY:
      case NONE:
      default:
        return false;
    }
  }

  //
  // Static methods
  //

  /**
   * Check that annotation of a class is compatible with the Eoulsan mode (local or Hadoop).
   *
   * @param clazz class to test
   * @param hadoopMode Hadoop mode
   * @return true if the annotation of the class is compatible with the Eoulsan mode
   */
  public static boolean accept(final Class<?> clazz, final boolean hadoopMode) {

    if (clazz == null) {
      return false;
    }

    final ExecutionMode mode = getExecutionMode(clazz);

    switch (mode) {
      case LOCAL_ONLY:
        return !hadoopMode;

      case HADOOP_COMPATIBLE:
        return true;

      case HADOOP_ONLY:
        return hadoopMode;

      case NONE:
      default:
        return false;
    }
  }

  /**
   * Get the execution mode related to an annotation class.
   *
   * @param annotationClazz class to test
   * @return an EoulsanMode object
   */
  public static ExecutionMode getExecutionMode(final Class<?> annotationClazz) {

    if (annotationClazz == null) {
      return null;
    }

    ExecutionMode result = null;

    for (ExecutionMode mode : ExecutionMode.values()) {

      final Class<? extends Annotation> annotation = mode.getAnnotationClass();
      if (annotation != null && annotationClazz.getAnnotation(annotation) != null) {

        if (result != null) {
          throw new IllegalStateException(
              "A class can not have more than one Eoulsan mode: " + annotationClazz.getName());
        }

        result = mode;
      }
    }

    if (result == null) {
      return NONE;
    }

    return result;
  }
}
