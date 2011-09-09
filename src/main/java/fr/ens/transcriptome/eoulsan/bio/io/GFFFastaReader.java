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
   * @param is InputStream to use
   */
  public GFFFastaReader(final InputStream is) throws FileNotFoundException {
    super(is);
  }

}
