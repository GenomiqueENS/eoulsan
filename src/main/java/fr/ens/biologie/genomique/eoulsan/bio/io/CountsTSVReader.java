package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * This class define a counts reader for TSV format.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class CountsTSVReader implements CountsReader {

  private InputStream is;

  @Override
  public Map<String, Integer> read() throws IOException {

    final Map<String, Integer> result = new LinkedHashMap<>();

    String line = null;
    int lineCount = 0;

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(this.is))) {

      Splitter splitter = Splitter.on('\t').trimResults();

      boolean first = true;
      while ((line = reader.readLine()) != null) {

        line = line.trim();
        lineCount++;

        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }

        if (first) {
          first = false;
          continue;
        }

        List<String> fields = GuavaCompatibility.splitToList(splitter, line);
        if (fields.size() != 2) {
          throw new IOException("Invalid number of fields found line "
              + lineCount + ", 2 fields are expected: " + line);
        }

        try {
          result.put(fields.get(0), Integer.parseInt(fields.get(1)));
        } catch (NumberFormatException e) {
          throw new IOException(
              "Invalid count found line " + lineCount + ": " + line);
        }
      }
    }

    return result;

  }

  @Override
  public void close() throws IOException {

    this.is.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public CountsTSVReader(final InputStream is) {

    Objects.requireNonNull(is, "InputStream is null");

    this.is = is;
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public CountsTSVReader(final File file) throws FileNotFoundException {

    Objects.requireNonNull(file, "file argument is null");

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public CountsTSVReader(final String filename) throws FileNotFoundException {

    Objects.requireNonNull(filename, "filename argument is null");

    this.is = new FileInputStream(filename);
  }

}
