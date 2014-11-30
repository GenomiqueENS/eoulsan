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

package fr.ens.transcriptome.eoulsan.core.schedulers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_CONTEXT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DONE_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_RESULT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_STDERR_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_STDOUT_EXTENSION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Queues;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.actions.ClusterTaskAction;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskResult;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskRunner;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class is a scheduler for task running on a cluster.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ClusterMultiThreadTaskScheduler extends AbstractTaskScheduler {

  private Queue<TaskThread> queue = Queues.newLinkedBlockingQueue();

  /**
   * This class allow to fetch standard output or standard error.
   */
  public static final class ProcessThreadOutput extends Thread {

    final InputStream in;
    final OutputStream out;

    @Override
    public void run() {

      try {
        FileUtils.copy(in, out);
      } catch (IOException e) {
        getLogger().severe(e.getMessage());
      }
    }

    /**
     * Constructor.
     * @param in Input stream
     * @param out Output Stream
     */
    public ProcessThreadOutput(final InputStream in, final OutputStream out) {

      this.in = in;
      this.out = out;
    }
  }

  /**
   * Wrapper class around a call to executeTask methods.
   * @author Laurent Jourdren
   */
  private final class TaskThread extends Thread {

    private final TaskContext context;
    private final File taskDir;
    private final String taskPrefix;
    private Process process;

    /**
     * Create the process
     * @throws IOException if an error occurs while creating the process
     */
    private void createProcess() throws IOException {

      // Define the file for the task context
      final File taskContextFile =
          new File(this.taskDir, this.taskPrefix + TASK_CONTEXT_EXTENSION);

      // Serialize the context object
      context.serialize(taskContextFile);

      final List<String> command = new ArrayList<>();

      command.add(Main.getInstance().getEoulsanScriptPath());

      final String logLevel = Main.getInstance().getLogLevelArgument();

      if (logLevel != null) {
        command.add("-loglevel");
        command.add(logLevel);
      }

      command.add(ClusterTaskAction.ACTION_NAME);
      command.add(taskContextFile.getAbsolutePath());

      final ProcessBuilder pb = new ProcessBuilder(command);

      // Start the process
      this.process = pb.start();
    }

    /**
     * Load the result of the step
     * @return a TaskResult object
     * @throws EoulsanException if the done task is not found
     * @throws IOException if an error occurs while reading the result file
     */
    private TaskResult loadResult() throws EoulsanException, IOException {

      // Define the file for the task done
      final File taskDoneFile =
          new File(taskDir, taskPrefix + TASK_DONE_EXTENSION);

      if (!taskDoneFile.exists()) {
        throw new EoulsanException("No done file found for task #"
            + context.getId() + " in step " + getStep(context).getId());
      }

      // Define the file for the task result
      final File taskResultFile =
          new File(taskDir, taskPrefix + TASK_RESULT_EXTENSION);
      // Load output data objects
      context.deserializeOutputData(new File(taskDir, taskPrefix
          + TASK_DATA_EXTENSION));

      return TaskResult.deserialize(taskResultFile);
    }

    /**
     * Redirect standard and error output of the process to files
     * @throws FileNotFoundException if file to redirect are not found
     */
    private void redirectProcessStream() throws FileNotFoundException {

      checkState(this.process != null, "Process has not been created");

      // Define stdout file
      final File taskStdoutFile =
          new File(taskDir, taskPrefix + TASK_STDOUT_EXTENSION);

      // Start stdout thread
      new ProcessThreadOutput(this.process.getInputStream(),
          new FileOutputStream(taskStdoutFile)).start();

      // Define stderr file
      final File taskStderrFile =
          new File(taskDir, taskPrefix + TASK_STDERR_EXTENSION);

      // Start stderr thread
      new ProcessThreadOutput(this.process.getErrorStream(),
          new FileOutputStream(taskStderrFile)).start();
    }

    @Override
    public void run() {

      TaskResult result = null;

      try {

        // Change task state
        beforeExecuteTask(this.context);

        // Start process
        createProcess();

        // Redirect stdout and stderr to files
        redirectProcessStream();

        // Wait process
        final int exitCode = this.process.waitFor();

        // Set exception if exit code is not 0
        if (exitCode != 0) {
          throw new EoulsanException("Invalid task exit code: "
              + exitCode + " for task #" + context.getId() + " in step "
              + getStep(context).getId());
        }

        // Load result
        result = loadResult();

        // Send tokens
        TaskRunner.sendTokens(this.context, result);

      } catch (IOException | EoulsanException | InterruptedException e) {
        e.printStackTrace();
        result = TaskRunner.createStepResult(this.context, e);
      } finally {

        // Change task state
        afterExecuteTask(this.context, result);

        // Remove the thread from the queue
        queue.remove(this);
      }
    }

    @SuppressWarnings("deprecation")
    public void destroy() {

      if (this.process != null) {
        this.process.destroy();
      }
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param context context to execute
     */
    TaskThread(final TaskContext context) {

      checkNotNull(context, "context argument cannot be null");

      this.context = context;
      this.taskDir = new File(context.getTaskPathname());
      this.taskPrefix = TaskRunner.createTaskPrefixFile(context);
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

  @Override
  public void stop() {

    for (TaskThread thread : this.queue) {

      // Kill the subprocess
      thread.destroy();
    }

    this.queue.clear();
  }

}
