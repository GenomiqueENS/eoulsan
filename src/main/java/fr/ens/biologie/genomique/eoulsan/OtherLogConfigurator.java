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
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * This class allow to configure non Eoulsan log using Log4J.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class OtherLogConfigurator {

  /** Default Hadoop log level. */
  private static final Level DEFAULT_LOG_LEVEL = Level.INFO;

  //
  // Hadoop log configuration
  //

  /**
   * Configure Log4J. If no log level is provided, the INFO log level will be used.
   *
   * @param logLevel logLevel as string.
   * @param logFilename the log filename
   */
  public static void configureLog4J(final String logLevel, final String logFilename) {

    if (logFilename == null) {
      throw new NullPointerException("The logFilename argument cannot be null");
    }

    // This is the root logger provided by log4j
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.toLevel(logLevel, DEFAULT_LOG_LEVEL));

    // Remove all appenders
    rootLogger.removeAllAppenders();

    // Define log pattern layout
    final PatternLayout layout = new PatternLayout("%p\t%d{yyyy.MM.dd HH:mm:ss}\t%m\n");

    try {
      // Define file appender with layout and output log file name
      final RollingFileAppender fileAppender = new RollingFileAppender(layout, logFilename);

      // Add the appender to root logger
      rootLogger.addAppender(fileAppender);
    } catch (IOException e) {
      EoulsanLogger.getLogger().warning("Failed to add appender !!");
    }
  }
}
