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

/**
 * This class define utility methods on Context.
 * @author Laurent Jourdren
 * @since 2.0
 */
public final class ContextUtils {

  /**
   * Get the step working path.
   * @return Returns the step working path
   */
  public static String getStepWorkingPathname(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getStepWorkingPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context)
          .getStepWorkingPathname();
    }

    return null;
  }

  /**
   * Get the job path.
   * @return Returns the job Path
   */
  public static String getJobPathname(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getJobPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context).getJobPathname();
    }

    return null;
  }

  /**
   * Get the application jar path.
   * @return Returns the jar path
   */
  public static String getJarPathname(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getJarPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context).getJarPathname();
    }

    return null;
  }

  /**
   * Get the local working path.
   * @return Returns the local working Path
   */
  public static String getLocalWorkingPathname(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getLocalWorkingPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context)
          .getLocalWorkingPathname();
    }

    return null;
  }

  /**
   * Get the Hadoop working path.
   * @return Returns the Hadoop working Path
   */
  public static String getHadoopWorkingPathname(final StepContext context) {

    if (context instanceof TaskContext) {

      return ((TaskContext) context).getHadoopWorkingPathname();
    } else if (context instanceof WorkflowStepConfigurationContext) {

      return ((WorkflowStepConfigurationContext) context)
          .getHadoopWorkingPathname();
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
