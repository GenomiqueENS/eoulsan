package fr.ens.transcriptome.eoulsan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class define a settings class.
 * @author Laurent Jourdren
 */
public class Settings {

  private static Logger logger = Logger.getLogger(Globals.APP_NAME);
  private static Properties properties = new Properties();

  static {
    properties.setProperty("hadoop.conf.fs.s3n.awsAccessKeyId",
        "AKIAJPXBAOLESJ2TOABA");
    properties.setProperty("hadoop.conf.fs.s3n.awsSecretAccessKey",
        "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");

    properties
        .setProperty("hadoop.conf.fs.ftp.user.hestia.ens.fr", "anonymous");
    properties.setProperty("hadoop.conf.fs.ftp.password.hestia.ens.fr",
        "toto@toto.com");
  }

  //
  // Getters
  //

  /**
   * Get a setting value.
   * @return setting value as a String
   */
  public static String getSetting(final String settingName) {

    return properties.getProperty(settingName);
  }

  /**
   * Get a set of settings names.
   * @return a set with all the name of the settings
   */
  public static Set<String> getSettingsNames() {

    return properties.stringPropertyNames();
  }

  //
  // Setters
  //

  /**
   * Set a setting value.
   * @param settingName name of the setting to set
   * @param settingValue value of the setting to set
   */
  public static void setSetting(final String settingName,
      final String settingValue) {

    if (settingName == null || settingValue == null)
      return;

    properties.setProperty(settingName, settingValue);
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
  public static void saveSettings() throws IOException {

    saveSettings(new File(getConfigurationFilePath()));
  }

  /**
   * Save application options
   * @param file File to save.
   * @throws IOException if an error occurs while writing settings
   */
  public static void saveSettings(final File file) throws IOException {

    FileOutputStream fos = new FileOutputStream(file);

    properties.store(fos, " "
        + Globals.APP_NAME + " version " + Globals.APP_VERSION_STRING
        + " configuration file");
    fos.close();
  }

  /**
   * Load application options
   * @throws IOException if an error occurs while reading settings
   */
  public static void loadSettings() throws IOException {

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
  public static void loadSettings(final File file) throws IOException {

    logger.info("Load configuration file: " + file.getAbsolutePath());
    FileInputStream fis = new FileInputStream(file);

    properties.load(fis);
    fis.close();
  }

}
