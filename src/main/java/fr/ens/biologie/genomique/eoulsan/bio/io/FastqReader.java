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

import static fr.ens.biologie.genomique.eoulsan.bio.io.BioCharsets.FASTQ_CHARSET;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class implements a Fastq reader.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastqReader implements ReadSequenceReader {

  private final BufferedReader reader;

  private ReadSequence result = null;
  private final StringBuilder sb = new StringBuilder();
  private int lineCount = 0;

  private boolean end = false;
  private boolean nextCallDone = true;
  protected IOException ioException;
  protected BadBioEntryException bbeException;

  @Override
  public void close() throws IOException {

    this.reader.close();
  }

  @Override
  public Iterator<ReadSequence> iterator() {

    return this;
  }

  @Override
  public boolean hasNext() {

    if (this.end) {
      return false;
    }

    this.nextCallDone = false;

    String line = null;
    int entryLine = 0;

    try {
      while ((line = this.reader.readLine()) != null) {

        // Increment line count
        this.lineCount++;

        // Trim the line
        final String trim = line.trim();

        entryLine++;
        this.sb.append(trim);

        if (entryLine == 1 && trim.charAt(0) != '@') {
          throw new BadBioEntryException(
              "Invalid Fastq file. First line of the entry don't start with '@' at line "
                  + this.lineCount,
              line);
        }

        if (entryLine == 3 && trim.charAt(0) != '+') {
          throw new BadBioEntryException(
              "Invalid Fastq file. Third line of the entry don't start with '+' at line "
                  + this.lineCount,
              line);
        }

        if (entryLine == 4) {

          // Fill the ReadSequence object
          this.result = new ReadSequence();
          this.result.parseFastQ(this.sb.toString());
          this.sb.setLength(0);
          return true;
        }
        this.sb.append('\n');
      }

      this.sb.setLength(0);
      this.end = true;

      return false;
    } catch (IOException e) {

      this.ioException = e;
      this.end = true;
      return false;
    } catch (BadBioEntryException e) {

      this.bbeException = e;
      this.end = true;
      return false;
    }
  }

  @Override
  public ReadSequence next() {

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
  public void throwException() throws IOException, BadBioEntryException {

    if (this.ioException != null) {
      throw this.ioException;
    }

    if (this.bbeException != null) {
      throw this.bbeException;
    }
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public FastqReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, FASTQ_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public FastqReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    this.reader = FileUtils.createBufferedReader(file, FASTQ_CHARSET);
  }

  /**
   * Public constructor
   * @param filename File to use
   */
  public FastqReader(final String filename) throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, FASTQ_CHARSET);
  }

}
