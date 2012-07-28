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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GFFEntry;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a GFF reader.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class GFFReader implements Iterator<GFFEntry>, Iterable<GFFEntry>,
    Closeable {

  /* Default Charset. */
  private static final Charset CHARSET = Charset.forName("ISO-8859-1");

  private BufferedReader reader;
  private GFFEntry result = null;
  private int count;
  private boolean end;
  private boolean fastaSectionFound;

  private Map<String, List<String>> metadata =
      new LinkedHashMap<String, List<String>>();
  private boolean nextCallDone = true;
  protected IOException ioException;
  protected BadBioEntryException bbeException;

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

    if (this.end)
      return false;

    String line = null;

    result = new GFFEntry();

    try {
      while ((line = this.reader.readLine()) != null) {

        if (line.startsWith("###"))
          continue;

        if (line.startsWith("##FASTA")) {
          this.fastaSectionFound = true;
          this.end = true;
          return false;
        }

        if (line.startsWith("##")) {

          final int posTab = line.indexOf(' ');
          if (posTab == -1)
            continue;

          final String mdKey = line.substring(2, posTab).trim();
          final String mdValue = line.substring(posTab + 1).trim();

          final List<String> list;
          if (!this.metadata.containsKey(mdKey)) {
            list = new ArrayList<String>();
            this.metadata.put(mdKey, list);
          } else
            list = this.metadata.get(mdKey);

          list.add(mdValue);

        } else if (line.startsWith("#"))
          continue;
        else {

          result.parse(line.trim());
          result.setId(count++);

          // Add metadata if not reuse result object
          result.addMetaDataEntries(this.metadata);

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

    if (this.nextCallDone)
      throw new NoSuchElementException();

    this.nextCallDone = true;

    return this.result;
  }

  @Override
  public void remove() {

    throw new UnsupportedOperationException("Unsupported operation");
  }

  /**
   * Close the stream.
   * @throws IOException
   */
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

    if (this.ioException != null)
      throw this.ioException;

    if (this.bbeException != null)
      throw this.bbeException;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public GFFReader(final InputStream is) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.reader = new BufferedReader(new InputStreamReader(is, CHARSET));
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public GFFReader(final File file) throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    if (!file.isFile())
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());

    this.reader = FileUtils.createBufferedReader(file);
  }

}
