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

package fr.ens.transcriptome.eoulsan.translators.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.translators.MultiColumnTranslator;

/**
 * This class define a reader for load annotation into a translator.
 * @since 2.0
 * @author Laurent Jourdren T
 */
public class MultiColumnTranslatorReader {

  // TODO put removeDoubleQuotesAndTrim to StringUtils

  private InputStream is;

  private BufferedReader bufferedReader;
  private static final String SEPARATOR = "\t";
  private boolean removeQuotes = true;
  private boolean noHeader;

  //
  // Getters
  //

  /**
   * Get the input stream.
   * @return Returns the input stream
   */
  protected InputStream getInputStream() {
    return is;
  }

  /**
   * Get the buffered reader of the stream.
   * @return Returns the bufferedReader
   */
  protected BufferedReader getBufferedReader() {
    return bufferedReader;
  }

  /**
   * Get the separator field of the file.
   * @return The separator field of the file
   */
  protected String getSeparatorField() {
    return SEPARATOR;
  }

  /**
   * Test if quotes of the fields must be removed
   * @return Returns the removeQuotes
   */
  public boolean isRemoveQuotes() {
    return removeQuotes;
  }

  //
  // Setters
  //

  /**
   * Set the buffered reader of the stream.
   * @param bufferedReader The bufferedReader to set
   */
  protected void setBufferedReader(final BufferedReader bufferedReader) {
    this.bufferedReader = bufferedReader;
  }

  /**
   * Set the input stream.
   * @param is The input stream to set
   * @throws EoulsanIOException if the stream is null
   */
  protected void setInputStream(final InputStream is) throws EoulsanIOException {

    if (is == null)
      throw new EoulsanIOException("No stream to read");
    this.is = is;
  }

  /**
   * Set if the quotes of the fields must be removed
   * @param removeQuotes The removeQuotes to set
   */
  public void setRemoveQuotes(final boolean removeQuotes) {
    this.removeQuotes = removeQuotes;
  }

  //
  // Other methods
  //

  /**
   * Read the design.
   * @return a new Design object
   * @throws EoulsanIOException if an error occurs while reading the design
   */
  public MultiColumnTranslator read() throws EoulsanIOException {

    setBufferedReader(new BufferedReader(new InputStreamReader(
        getInputStream(), Globals.DEFAULT_CHARSET)));

    final boolean removeQuotes = isRemoveQuotes();

    BufferedReader br = getBufferedReader();
    final String separator = getSeparatorField();
    String line = null;

    MultiColumnTranslator result = null;

    try {

      while ((line = br.readLine()) != null) {

        if ("".equals(line))
          continue;

        String[] cols = line.split(separator);

        if (removeQuotes)
          for (int i = 0; i < cols.length; i++)
            cols[i] = removeDoubleQuotesAndTrim(cols[i]);

        if (result == null && this.noHeader) {
          final String[] header = new String[cols.length];
          for (int i = 0; i < header.length; i++)
            header[i] = "#" + i;
          result = new MultiColumnTranslator(header);
        }

        if (result == null)
          result = new MultiColumnTranslator(cols);
        else
          result.addRow(cols);

      }

    } catch (IOException e) {

      throw new EoulsanIOException("Error while reading the file");
    }

    return result;
  }

  //
  // Utility methods
  //

  /**
   * Remove double quote from a string.
   * @param s The string parameter
   * @return a string without double quotes
   */
  private static String removeDoubleQuotes(final String s) {

    if (s == null)
      return null;

    String result = s;

    if (result.startsWith("\""))
      result = result.substring(1);
    if (result.endsWith("\""))
      result = result.substring(0, result.length() - 1);

    return result;
  }

  /**
   * Remove double quote and trim a string.
   * @param s The string parameter
   * @return a string without space and double quotes
   */
  private static String removeDoubleQuotesAndTrim(final String s) {

    if (s == null)
      return null;

    return removeDoubleQuotes(s.trim());
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param filename file to read
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public MultiColumnTranslatorReader(final String filename)
      throws EoulsanIOException {

    this(new File(filename), false);
  }

  /**
   * Public constructor.
   * @param filename file to read
   * @param noHeader true if there is no header for column names
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public MultiColumnTranslatorReader(final String filename,
      final boolean noHeader) throws EoulsanIOException {

    this(new File(filename), noHeader);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public MultiColumnTranslatorReader(final File file) throws EoulsanIOException {

    this(file, false);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @param noHeader true if there is no header for column names
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public MultiColumnTranslatorReader(final File file, final boolean noHeader)
      throws EoulsanIOException {

    if (file == null)
      throw new EoulsanIOException("No file to load");

    this.noHeader = noHeader;

    try {
      setInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new EoulsanIOException("File not found : " + file.getName());
    }

  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws EoulsanIOException if the stream is null
   */
  public MultiColumnTranslatorReader(final InputStream is)
      throws EoulsanIOException {

    this(is, false);
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @param noHeader true if there is no header for column names
   * @throws EoulsanIOException if the stream is null
   */
  public MultiColumnTranslatorReader(final InputStream is,
      final boolean noHeader) throws EoulsanIOException {

    this.noHeader = noHeader;
    setInputStream(is);
  }

}
