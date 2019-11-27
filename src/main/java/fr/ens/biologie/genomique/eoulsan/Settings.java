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

package fr.ens.biologie.genomique.eoulsan;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.Utils;

/**
 * This class define a settings class.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class Settings implements Serializable {

  private static final long serialVersionUID = -7897805708866950402L;

  private static final String MAIN_PREFIX_KEY = "main.";
  private final Properties properties = new Properties();

  private static final String DEBUG_KEY = MAIN_PREFIX_KEY + "debug";
  private static final String AWS_ACCESS_KEY = "aws.access.key";
  private static final String AWS_SECRET_KEY = "aws.secret.key";

  private static final String PRINT_STACK_TRACE_KEY =
      MAIN_PREFIX_KEY + "printstacktrace";

  private static final String BYPASS_PLATFORM_CHECKING_KEY =
      MAIN_PREFIX_KEY + "bypass.platform.checking";

  private static final String TMP_DIR_KEY = MAIN_PREFIX_KEY + "tmp.dir";

  private static final String EXECUTABLES_TMP_DIR_KEY =
      MAIN_PREFIX_KEY + "executables.tmp.dir";

  private static final String LOCAL_THREADS_NUMBER =
      MAIN_PREFIX_KEY + "local.threads";

  private static final String OUTPUT_TREE_TYPE =
      MAIN_PREFIX_KEY + "output.tree.type";

  private static final String SAVE_WORKFLOW_IMAGE_KEY =
      MAIN_PREFIX_KEY + "generate.workflow.image";

  public static final String DATA_FORMAT_PATH_KEY =
      MAIN_PREFIX_KEY + "format.path";

  public static final String GALAXY_TOOL_PATH_KEY =
      MAIN_PREFIX_KEY + "galaxy.tool.path";

  private static final String HADOOP_AWS_ACCESS_KEY =
      "hadoop.conf.fs.s3n.awsAccessKeyId";
  private static final String HADOOP_AWS_SECRET_KEY =
      "hadoop.conf.fs.s3n.awsSecretAccessKey";

  private static final String CLUSTER_SCHEDULER_NAME_KEY =
      MAIN_PREFIX_KEY + "cluster.scheduler.name";

  private static final String CLUSTER_DEFAULT_MEMORY_REQUIRED =
      MAIN_PREFIX_KEY + "cluster.memory.required";

  private static final String HADOOP_LOG_LEVEL_KEY =
      MAIN_PREFIX_KEY + "hadoop.log.level";

  private static final String RSERVE_ENABLED_KEY =
      MAIN_PREFIX_KEY + "rserve.enable";
  private static final String RSERVE_SERVER_NAME_KEY =
      MAIN_PREFIX_KEY + "rserve.servername";

  private static final String RSERVE_KEEP_FILES_KEY =
      MAIN_PREFIX_KEY + "rserve.keep.files";
  private static final String SAVE_RSCRIPTS_KEY =
      MAIN_PREFIX_KEY + "save.r.scripts";

  private static final String OBFUSCATE_DESIGN_KEY =
      MAIN_PREFIX_KEY + "design.obfuscate";
  private static final String OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_KEY =
      MAIN_PREFIX_KEY + "design.remove.replicate.info";

  // TODO To keep ?
  private static final String DEFAULT_FASTQ_FORMAT_KEY =
      MAIN_PREFIX_KEY + "default.fastq.format";

  private static final String GENOME_MAPPER_INDEX_STORAGE_KEY =
      MAIN_PREFIX_KEY + "genome.mapper.index.storage.path";

  private static final String GENOME_DESC_STORAGE_KEY =
      MAIN_PREFIX_KEY + "genome.desc.storage.path";

  private static final String GENOME_STORAGE_KEY =
      MAIN_PREFIX_KEY + "genome.storage.path";

  private static final String GFF_STORAGE_KEY =
      MAIN_PREFIX_KEY + "gff.storage.path";

  private static final String GTF_STORAGE_KEY =
      MAIN_PREFIX_KEY + "gtf.storage.path";

  private static final String ADDITIONAL_ANNOTATION_STORAGE_KEY =
      MAIN_PREFIX_KEY + "additional.annotation.storage.path";

  private static final String ADDITIONAL_ANNOTATION_HYPERTEXT_LINKS_KEY =
      MAIN_PREFIX_KEY + "additional.annotation.hypertext.links.path";

  private static final String SEND_RESULT_MAIL_KEY =
      MAIN_PREFIX_KEY + "mail.send.result.mail";

  private static final String RESULT_MAIL_KEY =
      MAIN_PREFIX_KEY + "mail.send.result.mail.to";

  private static final String SMTP_HOST_KEY =
      MAIN_PREFIX_KEY + "mail.smtp.host";

  private static final String DOCKER_URI_KEY = MAIN_PREFIX_KEY + "docker.uri";

  private static final String DOCKER_SINGULARITY_ENABLED_KEY =
      MAIN_PREFIX_KEY + "docker.singularity.enabled";

  private static final String DOCKER_SINGULARITY_STORAGE_KEY =
      MAIN_PREFIX_KEY + "docker.singularity.storage.path";

  private static final String DOCKER_MOUNT_NFS_ROOTS_KEY =
      MAIN_PREFIX_KEY + "docker.mount.nfs.roots";

  private static final String ZOOKEEPER_CONNECT_STRING_KEY =
      "zookeeper.connect.string";

  private static final String ZOOKEEPER_DEFAULT_PORT_KEY =
      "zookeeper.default.port";

  private static final String ZOOKEEPER_SESSION_TIMEOUT_KEY =
      "zookeeper.session.timeout";

  private static final String USE_OLD_EOULSAN_RESULT_FORMAT_KEY =
      MAIN_PREFIX_KEY + "old.result.format";

  private static final String UI_NAME_KEY = MAIN_PREFIX_KEY + "ui.name";

  public static final String STANDARD_EXTERNAL_MODULES_ENABLED_KEY =
      MAIN_PREFIX_KEY + "standard.external.modules.enabled";

  private static final Set<String> FORBIDDEN_KEYS = Utils.unmodifiableSet(
      new String[] {HADOOP_AWS_ACCESS_KEY, HADOOP_AWS_SECRET_KEY});

  private static final Set<String> OBFUSCATED_KEYS =
      Utils.unmodifiableSet(new String[] {AWS_ACCESS_KEY, AWS_SECRET_KEY,
          HADOOP_AWS_ACCESS_KEY, HADOOP_AWS_SECRET_KEY});

  //
  // Getters
  //

  /**
   * Test if a setting key exists.
   * @return true if the setting exist
   */
  public boolean isSetting(final String key) {

    return this.properties.containsKey(key);
  }

  /**
   * Test is the debug mode is enabled.
   * @return true if the debug mode is enable
   */
  public boolean isDebug() {

    final String value =
        this.properties.getProperty(DEBUG_KEY, Boolean.toString(Globals.DEBUG));

    return Boolean.valueOf(value);
  }

  /**
   * Test is the debug mode is enabled.
   * @return true if the debug mode is enable
   */
  public boolean isPrintStackTrace() {

    final String value = this.properties.getProperty(PRINT_STACK_TRACE_KEY,
        Boolean.toString(Globals.PRINT_STACK_TRACE_DEFAULT));

    return Boolean.valueOf(value);
  }

  /**
   * Get the AWS access key.
   * @return the AWS access key
   */
  public String getAWSAccessKey() {

    return this.properties.getProperty(AWS_ACCESS_KEY);
  }

  /**
   * Get the AWS secret key.
   * @return the AWS secret key
   */
  public String getAWSSecretKey() {

    return this.properties.getProperty(AWS_SECRET_KEY);
  }

  /**
   * Get the Hadoop log level.
   * @return the Hadoop log level
   */
  public String getHadoopLogLevel() {

    return this.properties.getProperty(HADOOP_LOG_LEVEL_KEY);
  }

  /**
   * Get the name of the cluster scheduler.
   * @return the name of the cluster scheduler
   */
  public String getClusterSchedulerName() {

    return this.properties.getProperty(CLUSTER_SCHEDULER_NAME_KEY);
  }

  /**
   * Get the default memory required for the steps in cluster mode.
   * @return the default memory required for the cluster mode
   */
  public int getDefaultClusterMemoryRequired() {

    String value = this.properties.getProperty(CLUSTER_DEFAULT_MEMORY_REQUIRED);

    if (value == null) {
      return -1;
    }

    value = value.trim();

    if (value.isEmpty()) {
      return -1;
    }

    return Integer.parseInt(value);
  }

  /**
   * Test if RServe is enabled.
   * @return true if the RServe server is enabled
   */
  public boolean isRServeServerEnabled() {

    return Boolean
        .parseBoolean(this.properties.getProperty(RSERVE_ENABLED_KEY));
  }

  /**
   * Test if save.r.script is true
   * @return boolean with keep Rscripts values
   */
  public boolean isSaveRscripts() {

    return Boolean.parseBoolean(this.properties.getProperty(SAVE_RSCRIPTS_KEY));
  }

  /**
   * Get the RServe server name.
   * @return The name of the RServe to use
   */
  public String getRServeServerName() {

    return this.properties.getProperty(RSERVE_SERVER_NAME_KEY);
  }

  /**
   * Test if save.r.keep.files is true
   * @return boolean if the RServe files must be kept
   */
  public boolean isKeepRServeFiles() {

    return Boolean
        .parseBoolean(this.properties.getProperty(RSERVE_KEEP_FILES_KEY));
  }

  /**
   * Get the temporary directory.
   * @return The temporary directory
   */
  public String getTempDirectory() {

    return this.properties.getProperty(TMP_DIR_KEY,
        System.getProperty("java.io.tmpdir"));
  }

  /**
   * Get the temporary directory File.
   * @return The temporary directory as a File object
   */
  public File getTempDirectoryFile() {

    return new File(getTempDirectory());
  }

  /**
   * Test if the temporary directory File has been defined by user.
   * @return true id the temporary directory has defined by user
   */
  public boolean isUserDefinedTempDirectory() {

    return this.properties.containsKey(TMP_DIR_KEY);
  }

  /**
   * Get the temporary directory for executables.
   * @return The temporary directory for executables
   */
  public String getExecutablesTempDirectory() {

    return this.properties.getProperty(EXECUTABLES_TMP_DIR_KEY,
        getTempDirectory());
  }

  /**
   * Get the temporary directory File for executables.
   * @return The temporary directory for executables as a File object
   */
  public File getExecutablesTempDirectoryFile() {

    return new File(getExecutablesTempDirectory());
  }

  /**
   * Test if design must be obfuscated
   * @return true if design must be obfuscated
   */
  public boolean isObfuscateDesign() {

    return Boolean
        .parseBoolean(this.properties.getProperty(OBFUSCATE_DESIGN_KEY,
            Boolean.toString(Globals.OBFUSCATE_DESIGN_DEFAULT)));
  }

  /**
   * Test if replicate information must be removed from design.
   * @return true if replicate information must be removed
   */
  public boolean isObfuscateDesignRemoveReplicateInfo() {

    return Boolean.parseBoolean(this.properties
        .getProperty(OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_KEY, Boolean
            .toString(Globals.OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_DEFAULT)));
  }

  /**
   * Get the number of threads to use in Steps computation in local mode.
   * @return the number of threads to use
   */
  public int getLocalThreadsNumber() {

    return Integer.parseInt(this.properties.getProperty(LOCAL_THREADS_NUMBER,
        "" + Runtime.getRuntime().availableProcessors()));
  }

  /**
   * Get the default fastq format.
   * @return the default fastq format
   */
  public FastqFormat getDefaultFastqFormat() {

    return FastqFormat.getFormatFromName(this.properties.getProperty(
        DEFAULT_FASTQ_FORMAT_KEY, Globals.FASTQ_FORMAT_DEFAULT.getName()));
  }

  /**
   * Test if the platform checking must be avoided at Eoulsan startup.
   * @return true if the platform checking must be avoided
   */
  public boolean isBypassPlatformChecking() {

    return Boolean.parseBoolean(
        this.properties.getProperty(BYPASS_PLATFORM_CHECKING_KEY));
  }

  /**
   * Get the genome mapper index storage path.
   * @return the path to genome mapper index storage path
   */
  public String getGenomeMapperIndexStoragePath() {

    return this.properties.getProperty(GENOME_MAPPER_INDEX_STORAGE_KEY);
  }

  /**
   * Get the genome description storage path.
   * @return the path to genome description storage path
   */
  public String getGenomeDescStoragePath() {

    return this.properties.getProperty(GENOME_DESC_STORAGE_KEY);
  }

  /**
   * Get the genome storage path.
   * @return the path to genome storage path
   */
  public String getGenomeStoragePath() {

    return this.properties.getProperty(GENOME_STORAGE_KEY);
  }

  /**
   * Get the GFF storage path.
   * @return the path to GFF storage path
   */
  public String getGFFStoragePath() {

    return this.properties.getProperty(GFF_STORAGE_KEY);
  }

  /**
   * Get the GTF storage path.
   * @return the path to GTF storage path
   */
  public String getGTFStoragePath() {

    return this.properties.getProperty(GTF_STORAGE_KEY);
  }

  /**
   * Get the additional annotation storage path.
   * @return the path to the additional annotation storage path
   */
  public String getAdditionalAnnotationStoragePath() {

    return this.properties.getProperty(ADDITIONAL_ANNOTATION_STORAGE_KEY);
  }

  /**
   * Get the additional annotation hypertext links path.
   * @return the additional annotation hypertext links path
   */
  public String getAdditionalAnnotationHypertextLinksPath() {

    return this.properties
        .getProperty(ADDITIONAL_ANNOTATION_HYPERTEXT_LINKS_KEY);
  }

  /**
   * Test if an email must be sent at the end of the analysis.
   * @return true if an email must be sent at the end of the analysis
   */
  public boolean isSendResultMail() {

    return Boolean
        .parseBoolean(this.properties.getProperty(SEND_RESULT_MAIL_KEY));
  }

  /**
   * Get the mail address for eoulsan results.
   * @return the mail address as a string
   */
  public String getResultMail() {

    return this.properties.getProperty(RESULT_MAIL_KEY);
  }

  /**
   * Get the name of the SMTP server host.
   * @return the name of the SMTP server host
   */
  public String getSMTPHost() {

    return this.properties.getProperty(SMTP_HOST_KEY);
  }

  /**
   * Get the ZooKeeper connect string.
   * @return the ZooKeeper connect string
   */
  public String getZooKeeperConnectString() {

    return this.properties.getProperty(ZOOKEEPER_CONNECT_STRING_KEY);
  }

  /**
   * Get the ZooKeeper default port.
   * @return the ZooKeeper default port
   */
  public int getZooKeeperDefaultPort() {

    return Integer
        .parseInt(this.properties.getProperty(ZOOKEEPER_DEFAULT_PORT_KEY,
            "" + Globals.ZOOKEEPER_DEFAULT_PORT_DEFAULT));
  }

  /**
   * Get the ZooKeeper session timeout.
   * @return the ZooKeeper session timeout
   */
  public int getZooKeeperSessionTimeout() {

    return Integer
        .parseInt(this.properties.getProperty(ZOOKEEPER_SESSION_TIMEOUT_KEY,
            "" + Globals.ZOOKEEPER_SESSION_TIMEOUT_DEFAULT));
  }

  /**
   * Test if Eoulsan result files must be written using the old format.
   * @return true if Eoulsan result files must be written using the old format
   */
  public boolean isUseOldEoulsanResultFormat() {

    return Boolean.parseBoolean(
        this.properties.getProperty(USE_OLD_EOULSAN_RESULT_FORMAT_KEY,
            "" + Globals.USE_OLD_EOULSAN_RESULT_FORMAT_DEFAULT));
  }

  /**
   * Get the UI name.
   * @return the UI name
   */
  public String getUIName() {

    return this.properties.getProperty(UI_NAME_KEY, Globals.UI_NAME_DEFAULT);
  }

  /**
   * Get the Docker connection string.
   * @return the docker connection string
   */
  public String getDockerConnection() {

    return this.properties.getProperty(DOCKER_URI_KEY);
  }

  /**
   * Is Docker features are enabled using singularity.
   * @return true if the Docker features are enabled using Singularity
   */
  public boolean isDockerBySingularityEnabled() {

    return Boolean.parseBoolean(
        this.properties.getProperty(DOCKER_SINGULARITY_ENABLED_KEY));
  }

  /**
   * Get the Docker singularity storage path.
   * @return the path to GFF storage path
   */
  public String getDockerSingularityStoragePath() {

    return this.properties.getProperty(DOCKER_SINGULARITY_STORAGE_KEY);
  }

  /**
   * Test if a Docker connection has been set.
   * @return true if a Docker connection has been set
   */
  public boolean isDockerConnectionDefined() {

    String connection = getDockerConnection();

    if (connection == null || connection.trim().isEmpty()) {
      return false;
    }

    return true;
  }

  /**
   * Test if when use Docker, NFS roots must been mounted instead of file paths.
   * @return true if if when use Docker, NFS roots must been mounted instead of
   *         file paths
   */
  public boolean isDockerMountNFSRoots() {

    return Boolean.parseBoolean(
        this.properties.getProperty(DOCKER_MOUNT_NFS_ROOTS_KEY, "" + false));
  }

  /**
   * Get the Docker connection URI.
   * @return the docker connection URI
   */
  public URI getDockerConnectionURI() {

    final String connectionString = getDockerConnection();

    if (connectionString == null || connectionString.trim().isEmpty()) {
      return null;
    }

    try {
      return new URI(connectionString);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Get the format paths.
   * @return the format path
   */
  public List<String> getDataFormatPaths() {

    String value = this.properties.getProperty(DATA_FORMAT_PATH_KEY);

    if (value == null) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();

    for (String s : value.split(" ")) {

      if (!s.isEmpty()) {
        result.add(s);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Get the Galaxy tool path.
   * @return the Galaxy tool path
   */
  public List<String> getGalaxyToolPaths() {

    String value = this.properties.getProperty(GALAXY_TOOL_PATH_KEY);

    if (value == null) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();

    for (String s : value.split(" ")) {

      if (!s.isEmpty()) {
        result.add(s);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Get the output tree type.
   * @return the output tree type
   */
  public String getOutputTreeType() {

    return this.properties.getProperty(OUTPUT_TREE_TYPE,
        Globals.OUTPUT_TREE_TYPE_DEFAULT);
  }

  /**
   * Test if an image of the workflow must be saved.
   * @return true if an image of the workflow must be saved
   */
  public boolean isSaveWorkflowImage() {

    return Boolean.parseBoolean(this.properties.getProperty(
        SAVE_WORKFLOW_IMAGE_KEY, "" + Globals.SAVE_WORKFLOW_IMAGE_DEFAULT));
  }

  /**
   * Test if standard external modules must be used.
   * @return true if standard external modules must be used
   */
  public boolean isUseStandardExternalModules() {

    return Boolean.parseBoolean(
        this.properties.getProperty(STANDARD_EXTERNAL_MODULES_ENABLED_KEY,
            "" + Globals.STANDARD_EXTERNAL_MODULES_ENABLED_DEFAULT));
  }

  /**
   * Get a setting value.
   * @return settingName value as a String
   */
  public String getSetting(final String settingName) {

    if (settingName == null) {
      return null;
    }

    if (settingName.startsWith(MAIN_PREFIX_KEY)) {
      return null;
    }

    return this.properties.getProperty(settingName);
  }

  /**
   * Get the value of the setting as a integer value
   * @return the value of the setting as an integer
   * @throws EoulsanException if the value is not an integer
   */
  public int getIntSetting(final String settingName) throws EoulsanException {

    if (settingName == null) {
      throw new EoulsanException("The setting name is null");
    }

    final String value = getSetting(settingName);
    if (value == null) {
      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: null");
    }

    try {

      return Integer.parseInt(value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: " + value);
    }

  }

  /**
   * Get the value of the setting as a double value
   * @return the value of the setting as an double
   * @throws EoulsanException if the value is not an double
   */
  public double getDoubleSetting(final String settingName)
      throws EoulsanException {

    if (settingName == null) {
      throw new EoulsanException("The setting name is null");
    }

    final String value = getSetting(settingName);
    if (value == null) {
      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: null");
    }

    try {

      return Double.parseDouble(value);
    } catch (NumberFormatException e) {

      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: " + value);
    }

  }

  /**
   * Get the value of the setting as a boolean value
   * @return the value of the setting as an integer
   */
  public boolean getBooleanSetting(final String settingName) {

    return Boolean.parseBoolean(getSetting(settingName));
  }

  /**
   * Get a set of settings names.
   * @return a set with all the name of the settings
   */
  public Set<String> getSettingsNames() {

    final Set<String> result = new HashSet<>();

    for (String key : this.properties.stringPropertyNames()) {
      if (!key.startsWith(MAIN_PREFIX_KEY)) {
        result.add(key);
      }
    }

    return result;
  }

  //
  // Setters
  //

  /**
   * Set the debug setting.
   * @param debug value of the debug setting
   */
  public void setDebug(final boolean debug) {

    this.properties.setProperty(DEBUG_KEY, Boolean.toString(debug));
  }

  /**
   * Set the print stack trace setting.
   * @param printStackTrace value of the print stack trace setting
   */
  public void setPrintStackTrace(final boolean printStackTrace) {

    this.properties.setProperty(PRINT_STACK_TRACE_KEY,
        Boolean.toString(printStackTrace));
  }

  /**
   * Set the AWS access key.
   * @param value the AWS access key
   */
  public void setAWSAccessKey(final String value) {

    if (value == null) {
      return;
    }

    this.properties.setProperty(AWS_ACCESS_KEY, value);
    this.properties.setProperty(HADOOP_AWS_ACCESS_KEY, value);
  }

  /**
   * Set the AWS secret key.
   * @param value the AWS secret key
   */
  public void setAWSSecretKey(final String value) {

    if (value == null) {
      return;
    }

    this.properties.setProperty(AWS_SECRET_KEY, value);
    this.properties.setProperty(HADOOP_AWS_SECRET_KEY, value);
  }

  /**
   * Set the Hadoop log level.
   * @param value the HAdoop log level
   */
  public void setHadoopLogLevel(final String value) {

    this.properties.setProperty(HADOOP_LOG_LEVEL_KEY, value);
  }

  /**
   * Set the name of the cluster scheduler.
   * @param schedulerName the name of the cluster scheduler
   */
  public void setClusterSchedulerName(final String schedulerName) {

    this.properties.setProperty(CLUSTER_SCHEDULER_NAME_KEY, schedulerName);
  }

  /**
   * Set the default memory required for the steps in cluster mode.
   * @param memory the required memory
   */
  public void setDefaultClusterMemoryRequired(final int memory) {

    this.properties.setProperty(CLUSTER_DEFAULT_MEMORY_REQUIRED, "" + memory);
  }

  /**
   * Set if RServe is enabled.
   * @param enable true if the RServe server is enable
   */
  public void setRServeServerEnabled(final boolean enable) {

    this.properties.setProperty(RSERVE_ENABLED_KEY, Boolean.toString(enable));
  }

  /**
   * Set if save Rscripts is true
   * @param krs boolean
   */
  public void setSaveRscript(final boolean krs) {

    this.properties.setProperty(SAVE_RSCRIPTS_KEY, Boolean.toString(krs));
  }

  /**
   * Set if save Rscripts is true
   * @param keepRServeFiles true if Rserve file must be kept
   */
  public void setKeepRServeFiles(final boolean keepRServeFiles) {

    this.properties.setProperty(RSERVE_KEEP_FILES_KEY,
        Boolean.toString(keepRServeFiles));
  }

  /**
   * Set the RServe server name.
   * @param serverName The name of the RServe to use
   */
  public void setRServeServerName(final String serverName) {

    this.properties.setProperty(RSERVE_SERVER_NAME_KEY, serverName);
  }

  /**
   * Set the temporary directory.
   * @param tempDirectory The path to the temporary directory
   */
  public void setTempDirectory(final String tempDirectory) {

    if (tempDirectory != null) {
      this.properties.setProperty(TMP_DIR_KEY, tempDirectory);
    }
  }

  /**
   * Set the temporary directory for executables.
   * @param executablesTempDirectory The path to the temporary directory for
   *          executables
   */
  public void setExecutablesTempDirectory(
      final String executablesTempDirectory) {

    if (executablesTempDirectory != null) {
      this.properties.setProperty(EXECUTABLES_TMP_DIR_KEY,
          executablesTempDirectory);
    }
  }

  /**
   * Set if the design must be obfuscated
   * @param obfuscate true if the design must be obfuscated
   */
  public void setObfuscateDesign(final boolean obfuscate) {

    this.properties.setProperty(OBFUSCATE_DESIGN_KEY,
        Boolean.toString(obfuscate));
  }

  /**
   * Set if the replicate information must be removed from the design.
   * @param remove true if the replicate information must be remove
   */
  public void setObfuscateRemoveDesignInfo(final boolean remove) {

    this.properties.setProperty(OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_KEY,
        Boolean.toString(remove));
  }

  /**
   * Set the number of threads to use in local mode.
   * @param threadsNumber the number of threads to use in local mode
   */
  public void setLocalThreadsNumber(final int threadsNumber) {

    if (threadsNumber < 0) {
      return;
    }

    this.properties.setProperty(LOCAL_THREADS_NUMBER,
        Integer.toString(threadsNumber));
  }

  /**
   * Set the Fastq format default value.
   * @param format the value to set
   */
  public void setDefaultFastqFormat(final FastqFormat format) {

    if (format == null) {
      throw new NullPointerException("The FastqFormat is null");
    }

    this.properties.setProperty(DEFAULT_FASTQ_FORMAT_KEY, format.getName());
  }

  /**
   * Set if the platform checking must be avoided.
   * @param bypass true to bypass the platform checking
   */
  public void setBypassPlatformChecking(final boolean bypass) {

    this.properties.setProperty(BYPASS_PLATFORM_CHECKING_KEY,
        Boolean.toString(bypass));
  }

  /**
   * Set the genome index storage path.
   * @param genomeMapperIndexStoragePath the path to genome index storage path
   */
  public void setGenomeMapperIndexStoragePath(
      final String genomeMapperIndexStoragePath) {

    this.properties.setProperty(GENOME_MAPPER_INDEX_STORAGE_KEY,
        genomeMapperIndexStoragePath);
  }

  /**
   * Set the genome description storage path.
   * @param genomeDescStoragePath the path to genome index storage path
   */
  public void setGenomeDescStoragePath(final String genomeDescStoragePath) {

    this.properties.setProperty(GENOME_DESC_STORAGE_KEY, genomeDescStoragePath);
  }

  /**
   * Set the genome storage path.
   * @param genomeStoragePath the path to genome index storage path
   */
  public void setGenomeStoragePath(final String genomeStoragePath) {

    this.properties.setProperty(GENOME_STORAGE_KEY, genomeStoragePath);
  }

  /**
   * Set the GFF storage path.
   * @param gffStoragePath the path to GFF index storage path
   */
  public void setGFFStoragePath(final String gffStoragePath) {

    this.properties.setProperty(GFF_STORAGE_KEY, gffStoragePath);
  }

  /**
   * Set the GTF storage path.
   * @param gtfStoragePath the path to GTF index storage path
   */
  public void setGTFStoragePath(final String gtfStoragePath) {

    this.properties.setProperty(GTF_STORAGE_KEY, gtfStoragePath);
  }

  /**
   * Set the additional annotation storage path.
   * @param additionalAnnotationStoragePath the path to the additional
   *          annotation index storage path
   */
  public void setAdditionalAnnotationStoragePath(
      final String additionalAnnotationStoragePath) {

    this.properties.setProperty(ADDITIONAL_ANNOTATION_STORAGE_KEY,
        additionalAnnotationStoragePath);
  }

  /**
   * Set the additional annotation hypertext links path.
   * @param additionalAnnotationHypertextLinksPath the path to the additional
   *          annotation hypertext links path
   */
  public void setAdditionalAnnotationHypertextLinksPath(
      final String additionalAnnotationHypertextLinksPath) {

    this.properties.setProperty(ADDITIONAL_ANNOTATION_HYPERTEXT_LINKS_KEY,
        additionalAnnotationHypertextLinksPath);
  }

  /**
   * Set if an email must be sent at the end of the analysis.
   * @param enableSendResultMail true if an email must be sent at the end of the
   *          analysis
   */
  public void setSendResultMail(final boolean enableSendResultMail) {

    this.properties.setProperty(SEND_RESULT_MAIL_KEY,
        Boolean.toString(enableSendResultMail));
  }

  /**
   * Set the mail address for eoulsan results.
   * @param mail the mail address as a string
   */
  public void setResultMail(final String mail) {

    this.properties.setProperty(RESULT_MAIL_KEY, mail);
  }

  /**
   * Set the SMTP server host.
   * @param smtpHost the name of the SMTP server host
   */
  public void setSMTPHost(final String smtpHost) {

    this.properties.setProperty(SMTP_HOST_KEY, smtpHost);
  }

  /**
   * Set the ZooKeeper connect string.
   * @param connectString the ZooKeeper connect string
   */
  public void setZooKeeperConnectString(final String connectString) {

    this.properties.setProperty(ZOOKEEPER_CONNECT_STRING_KEY, connectString);
  }

  /**
   * Set the ZooKeeper default port.
   * @param port the ZooKeeper default port
   */
  public void setZooKeeperDefaultPort(final int port) {

    this.properties.setProperty(ZOOKEEPER_DEFAULT_PORT_KEY, "" + port);
  }

  /**
   * Set the ZooKeeper session timeout.
   * @param timeout the ZooKeeper session timeout
   */
  public void setZooKeeperSessionTimeout(final int timeout) {

    this.properties.setProperty(ZOOKEEPER_SESSION_TIMEOUT_KEY, "" + timeout);
  }

  /**
   * Set if Eoulsan result files must be written using the old format.
   * @param useOldEoulsanResultFormat true if Eoulsan result files must be
   *          written using the old format
   */
  public void setUseOldEoulsanResultFormat(
      final boolean useOldEoulsanResultFormat) {

    this.properties.setProperty(USE_OLD_EOULSAN_RESULT_FORMAT_KEY,
        Boolean.toString(useOldEoulsanResultFormat));
  }

  /**
   * Set the UI name.
   * @param uiName the UI name
   */
  public void setUIName(final String uiName) {

    this.properties.setProperty(UI_NAME_KEY, uiName);
  }

  /**
   * Set the Docker URI.
   * @param uri the Docker URI
   */
  public void setDockerConnectionURI(final String uri) {

    this.properties.setProperty(DOCKER_URI_KEY, uri);
  }

  /**
   * Enable Docker features using Singularity
   * @param enabled true to enable the feature
   */
  public void setDockerBySingularityEnabled(final boolean enabled) {

    this.properties.setProperty(DOCKER_SINGULARITY_ENABLED_KEY, "" + enabled);
  }

  /**
   * Set the Docker singularity storage path.
   * @param storagePath the path to Docker singularity storage path
   */
  public void setDockerSingularityStoragePath(final String storagePath) {

    this.properties.setProperty(DOCKER_SINGULARITY_STORAGE_KEY, storagePath);
  }

  /**
   * Set if when use Docker, NFS roots must been mounted instead of file paths.
   * param value the value of the parameter
   */
  public void setDockerMountNFSRoots(final boolean enable) {

    this.properties.setProperty(DOCKER_MOUNT_NFS_ROOTS_KEY, "" + enable);
  }

  /**
   * Set the format path.
   * @param path the format
   */
  public void setDataFormatPath(final String path) {

    this.properties.setProperty(DATA_FORMAT_PATH_KEY, path);
  }

  /**
   * Set the data format paths
   * @param paths the path to set
   */
  public void setDataFormatPaths(List<String> paths) {

    Objects.requireNonNull(paths);

    StringBuilder sb = new StringBuilder();

    boolean first = true;
    for (String s : paths) {
      if (first) {
        first = false;
      } else {
        sb.append(' ');
      }
      sb.append(s);
    }

    setDataFormatPath(sb.toString());
  }

  /**
   * Set the Galaxy tool path.
   * @param path the format
   */
  public void setGalaxyToolPath(final String path) {

    this.properties.setProperty(GALAXY_TOOL_PATH_KEY, path);
  }

  /**
   * Set the Galaxy tools paths
   * @param paths the path to set
   */
  public void setGalaxyToolsPaths(List<String> paths) {

    Objects.requireNonNull(paths);

    StringBuilder sb = new StringBuilder();

    boolean first = true;
    for (String s : paths) {
      if (first) {
        first = false;
      } else {
        sb.append(' ');
      }
      sb.append(s);
    }

    setGalaxyToolPath(sb.toString());
  }

  /**
   * Set the output tree type.
   * @param outputTreeType the output tree type
   */
  public void setOutputTreeType(final String outputTreeType) {

    this.properties.getProperty(OUTPUT_TREE_TYPE, outputTreeType);
  }

  /**
   * Set if an image of the workflow must be saved.
   * @param save the value
   */
  public void setSaveWorkflowImage(final boolean save) {

    this.properties.setProperty(SAVE_WORKFLOW_IMAGE_KEY, "" + save);
  }

  /**
   * Set if standard external modules must be used.
   * @param enable the value
   */
  public void setUseStandardExternalModules(final boolean enable) {

    this.properties.getProperty(STANDARD_EXTERNAL_MODULES_ENABLED_KEY,
        "" + enable);
  }

  /**
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public void setSetting(final String settingName, final String settingValue) {

    setSetting(settingName, settingValue, true);
  }

  /**
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   * @param logChange if true the change will be logged
   */
  public void setSetting(final String settingName, final String settingValue,
      final boolean logChange) {

    if (settingName == null || settingValue == null) {
      return;
    }

    final String key = settingName.toLowerCase();

    if (FORBIDDEN_KEYS.contains(key)) {
      return;
    }

    // Set the property with the current setting name
    this.properties.setProperty(checkDeprecatedKey(key), settingValue);
    if (logChange) {
      logSetting(key);
    }
  }

  //
  // Other methods
  //

  /**
   * Get the configuration file path.
   * @return the configuration file path
   */
  public static String getConfigurationFilePath() {

    final String os = System.getProperty("os.name");
    final String home = System.getProperty("user.home");

    if (os.toLowerCase(Globals.DEFAULT_LOCALE).startsWith("windows")) {
      return home
          + File.separator + "Application Data" + File.separator
          + Globals.APP_NAME_LOWER_CASE + ".conf";
    }

    return home + File.separator + "." + Globals.APP_NAME_LOWER_CASE;
  }

  /**
   * Create a property object for javamail smtp configuration from the settings.
   * @return a property object
   */
  public Properties getJavaMailSMTPProperties() {

    final Properties result = new Properties();

    for (Map.Entry<Object, Object> e : this.properties.entrySet()) {

      final String key = (String) e.getKey();
      final String value = (String) e.getValue();

      final String prefix = MAIN_PREFIX_KEY + "mail.smtp.";
      final int keyPosStart = MAIN_PREFIX_KEY.length();

      if (key != null && key.startsWith(prefix)) {
        result.setProperty(key.substring(keyPosStart), value);
      }

    }

    return result;
  }

  /**
   * Save application options.
   * @throws IOException if an error occurs while writing results
   */
  public void saveSettings() throws IOException {

    saveSettings(new File(getConfigurationFilePath()));
  }

  /**
   * Save application options.
   * @param file File to save
   * @throws IOException if an error occurs while writing settings
   */
  public void saveSettings(final File file) throws IOException {

    final OutputStream os = FileUtils.createOutputStream(file);

    this.properties.store(os,
        " "
            + Globals.APP_NAME + " version " + Globals.APP_VERSION_STRING
            + " configuration file");
    os.close();
  }

  /**
   * Load application options.
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings() throws IOException, EoulsanException {

    final File confFile = new File(getConfigurationFilePath());
    if (confFile.exists()) {
      loadSettings(confFile);
    } else {
      getLogger().config("No configuration file found.");
    }
  }

  /**
   * Load application options.
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings(final File file)
      throws IOException, EoulsanException {

    getLogger().info("Load configuration file: " + file.getAbsolutePath());
    final InputStream is = FileUtils.createInputStream(file);

    // Load properties in a temporary object
    final Properties tmpProperties = new Properties();
    tmpProperties.load(FileUtils.createInputStream(file));
    is.close();

    for (String key : tmpProperties.stringPropertyNames()) {

      // Check for forbidden settings
      if (FORBIDDEN_KEYS.contains(key)) {
        throw new EoulsanException(
            "Forbiden key found in configuration file: " + key);
      }

      // Set the property with the current setting name
      this.properties.setProperty(checkDeprecatedKey(key),
          tmpProperties.getProperty(key));
    }
  }

  /**
   * Set the values of the settings with another Settings object.
   * @param settings Settings object which values must be set in the current
   *          object
   */
  public void setSettings(final Settings settings) {

    if (settings == null) {
      throw new NullPointerException("settings arguments cannot be null");
    }

    // Set all the values
    this.properties.putAll(settings.properties);
  }

  private void init() {

    getLogger()
        .info("System temp directory: " + System.getProperty("java.io.tmpdir"));
  }

  /**
   * Add all the settings to the log.
   */
  public void logSettings() {

    for (Object key : this.properties.keySet()) {
      logSetting((String) key);
    }
  }

  /**
   * Log a setting value.
   * @param key key to log
   */
  private void logSetting(final String key) {

    if (OBFUSCATED_KEYS.contains(key)) {
      getLogger().info("Setting: " + key + "=xxxx value not shown xxxx");
    } else {
      getLogger()
          .info("Setting: " + key + "=" + this.properties.getProperty(key));
    }
  }

  /**
   * Get a set with the names of the settings to obfuscate.
   * @return a set of strings with the name of the settings to obfuscate
   */
  public Set<String> getSettingsKeyToObfuscated() {

    return OBFUSCATED_KEYS;
  }

  /**
   * Check deprecated setting key.
   * @param key the key to check
   * @return the new name of the key if exists or the current key name
   */
  private static String checkDeprecatedKey(final String key) {

    if (key == null) {
      return null;
    }

    final String trimmedKey = key.trim().toLowerCase();

    switch (trimmedKey) {

    case "main.accesskey":
      printWarningRenamedSetting(trimmedKey, AWS_ACCESS_KEY);
      break;

    case "main.awssecretkey":
      printWarningRenamedSetting(trimmedKey, AWS_SECRET_KEY);
      break;

    case "main.annotation.storage.path":
      printWarningRenamedSetting(trimmedKey, GFF_STORAGE_KEY);
      return GFF_STORAGE_KEY;

    default:
      return key;
    }

    return key;
  }

  /**
   * Print a warning message to inform user that a setting key has been renamed.
   * @param oldName the old setting key
   * @param newName the new setting key
   */
  private static void printWarningRenamedSetting(final String oldName,
      final String newName) {

    Common.printWarning("The global/configuration parameter \""
        + oldName + "\" is now deprecated. Please use the \"" + newName
        + "\" parameter instead");
  }

  //
  // Constructor
  //

  /**
   * Public constructor. Load application options.
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  Settings() throws IOException, EoulsanException {

    this(false);

  }

  /**
   * Public constructor. Load application options.
   * @param loadDefaultConfigurationFile true if default configuration file must
   *          be read
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  Settings(final boolean loadDefaultConfigurationFile)
      throws IOException, EoulsanException {

    init();

    if (!loadDefaultConfigurationFile) {
      loadSettings();
    }
  }

  /**
   * Public constructor. Load application options.
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  Settings(final File file) throws IOException, EoulsanException {

    init();
    loadSettings(file);
  }

  @Override
  public String toString() {
    return this.properties.toString();
  }

}
