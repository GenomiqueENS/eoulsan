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

package fr.ens.transcriptome.eoulsan.design.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a writer for designs.
 * @since 1.0
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
    return this.outputStream;
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
   * @throws EoulsanIOException if an exception occurs while writing the design
   */
  public abstract void write(final Design design) throws EoulsanIOException;

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public DesignWriter(final File file) throws EoulsanIOException {
    if (file == null) {
      throw new EoulsanIOException("No file to load");
    }

    try {
      setOutputStream(FileUtils.createOutputStream(file));
    } catch (IOException e) {
      throw new EoulsanIOException("Error while reading file : "
          + file.getName(), e);
    }

    this.dataSource = file.getAbsolutePath();
  }

  /**
   * Public constructor
   * @param os Input stream to read
   * @throws EoulsanIOException if the stream is null
   */
  public DesignWriter(final OutputStream os) throws EoulsanIOException {
    setOutputStream(os);
  }

}
