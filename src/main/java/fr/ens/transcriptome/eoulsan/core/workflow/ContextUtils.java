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

package fr.ens.transcriptome.eoulsan.core.workflow;

import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class define utility methods on Context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public final class ContextUtils {

  /**
   * Get the job directory.
   * @return Returns the job directory
   */
  public static DataFile getJobDirectory(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getJobDirectory();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context).getJobDirectory();
    }

    return null;
  }

  /**
   * Get the application jar file.
   * @return Returns the jar file
   */
  public static DataFile getJarPathname(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getJarPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context).getJarPathname();
    }

    return null;
  }

  /**
   * Get the local working directory.
   * @return Returns the local working directory
   */
  public static DataFile getLocalWorkingDirectory(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getLocalWorkingPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context)
          .getLocalWorkingDirectory();
    }

    return null;
  }

  /**
   * Get the Hadoop working directory.
   * @return Returns the Hadoop working directory
   */
  public static DataFile getHadoopWorkingDirectory(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getHadoopWorkingPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context)
          .getLocalWorkingDirectory();
    }

    return null;
  }

  /**
   * Get the task output directory.
   * @return Returns the task output directory
   */
  public static DataFile getTaskOutputDirectory(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getTaskOutputDirectory();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context)
          .getTaskOutputDirectory();
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
