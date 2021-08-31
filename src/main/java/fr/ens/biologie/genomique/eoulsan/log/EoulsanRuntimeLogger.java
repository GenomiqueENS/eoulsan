package fr.ens.biologie.genomique.eoulsan.log;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

/**
 * This class implement a logger using the Eoulsan standard logger.
 * @author Laurent Jourdren
 * @since 2.6
 */
public class EoulsanRuntimeLogger implements GenericLogger {

  @Override
  public void debug(String message) {
    getLogger().fine(message);
  }

  @Override
  public void info(String message) {
    getLogger().info(message);
  }

  @Override
  public void warn(String message) {
    getLogger().warning(message);
  }

  @Override
  public void error(String message) {
    getLogger().severe(message);
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
  }

}
