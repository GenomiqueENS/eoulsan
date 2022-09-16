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

package fr.ens.biologie.genomique.eoulsan.core.schedulers;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_DATA_EXTENSION;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_DONE_EXTENSION;
import static fr.ens.biologie.genomique.eoulsan.Globals.TASK_RESULT_EXTENSION;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toTimeHumanReadable;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskResultImpl;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskRunner;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskSerializationUtils;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopJobEmergencyStopTask;

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

    private static final String SUBMIT_FILE_NAME = "submitfile";
    private final TaskContextImpl context;
    private final Configuration conf;
    private final DataFile taskDir;
    private final String taskPrefix;
    private String jobId;
    private Job hadoopJob;

    private Job createHadoopJob(final Configuration conf,
        final DataFile submitFile, final int requiredMemory,
        final String jobDescription) throws IOException {

      final Configuration jobConf = new Configuration(conf);

      // Set one task per map
      jobConf.set("mapreduce.input.lineinputformat.linespermap", "" + 1);

      if (requiredMemory > 0) {

        // Set the memory required by the reads mapper
        jobConf.set("mapreduce.map.memory.mb", "" + requiredMemory);

        int jvmMemory = requiredMemory - 128;
        if (jvmMemory <= 0) {
          jvmMemory = requiredMemory;
        }

        // Set the memory required by JVM
        jobConf.set("mapreduce.map.java.opts", "-Xmx" + jvmMemory + "M");
      }

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
     * Create the submit file for the Hadoop job.
     * @param taskContextFile the task context file
     * @return the path to the submit file
     * @throws IOException if an error occurs while creating the submit file
     */
    private DataFile createSubmitFile(final DataFile taskContextFile)
        throws IOException {

      final DataFile submitFile =
          new DataFile(taskContextFile.getParent(), SUBMIT_FILE_NAME);

      final Writer writer = new OutputStreamWriter(submitFile.create());
      writer.write(taskContextFile.getSource());
      writer.close();

      return submitFile;
    }

    /**
     * Load the result of the step
     * @return a TaskResult object
     * @throws EoulsanException if the done task is not found
     * @throws IOException if an error occurs while reading the result file
     */
    private TaskResultImpl loadResult() throws EoulsanException, IOException {

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
      this.context.deserializeOutputData(
          new DataFile(this.taskDir, this.taskPrefix + TASK_DATA_EXTENSION));

      return TaskResultImpl.deserialize(taskResultFile);
    }

    @Override
    public void run() {

      TaskResultImpl result = null;

      try {

        // Create job directory
        this.taskDir.mkdir();

        final DataFile taskContextFile = new DataFile(this.taskDir,
            this.taskPrefix + Globals.TASK_CONTEXT_EXTENSION);

        // Serialize the context object
        this.context.serialize(taskContextFile);

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          return;
        }

        // Set task in running state
        beforeExecuteTask(this.context);

        // Create submit file
        final DataFile sumbitFile = createSubmitFile(taskContextFile);

        // Submit Job
        this.hadoopJob = createHadoopJob(this.conf, sumbitFile,
            this.context.getCurrentStep().getRequiredMemory(),
            "Eoulsan Step "
                + this.context.getCurrentStep().getId() + " ("
                + this.context.getCurrentStep().getModuleName() + ") Task #"
                + this.context.getId() + " (" + this.context.getContextName()
                + ")");

        // Submit the Hadoop job
        this.hadoopJob.submit();

        // Add the Hadoop job to the list of job to kill if workflow fails
        HadoopJobEmergencyStopTask
            .addHadoopJobEmergencyStopTask(this.hadoopJob);

        // Submit the job to the Hadoop scheduler, and wait the end of the job
        // in non verbose mode
        this.hadoopJob.waitForCompletion(false);

        // Remove the Hadoop job to the list of job to kill if workflow fails
        HadoopJobEmergencyStopTask
            .removeHadoopJobEmergencyStopTask(this.hadoopJob);

        if (!this.hadoopJob.isSuccessful()) {

          // Try to load the task result
          try {
            // Load result
            result = loadResult();
          } catch (EoulsanException | IOException e) {

            throw new EoulsanException(
                "Error while running Hadoop job for Eoulsan task #"
                    + this.context.getId() + "(" + this.context.getContextName()
                    + ")");
          }
        }

        // Load result
        result = loadResult();

        // Remove task files
        this.taskDir.delete(true);

        // Do nothing if scheduler is stopped
        if (isStopped()) {
          return;
        }

        // Send tokens
        TaskRunner.sendTokens(this.context, result);

      } catch (IOException | EoulsanException | InterruptedException
          | ClassNotFoundException e) {

        result = TaskRunner.createStepResult(this.context, e);
      }

      // Do nothing if scheduler is stopped
      if (isStopped()) {
        return;
      }

      // Set task in done state
      afterExecuteTask(this.context, result);

      // Remove the thread from the queue
      queue.remove(this);
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
    TaskThread(final Configuration conf, final TaskContextImpl context) {

      requireNonNull(conf, "conf argument cannot be null");
      requireNonNull(context, "context argument cannot be null");

      final DataFile hadoopWorkDir = context.getHadoopWorkingPathname();

      this.conf = conf;
      this.context = context;
      this.taskDir = new DataFile(hadoopWorkDir,
          "eoulsan-hadoop-compatible-task-" + this.context.getId());
      this.taskPrefix = context.getTaskFilePrefix();
    }
  }

  //
  // Hadoop Mapper class
  //

  public static final class HadoopCompatibleMapper
      extends Mapper<LongWritable, Text, NullWritable, NullWritable> {

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

      getLogger().info("Start of map()");
      getLogger().info("Task context file: " + value);

      try {

        // Execute the task
        final TaskResultImpl result =
            TaskSerializationUtils.execute(new DataFile(value.toString()));

        // Log task result informations
        if (result != null) {

          getLogger().info(
              "Task result: " + (result.isSuccess() ? "SUCCESS" : "FAIL"));
          getLogger().info(
              "Task Duration: " + toTimeHumanReadable(result.getDuration()));

          if (!result.isSuccess()) {

            getLogger()
                .severe("Task error message: " + result.getErrorMessage());

            if (result.getException() != null) {
              result.getException().printStackTrace();
            }
          }
        }

      } catch (EoulsanException e) {
        throw new IOException(e);
      }

      getLogger().info("End of map()");
    }

  }

  //
  // Task scheduler methods
  //

  @Override
  public void submit(final Step step, final TaskContextImpl context) {

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

    // Call to the super method
    super.stop();

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
