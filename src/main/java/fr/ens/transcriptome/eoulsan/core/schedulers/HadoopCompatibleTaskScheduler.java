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
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_DONE_EXTENSION;
import static fr.ens.transcriptome.eoulsan.Globals.TASK_RESULT_EXTENSION;
import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.createConfiguration;

import java.io.IOException;
import java.util.Queue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;

import com.google.common.collect.Queues;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskContext;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskResult;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskRunner;
import fr.ens.transcriptome.eoulsan.core.workflow.TaskSerializationUtils;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class is a scheduler for tasks from step with the @HadoopComptible
 * annotation in Hadoop mode.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class HadoopCompatibleTaskScheduler extends AbstractTaskScheduler {

  private final Configuration conf;
  private final Queue<TaskThread> queue = Queues.newLinkedBlockingQueue();

  /**
   * Wrapper class around a call to executeTask methods.
   * @author Laurent Jourdren
   */
  private final class TaskThread extends Thread {

    private final TaskContext context;
    private final Configuration conf;
    private final DataFile taskDir;
    private final String taskPrefix;
    private String jobId;
    private Job hadoopJob;

    private Job createHadoopJob(final Configuration conf,
        final DataFile submitFile, final String jobDescription)
        throws IOException {

      final Configuration jobConf = new Configuration(conf);

      // Set one task per map
      jobConf.set("mapreduce.input.lineinputformat.linespermap", "" + 1);

      // Set Job name
      // Create the job and its name
      final Job job = Job.getInstance(jobConf, jobDescription);

      // Set the jar
      job.setJarByClass(HadoopCompatibleTaskScheduler.class);

      // Set input path
      FileInputFormat.addInputPath(job, new Path(submitFile.getSource()));

      job.setInputFormatClass(NLineInputFormat.class);

      // Set the Mapper class
      job.setMapperClass(HadoopCompatibleMapper.class);

      // Set the output key class
      job.setOutputKeyClass(NullWritable.class);

      // Set the output value class
      job.setOutputValueClass(NullWritable.class);

      // Set the output format
      job.setOutputFormatClass(NullOutputFormat.class);

      // Set the number of reducers
      job.setNumReduceTasks(0);

      return job;
    }

    /**
     * Load the result of the step
     * @return a TaskResult object
     * @throws EoulsanException if the done task is not found
     * @throws IOException if an error occurs while reading the result file
     */
    private TaskResult loadResult() throws EoulsanException, IOException {

      // Define the file for the task done
      final DataFile taskDoneFile =
          new DataFile(this.taskDir, this.taskPrefix + TASK_DONE_EXTENSION);

      if (!taskDoneFile.exists()) {
        throw new EoulsanException("No done file found for task #"
            + this.context.getId() + " in step "
            + getStep(this.context).getId());
      }

      // Define the file for the task result
      final DataFile taskResultFile =
          new DataFile(this.taskDir, this.taskPrefix + TASK_RESULT_EXTENSION);
      // Load output data objects
      this.context.deserializeOutputData(new DataFile(this.taskDir,
          this.taskPrefix + TASK_DATA_EXTENSION));

      return TaskResult.deserialize(taskResultFile);
    }

    @Override
    public void run() {

      TaskResult result = null;

      try {

        // Create job directory
        this.taskDir.mkdir();

        final DataFile taskContextFile =
            new DataFile(this.taskDir, this.taskPrefix
                + Globals.TASK_CONTEXT_EXTENSION);

        // Serialize the context object
        this.context.serialize(taskContextFile);

        // Change task state
        beforeExecuteTask(this.context);

        // Submit Job
        this.hadoopJob =
            createHadoopJob(this.conf, taskContextFile, "Eoulsan Task #"
                + this.context.getId() + " (" + this.context.getContextName()
                + ")");

        // Submit the job to the Hadoop scheduler, and wait the end of the job
        // in non verbose mode
        this.hadoopJob.waitForCompletion(false);

        if (!this.hadoopJob.isSuccessful()) {
          throw new EoulsanException(
              "Error while running Hadoop job for Eoulsan task #"
                  + this.context.getId() + "(" + this.context.getContextName()
                  + ")");
        }

        // Load result
        result = loadResult();

        // Send tokens
        TaskRunner.sendTokens(this.context, result);

        // Remove task files
        this.taskDir.delete(true);

      } catch (IOException | EoulsanException | InterruptedException
          | ClassNotFoundException e) {
        e.printStackTrace();
        result = TaskRunner.createStepResult(this.context, e);
      } finally {

        // Change task state
        afterExecuteTask(this.context, result);

        // Remove the thread from the queue
        queue.remove(this);
      }
    }

    /**
     * Stop the thread.
     */
    public void stopThread() {

      if (this.jobId != null) {

        try {
          if (this.hadoopJob != null) {
            this.hadoopJob.killJob();
          }
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
    TaskThread(final Configuration conf, final TaskContext context) {

      checkNotNull(conf, "conf argument cannot be null");
      checkNotNull(context, "context argument cannot be null");

      final DataFile hadoopWorkDir = context.getHadoopWorkingPathname();

      this.conf = conf;
      this.context = context;
      this.taskDir =
          new DataFile(hadoopWorkDir, "eoulsan-hadoop-compatible-task-"
              + this.context.getId());
      this.taskPrefix = context.getTaskFilePrefix();
    }
  }

  //
  // Hadoop Mapper class
  //

  public static final class HadoopCompatibleMapper extends
      Mapper<LongWritable, Text, NullWritable, NullWritable> {

    @Override
    protected void setup(
        Mapper<LongWritable, Text, NullWritable, NullWritable>.Context context)
        throws IOException, InterruptedException {

      EoulsanLogger.initConsoleHandler();
      getLogger().info("Start of setup()");

      // Get configuration object
      final Configuration conf = context.getConfiguration();

      // Initialize Eoulsan Settings
      if (!EoulsanRuntime.isRuntime()) {
        HadoopEoulsanRuntime.newEoulsanRuntime(conf);
      }

      getLogger().info("End of setup()");
    }

    @Override
    protected void map(final LongWritable key, final Text value,
        final Context context) throws IOException, InterruptedException {

      try {

        // Execute the task
        TaskSerializationUtils.execute(new DataFile(value.toString()));

      } catch (EoulsanException e) {
        throw new IOException(e);
      }
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
    final TaskThread st = new TaskThread(this.conf, context);

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

  //
  // Constructor
  //

  /**
   * Constructor.
   */
  HadoopCompatibleTaskScheduler() {

    // Create configuration object
    this.conf = createConfiguration();
  }

}
