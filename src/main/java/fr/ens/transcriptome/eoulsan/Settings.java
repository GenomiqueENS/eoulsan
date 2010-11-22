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
public class Settings {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static final String MAIN_PREFIX_KEY = "main.";
  private final Properties properties = new Properties();

  private static final String DEBUG_KEY = MAIN_PREFIX_KEY + "debug";
  private static final String AWS_ACCESS_KEY = MAIN_PREFIX_KEY + "accessKey";
  private static final String AWS_SECRET_KEY = MAIN_PREFIX_KEY + "awssecretkey";

  private static final String PRINT_STACK_TRACE_KEY =
      MAIN_PREFIX_KEY + "printstacktrace";

  private static final String HADOOP_AWS_ACCESS_KEY =
      "hadoop.conf.fs.s3n.awsAccessKeyId";
  private static final String HADOOP_AWS_SECRET_KEY =
      "hadoop.conf.fs.s3n.awsSecretAccessKey";

  private static final String RSERVE_ENABLED_KEY =
      MAIN_PREFIX_KEY + "rserve.enable";
  private static final String RSERVE_SERVER_NAME_KEY =
      MAIN_PREFIX_KEY + "rserve.servername";

  private static final Set<String> forbiddenKeys =
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
        this.properties.getProperty(DEBUG_KEY, "" + Globals.DEBUG);

    return Boolean.valueOf(value);
  }

  /**
   * Test is the debug mode is enabled.
   * @return true if the debug mode is enable
   */
  public boolean isPrintStackTrace() {

    final String value =
        this.properties.getProperty(PRINT_STACK_TRACE_KEY, ""
            + Globals.PRINT_STACK_TRACE_DEFAULT);

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
   * Get a setting value.
   * @return setting value as a String
   */
  public String getSetting(final String settingName) {

    if (settingName == null)
      return null;

    if (settingName.startsWith(MAIN_PREFIX_KEY))
      return null;

    return this.properties.getProperty(settingName);
  }

  /**
   * Get a set of settings names.
   * @return a set with all the name of the settings
   */
  public Set<String> getSettingsNames() {

    final Set<String> result = new HashSet<String>();

    for (String key : this.properties.stringPropertyNames())
      if (!key.startsWith(MAIN_PREFIX_KEY))
        result.add(key);

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

    if (value == null)
      return;

    this.properties.setProperty(AWS_ACCESS_KEY, value);
    this.properties.setProperty(HADOOP_AWS_ACCESS_KEY, value);
  }

  /**
   * Set the AWS secret key.
   * @param value the AWS secret key
   */
  public void setAWSSecretKey(final String value) {

    if (value == null)
      return;

    this.properties.setProperty(AWS_SECRET_KEY, value);
    this.properties.setProperty(HADOOP_AWS_SECRET_KEY, value);
  }

  /**
   * Set if RServe is enabled.
   * @param enable true if the RServe server is enable
   */
  public void setRServeServerEnabled(final boolean enable) {

    this.properties.setProperty(RSERVE_ENABLED_KEY, "" + enable);
  }

  /**
   * Set the RServe server name.
   * @param serverName The name of the RServe to use
   */
  public void setRServeServername(final String serverName) {

    this.properties.setProperty(RSERVE_SERVER_NAME_KEY, serverName);
  }

  /**
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public void setSetting(final String settingName, final String settingValue) {

    if (settingName == null
        || settingValue == null || forbiddenKeys.contains(settingName))
      return;

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

    if (os.toLowerCase().startsWith("windows"))
      return home
          + File.separator + "Application Data" + File.separator
          + Globals.APP_NAME_LOWER_CASE + ".conf";

    return home + File.separator + "." + Globals.APP_NAME_LOWER_CASE;
  }

  /**
   * Save application options
   * @throws IOException if an error occurs while writing results
   */
  public void saveSettings() throws IOException {

    saveSettings(new File(getConfigurationFilePath()));
  }

  /**
   * Save application options
   * @param file File to save.
   * @throws IOException if an error occurs while writing settings
   */
  public void saveSettings(final File file) throws IOException {

    FileOutputStream fos = new FileOutputStream(file);

    this.properties.store(fos, " "
        + Globals.APP_NAME + " version " + Globals.APP_VERSION_STRING
        + " configuration file");
    fos.close();
  }

  /**
   * Load application options
   * @throws IOException if an error occurs while reading settings
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings() throws IOException, EoulsanException {

    final File confFile = new File(getConfigurationFilePath());
    if (!confFile.exists())
      logger.config("No configuration file found.");
    else
      loadSettings(confFile);
  }

  /**
   * Load application options
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   * @throws EoulsanException if an invalid key is found in configuration file
   */
  public void loadSettings(final File file) throws IOException,
      EoulsanException {

    logger.info("Load configuration file: " + file.getAbsolutePath());
    FileInputStream fis = new FileInputStream(file);

    this.properties.load(fis);
    fis.close();

    for (String key : this.properties.stringPropertyNames())
      if (forbiddenKeys.contains(key)) {
        throw new EoulsanException("Forbiden key found in configuration file: "
            + key);
      }

  }

  //
  // Default values
  //

  private void init() {

    this.properties.setProperty(AWS_ACCESS_KEY, "AKIAJPXBAOLESJ2TOABA");
    this.properties.setProperty(AWS_SECRET_KEY,
        "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");

    this.properties.setProperty("hadoop.conf.fs.ftp.user.hestia.ens.fr",
        "anonymous");
    this.properties.setProperty("hadoop.conf.fs.ftp.password.hestia.ens.fr",
        "toto@toto.com");

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

    if (!loadDefaultConfigurationFile) {
      init();
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
