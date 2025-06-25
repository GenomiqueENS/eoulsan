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

package fr.ens.biologie.genomique.eoulsan.core;

import fr.ens.biologie.genomique.eoulsan.core.workflow.StepConfigurationContextImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class define utility methods on Context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public final class ContextUtils {

  /**
   * Get the job directory.
   * @param context task context
   * @return Returns the job directory
   */
  public static DataFile getJobDirectory(final TaskContext context) {

    if (context instanceof TaskContextImpl) {

      return ((TaskContextImpl) context).getJobDirectory();
    } else if (context instanceof StepConfigurationContextImpl) {

      return ((StepConfigurationContextImpl) context).getJobDirectory();
    }

    return null;
  }

  /**
   * Get the application jar file.
   * @param context task context
   * @return Returns the jar file
   */
  public static DataFile getJarPathname(final TaskContext context) {

    if (context instanceof TaskContextImpl) {

      return ((TaskContextImpl) context).getJarPathname();
    } else if (context instanceof StepConfigurationContextImpl) {

      return ((StepConfigurationContextImpl) context).getJarPathname();
    }

    return null;
  }

  /**
   * Get the local working directory.
   * @param context task context
   * @return Returns the local working directory
   */
  public static DataFile getLocalWorkingDirectory(final TaskContext context) {

    if (context instanceof TaskContextImpl) {

      return ((TaskContextImpl) context).getLocalWorkingPathname();
    } else if (context instanceof StepConfigurationContextImpl) {

      return ((StepConfigurationContextImpl) context)
          .getLocalWorkingDirectory();
    }

    return null;
  }

  /**
   * Get the Hadoop working directory.
   * @param context task context
   * @return Returns the Hadoop working directory
   */
  public static DataFile getHadoopWorkingDirectory(final TaskContext context) {

    if (context instanceof TaskContextImpl) {

      return ((TaskContextImpl) context).getHadoopWorkingPathname();
    } else if (context instanceof StepConfigurationContextImpl) {

      return ((StepConfigurationContextImpl) context)
          .getLocalWorkingDirectory();
    }

    return null;
  }

  /**
   * Get the task output directory.
   * @param context task context
   * @return Returns the task output directory
   */
  public static DataFile getTaskOutputDirectory(final TaskContext context) {

    if (context instanceof TaskContextImpl) {

      return ((TaskContextImpl) context).getTaskOutputDirectory();
    } else if (context instanceof StepConfigurationContextImpl) {

      return ((StepConfigurationContextImpl) context).getTaskOutputDirectory();
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private ContextUtils() {
    throw new IllegalStateException();
  }

}
