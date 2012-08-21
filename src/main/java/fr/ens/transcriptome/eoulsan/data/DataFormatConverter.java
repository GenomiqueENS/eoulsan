/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastqWriter;
import fr.ens.transcriptome.eoulsan.bio.io.ReadSequenceReader;
import fr.ens.transcriptome.eoulsan.bio.io.ReadSequenceWriter;
import fr.ens.transcriptome.eoulsan.bio.io.TFQReader;
import fr.ens.transcriptome.eoulsan.bio.io.TFQWriter;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class allow to copy and transform data while copying.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFormatConverter {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private DataFormat inFormat;
  private DataFormat outFormat;
  final DataFile inFile;
  final DataFile outFile;
  final OutputStream os;

  public void convert() throws IOException {

    if (this.outFormat == null) {

      final OutputStream destOs = this.os == null ? outFile.create() : this.os;
      FileUtils.copy(inFile.rawOpen(), destOs);

      return;
    }

    if (this.inFormat.getType() != this.outFormat.getType())
      throw new IOException("Can convert data from different DataType.");

    final CompressionType srcCT =
        CompressionType.getCompressionTypeByContentEncoding(inFile
            .getMetaData().getContentEncoding());
    final CompressionType destCT =
        CompressionType.getCompressionTypeByFilename(outFile.getName());

    LOGGER.fine("Convert "
        + inFile + " (" + inFormat + "/" + srcCT + ") to " + outFile + " ("
        + outFormat + "/" + destCT + ").");

    if (this.inFormat.equals(this.outFormat) && srcCT.equals(destCT)) {

      inFile.copyTo(outFile);
      return;
    }

    final OutputStream destOs = this.os == null ? outFile.create() : this.os;

    if (this.inFormat.equals(this.outFormat)) {

      final InputStream is = inFile.open();
      final OutputStream os = destCT.createOutputStream(destOs);

      FileUtils.copy(is, os);
      return;
    }

    if ((this.inFormat == DataFormats.READS_FASTQ || this.inFormat == DataFormats.READS_TFQ)
        && (this.outFormat == DataFormats.READS_FASTQ || this.outFormat == DataFormats.READS_TFQ)) {

      final ReadSequenceReader reader;

      if (this.inFormat == DataFormats.READS_FASTQ)
        reader = new FastqReader(this.inFile.open());
      else
        reader = new TFQReader(this.inFile.open(), true);

      final OutputStream os = destCT.createOutputStream(destOs);

      final ReadSequenceWriter writer;

      if (this.outFormat == DataFormats.READS_FASTQ)
        writer = new FastqWriter(os);
      else
        writer = new TFQWriter(os);

      try {
        for (final ReadSequence read : reader)
          writer.write(read);

        reader.throwException();

      } catch (BadBioEntryException e) {
        throw new IOException("Bad read sequence entry: " + e.getEntry());
      } finally {

        reader.close();
        writer.close();
      }

      return;
    }

    destOs.close();
    throw new IOException("This copy case is not implementated");
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   * @param os outputStream
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile,
      final OutputStream os) throws IOException {

    this(inFile, outFile, outFile == null ? null : DataFormatRegistry
        .getInstance().getDataFormatFromFilename(outFile.getName()), os);
  }

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile)
      throws IOException {

    this(inFile, outFile, null);
  }

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   * @param outFormat output format
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile,
      final DataFormat outFormat, final OutputStream os) throws IOException {

    if (inFile == null)
      throw new NullPointerException("The input file is null");

    this.inFile = inFile;
    this.inFormat = inFile.getMetaData().getDataFormat();

    if (inFormat == null && outFormat != null)
      throw new NullPointerException("The input file format is null");

    if (outFile == null)
      throw new NullPointerException("The output file format is null");

    this.outFile = outFile;
    this.outFormat = outFormat;
    this.os = os;
  }

}
