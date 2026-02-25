package fr.ens.biologie.genomique.eoulsan.data.protocols;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class define a deprecated protocol.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class DeprecatedDataProtocol extends AbstractDataProtocol {

  private final String newName;

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    throwException();
    return null;
  }

  @Override
  public OutputStream putData(final DataFile dest) throws IOException {

    throwException();
    return null;
  }

  @Override
  public boolean exists(final DataFile src, boolean followLink) {
    return false;
  }

  @Override
  public DataFileMetadata getMetadata(final DataFile src) throws IOException {

    throwException();
    return null;
  }

  @Override
  public boolean canRead() {
    return false;
  }

  @Override
  public boolean canWrite() {
    return false;
  }

  private void throwException() throws IOException {

    if (newName != null) {
      throw new IOException(
          "The \""
              + getName()
              + "\" protocol is now deprecated. "
              + "Please use the \""
              + newName
              + "\" protocol instead");
    }

    throw new IOException("The \"" + getName() + "\" protocol is now deprecated.");
  }

  //
  // Constructor
  //

  /** Constructor. */
  protected DeprecatedDataProtocol() {

    this.newName = null;
  }

  /**
   * Constructor.
   *
   * @param newName new protocol name.
   */
  protected DeprecatedDataProtocol(final String newName) {

    if (newName == null) {
      throw new NullPointerException("newName cannot be null");
    }

    this.newName = newName;
  }
}
