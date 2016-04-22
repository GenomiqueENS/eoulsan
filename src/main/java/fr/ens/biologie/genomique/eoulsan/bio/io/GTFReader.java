package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * This class defines a GTF reader.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class GTFReader extends GFFReader {

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public GTFReader(final InputStream is) {

    super(is);
    setGFF3Format(false);
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public GTFReader(final File file) throws FileNotFoundException {

    super(file);
    setGFF3Format(false);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public GTFReader(final String filename) throws FileNotFoundException {

    super(filename);
    setGFF3Format(false);
  }

}
