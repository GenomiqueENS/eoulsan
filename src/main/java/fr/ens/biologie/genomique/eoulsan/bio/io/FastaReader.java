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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.ens.biologie.genomique.eoulsan.bio.Sequence;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a Fasta reader.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastaReader implements SequenceReader {

  protected BufferedReader reader;

  private Sequence result = null;
  private final StringBuilder sb = new StringBuilder();

  private String nextSequenceName;
  private boolean end = false;
  private boolean nextCallDone = true;
  protected IOException exception;

  @Override
  public void close() throws IOException {

    this.reader.close();
  }

  @Override
  public Iterator<Sequence> iterator() {

    return this;
  }

  @Override
  public boolean hasNext() {

    if (this.end) {
      return false;
    }

    this.result = new Sequence();

    String line = null;

    try {
      while ((line = this.reader.readLine()) != null) {

        // Trim the line
        final String trim = line.trim();

        // discard empty lines
        if ("".equals(trim)) {
          continue;
        }

        if (trim.charAt(0) == '>') {

          if (this.nextSequenceName != null) {

            this.result.setName(this.nextSequenceName);
            this.result.setSequence(this.sb.toString());
            this.sb.setLength(0);
            this.nextSequenceName = trim.substring(1);
            this.nextCallDone = false;

            return true;
          }

          this.nextSequenceName = trim.substring(1);

        } else {

          if (this.nextSequenceName == null) {
            throw new IOException(
                "No fasta header found at the beginning of the fasta file: "
                    + line);
          }

          this.sb.append(trim);
        }
      }

      this.result.setName(this.nextSequenceName);
      this.result.setSequence(this.sb.toString());
      this.sb.setLength(0);

      this.nextCallDone = false;
      this.end = true;

      return true;
    } catch (IOException e) {

      this.exception = e;
      this.end = true;
      return false;
    }
  }

  @Override
  public Sequence next() {

    if (this.nextCallDone) {
      throw new NoSuchElementException();
    }

    this.nextCallDone = true;

    return this.result;
  }

  @Override
  public void remove() {

    throw new UnsupportedOperationException("Unsupported operation");
  }

  @Override
  public void throwException() throws IOException {

    if (this.exception != null) {
      throw this.exception;
    }
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public FastaReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, FASTA_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public FastaReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    this.reader = FileUtils.createBufferedReader(file, FASTA_CHARSET);
  }

  /**
   * Public constructor
   * @param filename File to use
   */
  public FastaReader(final String filename) throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, FASTA_CHARSET);
  }

}
