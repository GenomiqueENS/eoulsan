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

package fr.ens.transcriptome.eoulsan.core.executors;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepContext;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This interface define a context executor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ContextExecutor {

  /**
   * Submit contexts to execute.
   * @param step step related to the contexts
   * @param contexts contexts to execute
   */
  void submit(WorkflowStep step, Set<WorkflowStepContext> contexts);

  /**
   * Submit a context to execute.
   * @param step step related to the context
   * @param context context to execute
   */
  void submit(WorkflowStep step, WorkflowStepContext context);

  /**
   * Get the status related to a step.
   * @param step a workflow step
   * @return the step status object related to the step
   */
  WorkflowStepStatus getStatus(WorkflowStep step);

  /**
   * Get the result related to a step.
   * @param step a workflow step
   * @return the step result object related to the step
   */
  WorkflowStepResult getResult(WorkflowStep step);

  /**
   * Get the count of submitted contexts of a step.
   * @param step a workflow step
   * @return the count of submitted contexts
   */
  int getContextSubmitedCount(WorkflowStep step);

  /**
   * Get the count of running contexts of a step.
   * @param step a workflow step
   * @return the count of running contexts
   */
  int getContextRunningCount(WorkflowStep step);

  /**
   * Get the count of done contexts of a step.
   * @param step a workflow step
   * @return the count of done contexts
   */
  int getContextDoneCount(WorkflowStep step);

  /**
   * Get the count of submitted contexts for the wokflow.
   * @return the count of submitted contexts
   */
  int getTotalContextSubmitedCount();

  /**
   * Get the count of running contexts for the workflow.
   * @return the count of running contexts
   */
  int getTotalContextRunningCount();

  /**
   * Get the count of done contexts for the workflow.
   * @return the count of done contexts
   */
  int getTotalContextDoneCount();

  /**
   * Start the executor.
   */
  void start();

  /**
   * Stop the executor.
   */
  void stop();

  /**
   * Pause the executor.
   */
  void pause();

  /**
   * Resume the executor.
   */
  void resume();

  /**
   * Wait the end of the contexts.
   */
  void waitEndOfContexts();

}
