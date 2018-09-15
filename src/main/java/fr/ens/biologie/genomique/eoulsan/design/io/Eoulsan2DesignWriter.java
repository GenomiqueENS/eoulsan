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

package fr.ens.biologie.genomique.eoulsan.design.io;

import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getAllSamplesMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getExperimentSampleAllMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.UUID_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.DOT_SEPARATOR;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.EQUAL_SEPARATOR;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.EXPERIMENT_FIELD_PREFIX;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.EXPERIMENT_NAME_SUFFIX;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.SAMPLE_ID_FIELDNAME;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.SAMPLE_NAME_FIELDNAME;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan2DesignReader.TAB_SEPARATOR;
import static java.util.Objects.requireNonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSampleMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;

/**
 * This class implements a writer for Eoulsan 2 design files.
 * @since 2.0
 * @author Xavier Bauquet
 */

public class Eoulsan2DesignWriter implements DesignWriter {

  private final OutputStream out;

  private static final String HEADER_SECTION = "[Header]";
  private static final String EXPERIMENT_SECTION = "[Experiments]";
  private static final String COLUMN_SECTION = "[Columns]";

  private static final String NEWLINE = "\r\n";

  @Override
  public void write(final Design design) throws IOException {

    requireNonNull(design, "Design argument cannot be null");

    final BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(this.out, Globals.DEFAULT_CHARSET));

    requireNonNull(design, "design argument cannot be null");

    // Write design metadata
    bw.append(HEADER_SECTION);
    bw.append(NEWLINE);
    bw.append(Eoulsan2DesignReader.DESIGN_FORMAT_VERSION_METADATA_KEY);
    bw.append(EQUAL_SEPARATOR);
    bw.append(Eoulsan2DesignReader.FORMAT_VERSION);
    bw.append(NEWLINE);

    for (Map.Entry<String, String> e : design.getMetadata().entrySet()) {
      bw.append(e.getKey());
      bw.append(EQUAL_SEPARATOR);
      bw.append(e.getValue());
      bw.append(NEWLINE);
    }
    bw.append(NEWLINE);

    // Write experiment metadata
    if (!design.getExperiments().isEmpty()) {
      bw.append(EXPERIMENT_SECTION);
      bw.append(NEWLINE);
    }
    for (Experiment e : design.getExperiments()) {
      final String expId = e.getId();
      bw.append(EXPERIMENT_FIELD_PREFIX);
      bw.append(expId).append(DOT_SEPARATOR);
      bw.append(EXPERIMENT_NAME_SUFFIX);
      bw.append(EQUAL_SEPARATOR);
      bw.append(e.getName());
      bw.append(NEWLINE);
      for (Map.Entry<String, String> m : e.getMetadata().entrySet()) {
        bw.append(EXPERIMENT_FIELD_PREFIX);
        bw.append(expId);
        bw.append(DOT_SEPARATOR);
        bw.append(m.getKey());
        bw.append(EQUAL_SEPARATOR);
        bw.append(m.getValue());
        bw.append(NEWLINE);
      }
      bw.append(NEWLINE);
    }

    // Write column names
    bw.append(COLUMN_SECTION);
    bw.append(NEWLINE);

    //
    // Print column names
    //
    bw.append(SAMPLE_ID_FIELDNAME);
    bw.append(TAB_SEPARATOR);
    bw.append(SAMPLE_NAME_FIELDNAME);

    final List<String> sampleMDKeys = getAllSamplesMetadataKeys(design);

    // Print common column names
    for (String key : sampleMDKeys) {

      // The UUID must be the last field
      if (SampleMetadata.UUID_KEY.equals(key)) {
        continue;
      }

      bw.append(TAB_SEPARATOR);
      bw.append(key);
    }

    // Print experiments column names
    for (Experiment experiment : design.getExperiments()) {

      final String prefix =
          EXPERIMENT_FIELD_PREFIX + experiment.getId() + DOT_SEPARATOR;

      final List<String> experimentMDKeys =
          getExperimentSampleAllMetadataKeys(experiment);
      for (String key : experimentMDKeys) {

        bw.append(TAB_SEPARATOR);
        bw.append(prefix);
        bw.append(key);
      }
    }

    // The UUID if exists, must be the last field
    if (sampleMDKeys.contains(UUID_KEY)) {
      bw.append(TAB_SEPARATOR);
      bw.append(UUID_KEY);
    }

    bw.append(NEWLINE);

    // Print samples metadata
    for (Sample sample : design.getSamples()) {

      bw.append(sample.getId());
      bw.append(TAB_SEPARATOR);
      bw.append(sample.getName());

      final SampleMetadata smd = sample.getMetadata();

      for (String key : sampleMDKeys) {

        // The UUID must be the last field
        if (UUID_KEY.equals(key)) {
          continue;
        }

        bw.append(TAB_SEPARATOR);

        if (smd.contains(key)) {
          bw.append(smd.get(key));
        }
      }

      for (Experiment experiment : design.getExperiments()) {

        if (experiment.containsSample(sample)) {

          final ExperimentSampleMetadata expSampleMetadata =
              experiment.getExperimentSample(sample).getMetadata();

          final List<String> experimentMDKeys =
              getExperimentSampleAllMetadataKeys(experiment);

          for (String key : experimentMDKeys) {

            bw.append(TAB_SEPARATOR);

            if (expSampleMetadata.contains(key)) {
              bw.append(expSampleMetadata.get(key));
            }
          }
        }

      }

      // The UUID if exists, must be the last field
      if (sampleMDKeys.contains(UUID_KEY)) {
        bw.append(TAB_SEPARATOR);
        bw.append(smd.get(UUID_KEY));
      }

      bw.append(NEWLINE);
    }

    bw.close();
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while creating the file
   */
  public Eoulsan2DesignWriter(final File file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    this.out = new FileOutputStream(file);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while creating the file
   */
  public Eoulsan2DesignWriter(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    this.out = file.create();
  }

  /**
   * Public constructor.
   * @param out Output stream to read
   * @throws IOException if the stream cannot be created
   */
  public Eoulsan2DesignWriter(final OutputStream out) throws IOException {

    requireNonNull(out, "out argument cannot be null");

    this.out = out;
  }

  /**
   * Public constructor.
   * @param filename File to write
   * @throws FileNotFoundException if the file doesn't exist
   */
  public Eoulsan2DesignWriter(final String filename)
      throws FileNotFoundException {

    requireNonNull(filename, "filename argument cannot be null");

    this.out = new FileOutputStream(filename);
  }
}
