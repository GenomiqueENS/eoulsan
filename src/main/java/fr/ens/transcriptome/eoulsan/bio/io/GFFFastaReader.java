package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This method define a Fasta reader for fasta section of GFF files.
 * @author Laurent Jourdren
 */
public class GFFFastaReader extends FastaReader {

  @Override
  public boolean hasNext() {

    String line = null;

    try {
      while ((line = this.reader.readLine()) != null) {

        if (line.startsWith("##FASTA"))
          return super.hasNext();

      }
    } catch (IOException e) {

      this.exception = e;
    }

    return false;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param file File to use
   */
  public GFFFastaReader(final File file) throws FileNotFoundException {
    super(file);
  }

  /**
   * Public constructor
   * @param file File to use
   * @param reuseResultObject if the object returns by the next() method will be
   *          always the same
   */
  public GFFFastaReader(final File file, final boolean reuseResultObject)
      throws FileNotFoundException {
    super(file, reuseResultObject);
  }

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public GFFFastaReader(final InputStream is) throws FileNotFoundException {
    super(is);
  }

  /**
   * Public constructor
   * @param is InputStream to use
   * @param reuseResultObject if the object returns by the next() method will be
   *          always the same
   */
  public GFFFastaReader(final InputStream is, final boolean reuseResultObject)
      throws FileNotFoundException {
    super(is, reuseResultObject);
  }

}
