/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
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
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.AWSMapReduceBuilder;
import fr.ens.transcriptome.eoulsan.util.AWSMapReduceJob;

/**
 * This class launch Eoulsan on Amazon MapReduce.
 * @author Laurent Jourdren
 */
@LocalOnly
public class AWSMapReduceExecStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final int COUNTDOWN_START = 30;

  private static final int SECONDS_WAIT_BETWEEN_CHECKS = 30;

  /** Version of hadoop to use with AWS MapReduce. */
  private String hadoopVersion = "0.20";

  /** Number of instance to use with AWS MapReduce. */
  private int nInstances = -1;

  /** Type of instance to use with AWS MapReduce. */
  private String instanceType = InstanceType.M1Xlarge.toString();

  /** End point to use with AWS MapReduce. */
  private String endpoint = "eu-west-1.elasticmapreduce.amazonaws.com";

  /** Log path to use with AWS MapReduce. */
  private String logPathname = "s3n://sgdb-test/awslog";

  /** Wait the end of AWS Job. */
  private boolean waitJob = false;

  @Override
  public void configure(Set<Parameter> stepParameters,
      Set<Parameter> globalParameters) throws EoulsanException {

    for (Parameter param : globalParameters) {

      if ("aws.mapreduce.hadoop.version".equals(param.getName()))
        this.hadoopVersion = param.getStringValue().trim();

      if ("aws.mapreduce.instances.number".equals(param.getName()))
        this.nInstances = param.getIntValue();

      if ("aws.mapreduce.instances.type".equals(param.getName()))
        this.instanceType = param.getStringValue().trim();

      if ("aws.mapreduce.endpoint".equals(param.getName()))
        this.endpoint = param.getStringValue().trim();

      if ("aws.mapreduce.log.path".equals(param.getName()))
        this.logPathname = param.getStringValue().trim();

      if ("aws.mapreduce.wait.job".equals(param.getName()))
        this.waitJob = param.getBooleanValue();

    }

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
    eoulsanArgsList.add("exec");
    eoulsanArgsList.add("-d");
    eoulsanArgsList.add(context.getJobDescription());
    eoulsanArgsList.add("-e");
    eoulsanArgsList.add(sb.toString());
    eoulsanArgsList.add(context.getParameterPathname());
    eoulsanArgsList.add(context.getDesignPathname());
    eoulsanArgsList.add("hdfs:///test");

    final String[] eoulsanArgs = eoulsanArgsList.toArray(new String[0]);

    // AWS builder
    final AWSMapReduceBuilder builder = new AWSMapReduceBuilder();

    // Set Job flow name
    builder.withJobFlowName(context.getJobDescription());

    // Set the credentials
    builder.withAWSAccessKey(settings.getAWSAccessKey()).withAWSsecretKey(
        settings.getAWSSecretKey());

    // Set end point
    builder.withEndpoint(this.endpoint);

    // Set command
    builder.withJarLocation(context.getJarPathname()).withJarArguments(
        eoulsanArgs);

    // Set Instances
    builder.withMasterInstanceType(this.instanceType).withSlavesInstanceType(
        this.instanceType).withInstancesNumber(this.nInstances);

    // Set Hadoop version
    builder.withHadoopVersion(this.hadoopVersion);

    // Set log path
    builder.withLogPathname(this.logPathname);

    // Create job
    final AWSMapReduceJob job = builder.create();

    showCountDown(COUNTDOWN_START);

    LOGGER.info("Start Amazon MapReduce job.");

    // Run job
    final String jobFlowId = job.runJob();

    LOGGER.info("Ran job flow with id: " + jobFlowId);

    if (this.waitJob) {

      job.waitForJob(SECONDS_WAIT_BETWEEN_CHECKS);

      return new StepResult(context, startTime, "End of Amazon MapReduce Job "
          + jobFlowId);
    }

    return new StepResult(context, startTime, "Launch of Amazon MapReduce Job "
        + jobFlowId);
  }

  @Override
  public String getName() {

    return "_aws_mapreduce_exec";
  }

  private static void showCountDown(final int sec) {

    for (int i = sec; i > 0; i--) {

      if (i < 10 || i % 5 == 0) {
        System.out.println("WARNING: Start Amazon MapReduce job in "
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
