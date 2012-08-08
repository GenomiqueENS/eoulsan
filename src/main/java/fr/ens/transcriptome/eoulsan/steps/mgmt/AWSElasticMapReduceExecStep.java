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

package fr.ens.transcriptome.eoulsan.steps.mgmt;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.actions.ExecJarHadoopAction;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.cloud.AWSElasticMapReduceBuilder;
import fr.ens.transcriptome.eoulsan.util.cloud.AWSElasticMapReduceJob;

/**
 * This class launch Eoulsan on Amazon Elastic MapReduce.
 * @since 1.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class AWSElasticMapReduceExecStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

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

  /** Version of hadoop to use with AWS Elastic MapReduce. */
  private String hadoopVersion = "0.20";

  /** Number of instance to use with AWS Elastic MapReduce. */
  private int nInstances = -1;

  /** Type of instance to use with AWS Elastic MapReduce. */
  private String instanceType = InstanceType.M1Xlarge.toString();

  /** End point to use with AWS Elastic MapReduce. */
  private String endpoint = "eu-west-1.elasticmapreduce.amazonaws.com";

  /** Log path to use with AWS Elastic MapReduce. */
  private String logPathname = null;

  /** Wait the end of AWS Elastic MapReduce Job. */
  private boolean waitJob = false;

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {

    final Settings settings = EoulsanRuntime.getSettings();

    if (settings.isSetting(HADOOP_VERSION_KEY))
      this.hadoopVersion = settings.getSetting(HADOOP_VERSION_KEY).trim();

    if (settings.isSetting(INSTANCES_NUMBER_KEY))
      this.nInstances = settings.getIntSetting(INSTANCES_NUMBER_KEY);

    if (settings.isSetting(INSTANCE_TYPE_KEY))
      this.instanceType = settings.getSetting(INSTANCE_TYPE_KEY);

    if (settings.isSetting(MAPREDUCE_ENDPOINT_KEY))
      this.endpoint = settings.getSetting(MAPREDUCE_ENDPOINT_KEY);

    if (settings.isSetting(LOG_PATH_KEY))
      this.logPathname = settings.getSetting(LOG_PATH_KEY);

    if (settings.isSetting(WAIT_JOB_KEY))
      this.waitJob = settings.getBooleanSetting(WAIT_JOB_KEY);

    if (this.nInstances == -1)
      throw new EoulsanException("The number of instance is not set.");

  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    final long startTime = System.currentTimeMillis();

    final Settings settings = EoulsanRuntime.getSettings();

    // Envrionment argument
    final StringBuilder sb = new StringBuilder();
    sb.append("hadoopVersion=");
    sb.append(this.hadoopVersion);
    sb.append(", ");

    sb.append("nInstances=");
    sb.append(this.nInstances);
    sb.append(", ");

    sb.append("instanceType=");
    sb.append(this.instanceType);
    sb.append(", ");

    sb.append("endpoint=");
    sb.append(this.endpoint);
    sb.append(", ");

    sb.append("logPathname=");
    sb.append(this.logPathname);

    // Command arguments
    final List<String> eoulsanArgsList = Lists.newArrayList();
    eoulsanArgsList.add(ExecJarHadoopAction.ACTION_NAME);
    eoulsanArgsList.add("-p");
    eoulsanArgsList.add(Long.toString(context.getContextCreationTime()));
    eoulsanArgsList.add("-d");
    eoulsanArgsList.add(context.getJobDescription());
    eoulsanArgsList.add("-e");
    eoulsanArgsList.add(sb.toString());
    eoulsanArgsList.add(context.getParameterPathname());
    eoulsanArgsList.add(context.getDesignPathname());
    eoulsanArgsList.add("hdfs:///test");

    final String[] eoulsanArgs = eoulsanArgsList.toArray(new String[0]);

    // AWS builder
    final AWSElasticMapReduceBuilder builder = new AWSElasticMapReduceBuilder();

    // Set Job flow name
    builder.withJobFlowName(context.getJobDescription());

    // Set the credentials
    builder.withAWSAccessKey(settings.getAWSAccessKey()).withAWSSecretKey(
        settings.getAWSSecretKey());

    // Set end point
    builder.withEndpoint(this.endpoint);

    // Set command
    builder.withJarLocation(context.getJarPathname()).withJarArguments(
        eoulsanArgs);

    // Set Instances
    builder.withMasterInstanceType(this.instanceType)
        .withSlavesInstanceType(this.instanceType)
        .withInstancesNumber(this.nInstances);

    // Set Hadoop version
    builder.withHadoopVersion(this.hadoopVersion);

    // Set log path
    if (this.logPathname != null)
      builder.withLogPathname(this.logPathname);

    // Create job
    final AWSElasticMapReduceJob job = builder.create();

    showCountDown(COUNTDOWN_START);

    LOGGER.info("Start Amazon Elastic MapReduce job.");

    // Run job
    final String jobFlowId = job.runJob();

    LOGGER.info("Ran job flow with id: " + jobFlowId);

    if (this.waitJob) {

      final String jobStatus = job.waitForJob(SECONDS_WAIT_BETWEEN_CHECKS);

      if ("FAILED".equals(jobStatus)) {

        return new StepResult(context, false, startTime,
            "End of Amazon MapReduce Job "
                + jobFlowId + " with " + jobStatus + " status.");
      }

      return new StepResult(context, startTime,
          "End of Amazon Elastic MapReduce Job "
              + jobFlowId + " with " + jobStatus + " status.");
    }

    return new StepResult(context, startTime,
        "Launch of Amazon Elastic MapReduce Job " + jobFlowId + ".");
  }

  @Override
  public String getName() {

    return "_aws_elasticmapreduce_exec";
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
        LOGGER.warning("Error while sleeping: " + e.getMessage());
      }

    }

  }

}
