/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastQWriter;
import fr.ens.transcriptome.eoulsan.bio.io.ReadSequenceReader;
import fr.ens.transcriptome.eoulsan.bio.io.ReadSequenceWriter;
import fr.ens.transcriptome.eoulsan.bio.io.TFQReader;
import fr.ens.transcriptome.eoulsan.bio.io.TFQWriter;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class allow to copy and transform data while copying.
 * @author Laurent Jourdren
 */
public class DataFormatConverter {

  /** Logger */
  private static final Logger logger = Logger.getLogger(Globals.APP_NAME);

  private DataFormat inFormat;
  private DataFormat outFormat;
  final DataFile inFile;
  final DataFile outFile;

  public void convert() throws IOException {

    if (this.inFormat.getType() != this.outFormat.getType())
      throw new IOException("Can convert data from different DataType.");

    final CompressionType srcCT =
        CompressionType.getCompressionTypeByContentEncoding(inFile
            .getMetaData().getContentEncoding());
    final CompressionType destCT =
        CompressionType.getCompressionTypeByFilename(outFile.getName());

    logger.info("Convert "
        + inFile + " (" + inFormat + "/" + srcCT + ") to " + outFile + " ("
        + outFormat + "/" + destCT + ").");

    if (this.inFormat.equals(this.outFormat) && srcCT.equals(destCT)) {

      FileUtils.copy(inFile.rawOpen(), outFile.create());
      return;
    }

    if (this.inFormat.equals(this.outFormat)) {

      final InputStream is = inFile.open();
      final OutputStream os = destCT.createOutputStream(outFile.create());

      FileUtils.copy(is, os);
      return;
    }

    if ((this.inFormat == DataFormats.READS_FASTQ || this.inFormat == DataFormats.READS_TFQ)
        && (this.outFormat == DataFormats.READS_FASTQ || this.outFormat == DataFormats.READS_TFQ)) {

      final ReadSequenceReader reader;

      if (this.inFormat == DataFormats.READS_FASTQ)
        reader = new FastQReader(this.inFile.open());
      else
        reader = new TFQReader(this.inFile.open());

      final OutputStream os = destCT.createOutputStream(outFile.create());

      final ReadSequenceWriter writer;

      if (this.outFormat == DataFormats.READS_FASTQ)
        writer = new FastQWriter(os);
      else
        writer = new TFQWriter(os);

      try {
        while (reader.readEntry()) {

          writer.set(reader);
          writer.write();
        }
      } catch (BadBioEntryException e) {
        throw new IOException("Bad read sequence entry: " + e.getEntry());
      }

      reader.close();
      writer.close();

      return;

    }

    throw new IOException("This copy case is not implementated");
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile)
      throws IOException {

    this(inFile, outFile, outFile == null ? null : DataFormatRegistry
        .getInstance().getDataFormat(outFile.getName()));
  }

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   * @param outFormat output format
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile,
      final DataFormat outFormat) throws IOException {

    if (inFile == null)
      throw new NullPointerException("The input file is null");

    this.inFile = inFile;
    this.inFormat = inFile.getMetaData().getDataFormat();

    if (inFile == null)
      throw new NullPointerException("The input file format is null");

    if (outFile == null)
      throw new NullPointerException("The output file format is null");

    if (outFormat == null)
      throw new NullPointerException("The output format format is null");

    this.outFile = outFile;
    this.outFormat = outFormat;

  }

}
