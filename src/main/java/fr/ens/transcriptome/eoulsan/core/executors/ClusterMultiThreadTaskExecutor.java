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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_CONTEXT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DONE_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_RESULT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_STDERR_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_STDOUT_EXTENSION;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.actions.ClusterTaskAction;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskResult;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskRunner;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

public class ClusterMultiThreadTaskExecutor extends AbstractTaskExecutor {

  private Queue<TaskThread> queue = Queues.newLinkedBlockingQueue();

  /**
   * Wrapper class around a call to executeTask method.
   * @author Laurent Jourdren
   */
  private final class TaskThread extends Thread {

    private final TaskContext context;

    @Override
    public void run() {

      // Execute task
      execute(this.context);

      // Remove the thread from the queue
      queue.remove(this);
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param context context to execute
     */
    TaskThread(final TaskContext context) {
      this.context = context;
    }
  }

  /**
   * Code to execute by the TaskThread.
   * @param context context to execute
   */
  @Override
  protected TaskResult executeTask(final TaskContext context) {

    // Get task dir
    final File taskDir = new File(context.getTaskPathname());

    // Get the prefix for the task files
    final String taskPrefix = TaskRunner.createTaskPrefixFile(context);

    // Define the file for the task context
    final File taskContextFile =
        new File(taskDir, taskPrefix + TASK_CONTEXT_EXTENSION);

    // Define stdout file
    final File taskStdoutFile =
        new File(taskDir, taskPrefix + TASK_STDOUT_EXTENSION);

    // Define stderr file
    final File taskStderrFile =
        new File(taskDir, taskPrefix + TASK_STDERR_EXTENSION);

    // Define the file for the task done
    final File taskDoneFile =
        new File(taskDir, taskPrefix + TASK_DONE_EXTENSION);

    // Define the file for the task result
    final File taskResultFile =
        new File(taskDir, taskPrefix + TASK_RESULT_EXTENSION);

    try {

      // Serialize the context object
      context.serialize(taskContextFile);

      final List<String> command = Lists.newArrayList();

      command.add(Main.getInstance().getEoulsanScriptPath());

      final String logLevel = Main.getInstance().getLogLevelArgument();

      if (logLevel != null) {
        command.add("-loglevel");
        command.add(logLevel);
      }

      command.add(ClusterTaskAction.ACTION_NAME);
      command.add(taskContextFile.getAbsolutePath());

      command.add(">");
      command.add(taskStdoutFile.getAbsolutePath());

      command.add("2>");
      command.add(taskStderrFile.getAbsolutePath());

      getLogger().info("Launch cluster task: " + command);

      // String cmd = "/bin/bash -c '" + Joiner.on(' ').join(command) + "'";
      String cmd = "/bin/ls -l /tmp";
      cmd =
          "/home/jourdren/workspace/eoulsan/target/dist/eoulsan-1.3-SNAPSHOT/eoulsan.sh "
              + ClusterTaskAction.ACTION_NAME + " "
              + taskContextFile.getAbsolutePath();

      // Execute task
      // final int exitCode = ProcessUtils.sh(command);
      // final int exitCode = ProcessUtils.system(cmd);
      // System.out.println("exit code: " + exitCode);

      // System.out.println("ProcessUtils output:\n"
      // + ProcessUtils.execToString(cmd, true, false));

      ProcessUtils.execToString(cmd, true, false);

      // if (exitCode != 0) {
      // throw new EoulsanException("Invalid task exit code: "
      // + exitCode + " for task #" + context.getId() + " in step "
      // + getStep(context).getId());
      // }

      if (!taskDoneFile.exists()) {
        throw new EoulsanException("No done file found for task #"
            + context.getId() + " in step " + getStep(context).getId());
      }

      TaskResult result = TaskResult.deserialize(taskResultFile);

      // Load output data objects
      context.deserializeOutputData(new File(taskDir, taskPrefix
          + TASK_DATA_EXTENSION));

      // Send tokens
      TaskRunner.sendTokens(context, result);

      return result;
    } catch (IOException e) {
      e.printStackTrace();
      return TaskRunner.createStepResult(context, e);
    } catch (EoulsanException e) {
      e.printStackTrace();
      return TaskRunner.createStepResult(context, e);
    }
  }

  @Override
  public void submit(final WorkflowStep step, final TaskContext context) {

    // Call to the super method
    super.submit(step, context);

    // Create the thread object
    final TaskThread st = new TaskThread(context);

    // Add the thread to the queue
    this.queue.add(st);

    // Start the Thread
    st.start();
  }

}
