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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.design.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.EXPERIMENT_FIELD;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.SAMPLE_NAME_FIELD;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.SAMPLE_NUMBER_FIELD;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;

/**
 * This class implements a writer for limma design files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Eoulsan1DesignWriter implements DesignWriter {

  private final OutputStream out;

  private BufferedWriter bw;
  private static final String SEPARATOR = "\t";
  private static final String NEWLINE = "\r\n"; // System.getProperty("line.separator");
  private boolean writeScanLabelsSettings = true;

  @Override
  public void write(final Design design) throws IOException {

    if (design == null) {
      throw new NullPointerException("Design is null");
    }

    this.bw = new BufferedWriter(
        new OutputStreamWriter(this.out, Globals.DEFAULT_CHARSET));

    // Insert the names of the fields for the columns
    this.bw.append(SAMPLE_NUMBER_FIELD);
    this.bw.append(SEPARATOR);
    this.bw.append(SAMPLE_NAME_FIELD);

    final List<String> sampleMDKeys =
        DesignUtils.getAllSamplesMetadataKeys(design);
    for (String key : sampleMDKeys) {

      this.bw.append(SEPARATOR);
      this.bw.append(key);
    }
    if (!design.getExperiments().isEmpty()) {
      this.bw.append(SEPARATOR);
      this.bw.append(EXPERIMENT_FIELD);
    }
    this.bw.append(NEWLINE);

    // Insert the metadata for each sample
    for (Sample sample : design.getSamples()) {

      this.bw.append("" + sample.getNumber());
      this.bw.append(SEPARATOR);
      this.bw.append(sample.getId());

      final SampleMetadata smd = sample.getMetadata();

      for (String key : sampleMDKeys) {

        this.bw.append(SEPARATOR);

        if (smd.contains(key)) {
          this.bw.append(smd.get(key));
        }
      }
      for (Experiment experiment : design.getExperiments()) {

        this.bw.append(SEPARATOR);
        this.bw.append(experiment.getId());
      }
      this.bw.append(NEWLINE);
    }

    this.bw.close();
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
   * @throws IOException if an error occurs while reading the file or if the
   *           file is null.
   */
  public Eoulsan1DesignWriter(final File file) throws IOException {

    checkNotNull(file, "file argument cannot be null");

    this.out = new FileOutputStream(file);
  }

  /**
   * Public constructor
   * @param out Input stream to read
   * @throws IOException if the stream is null
   */
  public Eoulsan1DesignWriter(final OutputStream out) throws IOException {

    checkNotNull(out, "out argument cannot be null");

    this.out = out;
  }

  /**
   * Public constructor
   * @param filename File to write
   * @throws IOException if the stream is null
   * @throws FileNotFoundException if the file doesn't exist
   */
  public Eoulsan1DesignWriter(final String filename)
      throws IOException, FileNotFoundException {

    checkNotNull(filename, "filename argument cannot be null");

    this.out = new FileOutputStream(filename);
  }
}
