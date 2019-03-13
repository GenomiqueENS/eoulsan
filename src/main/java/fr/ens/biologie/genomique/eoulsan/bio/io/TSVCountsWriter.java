package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class define a TSV count writer.
 * @author Laurent Jourdren
 * @since 2.4
 */
public class TSVCountsWriter implements CountsWriter {

  private final OutputStream os;

  @Override
  public void write(Map<String, Integer> counts) throws IOException {

    Objects.requireNonNull(counts, "counts argument cannot be null");

    try (Writer writer = new OutputStreamWriter(this.os)) {
      final List<String> keysSorted = new ArrayList<>(counts.keySet());
      Collections.sort(keysSorted);

      writer.write("Id\tCount\n");
      for (String key : keysSorted) {
        writer.write(key + "\t" + counts.get(key) + "\n");
      }
    }

  }

  @Override
  public void close() throws IOException {

    this.os.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public TSVCountsWriter(final OutputStream os) throws FileNotFoundException {

    Objects.requireNonNull(os, "os argument cannot be null");

    this.os = os;
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public TSVCountsWriter(final File outputFile) throws IOException {

    Objects.requireNonNull(outputFile, "outputFile argument cannot be null");

    this.os = new FileOutputStream(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public TSVCountsWriter(final String outputFilename) throws IOException {

    Objects.requireNonNull(outputFilename,
        "outputFilename argument cannot be null");

    this.os = new FileOutputStream(outputFilename);
  }

}
