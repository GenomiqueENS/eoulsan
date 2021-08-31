package fr.ens.biologie.genomique.eoulsan.log;

/**
 * This class implements a dummy logger.
 * @author Laurent Jourdren
 * @since 2.6
 */
public class DummyLogger implements GenericLogger {

  @Override
  public void debug(String message) {
  }

  @Override
  public void info(String message) {
  }

  @Override
  public void warn(String message) {
  }

  @Override
  public void error(String message) {
  }

  @Override
  public void error(Throwable exception) {
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
  }

}
