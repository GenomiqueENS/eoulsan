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

package fr.ens.transcriptome.eoulsan.actions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.util.AWSMapReduceBuilder;
import fr.ens.transcriptome.eoulsan.util.AWSMapReduceJob;

public class BenchmarkAction extends AbstractAction {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String END_POINT =
      "eu-west-1.elasticmapreduce.amazonaws.com";
  private static final String LOG_PATH = "s3://sgdb-test/awslog";
  private static final String HADOOP_VERSION = "0.20";
  private static final String JAR_LOCATION =
      "s3://sgdb-test/benchmarks/" + "eoulsan-0.6-BENCHMARK.jar ";

  private static final String BASE_DIR = "s3n://sgdb-test/benchmarks/";

  private enum instancesEnum {
    // MEM_INSTANCE(InstanceType.M1Xlarge.toString());
    // CPU_INSTANCE(InstanceType.C1Xlarge.toString());
    MEDIUM_INSTANCE(InstanceType.M1Large.toString());

    final String name;

    public String toString() {
      return name;
    }

    private instancesEnum(String name) {
      this.name = name;
    }

  }

  private static final int COUNTDOWN_START = 30;
  private static final int SECONDS_WAIT_BETWEEN_CHECKS = 60;

  @Override
  public String getName() {

    return "benchmark";
  }

  @Override
  public String getDescription() {

    return "Execute " + Globals.APP_NAME + " benchmarks.";
  }

  @Override
  public void action(String[] arguments) {

    try {
      // Init Log
      initLog();

      // Load settings
      final Settings settings = new Settings();

      // final String instanceType = instancesEnum.MEM_INSTANCE.toString();
      // final int instanceCount = 3;
      //      
      // final String genome = "candida";
      // //final String genome = "mouse";
      // final String dir = BASE_DIR + genome + "/";
      // // final String designFile = dir + "design-one.txt";
      // final String designFile = dir + "design-two.txt";
      // final String mapper = "bwa";
      // final String paramFile =
      // dir + "param-" + genome + "-aws-" + mapper + ".xml";
      // final String desc =
      // genome + " data with " + mapper + " (validation test)";
      //      
      // exec(settings, instanceType, instanceCount, paramFile, designFile,
      // desc);

      // Test sur la souris en 1er, 3 mappers, 3 instances

      final String[] genomes = {"mouse"};
      final String[] mappers = {"bowtie"};
      // final String[] mappers = {"bwa", "bowtie", "soap"};
      // final String[] mappers = {"bowtie", "soap"};

      // int[] instanceCounts = {20,18,16,14,12,10};
      // String designFile = "design-x4.txt";

      int[] instanceCounts = new int[] {20};
      String designFile = "design-x2.txt";

      // loop for genome
      for (String genome : genomes) {

        final String dir = BASE_DIR + genome + "/";

        final String designPath = dir + designFile;

        // loop for instances
        for (instancesEnum instance : instancesEnum.values()) {

          // loop for mappers
          for (String mapper : mappers) {

            for (int instanceCount : instanceCounts) {

              final String paramFile =
                  dir + "param-" + genome + "-aws-" + mapper + ".xml";
              final String desc =
                  genome
                      + " data, " + designFile + ", " + mapper + " on "
                      + instanceCount + " " + instance.toString();

              // System.out.println(paramFile);
              // System.out.println(desc);

              exec(settings, instance.toString(), instanceCount, paramFile,
                  designPath, desc);

            } // End loop instanceCounts
          } // End loop mapper
        } // End loop instance type
      } // End loop genome

      instanceCounts = new int[] {20, 18, 16, 14, 12, 10};
      designFile = "design-x4.txt";

      // loop for genome
      for (String genome : genomes) {

        final String dir = BASE_DIR + genome + "/";

        final String designPath = dir + designFile;

        // loop for instances
        for (instancesEnum instance : instancesEnum.values()) {

          // loop for mappers
          for (String mapper : mappers) {

            for (int instanceCount : instanceCounts) {

              final String paramFile =
                  dir + "param-" + genome + "-aws-" + mapper + ".xml";
              final String desc =
                  genome
                      + " data, " + designFile + ", " + mapper + " on "
                      + instanceCount + " " + instance.toString();

              // System.out.println(paramFile);
              // System.out.println(desc);

              exec(settings, instance.toString(), instanceCount, paramFile,
                  designPath, desc);

              // Sleep 5 minutes between 2 launchs

            } // End loop instanceCounts
          } // End loop mapper
        } // End loop instance type
      } // End loop genome

      // String genome = "mouse";
      // String mapper = "bowtie";
      // String instanceName = InstanceType.M1Large.toString();
      // final String dir = BASE_DIR + genome + "/";
      // final String designFile = dir + "design.txt";
      // int instanceCount = 7;
      //
      // final String paramFile =
      // dir + "param-" + genome + "-aws-" + mapper + ".xml";
      // final String desc =
      // genome
      // + " data with " + mapper + " on " + instanceCount + " "
      // + instanceName;
      //
      // exec(settings, instanceName, instanceCount, paramFile, designFile,
      // desc);

      LOGGER.info("--- End at " + new Date() + " ---");
    } catch (IOException e) {
      LOGGER.severe("Error: " + e.getMessage());
    } catch (EoulsanException e) {
      LOGGER.severe("Error: " + e.getMessage());
    }

  }

