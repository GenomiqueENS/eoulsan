package fr.ens.biologie.genomique.eoulsan.design.io;

import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.SAMPLE_NUMBER_FIELD;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.DESIGN_FORMAT_VERSION_METADATA_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.EQUAL_SEPARATOR;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.biologie.genomique.eoulsan.Globals;

/**
 * This class allow to automatically detect the format of a design file.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DesignFormatFinderInputStream extends InputStream {

  private InputStream is;

  private static final int CACHE_SIZE = 5000;
  private static final int MAX_LINES_TO_READ = 100;

  private int versionFormat;
  private boolean testFormatDone;

  private int cacheIndex;
  private byte[] cache;

  /**
   * Find the design format version.
   * @return the design format version
   * @throws IOException if an error occurs while finding the version
   */
  private int findFormatVersion() throws IOException {

    final byte[] readed = new byte[CACHE_SIZE];

    int count = is.read(readed);

    if (count != CACHE_SIZE) {
      this.cache = new byte[count];
      System.arraycopy(readed, 0, this.cache, 0, count);
    } else {
      this.cache = readed;
    }

    try (final BufferedReader reader = new BufferedReader(new InputStreamReader(
        new ByteArrayInputStream(this.cache), Globals.DEFAULT_CHARSET))) {

      String line;
      int lineCount = 0;

      while (((line = reader.readLine()) != null)
          && lineCount < MAX_LINES_TO_READ) {

        line = line.trim();

        if ("".equals(line) || line.startsWith("#") || line.startsWith("[")) {
          continue;
        }

        if (line.startsWith(SAMPLE_NUMBER_FIELD + "\t")) {
          return 1;
        }

        if (line
            .startsWith(DESIGN_FORMAT_VERSION_METADATA_KEY + EQUAL_SEPARATOR)) {

          final int equalPos = line.indexOf(EQUAL_SEPARATOR);

          final String version =
              line.substring(equalPos + 1).replaceAll("\\s", "");

          try {
            return Integer.parseInt(version);
          } catch (NumberFormatException e) {
            throw new IOException("Unknown Design format version: " + version);
          }
        }

        lineCount++;
      }
    }

    return -1;
  }

  /**
   * Get the format of the data to read.
   * @return The format of the data to read
   * @throws IOException if an error occurs while reading data
   */
  public int getDesignFormatVersion() throws IOException {

    if (!this.testFormatDone)
      this.versionFormat = findFormatVersion();

    return this.versionFormat;
  }

  /**
   * Get the DesignReader for the data.
   * @return the DesignReader for the data
   * @throws IOException if an error occurs while reading data
   */
  public DesignReader getDesignReader() throws IOException {

    switch (getDesignFormatVersion()) {

    case 1:
      return new Eoulsan1DesignReader(this);

    case 2:
      return new Eoulsan2DesignReader(this);

    default:
      throw new IOException("Unknown Design format");
    }

  }

  //
  // InputStream methods
  //

  @Override
  public int read() throws IOException {

    if (this.cacheIndex == -1) {
      return -1;
    }

    if (this.cacheIndex < this.cache.length)
      return this.cache[this.cacheIndex++];

    return this.is.read();
  }

  @Override
  public void close() throws IOException {

    this.is.close();
    this.cacheIndex = -1;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param is InputStream to read
   */
  public DesignFormatFinderInputStream(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("The inputStream is null");
    }

    this.is = is;
  }

}
