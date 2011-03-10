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
  private static final String AWS_ACCESS_KEY = MAIN_PREFIX_KEY + "accessKey";
  private static final String AWS_SECRET_KEY = MAIN_PREFIX_KEY + "awssecretkey";

  private static final String PRINT_STACK_TRACE_KEY =
      MAIN_PREFIX_KEY + "printstacktrace";

  public static final String TMP_DIR_KEY = MAIN_PREFIX_KEY + "tmp.dir";

  private static final String HADOOP_AWS_ACCESS_KEY =
      "hadoop.conf.fs.s3n.awsAccessKeyId";
  private static final String HADOOP_AWS_SECRET_KEY =
      "hadoop.conf.fs.s3n.awsSecretAccessKey";

  public static final String RSERVE_ENABLED_KEY =
      MAIN_PREFIX_KEY + "rserve.enable";
  public static final String RSERVE_SERVER_NAME_KEY =
      MAIN_PREFIX_KEY + "rserve.servername";

  private static final String OBFUSCATE_DESIGN_KEY =
      MAIN_PREFIX_KEY + "design.obfuscate";
  private static final String REMOVE_REPLICATE_INFO_KEY =
      MAIN_PREFIX_KEY + "design.remove.replicate.info";

  private static final Set<String> FORBIDDEN_KEYS =
      Utils.unmodifiableSet(new String[] {HADOOP_AWS_ACCESS_KEY,
          HADOOP_AWS_SECRET_KEY});

  //
  // Getters
  //

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
        this.properties.getProperty(PRINT_STACK_TRACE_KEY, Boolean
            .toString(Globals.PRINT_STACK_TRACE_DEFAULT));

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
   * Test if RServe is enabled.
   * @return true if the RServe server is enable
   */
  public boolean isRServeServerEnabled() {

    return Boolean
        .parseBoolean(this.properties.getProperty(RSERVE_ENABLED_KEY));
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

    return this.properties.getProperty(TMP_DIR_KEY);
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

    this.properties.setProperty(PRINT_STACK_TRACE_KEY, Boolean
        .toString(printStackTrace));
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

    this.properties.setProperty(OBFUSCATE_DESIGN_KEY, Boolean
        .toString(obfuscate));
  }

  /**
   * Set if the replicate information must be removed from the design.
   * @param remove true if the replicate information must be remove
   */
  public void setRemoveDesignInfo(final boolean remove) {

    this.properties.setProperty(REMOVE_REPLICATE_INFO_KEY, Boolean
        .toString(remove));
  }

  /**
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public void setSetting(final String settingName, final String settingValue) {

    if (settingName == null
        || settingValue == null || FORBIDDEN_KEYS.contains(settingName)) {
      return;
    }

    this.properties.setProperty(settingName, settingValue);
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
    }

  }

  private void init() {

    setTempDirectory(System.getProperty("java.io.tmpdir"));
  }

  //
  // Constructor
  //

  /**
   * Public constructor. Load application options.
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public Settings() throws IOException, EoulsanException {

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
  public Settings(final File file) throws IOException, EoulsanException {

    init();
    loadSettings(file);
  }

}
