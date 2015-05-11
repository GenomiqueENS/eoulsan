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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;

/**
 * This class implements a writer for limma design files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class SimpleDesignWriter extends DesignWriter {

  private BufferedWriter bw;
  private static final String SEPARATOR = "\t";
  private static final String NEWLINE = "\r\n"; // System.getProperty("line.separator");
  private boolean writeScanLabelsSettings = true;

  @Override
  public void write(final Design design) throws EoulsanIOException {

    if (design == null) {
      throw new NullPointerException("Design is null");
    }

    try {
      this.bw =
          new BufferedWriter(new OutputStreamWriter(getOutputStream(),
              Globals.DEFAULT_CHARSET));

      List<String> metadataFields = design.getMetadataFieldsNames();

      // Write header
      this.bw.append(Design.SAMPLE_NUMBER_FIELD);
      this.bw.append(SEPARATOR);

      this.bw.append(Design.NAME_FIELD);

      for (String f : metadataFields) {

        this.bw.append(SEPARATOR);
        this.bw.append(f);
      }

      this.bw.append(NEWLINE);

      // Write data
      List<Sample> samples = design.getSamples();

      for (Sample s : samples) {

        this.bw.append(Integer.toString(s.getId()));
        this.bw.append(SEPARATOR);

        this.bw.append(s.getName());

        for (String f : metadataFields) {

          this.bw.append(SEPARATOR);
          this.bw.append(s.getMetadata().getField(f));
        }

        this.bw.append(NEWLINE);
      }

      this.bw.close();
    } catch (IOException e) {
      throw new EoulsanIOException("Error while writing stream: "
          + e.getMessage(), e);
    }

  }

  /**
   * Test if the scan labels settings must be written.
   * @return Returns true if thq scan labels settings must be written
   */
  boolean isWriteScanLabelsSettings() {
    return this.writeScanLabelsSettings;
  }

  /**
   * Set if the scan labels settings must be written.
   * @param writeScanLabelsSettings true if he scan labels settings must be
   *          written
   */
  void setWriteScanLabelsSettings(final boolean writeScanLabelsSettings) {
    this.writeScanLabelsSettings = writeScanLabelsSettings;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws EoulsanIOException if an error occurs while reading the file or if
   *           the file is null.
   */
  public SimpleDesignWriter(final File file) throws EoulsanIOException {

    super(file);
  }

  /**
   * Public constructor
   * @param os Input stream to read
   * @throws EoulsanIOException if the stream is null
   */
  public SimpleDesignWriter(final OutputStream os) throws EoulsanIOException {
    super(os);
  }

  /**
   * Public constructor
   * @param filename File to write
   * @throws EoulsanIOException if the stream is null
   * @throws FileNotFoundException if the file doesn't exist
   */
  public SimpleDesignWriter(final String filename) throws EoulsanIOException,
      FileNotFoundException {

    this(new FileOutputStream(filename));
  }
}
