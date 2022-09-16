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

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.io.FileCharsets;
import fr.ens.biologie.genomique.kenetre.bio.FastqFormat;

/**
 * This class contains globals constants for the application.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class Globals {

  private static Attributes manifestAttributes;
  private static final String MANIFEST_FILE = "/META-INF/MANIFEST.MF";

  /** The name of the application. */
  public static final String APP_NAME = "Eoulsan";

  /** The name of the application. */
  public static final String APP_NAME_LOWER_CASE = APP_NAME.toLowerCase();

  /** The prefix of the parameters of the application. */
  public static final String PARAMETER_PREFIX =
      "fr.ens.biologie.genomique." + APP_NAME_LOWER_CASE;

  /** The version of the application. */
  public static final String APP_VERSION_STRING = getVersion();

  /** The version of the application. */
  public static final Version APP_VERSION = new Version(APP_VERSION_STRING);

  /** The built number of the application. */
  public static final String APP_BUILD_NUMBER = getBuiltNumber();

  /** The built commit of the application. */
  public static final String APP_BUILD_COMMIT = getBuiltCommit();

  /** The built host of the application. */
  public static final String APP_BUILD_HOST = getBuiltHost();

  /** The build date of the application. */
  public static final String APP_BUILD_DATE = getBuiltDate();

  /** The build year of the application. */
  public static final String APP_BUILD_YEAR = getBuiltYear();

  /** The welcome message. */
  public static final String WELCOME_MSG = Globals.APP_NAME
      + " version " + Globals.APP_VERSION_STRING + " (" + APP_BUILD_COMMIT
      + ", " + Globals.APP_BUILD_NUMBER + " build on " + APP_BUILD_HOST + ", "
      + Globals.APP_BUILD_DATE + ")";

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

  /** Minimal java version required by Eoulsan. */
  public static final int MINIMAL_JAVA_VERSION_REQUIRED = 8;

  /** Platforms where the application is available. */
  public static final Set<String> AVAILABLE_BINARY_ARCH =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList("linux\tamd64", "linux\tx86_64")));

  /** Platforms alias. */
  public static final Map<String, String> AVAILABLE_BINARY_ARCH_ALIAS =
      Collections.unmodifiableMap(
          Collections.singletonMap("linux\tx86_64", "linux\tamd64"));

  /** Default locale of the application. */
  public static final Locale DEFAULT_LOCALE = Locale.US;

  /** Format of the log. */
  public static final Formatter LOG_FORMATTER = new Formatter() {

    private final DateFormat df =
        new SimpleDateFormat("yyyy.MM.dd kk:mm:ss", DEFAULT_LOCALE);

    @Override
    public String format(final LogRecord record) {
      return record.getLevel()
          + "\t" + this.df.format(new Date(record.getMillis())) + "\t"
          + record.getMessage() + "\n";
    }
  };

  private static final String WEBSITE_URL_DEFAULT =
      "http://outils.genomique.biologie.ens.fr/" + APP_NAME_LOWER_CASE;

  /** Application Website url. */
  public static final String WEBSITE_URL = getWebSiteURL();

  /** Project email. */
  public static final String CONTACT_EMAIL =
      APP_NAME_LOWER_CASE + "@biologie.ens.fr";

  /** Project discussion group. */
  public static final String DISCUSSION_GROUP =
      "http://groups.google.com/group/" + APP_NAME_LOWER_CASE;

  private static final String COPYRIGHT_DATE = "2010-" + APP_BUILD_YEAR;

  /** Licence text. */
  public static final String LICENSE_TXT =
      "This program is developed under the GNU Lesser General Public License"
          + " version 2.1 or later and CeCILL-C.";

  /** About string, plain text version. */
  public static final String ABOUT_TXT = Globals.APP_NAME
      + " version " + Globals.APP_VERSION_STRING + " (" + APP_BUILD_COMMIT
      + ", " + Globals.APP_BUILD_NUMBER + ")"
      + " is a pipeline for NGS analysis.\n" + "This version has been built on "
      + APP_BUILD_DATE + ".\n\n" + "Authors:\n"
      + "  Laurent Jourdren (Project leader and maintainer)\n"
      + "  Maria Bernard\n" + "  Stéphane Le Crom\n" + "  Claire Wallon\n"
      + "  Vivien Deshaies\n" + "  Sandrine Perrin\n" + "  Xavier Bauquet\n"
      + "  Cyril Firmo\n" + "  Runxin Du\n" + "  Aurélien Birer\n"
      + "Contacts:\n" + "  Email: " + CONTACT_EMAIL + "\n"
      + "  Discussion group: " + DISCUSSION_GROUP + "\n" + "  Website: "
      + WEBSITE_URL + "\n" + "Copyright " + COPYRIGHT_DATE
      + " IBENS genomics core facility\n" + LICENSE_TXT + "\n";

  /** Default standard output state. */
  public static final boolean STD_OUTPUT_DEFAULT = false;

  /** Design file version. */
  public static final double DESIGN_FILE_VERSION = 1.1;

  /**
   * The name of the system property that contains the list of libraries to
   * repack for hadoop mode.
   */
  public static final String LIBS_TO_HADOOP_REPACK_PROPERTY =
      APP_NAME_LOWER_CASE + ".hadoop.libs";

  /** Launch mode property. */
  public static final String LAUNCH_MODE_PROPERTY =
      APP_NAME_LOWER_CASE + ".launch.mode";

  /** Launch script path. */
  public static final String LAUNCH_SCRIPT_PATH =
      APP_NAME_LOWER_CASE + ".launch.script.path";

  /** Print stack trace default. */
  public static final boolean PRINT_STACK_TRACE_DEFAULT = DEBUG;

  /** Default UI name. */
  public static final String UI_NAME_DEFAULT = "lanterna";

  /** FASTA file width. */
  public static final int FASTA_FILE_WIDTH = 60;

  /** Default fastq format. */
  static final FastqFormat FASTQ_FORMAT_DEFAULT = FastqFormat.FASTQ_SANGER;

  /** Obfuscate design default. */
  public static final boolean OBFUSCATE_DESIGN_DEFAULT = true;

  /** Remove design replicate info when obfuscate design default. */
  public static final boolean OBFUSCATE_DESIGN_REMOVE_REPLICATE_INFO_DEFAULT =
      true;

  /** ZooKeeper default port. */
  public static final int ZOOKEEPER_DEFAULT_PORT_DEFAULT = 2181;

  /** ZooKeeper default session timeout. */
  public static final int ZOOKEEPER_SESSION_TIMEOUT_DEFAULT = 10000;

  /** Write results using old Eoulsan format. */
  public static final boolean USE_OLD_EOULSAN_RESULT_FORMAT_DEFAULT = false;

  /** Eoulsan log filename. */
  public static final String LOG_FILENAME = APP_NAME_LOWER_CASE + ".log";

  /** Other log filename. */
  public static final String OTHER_LOG_FILENAME = "additional.log";

  /** Output tree type. */
  public static final String OUTPUT_TREE_TYPE_DEFAULT = "step";

  /** Save workflow image. */
  public static final boolean SAVE_WORKFLOW_IMAGE_DEFAULT = true;

  /** Enable standard external modules. */
  public static final boolean STANDARD_EXTERNAL_MODULES_ENABLED_DEFAULT = true;

  /** Server name to check internet connection. */
  public static final String INTERNET_CHECK_SERVER =
      "raw.githubusercontent.com";

  /** Port of the server to check internet connection */
  public static final int INTERNET_CHECK_PORT = 443;

  /** Eoulsan tools website URL. */
  public static final String EOULSAN_TOOLS_WEBSITE_URL =
      "https://raw.githubusercontent.com/GenomicParisCentre/eoulsan-tools";

  //
  // Files encoding
  //

  /** Default file encoding. */
  public static final String DEFAULT_FILE_ENCODING =
      FileCharsets.UTF8_FILE_ENCODING;

  /** Default charset. */
  public static final Charset DEFAULT_CHARSET =
      Charset.forName(DEFAULT_FILE_ENCODING);

  //
  // Default file extensions
  //

  /** Log extension. */
  public static final String LOG_EXTENSION = ".log";

  /** Step log extension. */
  public static final String STEP_LOG_EXTENSION = ".step.log";

  /** Step result extension. */
  public static final String STEP_RESULT_EXTENSION = ".step.result";

  /** Old step result format extension. */
  public static final String STEP_RESULT_OLD_FORMAT_EXTENSION = ".log";

  /** Task context extension. */
  public static final String TASK_CONTEXT_EXTENSION = ".task.context";

  /** Task result extension. */
  public static final String TASK_RESULT_EXTENSION = ".task.result";

  /** Task done extension. */
  public static final String TASK_LOG_EXTENSION = ".task.log";

  /** Task output data extension. */
  public static final String TASK_DATA_EXTENSION = ".task.data";

  /** Task done extension. */
  public static final String TASK_DONE_EXTENSION = ".task.done";

  /** Task stdout extension. */
  public static final String TASK_STDOUT_EXTENSION = ".task.out";

  /** Task stderr extension. */
  public static final String TASK_STDERR_EXTENSION = ".task.err";

  /** Task jobb id extension. */
  public static final String TASK_JOB_ID = ".task.job.id";

  /** Step output directory suffix. */
  public static final String STEP_OUTPUT_DIRECTORY_SUFFIX = "_output";

  //
  // Private constants
  //

  private static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";
  private static final String UNKNOWN_BUILD = "UNKNOWN_BUILD";
  private static final String UNKNOWN_DATE = "UNKNOWN_DATE";
  private static final String UNKNOWN_YEAR = "UNKNOWN_YEAR";
  private static final String UNKNOWN_BUILD_COMMIT = "UNKNOWN_COMMIT";
  private static final String UNKNOWN_BUILD_HOST = "UNKNOWN_HOST";

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

  private static String getBuiltYear() {

    final String builtYear = getManifestProperty("Built-Year");

    return builtYear != null ? builtYear : UNKNOWN_YEAR;
  }

  private static String getWebSiteURL() {

    final String url = getManifestProperty("url");

    return url != null ? url : WEBSITE_URL_DEFAULT;
  }

  private static String getBuiltCommit() {

    final String buildCommit = getManifestProperty("Built-Commit");

    return buildCommit != null ? buildCommit : UNKNOWN_BUILD_COMMIT;
  }

  private static String getBuiltHost() {

    final String buildHost = getManifestProperty("Built-Host");

    return buildHost != null ? buildHost : UNKNOWN_BUILD_HOST;
  }

  private static String getManifestProperty(final String propertyKey) {

    if (propertyKey == null) {
      return null;
    }

    readManifest();

    if (manifestAttributes == null) {
      return null;
    }

    return manifestAttributes.getValue(propertyKey);
  }

  private static synchronized void readManifest() {

    if (manifestAttributes != null) {
      return;
    }

    try {

      Class<?> clazz = Globals.class;
      String className = clazz.getSimpleName() + ".class";
      String classPath = clazz.getResource(className).toString();

      final String manifestPath;
      if (!classPath.startsWith("jar")) {
        // Class not from JAR

        String basePath = classPath.substring(0,
            classPath.length() - clazz.getName().length() - ".class".length());
        manifestPath = basePath + MANIFEST_FILE;

      } else {
        manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
            + MANIFEST_FILE;
      }

      Manifest manifest = new Manifest(new URL(manifestPath).openStream());
      manifestAttributes = manifest.getMainAttributes();

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
