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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class allow to change the logger name for all Eoulsan classes. The
 * setLoggetName() method must be called before any other Eoulsan method.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EoulsanLogger {

  private static String loggerName = Globals.APP_NAME;

  private static Map<String, Logger> threadGroupLoggers = new HashMap<>();

  private static Logger logger;

  /**
   * Get the logger object.
   * @return a logger object for Eoulsan
   */
  public static Logger getLogger() {

    // Search Thread logger only if thread logger has been registered
    if (!threadGroupLoggers.isEmpty()) {

      ThreadGroup tg = Thread.currentThread().getThreadGroup();
      do {

        if (threadGroupLoggers.containsKey(tg.getName())) {
          return threadGroupLoggers.get(tg.getName());
        }

        tg = tg.getParent();
      } while (tg != null);
    }

    // Keep a reference of the Logger to avoid disappearance of the Handlers.
    // LogManager only keeps weak references to the Loggers it creates.
    if (logger == null) {
      logger = Logger.getLogger(loggerName);
    }

    return logger;
  }

  /**
   * Set the logger name.
   * @param newLoggerName the new logger name
   */
  public static void setLoggerName(final String newLoggerName) {

    if (newLoggerName == null) {
      throw new NullPointerException("New logger name is null");
    }

    loggerName = newLoggerName;
    logger = null;
  }

  /**
   * Get the logger name.
   * @return the logger name
   */
  public static String getLoggerName() {

    return loggerName;
  }

  /**
   * Register a logger for a thread group.
   * @param threadGroup thread group
   * @param logger logger to register
   */
  public static void registerThreadGroupLogger(final ThreadGroup threadGroup,
      final Logger logger) {

    if (threadGroup == null || logger == null) {
      return;
    }

    threadGroupLoggers.put(threadGroup.getName(), logger);
  }

  /**
   * Remove a logger for a thread group.
   * @param threadGroup thread group
   */
  public static void removeThreadGroupLogger(final ThreadGroup threadGroup) {

    if (threadGroup == null) {
      return;
    }

    threadGroupLoggers.remove(threadGroup.getName());
  }

  /**
   * Initialize the console logger handler for Hadoop mappers and reducers. This
   * method set the Eoulsan logger format and define the logger level.
   */
  public static void initConsoleHandler() {
    initConsoleHandler(null);
  }

  /**
   * Initialize the console logger handler for Hadoop mappers and reducers.
   * @param level log level to use
   */
  public static void initConsoleHandler(final Level level) {

    // Disable parent Handler
    getLogger().setUseParentHandlers(false);

    // Create the new console handler
    final Handler handler = new ConsoleHandler();
    handler.setLevel(level != null ? level : Globals.LOG_LEVEL);
    handler.setFormatter(Globals.LOG_FORMATTER);

    // Add the handler to the logger
    getLogger().addHandler(handler);

    // Set the log level of the logger
    getLogger().setLevel(handler.getLevel());
  }

}
