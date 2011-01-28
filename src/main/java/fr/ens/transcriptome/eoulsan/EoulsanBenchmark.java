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

package fr.ens.transcriptome.eoulsan;

import java.util.List;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.AWSMapReduceBuilder;
import fr.ens.transcriptome.eoulsan.util.AWSMapReduceJob;

public class EoulsanBenchmark {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String END_POINT =
      "eu-west-1.elasticmapreduce.amazonaws.com";
  private static final String LOG_PATH = "s3n://sgdb-test/awslog";
  private static final String HADOOP_VERSION = "0.20";
  private static final String JAR_LOCATION =
      "s3://sgdb-test/eoulsan-0.6-BENCHMARK.jar ";

  private static final String MEM_INSTANCE = InstanceType.M1Xlarge.toString();
  private static final String CPU_INSTANCE = InstanceType.C1Xlarge.toString();

  private static final String BASE_DIR = "s3n://sgdb-test/";

  private static final int COUNTDOWN_START = 30;
  private static final int SECONDS_WAIT_BETWEEN_CHECKS = 30;

  public static void main(String[] args) {

    final String instanceType = MEM_INSTANCE;
    final int instanceCount = 10;

    final String[] genomes = {"candida", "trichoderma", "mouse"};
    final String[] mappers = {"soap", "bwa", "bowtie"};

    for (String genome : genomes) {

      final String dir = BASE_DIR + genome + "/";
      final String designFile = dir + "design.txt";

      for (String mapper : mappers) {

        final String paramFile = dir + "param-" + mapper + ".txt";
        final String desc = genome + " data with " + mapper;

        exec(instanceType, instanceCount, paramFile, designFile, desc);
      }

    }

  }

  private static void exec(final String instanceType, final int nInstances,
      final String paramPath, String designPath, final String jobDescription) {

    if (true) {
      System.out.println("=== halt !!! ===");
      System.exit(0);
    }

    final long startTime = System.currentTimeMillis();

    final Settings settings = EoulsanRuntime.getSettings();

    // Envrionment argument
    final StringBuilder sb = new StringBuilder();
    sb.append("hadoopVersion=");
    sb.append(HADOOP_VERSION);
    sb.append(", ");

    sb.append("nInstances=");
    sb.append(nInstances);
    sb.append(", ");

    sb.append("instanceType=");
    sb.append(instanceType);
    sb.append(", ");

    sb.append("endpoint=");
    sb.append(END_POINT);
    sb.append(", ");

    sb.append("logPathname=");
    sb.append(LOG_PATH);

    // Command arguments
    final List<String> eoulsanArgsList = Lists.newArrayList();
    eoulsanArgsList.add("exec");
    eoulsanArgsList.add("-d");
    eoulsanArgsList.add(jobDescription);
    eoulsanArgsList.add("-e");
    eoulsanArgsList.add(sb.toString());
    eoulsanArgsList.add(paramPath);
    eoulsanArgsList.add(designPath);
    eoulsanArgsList.add("hdfs:///test");

    final String[] eoulsanArgs = eoulsanArgsList.toArray(new String[0]);

    // AWS builder
    final AWSMapReduceBuilder builder = new AWSMapReduceBuilder();

    // Set Job flow name
    builder.withJobFlowName(jobDescription);

    // Set the credentials
    builder.withAWSAccessKey(settings.getAWSAccessKey()).withAWSsecretKey(
        settings.getAWSSecretKey());

    // Set end point
    builder.withEndpoint(END_POINT);

    // Set command
    builder.withJarLocation(JAR_LOCATION).withJarArguments(eoulsanArgs);

    // Set Instances
    builder.withMasterInstanceType(instanceType).withSlavesInstanceType(
        instanceType).withInstancesNumber(nInstances);

    // Set Hadoop version
    builder.withHadoopVersion(HADOOP_VERSION);

    // Set log path
    builder.withLogPathname(LOG_PATH);

    // Create job
    final AWSMapReduceJob job = builder.create();

    showCountDown(COUNTDOWN_START);

    LOGGER.info("Start Amazon MapReduce job.");

//    // Run job
//    final String jobFlowId = job.runJob();
//
//    LOGGER.info("Ran job flow with id: " + jobFlowId);
//
//    job.waitForJob(SECONDS_WAIT_BETWEEN_CHECKS);
//    final long endTime = System.currentTimeMillis();
//
//    LOGGER.info("End of Amazon MapReduce Job "
//        + jobFlowId + " (duration: " + (endTime - startTime) + " ms");

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
