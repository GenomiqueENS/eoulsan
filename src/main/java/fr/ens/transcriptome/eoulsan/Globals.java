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

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.util.Version;

public final class Globals {

  private static Properties manifestProperties;
  private static final String MANIFEST_PROPERTIES_FILE = "/manifest.txt";

  /** The name of the application. */
  public static final String APP_NAME = "Eoulsan";

  /** The name of the application. */
  public static final String APP_NAME_LOWER_CASE = APP_NAME.toLowerCase();

  /** The prefix of the parameters of the application. */
  public static final String PARAMETER_PREFIX = "fr.ens.transcriptome."
      + APP_NAME_LOWER_CASE;

  /** The version of the application. */
  public static final String APP_VERSION_STRING = getVersion();

  /** The version of the application. */
  public static final Version APP_VERSION = new Version(APP_VERSION_STRING);

  /** The built number of the application. */
  public static final String APP_BUILD_NUMBER = getBuiltNumber();

  /** The build date of the application. */
  public static final String APP_BUILD_DATE = getBuiltDate();

  /** The welcome message. */
  public static final String WELCOME_MSG = Globals.APP_NAME
      + " version " + Globals.APP_VERSION_STRING + " ("
      + Globals.APP_BUILD_NUMBER + " on " + Globals.APP_BUILD_DATE + ")";

  /** The prefix for temporary files. */
  public static final String TEMP_PREFIX = APP_NAME_LOWER_CASE
      + "-" + APP_VERSION_STRING + "-" + APP_BUILD_NUMBER + "-";

  /** The log level of the application. */
  public static final Level LOG_LEVEL = Level.INFO; // Level.OFF;

  /** Set the debug mode. */
  public static final boolean DEBUG = APP_VERSION_STRING.endsWith("-SNAPSHOT")
      || "UNKNOWN_VERSION".equals(APP_VERSION_STRING);

  /** Bypass platform checking. */
  public static final boolean BYPASS_PLATFORM_CHECKING = false;

  /** Platforms where Eoulsan is available. */
  public static final Set<String> AVAILABLE_BINARY_ARCH = Collections
      .unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] {
          "linux\tamd64", "linux\tx86_64"})));

  /** Platforms alias. */
  public static final Map<String, String> AVAILABLE_BINARY_ARCH_ALIAS =
      Collections.unmodifiableMap(Collections.singletonMap("linux\tx86_64",
          "linux\tamd64"));

  /** Default locale of the application. */
  public static final Locale DEFAULT_LOCALE = Locale.US;

  /** Format of the log. */
  public static final Formatter LOG_FORMATTER = new Formatter() {

    private final DateFormat df = new SimpleDateFormat("yyyy.MM.dd kk:mm:ss",
        DEFAULT_LOCALE);

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

  private static final String COPYRIGHT_DATE = "2010-2011";

  /** Licence text. */
  public static final String LICENSE_TXT =
      "This program is developed under the GNU Lesser General Public License"
          + " version 2.1 or later and CeCILL-C.";

  /** About string, plain text version. */
  public static final String ABOUT_TXT = Globals.APP_NAME
      + " version " + Globals.APP_VERSION_STRING + " ("
      + Globals.APP_BUILD_NUMBER + ")"
      + " is a pipeline for RNAseq analysis.\n"
      + "This version has been built on " + APP_BUILD_DATE + ".\n\n"
      + "Authors:\n" + "  Laurent Jourdren <jourdren@biologie.ens.fr>\n"
      + "  Maria Bernard <mbernard@biologie.ens.fr>\n"
      + "  Stéphane Le Crom <lecrom@biologie.ens.fr>\n" + "Contacts:\n"
      + "  Mail: " + APP_NAME_LOWER_CASE + "@biologie.ens.fr\n"
      + "  Google group: http://groups.google.com/group/" + APP_NAME_LOWER_CASE
      + "\n" + "Copyright " + COPYRIGHT_DATE + " IBENS genomic platform\n"
      + LICENSE_TXT + "\n";

  /** Default standard output state. */
  public static final boolean STD_OUTPUT_DEFAULT = false;

  /** Design file version. */
  public static final double DESIGN_FILE_VERSION = 1.1;

  public static final String DEFAULT_FILE_ENCODING = "UTF-8";

  /**
   * The name of the system property that contains the list of libraries to
   * repack for hadoop mode.
   */
  public static final String LIBS_TO_HADOOP_REPACK_PROPERTY =
      "eoulsan.hadoop.libs";

  /** Launch mode property. */
  public static final String LAUNCH_MODE_PROPERTY = "eoulsan.launch.mode";

  /** Print stack trace default. */
  public static final boolean PRINT_STACK_TRACE_DEFAULT = DEBUG;

  /** AWS Multipart upload mode default. */
  public static final boolean AWS_UPLOAD_MULTIPART_DEFAULT = false;

  /** Default fastq format. */
  static final FastqFormat FASTQ_FORMAT_DEFAULT = FastqFormat.FASTQ_SANGER;

  //
  // Private constants
  //

  private static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";
  private static final String UNKNOWN_BUILD = "UNKNOWN_BUILD";
  private static final String UNKNOWN_DATE = "UNKNOWN_DATE";

  //
  // Methods
  //

  private static String getVersion() {

    final String version = getManifestProperty("Specification-Version");

    return version != null ? version : UNKNOWN_VERSION;
  }

  private static String getBuiltNumber() {

    final String builtNumber = getManifestProperty("Implementation-Version");

    return builtNumber != null ? builtNumber : UNKNOWN_BUILD;
  }

  private static String getBuiltDate() {

    final String builtDate = getManifestProperty("Built-Date");

    return builtDate != null ? builtDate : UNKNOWN_DATE;
  }

  private static String getWebSiteURL() {

    final String url = getManifestProperty("url");

    return url != null ? url : WEBSITE_URL_DEFAULT;
  }

  private static String getManifestProperty(final String propertyKey) {

    if (propertyKey == null) {
      return null;
    }

    readManifest();

    return manifestProperties.getProperty(propertyKey);
  }

  private static synchronized void readManifest() {

    if (manifestProperties != null) {
      return;
    }

    try {
      manifestProperties = new Properties();

      final InputStream is =
          Globals.class.getResourceAsStream(MANIFEST_PROPERTIES_FILE);

      if (is == null) {
        return;
      }

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

    throw new IllegalStateException();
  }
}
