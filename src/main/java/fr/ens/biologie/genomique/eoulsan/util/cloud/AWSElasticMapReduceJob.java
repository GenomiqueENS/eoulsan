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

package fr.ens.biologie.genomique.eoulsan.util.cloud;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Objects.requireNonNull;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.BootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsRequest;
import com.amazonaws.services.elasticmapreduce.model.DescribeJobFlowsResult;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowDetail;
import com.amazonaws.services.elasticmapreduce.model.JobFlowExecutionStatusDetail;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.ScriptBootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;
import com.amazonaws.services.elasticmapreduce.util.StepFactory;

/**
 * This class define an AWS Elastic MapReduce job.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class AWSElasticMapReduceJob {

  private static final int MAX_FAIL_COUNT = 5;

  /** Version of hadoop to use with AWS Elastic MapReduce. */
  private String hadoopVersion = "0.20";

  /** Number of instance to use with AWS Elastic MapReduce. */
  private int nInstances = -1;

  /** Type of instance to use with AWS Elastic MapReduce master. */
  private String masterInstanceType;

  /** Type of instance to use with AWS Elastic MapReduce slaves. */
  private String slavesInstanceType = InstanceType.M1Xlarge.toString();

  /** End point to use with AWS Elastic MapReduce. */
  private String endpoint = "eu-west-1.elasticmapreduce.amazonaws.com";

  /** Log path to use with AWS Elastic MapReduce. */
  private String logPathname;

  /** Path to jar file. */
  private String jarLocation;

  /** Path to jar arguments. */
  private String[] jarArguments;

  /** Job Flow name. */
  private String jobFlowName;

  /** AWS Access key. */
  private String AWSAccessKey;

  /** AWS secret key. */
  private String AWSSecretKey;

  /** Maximal number of map tasks in a tasktracker. */
  private int taskTrackerMaxMapTasks;

  /** EC2 Key pair name to use. */
  private String ec2KeyName;

  /** Enable debugging. */
  private boolean enableDebugging;

  private RunJobFlowRequest runFlowRequest;
  private RunJobFlowResult runFlowResult;
  private AmazonElasticMapReduce elasticMapReduceClient;

  //
  // Getters
  //

  /**
   * Get the hadoop version.
   * @return Returns the hadoopVersion
   */
  public String getHadoopVersion() {
    return this.hadoopVersion;
  }

  /**
   * Set the number of instances.
   * @return Returns the nInstances
   */
  public int getInstancesNumber() {
    return this.nInstances;
  }

  /**
   * Get the type of the master instance.
   * @return Returns the instanceType of the master
   */
  public String getMasterInstanceType() {
    return this.masterInstanceType;
  }

  /**
   * Get the type of the slave instances.
   * @return Returns the instanceType
   */
  public String getSlavesInstanceType() {
    return this.slavesInstanceType;
  }

  /**
   * Get the endpoint.
   * @return Returns the endpoint
   */
  public String getEndpoint() {
    return this.endpoint;
  }

  /**
   * Get the log path.
   * @return Returns the logPathname
   */
  public String getLogPathname() {
    return this.logPathname;
  }

  /**
   * Get the jar location.
   * @return Returns the jar location
   */
  public String getJarLocation() {
    return this.jarLocation;
  }

  /**
   * Get the jar arguments.
   * @return Returns the jar arguments
   */
  public String[] getJarArguments() {
    return this.jarArguments == null ? null : this.jarArguments.clone();
  }

  /**
   * Get the job flow name.
   * @return Returns the job flow name
   */
  public String getJobFlowName() {
    return this.jobFlowName;
  }

  /**
   * Get the AWS access key.
   * @return Returns AWS access key
   */
  public String getAWSAccessKey() {
    return this.AWSAccessKey;
  }

  /**
   * Get the AWS secret key.
   * @return Returns AWS secret key
   */
  public String getAWSSecretKey() {
    return this.AWSSecretKey;
  }

  /**
   * Get the number of maximal mapper tasks to use in a task tracker.
   * @return the number of maximal mapper tasks to use in a task tracker
   */
  public int getTaskTrackerMaxMapTasks() {
    return this.taskTrackerMaxMapTasks;
  }

  /**
   * Return the EC2 Key pair name to use.
   * @return EC2 Key pair name to use
   */
  public String getEC2KeyName() {
    return this.ec2KeyName;
  }

  /**
   * Test if debugging is enabled.
   * @return true if debugging is enabled
   */
  public boolean isDebuggingEnabled() {
    return this.enableDebugging;
  }

  //
  // Setters
  //

  /**
   * Set the hadoop version.
   * @param hadoopVersion The hadoop version to set
   */
  void setHadoopVersion(final String hadoopVersion) {
    this.hadoopVersion = hadoopVersion;
  }

  /**
   * Set the number of instance to use.
   * @param nInstances The number of instances to use
   */
  void setInstancesNumber(final int nInstances) {
    this.nInstances = nInstances;
  }

  /**
   * Set the type of master instance.
   * @param instanceType The instanceType to set for master
   */
  void setMasterInstanceType(final String instanceType) {
    this.masterInstanceType = instanceType;
  }

  /**
   * Set the type of slaves instances.
   * @param instanceType The instanceType to set
   */
  void setSlavesInstanceType(final String instanceType) {
    this.slavesInstanceType = instanceType;
  }

  /**
   * Set the endpoint.
   * @param endpoint The endpoint to set
   */
  void setEndpoint(final String endpoint) {
    this.endpoint = endpoint;
  }

  /**
   * Set the log path.
   * @param logPathname The logPathname to set
   */
  void setLogPathname(final String logPathname) {
    this.logPathname = logPathname;
  }

  /**
   * Set the jar location.
   * @param jarLocation The jar location to set
   */
  void setJarLocation(final String jarLocation) {
    this.jarLocation = jarLocation;
  }

  /**
   * Set the jar location.
   * @param jarArguments The jar arguments
   */
  void setJarArguments(final String[] jarArguments) {
    this.jarArguments = jarArguments;
  }

  /**
   * Set the job flow name.
   * @param jobFlowName The job flow name
   */
  void setJobFlowName(final String jobFlowName) {
    this.jobFlowName = jobFlowName;
  }

  /**
   * Set the AWS access key.
   * @param AWSAccessKey set AWS access key
   */
  void setAWSAccessKey(final String AWSAccessKey) {
    this.AWSAccessKey = AWSAccessKey;
  }

  /**
   * Set the AWS access key.
   * @param AWSSecretKey set AWS secret key
   */
  void setAWSSecretKey(final String AWSSecretKey) {
    this.AWSSecretKey = AWSSecretKey;
  }

  /**
   * Set the number of maximal mapper tasks to use in a task tracker.
   * @param taskTrackerMaxMapTasks the number of maximal mapper tasks to use in
   *          a task tracker
   */
  void setTaskTrackerMaxMapTasks(final int taskTrackerMaxMapTasks) {
    this.taskTrackerMaxMapTasks = taskTrackerMaxMapTasks;
  }

  /**
   * Set the EC2 Key pair name to use.
   * @param ec2KeyName EC2 Key pair name to use
   */
  void setEC2KeyName(final String ec2KeyName) {
    this.ec2KeyName = ec2KeyName;
  }

  /**
   * Set if debugging must be enabled.
   * @param enableDebugging true if debugging is enabled
   */
  public void setDebugging(final boolean enableDebugging) {
    this.enableDebugging = enableDebugging;
  }

  //
  // Other methods
  //

  void init() {

    requireNonNull(this.AWSAccessKey);
    requireNonNull(this.AWSAccessKey);
    requireNonNull(this.jarLocation);
    requireNonNull(this.jarArguments);
    requireNonNull(this.slavesInstanceType);
    requireNonNull(this.hadoopVersion);
    requireNonNull(this.jobFlowName);

    if (this.nInstances < 1) {
      throw new IllegalArgumentException(
          "the number of instance is lower than 1");
    }

    if (this.masterInstanceType == null) {
      this.masterInstanceType = this.slavesInstanceType;
    }

    // Set the hadoop jar step
    final HadoopJarStepConfig hadoopJarStep = new HadoopJarStepConfig()
        .withJar(this.jarLocation.trim()).withArgs(this.jarArguments);

    // Set step config
    final StepConfig stepConfig = new StepConfig()
        .withName(this.jobFlowName + "-step").withHadoopJarStep(hadoopJarStep)
        .withActionOnFailure("TERMINATE_JOB_FLOW");

    // Set the instance
    final JobFlowInstancesConfig instances =
        new JobFlowInstancesConfig().withInstanceCount(this.nInstances)
            .withMasterInstanceType(this.masterInstanceType)
            .withSlaveInstanceType(this.slavesInstanceType)
            .withHadoopVersion(this.hadoopVersion);

    // Configure hadoop
    final ScriptBootstrapActionConfig scriptBootstrapAction =
        new ScriptBootstrapActionConfig()
            .withPath(
                "s3n://eu-west-1.elasticmapreduce/bootstrap-actions/configure-hadoop")
            .withArgs("--site-key-value",
                "mapreduce.tasktracker.map.tasks.maximum="
                    + this.taskTrackerMaxMapTasks);

    final BootstrapActionConfig bootstrapActions =
        new BootstrapActionConfig().withName("Configure hadoop")
            .withScriptBootstrapAction(scriptBootstrapAction);

    // Enable debugging
    StepFactory stepFactory = new StepFactory();
    StepConfig enableDebugging = new StepConfig().withName("Enable Debugging")
        .withActionOnFailure("TERMINATE_JOB_FLOW")
        .withHadoopJarStep(stepFactory.newEnableDebuggingStep());

    // Run flow
    this.runFlowRequest = new RunJobFlowRequest().withName(this.jobFlowName);

    // Enable or not debugging
    if (this.enableDebugging) {
      this.runFlowRequest.withInstances(instances).withSteps(enableDebugging,
          stepConfig);
    } else {
      this.runFlowRequest.withInstances(instances).withSteps(stepConfig);
    }

    // Limit the number of task in a task tracker
    if (this.taskTrackerMaxMapTasks > 0) {
      this.runFlowRequest.withBootstrapActions(bootstrapActions);
    }

    if (this.logPathname != null && !"".equals(this.logPathname)) {
      this.runFlowRequest.withLogUri(this.logPathname);
    }

    // Set EC2 Key name
    if (this.ec2KeyName != null) {
      this.runFlowRequest.getInstances().setEc2KeyName(this.ec2KeyName);
    }
  }

  /**
   * Run the job.
   * @return a the JobFlowId of the job
   */
  public String runJob() {

    // Get the credentials
    final AWSCredentials credentials =
        new BasicAWSCredentials(this.AWSAccessKey, this.AWSSecretKey);

    // Create the Amazon Elastic MapReduce object
    this.elasticMapReduceClient = new AmazonElasticMapReduceClient(credentials);

    // Set the end point
    this.elasticMapReduceClient.setEndpoint(this.endpoint);

    this.runFlowResult =
        this.elasticMapReduceClient.runJobFlow(this.runFlowRequest);

    return this.runFlowResult.getJobFlowId();
  }

  /**
   * Wait the end of the job
   * @param secondBetweenChecking number of seconds to wait between 2 checks
   * @return the final state of the job
   */
  public String waitForJob(final int secondBetweenChecking) {

    if (this.runFlowResult == null) {
      return null;
    }

    final DescribeJobFlowsRequest describeJobFlowsRequest =
        new DescribeJobFlowsRequest()
            .withJobFlowIds(this.runFlowResult.getJobFlowId());

    String state = null;
    String lastState = null;
    int failCount = 0;

    try {

      do {

        Thread.sleep(secondBetweenChecking * 1000);

        try {
          final DescribeJobFlowsResult jobFlowsResult =
              this.elasticMapReduceClient
                  .describeJobFlows(describeJobFlowsRequest);
          final JobFlowDetail detail = jobFlowsResult.getJobFlows().get(0);
          final JobFlowExecutionStatusDetail executionStatusDetail =
              detail.getExecutionStatusDetail();
          failCount = 0;

          state = executionStatusDetail.getState();
        } catch (AmazonClientException ace) {

          failCount++;
          getLogger().warning("Amazon client exception: " + ace.getMessage());

          if (failCount >= MAX_FAIL_COUNT) {
            throw ace;
          }

        }

        if (lastState == null || !lastState.equals(state)) {

          getLogger().info("State of the job "
              + this.runFlowResult.getJobFlowId() + ": " + state);
          lastState = state;
        }

      } while (state != null
          && !state.equals("COMPLETED") && !state.equals("FAILED")
          && !state.equals("TERMINATED"));

      return state;

    } catch (InterruptedException e) {
      getLogger().warning(
          "Error while waiting AWS Elastic MapReduce Job: " + e.getMessage());
    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Package constructor
   */
  AWSElasticMapReduceJob() {
  }
}
