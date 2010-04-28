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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This class define a writer for designs.
 * @author Laurent Jourdren
 */
public abstract class DesignWriter {

  private OutputStream outputStream;
  private String dataSource;

  //
  // Getters
  //

  /**
   * Get the outputStream.
   * @return Returns the outputStream
   */
  public OutputStream getOutputStream() {
    return outputStream;
  }

  /**
   * Get the source of the data
   * @return The source of the data
   */
  public String getDataSource() {
    return this.dataSource;
  }

  //
  // Setters
  //

  /**
   * Set the output stream
   * @param outputStream The outputStream to set
   */
  public void setOutputStream(final OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  //
  // Abstract method
  //

  /**
   * Write a Design.
   * @param design Design to write
   * @throws NividicIOException if an exception occurs while writing the design
   */
  public abstract void write(final Design design) throws EoulsanIOException;

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws NividicIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public DesignWriter(final File file) throws EoulsanIOException {
    if (file == null)
      throw new EoulsanIOException("No file to load");

    try {
      setOutputStream(new FileOutputStream(file));
    } catch (FileNotFoundException e) {
      throw new EoulsanIOException("Error while reading file : "
          + file.getName());
    }

    this.dataSource = file.getAbsolutePath();
  }

  /**
   * Public constructor
   * @param os Input stream to read
   * @throws NividicIOException if the stream is null
   */
  public DesignWriter(final OutputStream os) throws EoulsanIOException {
    setOutputStream(os);
  }

}
