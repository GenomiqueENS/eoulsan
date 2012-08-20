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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.Sequence;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class implements a Fasta writer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastaWriter implements SequenceWriter {

  /* Default Charset. */
  private static final Charset CHARSET = Charset.forName("ISO-8859-1");
  
  private Writer writer;

  /**
   * Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  @Override
  public void write(final Sequence sequence) throws IOException {

    if (sequence == null)
      return;

    this.writer.write(sequence.toFasta(Globals.FASTA_FILE_WIDTH));
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
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public FastaWriter(final Writer writer) {

    if (writer == null)
      throw new NullPointerException("The writer is null.");

    this.writer = writer;
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public FastaWriter(final OutputStream os) throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os, CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public FastaWriter(final File outputFile) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile, CHARSET);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public FastaWriter(final String outputFilename) throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFilename, CHARSET);
  }

}
