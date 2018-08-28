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

package fr.ens.biologie.genomique.eoulsan.bio.io;

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.GFF_CHARSET;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a GFF3 writer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GFFWriter implements Closeable {

  private final Writer writer;
  private boolean gff3Format = true;
  private boolean first = true;

  private void writeMetadata(final GFFEntry entry) throws IOException {

    final StringBuilder sb = new StringBuilder();

    if (this.gff3Format && !entry.getMetadata().containsKey("gff-version")) {
      sb.append("##gff-version 3\n");
    }

    for (Map.Entry<String, List<String>> e : entry.getMetadata().entries()
        .entrySet()) {

      for (String v : e.getValue()) {
        sb.append("##");
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
  public void write(final GFFEntry entry) throws IOException {

    if (entry == null) {
      return;
    }

    if (this.first) {
      writeMetadata(entry);
      this.first = false;
    }

    if (this.gff3Format) {
      this.writer.write(entry.toGFF3() + '\n');
    } else {
      this.writer.write(entry.toGTF() + '\n');
    }
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
  // Protected methods
  //

  /**
   * Get the format of the data to write.
   * @return true if the data to write is in GFF format
   */
  protected boolean isGFF3Format() {

    return this.gff3Format;
  }

  /**
   * Set the format of the data to write.
   * @param gffFormat true if the data to write is in GFF3 format
   */
  protected void setGFF3Format(final boolean gffFormat) {

    this.gff3Format = gffFormat;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public GFFWriter(final Writer writer) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public GFFWriter(final OutputStream os) throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public GFFWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public GFFWriter(final String outputFilename) throws IOException {

    this.writer =
        FileUtils.createFastBufferedWriter(outputFilename, GFF_CHARSET);
  }
}
