package fr.ens.transcriptome.eoulsan.design.io;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This class allow to read a design file whatever the underlying design format.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DefaultDesignReader implements DesignReader {

  final InputStream is;

  @Override
  public Design read() throws IOException {

    try (final DesignFormatFinderInputStream dffis =
        new DesignFormatFinderInputStream(is)) {

      return dffis.getDesignReader().read();
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws FileNotFoundException
   */
  public DefaultDesignReader(final File file) throws FileNotFoundException {

    checkNotNull(file, "file argument cannot be null");

    this.is = new FileInputStream(file);

  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws IOException if the stream is null
   */
  public DefaultDesignReader(final InputStream is) throws IOException {

    checkNotNull(is, "is argument cannot be null");

    this.is = is;
  }

  /**
   * Public constructor
   * @param filename File to read
   * @throws IOException if the stream is null
   * @throws FileNotFoundException if the file doesn't exist
   */
  public DefaultDesignReader(final String filename)
      throws IOException, FileNotFoundException {

    checkNotNull(filename, "filename argument cannot be null");

    this.is = new FileInputStream(filename);
  }

}
