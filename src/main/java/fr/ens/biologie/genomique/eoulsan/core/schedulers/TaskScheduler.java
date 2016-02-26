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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core.schedulers;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.WorkflowStep;
import fr.ens.biologie.genomique.eoulsan.core.workflow.WorkflowStepResult;
import fr.ens.biologie.genomique.eoulsan.core.workflow.WorkflowStepStatus;

/**
 * This interface define a task scheduler.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface TaskScheduler {

  /**
   * Submit contexts to execute.
   * @param step step related to the contexts
   * @param contexts contexts to execute
   */
  void submit(WorkflowStep step, Set<TaskContextImpl> contexts);

  /**
   * Submit a context to execute.
   * @param step step related to the context
   * @param context context to execute
   */
  void submit(WorkflowStep step, TaskContextImpl context);

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
   * Get the count of submitted task contexts of a step.
   * @param step a workflow step
   * @return the count of submitted task contexts
   */
  int getTaskSubmittedCount(WorkflowStep step);

  /**
   * Get the count of running task contexts of a step.
   * @param step a workflow step
   * @return the count of running task contexts
   */
  int getTaskRunningCount(WorkflowStep step);

  /**
   * Get the count of done task contexts of a step.
   * @param step a workflow step
   * @return the count of done task contexts
   */
  int getTaskDoneCount(WorkflowStep step);

  /**
   * Wait the end of the task contexts.
   * @param step a workflow step
   */
  void waitEndOfTasks(WorkflowStep step);

  /**
   * Get the count of submitted task contexts for the workflow.
   * @return the count of submitted task contexts
   */
  int getTotalTaskSubmittedCount();

  /**
   * Get the count of running task contexts for the workflow.
   * @return the count of running task contexts
   */
  int getTotalTaskRunningCount();

  /**
   * Get the count of done task contexts for the workflow.
   * @return the count of done task contexts
   */
  int getTotalTaskDoneCount();

  /**
   * Start the scheduler.
   */
  void start();

  /**
   * Stop the scheduler.
   */
  void stop();

}
