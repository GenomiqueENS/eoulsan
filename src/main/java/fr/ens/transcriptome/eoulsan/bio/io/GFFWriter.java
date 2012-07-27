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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a GFF writer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GFFWriter implements Closeable {

  private Writer writer;

  private boolean first = true;
  private StringBuilder sb;

  private void writeMetadata(final GFFEntry entry) throws IOException {

    if (!entry.isMetaDataEntry("gff-version"))
      sb.append("##gff-version\t3\n");

    for (String k : entry.getMetadataKeyNames()) {
      for (String e : entry.getMetadataEntryValues(k)) {
        sb.append("##");
        sb.append(StringUtils.protectGFF(k));
        sb.append('\t');
        sb.append(StringUtils.protectGFF(e));
        sb.append('\n');
      }
    }

    this.writer.write(sb.toString());
    this.sb.setLength(0);
  }

  /**
   * /** Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  public void write(final GFFEntry entry) throws IOException {

    if (entry == null)
      return;

    if (first) {
      this.sb = new StringBuilder();
      writeMetadata(entry);
      this.first = false;
    }

  }

  /**
   * Close the writer.
   * @throws IOException if an error occurs while closing the writer
   */
  public void close() throws IOException {

    this.sb = null;
    this.writer.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public GFFWriter(final Writer writer) {

    if (writer == null)
      throw new NullPointerException("The writer is null.");

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public GFFWriter(final OutputStream os) throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public GFFWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public GFFWriter(final String outputFilename) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFilename);
  }
}
