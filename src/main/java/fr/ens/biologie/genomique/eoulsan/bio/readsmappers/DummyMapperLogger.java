package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

/**
 * This class implements a dummy MapperLogger
 * @author Laurent Jourdren
 * @since 2.6
 */
public class DummyMapperLogger implements MapperLogger {

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
  public void flush() {
  }

  @Override
  public void close() {
  }

}
