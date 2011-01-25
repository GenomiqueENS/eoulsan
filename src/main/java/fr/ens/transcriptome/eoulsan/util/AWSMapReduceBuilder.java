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

package fr.ens.transcriptome.eoulsan.util;

public class AWSMapReduceBuilder {

  private AWSMapReduceJob result = new AWSMapReduceJob();
  private boolean created = false;

  /**
   * Set the hadoop version.
   * @param hadoopVersion The hadoop version to set
   */
  public AWSMapReduceBuilder withHadoopVersion(final String hadoopVersion) {

    if (created)
      throw new IllegalStateException();

    result.setHadoopVersion(hadoopVersion);

    return this;
  }

  /**
   * Set the number of instance to use.
   * @param nInstances The number of instances to use
   */
  public AWSMapReduceBuilder withInstancesNumber(final int nInstances) {

    if (created)
      throw new IllegalStateException();

    result.setInstancesNumber(nInstances);

    return this;
  }

  /**
   * Set the type of slaves instances.
   * @param instanceType The instanceType to set
   */
  public AWSMapReduceBuilder withSlavesInstanceType(final String instanceType) {

    if (created)
      throw new IllegalStateException();

    result.setSlavesInstanceType(instanceType);

    return this;
  }

  /**
   * Set the type of master instance.
   * @param instanceType The instanceType to set
   */
  public AWSMapReduceBuilder withMasterInstanceType(final String instanceType) {

    if (created)
      throw new IllegalStateException();

    result.setMasterInstanceType(instanceType);

    return this;
  }

  /**
   * Set the endpoint.
   * @param endpoint The endpoint to set
   */
  public AWSMapReduceBuilder withEndpoint(final String endpoint) {

    if (created)
      throw new IllegalStateException();

    result.setEndpoint(endpoint);

    return this;
  }

  /**
   * Set the log path.
   * @param logPathname The logPathname to set
   */
  public AWSMapReduceBuilder withLogPathname(final String logPathname) {

    if (created)
      throw new IllegalStateException();

    this.result.setLogPathname(logPathname);

    return this;
  }

  /**
   * Set the jar location.
   * @param jarLocation jar location
   */
  public AWSMapReduceBuilder withJarLocation(final String jarLocation) {

    if (created)
      throw new IllegalStateException();

    this.result.setJarLocation(jarLocation);

    return this;
  }

  /**
   * Set the jar arguments.
   * @param jarArguments jar arguments
   */
  public AWSMapReduceBuilder withJarArguments(final String[] jarArguments) {

    if (created)
      throw new IllegalStateException();

    this.result.setJarArguments(jarArguments);

    return this;
  }

  /**
   * Set the jar location.
   * @param jarLocation jar location
   */
  public AWSMapReduceBuilder withJobFlowName(final String jobFlowName) {

    if (created)
      throw new IllegalStateException();

    this.result.setJobFlowName(jobFlowName);

    return this;
  }

  /**
   * Set the AWS access key.
   * @param jobFlowName set AWS access key
   */
  public AWSMapReduceBuilder withAWSAccessKey(final String AWSAccessKey) {

    if (created)
      throw new IllegalStateException();

    this.result.setAWSAccessKey(AWSAccessKey);

    return this;
  }

  /**
   * Set the AWS access key.
   * @param AWSSecretKey set AWS secret key
   */
  public AWSMapReduceBuilder withAWSsecretKey(final String AWSSecretKey) {

    if (created)
      throw new IllegalStateException();

    this.result.setAWSAccessKey(AWSSecretKey);

    return this;
  }

  /**
   * Create the instance of AWSMapReduce
   * @return an instance of AWSMapReduce
   */
  public AWSMapReduceJob create() {

    if (!this.created) {
      this.created = true;
      this.result.init();
    }

    return this.result;
  }

}
