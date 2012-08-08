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

  private AWSElasticMapReduceJob result = new AWSElasticMapReduceJob();
  private boolean created = false;

  /**
   * Set the hadoop version.
   * @param hadoopVersion The hadoop version to set
   */
  public AWSElasticMapReduceBuilder withHadoopVersion(final String hadoopVersion) {

    if (created)
      throw new IllegalStateException();

    result.setHadoopVersion(hadoopVersion);

    return this;
  }

  /**
   * Set the number of instance to use.
   * @param nInstances The number of instances to use
   */
  public AWSElasticMapReduceBuilder withInstancesNumber(final int nInstances) {

    if (created)
      throw new IllegalStateException();

    result.setInstancesNumber(nInstances);

    return this;
  }

  /**
   * Set the type of slaves instances.
   * @param instanceType The instanceType to set
   */
  public AWSElasticMapReduceBuilder withSlavesInstanceType(
      final String instanceType) {

    if (created)
      throw new IllegalStateException();

    result.setSlavesInstanceType(instanceType);

    return this;
  }

  /**
   * Set the type of master instance.
   * @param instanceType The instanceType to set
   */
  public AWSElasticMapReduceBuilder withMasterInstanceType(
      final String instanceType) {

    if (created)
      throw new IllegalStateException();

    result.setMasterInstanceType(instanceType);

    return this;
  }

  /**
   * Set the endpoint.
   * @param endpoint The endpoint to set
   */
  public AWSElasticMapReduceBuilder withEndpoint(final String endpoint) {

    if (created)
      throw new IllegalStateException();

    result.setEndpoint(endpoint);

    return this;
  }

  /**
   * Set the log path.
   * @param logPathname The logPathname to set
   */
  public AWSElasticMapReduceBuilder withLogPathname(final String logPathname) {

    if (created)
      throw new IllegalStateException();

    this.result.setLogPathname(logPathname);

    return this;
  }

  /**
   * Set the jar location.
   * @param jarLocation jar location
   */
  public AWSElasticMapReduceBuilder withJarLocation(final String jarLocation) {

    if (created)
      throw new IllegalStateException();

    this.result.setJarLocation(jarLocation);

    return this;
  }

  /**
   * Set the jar arguments.
   * @param jarArguments jar arguments
   */
  public AWSElasticMapReduceBuilder withJarArguments(final String[] jarArguments) {

    if (created)
      throw new IllegalStateException();

    this.result.setJarArguments(jarArguments);

    return this;
  }

  /**
   * Set the job flow name.
   * @param jobFlowName job flow name
   */
  public AWSElasticMapReduceBuilder withJobFlowName(final String jobFlowName) {

    if (created)
      throw new IllegalStateException();

    this.result.setJobFlowName(jobFlowName);

    return this;
  }

  /**
   * Set the AWS access key.
   * @param AWSAccessKey set AWS access key
   */
  public AWSElasticMapReduceBuilder withAWSAccessKey(final String AWSAccessKey) {

    if (created)
      throw new IllegalStateException();

    this.result.setAWSAccessKey(AWSAccessKey);

    return this;
  }

  /**
   * Set the AWS access key.
   * @param AWSSecretKey set AWS secret key
   */
  public AWSElasticMapReduceBuilder withAWSSecretKey(final String AWSSecretKey) {

    if (created)
      throw new IllegalStateException();

    this.result.setAWSSecretKey(AWSSecretKey);

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
