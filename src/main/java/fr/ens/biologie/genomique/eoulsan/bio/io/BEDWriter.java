package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.BED_CHARSET;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.bio.BEDEntry;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a BED writer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BEDWriter implements Closeable {

  public static final int DEFAULT_FORMAT = 12;

  private final Writer writer;
  private final int format;
  private boolean first = true;

  private void writeMetadata(final BEDEntry entry) throws IOException {

    final StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, List<String>> e : entry.getMetadata().entries()
        .entrySet()) {

      for (String v : e.getValue()) {
        sb.append(e.getKey());
        sb.append(' ');
        sb.append(v);
        sb.append('\n');
      }
    }

    this.writer.write(sb.toString());
  }

  /**
   * /** Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  public void write(final BEDEntry entry) throws IOException {

    if (entry == null) {
      return;
    }

    if (this.first) {
      writeMetadata(entry);
      this.first = false;
    }

    this.writer.write(entry.toBED(this.format) + '\n');
  }

  /**
   * Close the writer.
   * @throws IOException if an error occurs while closing the writer
   */
  @Override
  public void close() throws IOException {

    this.writer.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public BEDWriter(final Writer writer, final int format) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    // Check the number of BED fields
    BEDEntry.checkBEDFieldCount(format);

    this.writer = writer;
    this.format = format;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public BEDWriter(final OutputStream os, final int format)
      throws FileNotFoundException {

    // Check the number of BED fields
    BEDEntry.checkBEDFieldCount(format);

    this.writer = FileUtils.createFastBufferedWriter(os, BED_CHARSET);
    this.format = format;
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public BEDWriter(final File outputFile, final int format) throws IOException {

    // Check the number of BED fields
    BEDEntry.checkBEDFieldCount(format);

    this.writer = FileUtils.createFastBufferedWriter(outputFile, BED_CHARSET);
    this.format = format;
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public BEDWriter(final String outputFilename, final int format)
      throws IOException {

    // Check the number of BED fields
    BEDEntry.checkBEDFieldCount(format);

    this.writer =
        FileUtils.createFastBufferedWriter(outputFilename, BED_CHARSET);
    this.format = format;
  }

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public BEDWriter(final Writer writer) {

    this(writer, DEFAULT_FORMAT);
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public BEDWriter(final OutputStream os) throws FileNotFoundException {

    this(os, DEFAULT_FORMAT);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public BEDWriter(final File outputFile) throws IOException {

    this(outputFile, DEFAULT_FORMAT);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public BEDWriter(final String outputFilename) throws IOException {

    this(outputFilename, DEFAULT_FORMAT);
  }

  public static void main(String[] args) throws IOException {

    File dir = new File("/home/jourdren/tmp");
    File inputFile = new File(dir, "input.bed");
    File outputFile = new File(dir, "output.bed");

    try (BEDReader reader = new BEDReader(inputFile);
        BEDWriter writer = new BEDWriter(outputFile, 9)) {

      for (BEDEntry e : reader) {
        writer.write(e);
      }
    }

  }

}
