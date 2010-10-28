package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class define a settings class.
 * @author Laurent Jourdren
 */
public class Settings {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);
  private static final String MAIN_PREFIX_KEY = "main.";
  private final Properties properties = new Properties();

  private static final String DEBUG_KEY = MAIN_PREFIX_KEY + "debug";
  private static final String PRINT_STACK_TRACE_KEY =
      MAIN_PREFIX_KEY + "printstacktrace";

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

    return Boolean.getBoolean(value);
  }

  /**
   * Test is the debug mode is enabled.
   * @return true if the debug mode is enable
   */
  public boolean isPrintStackTrace() {

    final String value =
        this.properties.getProperty(PRINT_STACK_TRACE_KEY, ""
            + Globals.PRINT_STACK_TRACE_DEFAULT);

    return Boolean.getBoolean(value);
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
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public void setSetting(final String settingName, final String settingValue) {

    if (settingName == null || settingValue == null)
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
   */
  public void loadSettings() throws IOException {

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
   */
  public void loadSettings(final File file) throws IOException {

    logger.info("Load configuration file: " + file.getAbsolutePath());
    FileInputStream fis = new FileInputStream(file);

    this.properties.load(fis);
    fis.close();
  }

  //
  // Default values
  //

  private void init() {

    this.properties.setProperty("hadoop.conf.fs.s3n.awsAccessKeyId",
        "AKIAJPXBAOLESJ2TOABA");
    this.properties.setProperty("hadoop.conf.fs.s3n.awsSecretAccessKey",
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
   */
  public Settings() throws IOException {

    init();
    loadSettings();

  }

  /**
   * Public constructor. Load application options.
   * @param file file to save
   * @throws IOException if an error occurs while reading the file
   */
  public Settings(final File file) throws IOException {

    init();
    loadSettings(file);
  }

}
