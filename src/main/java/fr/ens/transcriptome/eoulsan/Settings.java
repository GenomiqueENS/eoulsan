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

package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class define a settings class.
 * @author Laurent Jourdren
 */
public final class Settings {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String MAIN_PREFIX_KEY = "main.";
  private final Properties properties = new Properties();

  private static final String DEBUG_KEY = MAIN_PREFIX_KEY + "debug";
  private static final String AWS_ACCESS_KEY = "aws.access.key";
  private static final String AWS_SECRET_KEY = "aws.secret.key";
  private static final String AWS_UPLOAD_MULTIPART = "aws.upload.multipart";

  private static final String PRINT_STACK_TRACE_KEY = MAIN_PREFIX_KEY
      + "printstacktrace";

  private static final String BYPASS_PLATFORM_CHECKING_KEY = MAIN_PREFIX_KEY
      + "bypass.platform.checking";

  private static final String TMP_DIR_KEY = MAIN_PREFIX_KEY + "tmp.dir";

  private static final String LOCAL_THREADS_NUMBER = MAIN_PREFIX_KEY
      + "local.threads";

  private static final String HADOOP_AWS_ACCESS_KEY =
      "hadoop.conf.fs.s3n.awsAccessKeyId";
  private static final String HADOOP_AWS_SECRET_KEY =
      "hadoop.conf.fs.s3n.awsSecretAccessKey";

  private static final String RSERVE_ENABLED_KEY = MAIN_PREFIX_KEY
      + "rserve.enable";
  private static final String RSERVE_SERVER_NAME_KEY = MAIN_PREFIX_KEY
      + "rserve.servername";

  private static final String OBFUSCATE_DESIGN_KEY = MAIN_PREFIX_KEY
      + "design.obfuscate";
  private static final String REMOVE_REPLICATE_INFO_KEY = MAIN_PREFIX_KEY
      + "design.remove.replicate.info";

  private static final String PHRED_OFFSET_DEFAULT_KEY = MAIN_PREFIX_KEY
      + "phred.offset.default";

  private static final Set<String> FORBIDDEN_KEYS = Utils
      .unmodifiableSet(new String[] {HADOOP_AWS_ACCESS_KEY,
          HADOOP_AWS_SECRET_KEY});

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

