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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.NoSuchElementException;

import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.EntryMetadata;
import fr.ens.biologie.genomique.eoulsan.bio.GFFEntry;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class defines a GFF3 reader.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GFFReader
    implements Iterator<GFFEntry>, Iterable<GFFEntry>, Closeable {

  private final BufferedReader reader;
  private GFFEntry result = null;
  private boolean end;
  private boolean fastaSectionFound;

  private final EntryMetadata metadata = new EntryMetadata();
  private boolean nextCallDone = true;
  protected IOException ioException;
  protected BadBioEntryException bbeException;

  private boolean gff3Format = true;

  @Override
  public Iterator<GFFEntry> iterator() {

    return this;
  }

  /**
   * Test if a fasta section was found.
   * @return true if a Fasta section was found
   */
  public boolean isFastaSectionFound() {

    return this.fastaSectionFound;
  }

  @Override
  public boolean hasNext() {

    if (this.end) {
      return false;
    }

    String line = null;

    try {
      while ((line = this.reader.readLine()) != null) {

        if (line.startsWith("###")) {
          continue;
        }

        if (line.startsWith("##FASTA")) {
          this.fastaSectionFound = true;
          this.end = true;
          return false;
        }

        if (line.startsWith("##")) {

          final int posTab = line.indexOf(' ');
          if (posTab == -1) {
            continue;
          }

          final String mdKey = line.substring(2, posTab).trim();
          final String mdValue = line.substring(posTab + 1).trim();
          this.metadata.add(mdKey, mdValue);

        } else if (line.startsWith("#")) {
          continue;
        } else {

          // Create a new object with metadata
          this.result = new GFFEntry(this.metadata);

          if (this.gff3Format) {
            this.result.parseGFF3(line.trim());
          } else {
            this.result.parseGTF(line.trim());
          }


          this.nextCallDone = false;
          return true;
        }
      }
    } catch (IOException e) {
      this.ioException = e;
    } catch (BadBioEntryException e) {
      this.bbeException = e;
    }

    this.end = true;

    return false;
  }

  @Override
  public GFFEntry next() {

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

  /**
   * Close the stream.
   * @throws IOException if an error occurs while closing the file
   */
  @Override
  public void close() throws IOException {

    this.reader.close();
  }

  /**
   * Throw an exception if an exception has been caught while last hasNext()
   * method call.
   * @throws IOException if an exception has been caught while last hasNext()
   *           method call
   * @throws BadBioEntryException if the last entry is not valid
   */
  public void throwException() throws IOException, BadBioEntryException {

    if (this.ioException != null) {
      throw this.ioException;
    }

    if (this.bbeException != null) {
      throw this.bbeException;
    }
  }

  //
  // Protected methods
  //

  /**
   * Get the format of the data to read.
   * @return true if the data to read is in GFF format
   */
  protected boolean isGFF3Format() {

    return this.gff3Format;
  }

  /**
   * Set the format of the data to read.
   * @param gffFormat true if the data to read is in GFF3 format
   */
  protected void setGFF3Format(final boolean gffFormat) {

    this.gff3Format = gffFormat;
  }

  //
  // Constructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public GFFReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.reader = new BufferedReader(new InputStreamReader(is, GFF_CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public GFFReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    this.reader = FileUtils.createBufferedReader(file, GFF_CHARSET);
  }

  /**
   * Public constructor.
   * @param filename File to use
   */
  public GFFReader(final String filename) throws FileNotFoundException {

    this.reader = FileUtils.createBufferedReader(filename, GFF_CHARSET);
  }
}
