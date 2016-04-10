package fr.ens.biologie.genomique.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 * This class define a GTF writer.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class GTFWriter extends GFFWriter {

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public GTFWriter(final Writer writer) {

    super(writer);
    setGFF3Format(false);
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public GTFWriter(final OutputStream os) throws FileNotFoundException {

    super(os);
    setGFF3Format(false);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public GTFWriter(final File outputFile) throws IOException {

    super(outputFile);
    setGFF3Format(false);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public GTFWriter(final String outputFilename) throws IOException {

    super(outputFilename);
    setGFF3Format(false);
  }

}
