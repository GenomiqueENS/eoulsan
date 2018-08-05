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

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.FASTA_CHARSET;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.Sequence;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class implements a Fasta writer.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastaWriter implements SequenceWriter {

  private final Writer writer;
  private final int lineLength;

  /**
   * Write the current entry.
   * @throws IOException if an error occurs while writing data
   */
  @Override
  public void write(final Sequence sequence) throws IOException {

    if (sequence == null) {
      return;
    }

    this.writer.write(sequence.toFasta(this.lineLength));
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
  // Other methods
  //

  /**
   * Check the line length.
   * @param lineLength the line length to check
   * @return the input argument
   * @throws IllegalArgumentException if the argument is invalid
   */
  private static int checkLineLength(final int lineLength) {

    if (lineLength < 1) {
      throw new IllegalArgumentException(
          "Invalid FASTA line length: " + lineLength);
    }

    return lineLength;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param writer Writer to use
   * @param lineLength FASTA line length
   */
  public FastaWriter(final Writer writer, final int lineLength) {

    if (writer == null) {
      throw new NullPointerException("The writer is null.");
    }

    this.writer = writer;
    this.lineLength = checkLineLength(lineLength);
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   * @param lineLength FASTA line length
   */
  public FastaWriter(final OutputStream os, final int lineLength)
      throws FileNotFoundException {

    this.writer = FileUtils.createFastBufferedWriter(os, FASTA_CHARSET);
    this.lineLength = checkLineLength(lineLength);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   * @param lineLength FASTA line length
   */
  public FastaWriter(final File outputFile, final int lineLength)
      throws IOException {

    this.writer = FileUtils.createFastBufferedWriter(outputFile, FASTA_CHARSET);
    this.lineLength = checkLineLength(lineLength);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   * @param lineLength FASTA line length
   */
  public FastaWriter(final String outputFilename, final int lineLength)
      throws IOException {

    this.writer =
        FileUtils.createFastBufferedWriter(outputFilename, FASTA_CHARSET);
    this.lineLength = checkLineLength(lineLength);
  }

  /**
   * Public constructor.
   * @param writer Writer to use
   */
  public FastaWriter(final Writer writer) {

    this(writer, Globals.FASTA_FILE_WIDTH);
  }

  /**
   * Public constructor.
   * @param os OutputStream to use
   */
  public FastaWriter(final OutputStream os) throws FileNotFoundException {

    this(os, Globals.FASTA_FILE_WIDTH);
  }

  /**
   * Public constructor.
   * @param outputFile file to use
   */
  public FastaWriter(final File outputFile) throws IOException {

    this(outputFile, Globals.FASTA_FILE_WIDTH);
  }

  /**
   * Public constructor.
   * @param outputFilename name of the file to use
   */
  public FastaWriter(final String outputFilename) throws IOException {

    this(outputFilename, Globals.FASTA_FILE_WIDTH);
  }

}
