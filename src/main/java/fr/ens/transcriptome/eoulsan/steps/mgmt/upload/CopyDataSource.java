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

package fr.ens.transcriptome.eoulsan.steps.mgmt.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.io.FastQReader;
import fr.ens.transcriptome.eoulsan.bio.io.FastQWriter;
import fr.ens.transcriptome.eoulsan.bio.io.ReadSequenceReader;
import fr.ens.transcriptome.eoulsan.bio.io.ReadSequenceWriter;
import fr.ens.transcriptome.eoulsan.bio.io.TFQReader;
import fr.ens.transcriptome.eoulsan.bio.io.TFQWriter;
import fr.ens.transcriptome.eoulsan.datasources.DataSource;
import fr.ens.transcriptome.eoulsan.datasources.DataSourceUtils;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class allow to copy and transform data while copying.
 * @author Laurent Jourdren
 */
public class CopyDataSource {

  // private DataSource src;
  private InputStream is;
  private String srcContentType;
  private String srcContentEncoding;

  private String src;
  private String dest;
  private String destContentType;
  private String destContentEncoding;

  private void identifyTypeAndEncoding() {

    this.srcContentEncoding = StringUtils.compressionExtension(this.src);
    this.srcContentType =
        StringUtils.extensionWithoutCompressionExtension(this.src);

    this.destContentEncoding = StringUtils.compressionExtension(this.dest);
    this.destContentType =
        StringUtils.extensionWithoutCompressionExtension(this.dest);

  }

  /**
   * Copy the data to an outputStream
   * @param os the output stream to use
   * @throws IOException if an error occurs while copying
   */
  public void copy(final OutputStream os) throws IOException {

    if (os == null)
      throw new IOException("The output stream is null");

    final InputStream is = this.is;

    if (this.srcContentType.equals(this.destContentType)
        && this.srcContentEncoding.equals(this.destContentEncoding)) {

      FileUtils.copy(is, os);
      return;
    }

    if ((this.srcContentType.equals(this.destContentType))) {

      // Create the output stream for the copy
      final OutputStream compressedOs =
          CompressionType.getCompressionTypeByContentEncoding(
              this.destContentEncoding).createOutputStream(os);
      FileUtils.copy(is, compressedOs);
      return;
    }

    if ((".fq".equals(this.srcContentType) || ".tfq"
        .equals(this.srcContentType))
        && (".fq".equals(this.destContentType) || ".tfq"
            .equals(this.destContentType))) {

      // Create the output stream for the copy
      final OutputStream compressedOs =
        CompressionType.getCompressionTypeByContentEncoding(
            this.destContentEncoding).createOutputStream(os);

      final ReadSequenceReader reader;
      if (".fq".equals(this.srcContentType))
        reader = new FastQReader(is);
      else
        reader = new TFQReader(is);

      final ReadSequenceWriter writer;
      if (".fq".equals(this.destContentType))
        writer = new FastQWriter(compressedOs);
      else
        writer = new TFQWriter(compressedOs);

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
   * Constructor.
   * @param src the datasource
   * @param dest the destination
   */
  public CopyDataSource(final String src, final String dest) {

    this(DataSourceUtils.identifyDataSource(src), dest);
  }

  /**
   * Constructor
   * @param src source
   * @param dest destination
   */
  public CopyDataSource(final DataSource src, final String dest) {

    if (src == null)
      throw new NullPointerException("The datasource is null");

    if (dest == null)
      throw new NullPointerException("The destination is null");

    this.is = src.getInputStream();
    this.src = src.getSourceInfo();
    this.dest = dest;

    identifyTypeAndEncoding();
  }

  /**
   * Constructor.
   * @param src the datasource
   * @param dest the destination
   */
  public CopyDataSource(final String src, final InputStream is,
      final String dest) {

    if (src == null)
      throw new NullPointerException("The datasource is null");

    if (dest == null)
      throw new NullPointerException("The destination is null");

    if (is == null)
      throw new NullPointerException("The input stream is null");

    this.is = is;
    this.src = src;
    this.dest = dest;

    identifyTypeAndEncoding();
  }

}
