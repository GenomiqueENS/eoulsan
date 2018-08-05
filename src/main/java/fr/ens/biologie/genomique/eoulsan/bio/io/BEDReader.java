package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.BED_CHARSET;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.ens.biologie.genomique.eoulsan.bio.BEDEntry;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.EntryMetadata;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class defines a BED reader.
 * @since 2.3
 * @author Laurent Jourdren
 */
public class BEDReader
    implements Iterator<BEDEntry>, Iterable<BEDEntry>, Closeable {

  private final BufferedReader reader;
  private BEDEntry result = null;
  private boolean end;

  private final EntryMetadata metadata = new EntryMetadata();
  private boolean nextCallDone = true;
  protected IOException ioException;
  protected BadBioEntryException bbeException;

  @Override
  public Iterator<BEDEntry> iterator() {

    return this;
  }

  @Override
  public boolean hasNext() {

    if (this.end) {
      return false;
    }

    String line = null;

    try {
      while ((line = this.reader.readLine()) != null) {

        if (line.startsWith("#")) {
          continue;
        }

        if (line.startsWith("track ") || line.startsWith("browser ")) {

          final int posTab = line.indexOf(' ');
          if (posTab == -1) {
            continue;
          }

          final String mdKey = line.substring(0, posTab).trim();
          final String mdValue = line.substring(posTab + 1).trim();
          this.metadata.add(mdKey, mdValue);

        } else {

          // Create a new object with metadata
          this.result = new BEDEntry(this.metadata);

          this.result.parse(line.trim());

          this.nextCallDone = false;
          return true;
        }
      }
    } catch (IOException e) {
      this.ioException = e;
    } catch (BadBioEntryException e) {
      this.bbeException = e;
    }

    this.end = true;

    return false;
  }

  @Override
  public BEDEntry next() {

    if (this.nextCallDone) {
      throw new NoSuchElementException();
    }

    this.nextCallDone = true;

    return this.result;
  }

  @Override
  public void remove() {

    throw new UnsupportedOperationException("Unsupported operation");
  }

  /**
   * Close the stream.
   * @throws IOException if an error occurs while closing the file
   */
  @Override
  public void close() throws IOException {

    this.reader.close();
  }

  /**
   * Throw an exception if an exception has been caught while last hasNext()
   * method call.
   * @throws IOException if an exception has been caught while last hasNext()
   *           method call
   * @throws BadBioEntryException if the last entry is not valid
   */
  public void throwException() throws IOException, BadBioEntryException {

    if (this.ioException != null) {
      throw this.ioException;
    }

    if (this.bbeException != null) {
      throw this.bbeException;
    }
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public BEDReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, BED_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public BEDReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    this.reader = FileUtils.createBufferedReader(file, BED_CHARSET);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public BEDReader(final String filename) throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, BED_CHARSET);
  }

}