  /**
   * Initialize log.
   * @throws SecurityException if error occurs
   * @throws IOException if error occurs
   */
  private static void initLog() throws SecurityException, IOException {

    final Handler fh =
        new FileHandler(System.getProperty("user.home") + "/aws.log", true);
    fh.setFormatter(Globals.LOG_FORMATTER);
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.setUseParentHandlers(false);

    LOGGER.addHandler(fh);
    LOGGER.addHandler(new ConsoleHandler());
    LOGGER.info("--- Start at " + new Date() + " ---");

  }

  private static void exec(final Settings settings, final String instanceType,
      final int nInstances, final String paramPath, String designPath,
      final String jobDescription) {

    LOGGER.info("Prepare Amazon MapReduce job");

    final long startTime = System.currentTimeMillis();

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
    eoulsanArgsList.add(ExecJarHadoopAction.ACTION_NAME);
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
    LOGGER.info("Job description: " + jobDescription);

    // Set the credentials
    builder.withAWSAccessKey(settings.getAWSAccessKey()).withAWSSecretKey(
        settings.getAWSSecretKey());
    LOGGER.info("AWS access key: " + settings.getAWSAccessKey());
    LOGGER.info("AWS secret key: " + settings.getAWSSecretKey());

    // Set end point
    builder.withEndpoint(END_POINT);
    LOGGER.info("End point: " + END_POINT);

    // Set command
    builder.withJarLocation(JAR_LOCATION).withJarArguments(eoulsanArgs);
    LOGGER.info("Jar Location: " + JAR_LOCATION);
    LOGGER.info("Jar arguments: " + Arrays.toString(eoulsanArgs));

    // Set Instances
    builder.withMasterInstanceType(instanceType).withSlavesInstanceType(
        instanceType).withInstancesNumber(nInstances);
    LOGGER.info("Instance type: " + instanceType);
    LOGGER.info("Instance number: " + nInstances);

    // Set Hadoop version
    builder.withHadoopVersion(HADOOP_VERSION);
    LOGGER.info("Hadoop version: " + HADOOP_VERSION);

    // Set log path
    builder.withLogPathname(LOG_PATH);
    LOGGER.info("Log path: " + LOG_PATH);

    // Create job
    final AWSMapReduceJob job = builder.create();

    showCountDown(COUNTDOWN_START);

    LOGGER.info("Start Amazon MapReduce job");

    // Run job
    final String jobFlowId = job.runJob();

    LOGGER.info("Ran job flow with id: " + jobFlowId);

    final String jobStatus = job.waitForJob(SECONDS_WAIT_BETWEEN_CHECKS);
    final long endTime = System.currentTimeMillis();

    LOGGER.info("End of Amazon MapReduce Job "
        + jobFlowId + " with status " + jobStatus + " (duration: "
        + (endTime - startTime) + " ms).");

    // Wait 5 minutes to prevent exceed of ec2 instance quota
    try {
      Thread.sleep(5 * 60 * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

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

  //
  // Main method()
  //

  /**
   * Main method.
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    new BenchmarkAction().action(args);
  }

}
