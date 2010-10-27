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

package fr.ens.transcriptome.eoulsan;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import fr.ens.transcriptome.eoulsan.util.Version;

public class Globals {

  private static Properties manifestProperties;
  private static final String MANIFEST_PROPERTIES_FILE = "/manifest.txt";

  /** The name of the application. */
  public static final String APP_NAME = "Eoulsan";

  /** The name of the application. */
  public static final String APP_NAME_LOWER_CASE = APP_NAME.toLowerCase();

  /** The prefix of the parameters of the application. */
  public static final String PARAMETER_PREFIX =
      "fr.ens.transcriptome." + APP_NAME_LOWER_CASE;

  /** The version of the application. */
  public static final String APP_VERSION_STRING = getVersion();
  
  /** The version of the application. */
  public static final Version APP_VERSION = new Version(APP_VERSION_STRING);

  /** The built number of the application. */
  public static final String APP_BUILD_NUMBER = getBuiltNumber();

  /** The build date of the application. */
  public static final String APP_BUILD_DATE = getBuiltDate();

  /** The prefix for temporary files. */
  public static final String TEMP_PREFIX =
      APP_NAME_LOWER_CASE + "-" + APP_VERSION_STRING + "-" + APP_BUILD_NUMBER + "-";

  /** The log level of the application. */
  public static final Level LOG_LEVEL = Level.INFO; // Level.OFF;

  /** Set the debug mode. */
  public static final boolean DEBUG = false;

  // Platforms where Eoulsan is available
  public static final String[][] AVAILABLE_BINARY_ARCH =
      { {"linux", "amd64"}, {"linux", "x86_64"}};

  // Platforms alias
  public static final String[][] AVAILABLE_BINARY_ARCH_ALIAS =
      {{"linux", "x86_64", "linux", "amd64"}};

  /** Format of the log. */
  public static final Formatter LOG_FORMATTER = new Formatter() {

    private final DateFormat df = new SimpleDateFormat("yyyy.MM.dd kk:mm:ss");

    public String format(final LogRecord record) {
      return record.getLevel()
          + "\t" + df.format(new Date(record.getMillis())) + "\t"
          + record.getMessage() + "\n";
    }
  };

  private static final String WEBSITE_URL_DEFAULT =
      "http://transcriptome.ens.fr/" + APP_NAME_LOWER_CASE;

  /** Teolenn Website url. */
  public static final String WEBSITE_URL = getWebSiteURL();

  private static final String COPYRIGHT_DATE = "2010";

  /** Licence text. */
  public static final String LICENSE_TXT =
      "This program is developed under the GNU General Public License version 2 or later and CeCILL.";

  /** About string, plain text version. */
  public static final String ABOUT_TXT =
      Globals.APP_NAME
          + " version " + Globals.APP_VERSION_STRING + " (" + Globals.APP_BUILD_NUMBER
          + ")" + " is a pipeline for RNAseq analysis.\n"
          + "This version has been built on " + APP_BUILD_DATE + ".\n\n"
          + "Authors:\n" + "  Laurent Jourdren <jourdren@biologie.ens.fr>\n"
          + "  Maria Bernard <mbernard@biologie.ens.fr>\n"
          + "  Stéphane Le Crom <lecrom@biologie.ens.fr>\n" + "Contacts:\n"
          + "  Mail: " + APP_NAME_LOWER_CASE + "@biologie.ens.fr\n"
          + "  Google group: http://groups.google.com/group/"
          + APP_NAME_LOWER_CASE + "\n" + "Copyright " + COPYRIGHT_DATE
          + " École Normale Supérieure microarray platform.\n" + LICENSE_TXT
          + "\n";

  /** Default standard output state. */
  public static final boolean STD_OUTPUT_DEFAULT = false;

  /** Design file version. */
  public static final double DESIGN_FILE_VERSION = 1.1;

  public static final String DEFAULT_FILE_ENCODING = "UTF-8";

  /** Default locale of the application. */
  public static final Locale DEFAULT_LOCALE = Locale.US;

  //
  // Methods
  //

  private static String getVersion() {

    String s = getManifestProperty("Specification-Version");

    return s != null ? s : "UNKNOWN_VERSION";
  }

  private static String getBuiltNumber() {

    String s = getManifestProperty("Implementation-Version");

    return s != null ? s : "UNKNOWN_BUILD";
  }

  private static String getBuiltDate() {

    final String unknown = "UNKNOWN_DATE";

    String s = getManifestProperty("Built-Date");

    return s != null ? s : unknown;
  }

  private static String getWebSiteURL() {

    String s = getManifestProperty("url");

    return s != null ? s : WEBSITE_URL_DEFAULT;
  }

  private static String getManifestProperty(final String propertyKey) {

    if (propertyKey == null)
      return null;

    readManifest();

    return manifestProperties.getProperty(propertyKey);
  }

  private static synchronized void readManifest() {

    if (manifestProperties != null)
      return;

    try {
      manifestProperties = new Properties();

      InputStream is =
          Globals.class.getResourceAsStream(MANIFEST_PROPERTIES_FILE);

      if (is == null)
        return;

      manifestProperties.load(is);
    } catch (IOException e) {
    }
  }

  /**
   * Set the default Local of the application
   */
  public static void setDefaultLocale() {
    
    Locale.setDefault(DEFAULT_LOCALE);
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private Globals() {
  }
}
