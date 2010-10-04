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

package fr.ens.transcriptome.eoulsan.bio.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class GFFWriter extends GFFEntry {

  private Writer writer;

  private boolean first = true;
  private StringBuilder sb;

  private void writeMetadata() throws IOException {

    if (!this.isMetaDataEntry("gff-version"))
      sb.append("##gff-version\t3\n");

    for (String k : this.getMetadataKeyNames()) {
      sb.append("##");
      sb.append(StringUtils.protectGFF(k));
      sb.append('\t');
      sb.append(StringUtils.protectGFF(getMetadataEntryValue(k)));
      sb.append('\n');
    }

    this.writer.write(sb.toString());
    this.sb.setLength(0);
  }

  /**
   * Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  public void write() throws IOException {

    if (first) {
      this.sb = new StringBuilder();
      writeMetadata();
      this.first = false;
    }

  }

  /**
   * Close the writer
   */
  public void close() {

    this.sb = null;
    this.close();
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

    this.writer = FileUtils.createBufferedWriter(os);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public GFFWriter(final File outputFile) throws FileNotFoundException {

    this.writer = FileUtils.createBufferedWriter(outputFile);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public GFFWriter(final String outputFilename) throws FileNotFoundException {

    this.writer = FileUtils.createBufferedWriter(outputFilename);
  }
}
