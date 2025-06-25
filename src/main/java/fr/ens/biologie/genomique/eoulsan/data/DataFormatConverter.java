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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.data;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.biologie.genomique.kenetre.io.CompressionType;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.ReadSequenceReader;
import fr.ens.biologie.genomique.kenetre.bio.io.ReadSequenceWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.TFQReader;
import fr.ens.biologie.genomique.kenetre.bio.io.TFQWriter;

/**
 * This class allow to copy and transform data while copying.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFormatConverter {

  private final DataFormat inFormat;
  private final DataFormat outFormat;
  final DataFile inFile;
  final DataFile outFile;
  final OutputStream os;

  public void convert() throws IOException {

    if (this.outFormat == null) {

      final OutputStream destOs =
          this.os == null ? this.outFile.create() : this.os;
      FileUtils.copy(this.inFile.rawOpen(), destOs);

      return;
    }

    final CompressionType srcCT =
        CompressionType.getCompressionTypeByContentEncoding(
            this.inFile.getMetaData().getContentEncoding());
    final CompressionType destCT =
        CompressionType.getCompressionTypeByFilename(this.outFile.getName());

    getLogger().fine("Convert "
        + this.inFile + " (" + this.inFormat + "/" + srcCT + ") to "
        + this.outFile + " (" + this.outFormat + "/" + destCT + ").");

    if (this.inFormat.equals(this.outFormat) && srcCT.equals(destCT)) {

      this.inFile.copyTo(this.outFile);
      return;
    }

    final OutputStream destOs =
        this.os == null ? this.outFile.create() : this.os;

    if (this.inFormat.equals(this.outFormat)) {

      final InputStream is = this.inFile.open();
      final OutputStream os = destCT.createOutputStream(destOs);

      FileUtils.copy(is, os);
      return;
    }

    if ((this.inFormat == DataFormats.READS_FASTQ
        || this.inFormat == DataFormats.READS_TFQ)
        && (this.outFormat == DataFormats.READS_FASTQ
            || this.outFormat == DataFormats.READS_TFQ)) {

      final ReadSequenceReader reader;

      if (this.inFormat == DataFormats.READS_FASTQ) {
        reader = new FastqReader(this.inFile.open());
      } else {
        reader = new TFQReader(this.inFile.open());
      }

      final OutputStream os = destCT.createOutputStream(destOs);

      final ReadSequenceWriter writer;

      if (this.outFormat == DataFormats.READS_FASTQ) {
        writer = new FastqWriter(os);
      } else {
        writer = new TFQWriter(os);
      }

      try {
        for (final ReadSequence read : reader) {
          writer.write(read);
        }

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
    throw new IOException("This copy case is not implemented");
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   * @param os outputStream
   * @throws IOException if an error occurs while creating converter
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile,
      final OutputStream os) throws IOException {

    this(inFile, outFile,
        outFile == null
            ? null : DataFormatRegistry.getInstance()
                .getDataFormatFromFilename(outFile.getName()),
        os);
  }

  /**
   * Constructor
   * @param inFile input file
   * @param outFile output file
   * @throws IOException if an error occurs while creating converter
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
   * @param os output stream
   * @throws IOException if an error occurs while creating converter
   */
  public DataFormatConverter(final DataFile inFile, final DataFile outFile,
      final DataFormat outFormat, final OutputStream os) throws IOException {

    if (inFile == null) {
      throw new NullPointerException("The input file is null");
    }

    this.inFile = inFile;
    this.inFormat = inFile.getMetaData().getDataFormat();

    if (this.inFormat == null && outFormat != null) {
      throw new NullPointerException("The input file format is null");
    }

    if (outFile == null) {
      throw new NullPointerException("The output file format is null");
    }

    this.outFile = outFile;
    this.outFormat = outFormat;
    this.os = os;
  }

}
