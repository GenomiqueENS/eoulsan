package fr.ens.biologie.genomique.eoulsan.design.io;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;

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
   * @throws FileNotFoundException if the file cannot be found
   */
  public DefaultDesignReader(final File file) throws FileNotFoundException {

    requireNonNull(file, "file argument cannot be null");

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while opening the file
   */
  public DefaultDesignReader(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    this.is = file.open();
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws IOException if an error occurs while reading the file
   */
  public DefaultDesignReader(final InputStream is) throws IOException {

    requireNonNull(is, "is argument cannot be null");

    this.is = is;
  }

  /**
   * Public constructor
   * @param filename File to read
   * @throws FileNotFoundException if the file doesn't exist
   */
  public DefaultDesignReader(final String filename)
      throws FileNotFoundException {

    requireNonNull(filename, "filename argument cannot be null");

    this.is = new FileInputStream(filename);
  }

}
