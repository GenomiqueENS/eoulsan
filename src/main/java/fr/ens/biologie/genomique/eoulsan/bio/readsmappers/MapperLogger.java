package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

/**
 * This interface define a MapperLogger
 * @author Laurent Jourdren
 * @since 2.6
 */
public interface MapperLogger {

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
   * Flush log entries.
   */
  void flush();

  /**
   * Close the logger
   */
  void close();

}
