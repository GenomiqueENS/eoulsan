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

package fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop;

import java.io.IOException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.BootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.HadoopJarStepConfig;
import com.amazonaws.services.elasticmapreduce.model.JobFlowInstancesConfig;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowRequest;
import com.amazonaws.services.elasticmapreduce.model.RunJobFlowResult;
import com.amazonaws.services.elasticmapreduce.model.ScriptBootstrapActionConfig;
import com.amazonaws.services.elasticmapreduce.model.StepConfig;

public class AmazonMapReduce {

  public static void main(String[] args) throws IOException,
      InterruptedException {

    // Regions endpoints
    // US-East (Northern Virginia) us-east-1.elasticmapreduce.amazonaws.com
    // US-West (Northern California) us-west-1.elasticmapreduce.amazonaws.com
    // EU (Ireland) eu-west-1.elasticmapreduce.amazonaws.com
    // Asia Pacific (Singapore) NA

    final String hadoopVersion = "0.20";
    final int nInstances = 2;
    final String instanceType = InstanceType.M1Xlarge.toString();
    final String endpoint = "eu-west-1.elasticmapreduce.amazonaws.com";
    final String eoulsanJarPathname = "s3://sgdb-test/eoulsan-0.5-SNAPSHOT.jar";
    final String logPathname = "s3n://sgdb-test/awslog";
    final String[] eoulsanArgs =
        new String[] {"exec", "s3n://sgdb-test/small/param.xml",
            "s3n://sgdb-test/small/design.txt", "hdfs:///test"};

    System.out.println(instanceType);

    // Get the credentials
    AWSCredentials credentials =
        new PropertiesCredentials(AmazonMapReduce.class
            .getResourceAsStream("AwsCredentials.properties"));

    // Create the Amazon MapReduce object
    AmazonElasticMapReduce mapReduceClient =
        new AmazonElasticMapReduceClient(credentials);
    mapReduceClient.setEndpoint(endpoint);

    // show(mapReduceClient, "j-16TJZ9ZJQ2CLA");
    //  
    // if (true)
    // return;

    // Set the hadoop jar step
    HadoopJarStepConfig hadoopJarStep =
        new HadoopJarStepConfig().withJar(eoulsanJarPathname)
        // .withMainClass("org.familysearch.www.solrload.hadoop.WordCount")
            .withArgs(eoulsanArgs);

    // Set step config
    StepConfig stepConfig =
        new StepConfig().withName("eoulsan-step").withHadoopJarStep(
            hadoopJarStep).withActionOnFailure("TERMINATE_JOB_FLOW");

    // Set the instance
    JobFlowInstancesConfig instances =
        new JobFlowInstancesConfig().withInstanceCount(nInstances)
            .withMasterInstanceType(instanceType).withSlaveInstanceType(
                instanceType).withHadoopVersion(hadoopVersion);

    // Configure hadoop
    ScriptBootstrapActionConfig scriptBootstrapAction =
        new ScriptBootstrapActionConfig()
            .withPath(
                "s3n://eu-west-1.elasticmapreduce/bootstrap-actions/configure-hadoop")
            .withArgs("--site-key-value",
                "mapred.tasktracker.map.tasks.maximum=2");

    BootstrapActionConfig bootstrapActions =
        new BootstrapActionConfig().withName("Configure hadoop")
            .withScriptBootstrapAction(scriptBootstrapAction);

    // Run flow
    RunJobFlowRequest runFlowRequest =
        new RunJobFlowRequest().withName("test-small-cmdline1").withInstances(
            instances).withSteps(stepConfig).withBootstrapActions(
            bootstrapActions).withLogUri(logPathname);

    System.out.println("start job");

    RunJobFlowResult runJobFlowResult =
        mapReduceClient.runJobFlow(runFlowRequest);

    String jobFlowId = runJobFlowResult.getJobFlowId();
    System.out.println("Ran job flow with id: " + jobFlowId);

    System.out.println("end job");
  }
}
