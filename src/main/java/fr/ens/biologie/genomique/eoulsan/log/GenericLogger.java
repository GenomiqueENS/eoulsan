package fr.ens.biologie.genomique.eoulsan.log;

/**
 * This interface define a generic logger for Eoulsan.
 * @author Laurent Jourdren
 * @since 2.6
 */
public interface GenericLogger {

  /**
   * Log a debug message.
   * @param message message to log
   */
  void debug(String message);

  /**
   * Log an info message.
   * @param message message to log
   */
  void info(String message);

  /**
   * Log a warning message.
   * @param message message to log
   */
  void warn(String message);

  /**
   * Log an error message.
   * @param message message to log
   */
  void error(String message);

  /**
   * Log an error message.
   * @param exception exception to log
   */
  default void error(Throwable exception) {

    error(exception != null ? exception.getMessage() : "exception is null");
  }

  /**
   * Flush log entries.
   */
  void flush();

  /**
   * Close the logger
   */
  void close();

}
