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

import java.lang.annotation.Annotation;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * This class contains utility methods for annotation package.
 * @author Laurent Jourdren
 * @since 1.3
 */
@SuppressWarnings("unchecked")
public class AnnotationUtils {

  private static final Set<Class<? extends Annotation>> HADOOP_AUTORIZED_ANNOTATIONS =
      Sets.newHashSet(HadoopOnly.class, HadoopCompatible.class);
  private static final Set<Class<? extends Annotation>> LOCAL_AUTORIZED_ANNOTATIONS =
      Sets.newHashSet(LocalOnly.class, HadoopCompatible.class);

  public static boolean accept(Class<?> clazz, final boolean hadoopMode) {

    final Set<Class<? extends Annotation>> authorizedAnnotations =
        hadoopMode ? HADOOP_AUTORIZED_ANNOTATIONS : LOCAL_AUTORIZED_ANNOTATIONS;

    for (Class<? extends Annotation> annot : authorizedAnnotations) {
      if (clazz.getAnnotation(annot) != null) {
        return true;
      }
    }

    return false;
  }

}
