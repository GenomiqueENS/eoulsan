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

package fr.ens.transcriptome.eoulsan.core.schedulers.clusters;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_CONTEXT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DONE_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_RESULT_EXTENSION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Queues;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Main;
import fr.ens.transcriptome.eoulsan.actions.ClusterTaskAction;
import fr.ens.transcriptome.eoulsan.core.schedulers.AbstractTaskScheduler;
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
public abstract class AbstractClusterTaskScheduler extends AbstractTaskScheduler
    implements ClusterTaskScheduler {

  private static final int STATUS_UPDATE_DELAY = 5 * 1000;

  private final Queue<TaskThread> queue = Queues.newLinkedBlockingQueue();

  /**
   * This class allow to fetch standard output or standard error.
   */
  public final class ProcessThreadOutput extends Thread {

    final InputStream in;
    final OutputStream out;

    @Override
    public void run() {

      try {
        FileUtils.copy(this.in, this.out);
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
    private String jobId;

    /**
     * Create the Eoulsan command to submit.
     * @return a list with the arguments of the command to submit
     * @throws IOException if an error occurs while creating the process
     */
    private List<String> createJobCommand() throws IOException {

      // Define the file for the task context
      final File taskContextFile =
          new File(this.taskDir, this.taskPrefix + TASK_CONTEXT_EXTENSION);

      // Serialize the context object
      this.context.serialize(taskContextFile);

      final List<String> command = new ArrayList<>();

      final File eoulsanScriptFile =
          new File(Main.getInstance().getEoulsanScriptPath());
      command.add(eoulsanScriptFile.getAbsolutePath());

      final String logLevel = Main.getInstance().getLogLevelArgument();

      command.add("-w");
      command.add(System.getProperty("user.dir"));

      if (logLevel != null) {
        command.add("-loglevel");
        command.add(logLevel);
      }

      command.add(ClusterTaskAction.ACTION_NAME);
      command.add(taskContextFile.getAbsolutePath());

      return Collections.unmodifiableList(command);
    }

    /**
     * Create the job name.
     * @return the job name
     */
    private String createJobName() {

      return this.context.getJobId() + "-" + this.taskPrefix;
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
          new File(this.taskDir, this.taskPrefix + TASK_DONE_EXTENSION);

      if (!taskDoneFile.exists()) {
        throw new EoulsanException("No done file found for task #"
            + this.context.getId() + " in step "
            + getStep(this.context).getId());
      }

      // Define the file for the task result
      final File taskResultFile =
          new File(this.taskDir, this.taskPrefix + TASK_RESULT_EXTENSION);
      // Load output data objects
      this.context.deserializeOutputData(
          new File(this.taskDir, this.taskPrefix + TASK_DATA_EXTENSION));

      return TaskResult.deserialize(taskResultFile);
    }

    @Override
    public void run() {

      TaskResult result = null;

      try {

        // Change task state
        beforeExecuteTask(this.context);

        // Submit Job
        this.jobId = submitJob(createJobName(), createJobCommand(),
            ((TaskContext) this.context).getTaskOutputDirectory().toFile(),
            this.context.getId());

        StatusResult status = null;

        boolean completed = false;

        do {

          status = statusJob(this.jobId);

          switch (status.getStatusValue()) {

          case COMPLETE:
            completed = true;

          case WAITING:
          case RUNNING:
          case UNKNOWN:
          default:
            break;
          }

          // Wait before do another query on job status
          Thread.sleep(STATUS_UPDATE_DELAY);

        } while (!completed);

        if (status.getExitCode() != 0) {
          throw new EoulsanException("Invalid task exit code: "
              + status.getExitCode() + " for task #" + this.context.getId()
              + " in step " + getStep(this.context).getId());
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
        AbstractClusterTaskScheduler.this.queue.remove(this);
      }
    }

    /**
     * Stop the thread.
     */
    public void stopThread() {

      if (this.jobId != null) {

        try {
          stopJob(this.jobId);
        } catch (IOException e) {
          getLogger().severe(
              "Error while stopping job " + this.jobId + ": " + e.getMessage());
        }
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
      this.taskDir = context.getTaskOutputDirectory().toFile();
      this.taskPrefix = context.getTaskFilePrefix();
    }
  }

  //
  // Task scheduler methods
  //

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
      thread.stopThread();
    }

    this.queue.clear();
  }

}