    final String value =
        this.properties.getProperty(PRINT_STACK_TRACE_KEY,
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
   * Test if AWS multipart upload is enabled.
   * @return true if the multipart upload is enabled
   */
  public boolean isAWSMultipartUpload() {

    final String value =
        this.properties.getProperty(AWS_UPLOAD_MULTIPART,
            Boolean.toString(Globals.AWS_UPLOAD_MULTIPART_DEFAULT));

    return Boolean.valueOf(value);
  }

  /**
   * Test if RServe is enabled.
   * @return true if the RServe server is enabled
   */
  public boolean isRServeServerEnabled() {

    return getBooleanSetting(RSERVE_ENABLED_KEY);
  }

  /**
   * Get the RServe server name.
   * @return The name of the RServe to use
   */
  public String getRServeServername() {

    return this.properties.getProperty(RSERVE_SERVER_NAME_KEY);
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
   * Test if design must be obfuscated
   * @return true if design must be obfuscated
   */
  public boolean isObfuscateDesign() {

    return Boolean.parseBoolean(this.properties
        .getProperty(OBFUSCATE_DESIGN_KEY));
  }

  /**
   * Test if replicate information must be removed from design.
   * @return true if replicate information must be removed
   */
  public boolean isRemoveReplicateInfo() {

    return Boolean.parseBoolean(this.properties
        .getProperty(REMOVE_REPLICATE_INFO_KEY));
  }

  /**
   * Get the number of threads to use in Steps computation in local mode.
   * @return the number of threads to use
   */
  public int getLocalThreadsNumber() {

    return Integer.parseInt(this.properties.getProperty(LOCAL_THREADS_NUMBER,
        "0"));
  }

  /**
   * Get the PHRED offset default value.
   * @return the PHRED offset default value
   */
  public int getPhredOffsetDefault() {

    return Integer.parseInt(this.properties.getProperty(
        PHRED_OFFSET_DEFAULT_KEY,
        Integer.toString(Globals.PHRED_OFFSET_DEFAULT)));
  }

  /**
   * Test if the platform checking must be avoided at Eoulsan startup.
   * @return true if the platform checking must be avoided
   */
  public boolean isBypassPlatformChecking() {

    return Boolean.parseBoolean(this.properties
        .getProperty(BYPASS_PLATFORM_CHECKING_KEY));
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

    final String value = getSetting(settingName);
    if (value == null)
      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: " + value);

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

    final String value = getSetting(settingName);
    if (value == null)
      throw new EoulsanException(
          "Invalid parameter, an integer parameter is need for "
              + settingName + " parameter: " + value);

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
   * @throws EoulsanException if the value is not an integer
   */
  public boolean getBooleanSetting(final String settingName) {

    return Boolean.parseBoolean(getSetting(settingName));
  }

  /**
   * Get a set of settings names.
   * @return a set with all the name of the settings
   */
  public Set<String> getSettingsNames() {

    final Set<String> result = new HashSet<String>();

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
   * Set if the AWS multipart upload mode is enabled.
   * @param multipartUpload true if the multipart upload is enabled
   */
  public void setAWSMultipartUpload(final boolean multipartUpload) {

    this.properties.setProperty(AWS_UPLOAD_MULTIPART,
        Boolean.toString(multipartUpload));
  }

  /**
   * Set if RServe is enabled.
   * @param enable true if the RServe server is enable
   */
  public void setRServeServerEnabled(final boolean enable) {

    this.properties.setProperty(RSERVE_ENABLED_KEY, Boolean.toString(enable));
  }

  /**
   * Set the RServe server name.
   * @param serverName The name of the RServe to use
   */
  public void setRServeServername(final String serverName) {

    this.properties.setProperty(RSERVE_SERVER_NAME_KEY, serverName);
  }

  /**
   * Set the RServe server name.
   * @param serverName The name of the RServe to use
   */
  public void setTempDirectory(final String tempDirectory) {

    if (tempDirectory != null) {
      this.properties.setProperty(TMP_DIR_KEY, tempDirectory);
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
  public void setRemoveDesignInfo(final boolean remove) {

    this.properties.setProperty(REMOVE_REPLICATE_INFO_KEY,
        Boolean.toString(remove));
  }

  /**
   * Set the number of threads to use in local mode.
   * @param threadsNumber the number of threads to use in local mode
   */
  public void setLocalThreadsNumber(final int threadsNumber) {

    if (threadsNumber < 0)
      return;

    this.properties.setProperty(LOCAL_THREADS_NUMBER,
        Integer.toString(threadsNumber));
  }

  /**
   * Set the PHRED offset default value.
   * @param phredOffset the value to set
   */
  public void setPhredOffsetDefault(final int phredOffset) {

    this.properties.setProperty(PHRED_OFFSET_DEFAULT_KEY,
        Integer.toString(phredOffset));
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
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public void setSetting(final String settingName, final String settingValue) {

    if (settingName == null || settingValue == null) {
      return;
    }

    final String key = settingName.toLowerCase();

    if (FORBIDDEN_KEYS.contains(key)) {
      return;
    }

    if ("main.accesskey".equals(key)) {
      setSetting(AWS_ACCESS_KEY, settingValue);
      return;
    }

    if ("main.awssecretkey".equals(key)) {
      setSetting(AWS_SECRET_KEY, settingValue);
      return;
    }

    this.properties.setProperty(key, settingValue);
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

    final FileOutputStream fos = new FileOutputStream(file);

    this.properties.store(fos, " "
        + Globals.APP_NAME + " version " + Globals.APP_VERSION_STRING
        + " configuration file");
    fos.close();
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
      LOGGER.config("No configuration file found.");
    }
  }

  /**
   * Load application options.
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings(final File file) throws IOException,
      EoulsanException {

    LOGGER.info("Load configuration file: " + file.getAbsolutePath());
    final FileInputStream fis = new FileInputStream(file);

    this.properties.load(fis);
    fis.close();

    for (String key : this.properties.stringPropertyNames()) {
      if (FORBIDDEN_KEYS.contains(key)) {
        throw new EoulsanException("Forbiden key found in configuration file: "
            + key);
      }

      if ("main.accesskey".equals(key.toLowerCase()))
        throw new EoulsanException("main.accesskey key in now invalid. Use "
            + AWS_ACCESS_KEY + " key instead.");

      if ("main.awssecretkey".equals(key.toLowerCase()))
        throw new EoulsanException("main.awssecretkey key in now invalid. Use "
            + AWS_SECRET_KEY + " key instead.");

    }

  }

  private void init() {

    LOGGER.info(Globals.WELCOME_MSG);
    LOGGER.info("System temp directory: "
        + System.getProperty("java.io.tmpdir"));
    setTempDirectory(System.getProperty("java.io.tmpdir"));
  }

  /**
   * Add all the settings to the log.
   */
  public void logSettings() {

    for (Object key : properties.keySet()) {

      final String sKey = (String) key;
      final String sValue = properties.getProperty(sKey);

      if (sKey.equals(HADOOP_AWS_ACCESS_KEY)
          || sKey.endsWith((HADOOP_AWS_SECRET_KEY)))
        LOGGER.info("Setting: " + sKey + "=xxxx value not shown xxxx");
      else
        LOGGER.info("Setting: " + sKey + "=" + sValue);
    }

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
  Settings(final boolean loadDefaultConfigurationFile) throws IOException,
      EoulsanException {

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
