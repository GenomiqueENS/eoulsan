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

package fr.ens.transcriptome.eoulsan.util.cloud;

/**
 * This class allow to easily create a AWS Elastic MapReduce job.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class AWSElasticMapReduceBuilder {

  private final AWSElasticMapReduceJob result = new AWSElasticMapReduceJob();
  private boolean created = false;

  /**
   * Set the hadoop version.
   * @param hadoopVersion The hadoop version to set
   */
  public AWSElasticMapReduceBuilder withHadoopVersion(final String hadoopVersion) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setHadoopVersion(hadoopVersion);

    return this;
  }

  /**
   * Set the number of instance to use.
   * @param nInstances The number of instances to use
   */
  public AWSElasticMapReduceBuilder withInstancesNumber(final int nInstances) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setInstancesNumber(nInstances);

    return this;
  }

  /**
   * Set the type of slaves instances.
   * @param instanceType The instanceType to set
   */
  public AWSElasticMapReduceBuilder withSlavesInstanceType(
      final String instanceType) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setSlavesInstanceType(instanceType);

    return this;
  }

  /**
   * Set the type of master instance.
   * @param instanceType The instanceType to set
   */
  public AWSElasticMapReduceBuilder withMasterInstanceType(
      final String instanceType) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setMasterInstanceType(instanceType);

    return this;
  }

  /**
   * Set the endpoint.
   * @param endpoint The endpoint to set
   */
  public AWSElasticMapReduceBuilder withEndpoint(final String endpoint) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setEndpoint(endpoint);

    return this;
  }

  /**
   * Set the log path.
   * @param logPathname The logPathname to set
   */
  public AWSElasticMapReduceBuilder withLogPathname(final String logPathname) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setLogPathname(logPathname);

    return this;
  }

  /**
   * Set the jar location.
   * @param jarLocation jar location
   */
  public AWSElasticMapReduceBuilder withJarLocation(final String jarLocation) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setJarLocation(jarLocation);

    return this;
  }

  /**
   * Set the jar arguments.
   * @param jarArguments jar arguments
   */
  public AWSElasticMapReduceBuilder withJarArguments(final String[] jarArguments) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setJarArguments(jarArguments);

    return this;
  }

  /**
   * Set the job flow name.
   * @param jobFlowName job flow name
   */
  public AWSElasticMapReduceBuilder withJobFlowName(final String jobFlowName) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setJobFlowName(jobFlowName);

    return this;
  }

  /**
   * Set the AWS access key.
   * @param AWSAccessKey set AWS access key
   */
  public AWSElasticMapReduceBuilder withAWSAccessKey(final String AWSAccessKey) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setAWSAccessKey(AWSAccessKey);

    return this;
  }

  /**
   * Set the AWS access key.
   * @param AWSSecretKey set AWS secret key
   */
  public AWSElasticMapReduceBuilder withAWSSecretKey(final String AWSSecretKey) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setAWSSecretKey(AWSSecretKey);

    return this;
  }

  /**
   * Set the number of maximal mapper tasks to use in a task tracker.
   * @param taskTrackerMaxMapTasks the number of maximal mapper tasks to use in
   *          a task tracker
   */
  public AWSElasticMapReduceBuilder withTaskTrackerMaxMapTasks(
      final int taskTrackerMaxMapTasks) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setTaskTrackerMaxMapTasks(taskTrackerMaxMapTasks);

    return this;
  }

  /**
   * Set the EC2 Key pair name to use.
   * @param ec2KeyName EC2 Key pair name to use
   */
  public AWSElasticMapReduceBuilder withEC2KeyName(final String ec2KeyName) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setEC2KeyName(ec2KeyName);

    return this;
  }

  /**
   * Set if debugging must be enabled.
   * @param enableDebugging true if debugging is enabled
   */
  public AWSElasticMapReduceBuilder withDebugging(final boolean enableDebugging) {

    if (this.created) {
      throw new IllegalStateException();
    }

    this.result.setDebugging(enableDebugging);

    return this;
  }

  /**
   * Create the instance of AWSElasticMapReduceJob
   * @return an instance of AWSElasticMapReduceJob
   */
  public AWSElasticMapReduceJob create() {

    if (!this.created) {
      this.created = true;
      this.result.init();
    }

    return this.result;
  }

}
