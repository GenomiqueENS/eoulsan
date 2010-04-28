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
 * of the �cole Normale Sup�rieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import fr.ens.transcriptome.eoulsan.design.Design;



/**
 * This abstract class define a reader for designs.
 * @author Laurent Jourdren
 */
public abstract class InputStreamDesignReader implements DesignReader {

  private InputStream is;
  private String dataSource;

  private BufferedReader bufferedReader;
  private static final String SEPARATOR = "\t";

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
   * Get the source of the data.
   * @return The source of the data
   */
  public String getDataSource() {
    return this.dataSource;
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
   * @throws NividicIOException if the stream is null
   */
  protected void setInputStream(final InputStream is) throws EoulsanIOException {

    if (is == null)
      throw new EoulsanIOException("No stream to read");
    this.is = is;
  }

  //
  // Other methods
  //

  /**
   * Read the design.
   * @return a new Design object
   * @throws NividicIOException if an error occurs while reading the design
   */
  public abstract Design read() throws EoulsanIOException;

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws NividicIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public InputStreamDesignReader(final File file) throws EoulsanIOException {

    if (file == null)
      throw new EoulsanIOException("No file to load");

    try {
      setInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new EoulsanIOException("Error while reading file : "
          + file.getName());
    }

    this.dataSource = file.getAbsolutePath();
  }

  /**
   * Public constructor
   * @param is Input stream to read
   * @throws NividicIOException if the stream is null
   */
  public InputStreamDesignReader(final InputStream is)
      throws EoulsanIOException {

    setInputStream(is);
  }

}
