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

package fr.ens.biologie.genomique.eoulsan.modules.mgmt;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.amazonaws.services.ec2.model.InstanceType;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.actions.ExecJarHadoopAction;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.ContextUtils;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.cloud.AWSElasticMapReduceBuilder;
import fr.ens.biologie.genomique.eoulsan.util.cloud.AWSElasticMapReduceJob;

/**
 * This class launch Eoulsan on Amazon Elastic MapReduce.
 * @since 1.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class AWSElasticMapReduceExecModule extends AbstractModule {

  private static final int COUNTDOWN_START = 30;

  private static final int SECONDS_WAIT_BETWEEN_CHECKS = 30;

  private static final String HADOOP_VERSION_KEY =
      "aws.mapreduce.hadoop.version";
  private static final String INSTANCES_NUMBER_KEY =
      "aws.mapreduce.instances.number";
  private static final String INSTANCE_TYPE_KEY =
      "aws.mapreduce.instances.type";
  private static final String MAPREDUCE_ENDPOINT_KEY = "aws.mapreduce.endpoint";
  private static final String LOG_PATH_KEY = "aws.mapreduce.log.path";
  private static final String WAIT_JOB_KEY = "aws.mapreduce.wait.job";
  private static final String TASK_TRACKER_MAPPER_MAX_TASKS_KEY =
      "aws.mapreduce.task.tracker.mapper.max.tasks";
  private static final String EC2_KEY_NAME_KEY = "aws.ec2.key.name";
  private static final String ENABLE_DEBUGGING_KEY =
      "aws.mapreduce.enable.debugging";

  /** AWS access key. */
  private String awsAccessKey;

  /** AWS secret key. */
  private String awsSecretKey;

  /** Version of hadoop to use with AWS Elastic MapReduce. */
  private String hadoopVersion = "1.0.3";

  /** Number of instance to use with AWS Elastic MapReduce. */
  private int nInstances = -1;

  /** Type of instance to use with AWS Elastic MapReduce. */
  private String instanceType = InstanceType.M1Xlarge.toString();

  /** End point to use with AWS Elastic MapReduce. */
  private String endpoint = "eu-west-1.elasticmapreduce.amazonaws.com";

  /** Log path to use with AWS Elastic MapReduce. */
  private String logPathname = null;

  /** Maximal number of map tasks in a tasktracker. */
  private int taskTrackerMaxMapTasks = 0;

  /** EC2 Key pair name to use. */
  private String ec2KeyName = null;

  /** Enable debugging. */
  private boolean enableDebugging = false;

  /** Wait the end of AWS Elastic MapReduce Job. */
  private boolean waitJob = false;

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final Settings settings = EoulsanRuntime.getSettings();

    this.awsAccessKey = settings.getAWSAccessKey();
    if (this.awsAccessKey == null) {
      throw new EoulsanException("The AWS access key is not set.");
    }

    this.awsSecretKey = settings.getAWSSecretKey();
    if (this.awsSecretKey == null) {
      throw new EoulsanException("The AWS secret key is not set.");
    }

    if (settings.isSetting(HADOOP_VERSION_KEY)) {
      this.hadoopVersion = settings.getSetting(HADOOP_VERSION_KEY).trim();
    }

    if (settings.isSetting(INSTANCES_NUMBER_KEY)) {
      this.nInstances = settings.getIntSetting(INSTANCES_NUMBER_KEY);
    }

    if (settings.isSetting(INSTANCE_TYPE_KEY)) {
      this.instanceType = settings.getSetting(INSTANCE_TYPE_KEY);
    }

    if (settings.isSetting(MAPREDUCE_ENDPOINT_KEY)) {
      this.endpoint = settings.getSetting(MAPREDUCE_ENDPOINT_KEY);
    }

    if (settings.isSetting(LOG_PATH_KEY)) {
      this.logPathname = settings.getSetting(LOG_PATH_KEY);
    }

    if (settings.isSetting(WAIT_JOB_KEY)) {
      this.waitJob = settings.getBooleanSetting(WAIT_JOB_KEY);
    }

    if (this.nInstances == -1) {
      throw new EoulsanException("The number of instance is not set.");
    }

    if (settings.isSetting(TASK_TRACKER_MAPPER_MAX_TASKS_KEY)) {
      this.taskTrackerMaxMapTasks =
          settings.getIntSetting(TASK_TRACKER_MAPPER_MAX_TASKS_KEY);
    }

    if (settings.isSetting(EC2_KEY_NAME_KEY)) {
      this.ec2KeyName = settings.getSetting(EC2_KEY_NAME_KEY);
    }

    if (settings.isSetting(ENABLE_DEBUGGING_KEY)) {
      this.enableDebugging = settings.getBooleanSetting(ENABLE_DEBUGGING_KEY);
    }

  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Environment argument

    // Command arguments
    final List<String> eoulsanArgsList = new ArrayList<>();
    eoulsanArgsList.add(ExecJarHadoopAction.ACTION_NAME);
    eoulsanArgsList.add("-p");
    eoulsanArgsList.add(Long.toString(context.getContextCreationTime()));
    eoulsanArgsList.add("-d");
    eoulsanArgsList.add(context.getJobDescription());
    eoulsanArgsList.add("-e");
    eoulsanArgsList.add("hadoopVersion="
        + this.hadoopVersion + ", " + "nInstances=" + this.nInstances + ", "
        + "instanceType=" + this.instanceType + ", " + "endpoint="
        + this.endpoint + ", " + "logPathname=" + this.logPathname);
    eoulsanArgsList.add(context.getWorkflowFile().getSource());
    eoulsanArgsList.add(context.getDesignFile().getSource());
    eoulsanArgsList.add("hdfs:///test");

    final String[] eoulsanArgs =
        eoulsanArgsList.toArray(new String[0]);

    // AWS builder
    final AWSElasticMapReduceBuilder builder = new AWSElasticMapReduceBuilder();

    // Set Job flow name
    builder.withJobFlowName(context.getJobDescription());

    // Set the credentials
    builder.withAWSAccessKey(this.awsAccessKey)
        .withAWSSecretKey(this.awsSecretKey);

    // Set end point
    builder.withEndpoint(this.endpoint);

    // Set command
    builder.withJarLocation(ContextUtils.getJarPathname(context).getSource())
        .withJarArguments(eoulsanArgs);

    // Set Instances
    builder.withMasterInstanceType(this.instanceType)
        .withSlavesInstanceType(this.instanceType)
        .withInstancesNumber(this.nInstances);

    // Set Hadoop version
    builder.withHadoopVersion(this.hadoopVersion);

    // Set log path
    if (this.logPathname != null) {
      builder.withLogPathname(this.logPathname);
    }

    // Set the maximal number of map task in a tasktracker
    builder.withTaskTrackerMaxMapTasks(this.taskTrackerMaxMapTasks);

    // Set the EC2 key pair key name
    if (this.ec2KeyName != null) {
      builder.withEC2KeyName(this.ec2KeyName);
    }

    // Enable debugging
    builder.withDebugging(this.enableDebugging);

    // Create job
    final AWSElasticMapReduceJob job = builder.create();

    showCountDown(COUNTDOWN_START);

    getLogger().info("Start Amazon Elastic MapReduce job.");

    // Run job
    final String jobFlowId = job.runJob();

    getLogger().info("Ran job flow with id: " + jobFlowId);

    if (this.waitJob) {

      final String jobStatus = job.waitForJob(SECONDS_WAIT_BETWEEN_CHECKS);

      if ("FAILED".equals(jobStatus)) {

        status.setProgressMessage("End of Amazon MapReduce Job "
            + jobFlowId + " with " + jobStatus + " status.");
        return status.createTaskResult(false);

      }

      status.setProgressMessage("End of Amazon Elastic MapReduce Job "
          + jobFlowId + " with " + jobStatus + " status.");

      return status.createTaskResult();
    }

    status.setProgressMessage(
        "Launch of Amazon Elastic MapReduce Job " + jobFlowId + ".");
    return status.createTaskResult();
  }

  @Override
  public String getName() {

    return "_aws_elasticmapreduce_exec";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  private static void showCountDown(final int sec) {

    for (int i = sec; i > 0; i--) {

      if (i < 10 || i % 5 == 0) {
        System.out.println("WARNING: Start Amazon Elastic MapReduce job in "
            + i + " seconds. Press Ctrl-C to cancel execution.");

      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        getLogger().warning("Error while sleeping: " + e.getMessage());
      }

    }

  }

}
