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

import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.UUID_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.EXPERIMENT_FIELD;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.SAMPLE_NAME_FIELD;
import static fr.ens.biologie.genomique.eoulsan.design.io.Eoulsan1DesignReader.SAMPLE_NUMBER_FIELD;
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

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignMetadata;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSampleMetadata;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;

/**
 * This class implements a writer for limma design files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Eoulsan1DesignWriter implements DesignWriter {

  private final OutputStream out;

  private static final String SEPARATOR = "\t";
  private static final String NEWLINE = "\r\n"; // System.getProperty("line.separator");
  private boolean writeScanLabelsSettings = true;

  @Override
  public void write(final Design design) throws IOException {

    if (design == null) {
      throw new NullPointerException("Design is null");
    }

    final BufferedWriter bw = new BufferedWriter(
        new OutputStreamWriter(this.out, Globals.DEFAULT_CHARSET));

    // Insert the names of the fields for the columns
    bw.append(SAMPLE_NUMBER_FIELD);
    bw.append(SEPARATOR);
    bw.append(SAMPLE_NAME_FIELD);

    final List<String> sampleMDKeys =
        DesignUtils.getAllSamplesMetadataKeys(design);

    for (String key : sampleMDKeys) {

      // The UUID must be the last field
      if (SampleMetadata.UUID_KEY.equals(key)) {
        continue;
      }

      bw.append(SEPARATOR);
      bw.append(key);
    }

    if (design.getMetadata().containsGenomeFile()) {
      bw.append(SEPARATOR);
      bw.append("Genome");
    }
    if (design.getMetadata().containsGffFile()) {
      bw.append(SEPARATOR);
      bw.append("Annotation");
    }
    if (design.getMetadata().containsGtfFile()) {
      bw.append(SEPARATOR);
      bw.append(DesignMetadata.GFF_FILE_KEY);
    }
    if (design.getMetadata().containsAdditionalAnnotationFile()) {
      bw.append(SEPARATOR);
      bw.append("AdditionalAnnotation");
    }

    final Experiment exp = design.getExperiments().isEmpty()
        ? null : design.getExperiments().get(0);
    final List<String> esmdk = exp == null
        ? null : DesignUtils.getExperimentSampleAllMetadataKeys(exp);

    if (exp != null) {
      bw.append(SEPARATOR);
      bw.append(EXPERIMENT_FIELD);

      if (esmdk.contains(ExperimentSampleMetadata.CONDITION_KEY)) {
        bw.append(SEPARATOR);
        bw.append("Condition");
      }
      if (esmdk.contains(ExperimentSampleMetadata.REP_TECH_GROUP_KEY)) {
        bw.append(SEPARATOR);
        bw.append("RepTechGroup");
      }
      if (esmdk.contains(ExperimentSampleMetadata.REFERENCE_KEY)) {
        bw.append(SEPARATOR);
        bw.append("Reference");
      }

    }

    // The UUID if exists, must be the last field
    if (sampleMDKeys.contains(UUID_KEY)) {
      bw.append(TAB_SEPARATOR);
      bw.append(UUID_KEY);
    }

    bw.append(NEWLINE);

    // Insert the metadata for each sample
    for (Sample sample : design.getSamples()) {

      bw.append(Integer.toString(sample.getNumber()));
      bw.append(SEPARATOR);
      bw.append(sample.getId());

      final SampleMetadata smd = sample.getMetadata();

      // Sample keys
      for (String key : sampleMDKeys) {

        // The UUID must be the last field
        if (UUID_KEY.equals(key)) {
          continue;
        }

        bw.append(SEPARATOR);

        if (smd.contains(key)) {
          bw.append(smd.get(key));
        }
      }

      // GenomeFile, GffFile, AdditionalAnnotationFile
      if (design.getMetadata().containsGenomeFile()) {
        bw.append(SEPARATOR);
        bw.append(design.getMetadata().getGenomeFile());
      }
      if (design.getMetadata().containsGffFile()) {
        bw.append(SEPARATOR);
        bw.append(design.getMetadata().getGffFile());
      }
      if (design.getMetadata().containsGtfFile()) {
        bw.append(SEPARATOR);
        bw.append(design.getMetadata().getGtfFile());
      }
      if (design.getMetadata().containsAdditionalAnnotationFile()) {
        bw.append(SEPARATOR);
        bw.append(design.getMetadata().getAdditionalAnnotationFile());
      }

      // Experiment keys

      // Experiment sample keys
      if (exp != null) {
        bw.append(SEPARATOR);
        bw.append(exp.getId());

        ExperimentSample es = exp.getExperimentSample(sample);

        if (esmdk.contains(ExperimentSampleMetadata.CONDITION_KEY)) {
          bw.append(SEPARATOR);
          if (es.getMetadata().containsCondition()) {
            bw.append(es.getMetadata().getCondition());
          }
        }
        if (esmdk.contains(ExperimentSampleMetadata.REP_TECH_GROUP_KEY)) {
          bw.append(SEPARATOR);
          if (es.getMetadata().containsRepTechGroup()) {
            bw.append(es.getMetadata().getRepTechGroup());
          }
        }
        if (esmdk.contains(ExperimentSampleMetadata.REFERENCE_KEY)) {
          bw.append(SEPARATOR);
          if (es.getMetadata().containsReference()) {
            bw.append(es.getMetadata().getReference());
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

    requireNonNull(file, "file argument cannot be null");

    this.out = new FileOutputStream(file);
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while reading the file or if the
   *           file is null.
   */
  public Eoulsan1DesignWriter(final DataFile file) throws IOException {

    requireNonNull(file, "file argument cannot be null");

    this.out = file.create();
  }

  /**
   * Public constructor.
   * @param out Output stream to read
   * @throws IOException if the stream is null
   */
  public Eoulsan1DesignWriter(final OutputStream out) throws IOException {

    requireNonNull(out, "out argument cannot be null");

    this.out = out;
  }

  /**
   * Public constructor.
   * @param filename File to write
   * @throws FileNotFoundException if the file doesn't exist
   */
  public Eoulsan1DesignWriter(final String filename)
      throws FileNotFoundException {

    requireNonNull(filename, "filename argument cannot be null");

    this.out = new FileOutputStream(filename);
  }

}
