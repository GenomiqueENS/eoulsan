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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.design.DesignMetadata.ADDITIONAL_ANNOTATION_FILE_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.DesignMetadata.GENOME_FILE_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.DesignMetadata.GFF_FILE_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.DesignMetadata.GTF_FILE_KEY;
import static java.util.Collections.unmodifiableMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignFactory;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.design.SampleMetadata;

/**
 * This class define a design reader for limma design files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Eoulsan1DesignReader implements DesignReader {

  private final static String TAB_SEPARATOR = "\t";

  // For backward compatibility
  static final String SAMPLE_NUMBER_FIELD = "SampleNumber";
  static final String SAMPLE_NAME_FIELD = "Name";
  static final String FILENAME_FIELD = "FileName";
  static final String READS_FIELD = "Reads";
  static final String EXPERIMENT_FIELD = "Experiment";

  private final InputStream is;

  private Map<String, String> defineDesignMetadataFields() {

    final Map<String, String> map = new HashMap<>();
    map.put("Genome", GENOME_FILE_KEY);
    map.put("Annotation", GFF_FILE_KEY);
    map.put("AdditionalAnnotation", ADDITIONAL_ANNOTATION_FILE_KEY);

    map.put(GENOME_FILE_KEY, GENOME_FILE_KEY);
    map.put(GFF_FILE_KEY, GFF_FILE_KEY);
    map.put(GTF_FILE_KEY, GTF_FILE_KEY);
    map.put(ADDITIONAL_ANNOTATION_FILE_KEY, ADDITIONAL_ANNOTATION_FILE_KEY);

    return unmodifiableMap(map);
  }

  private Map<String, String> defineSampleMetadataFields() {

    final Map<String, String> map = new HashMap<>();
    map.put(READS_FIELD, SampleMetadata.READS_KEY);
    map.put("FastqFormat", SampleMetadata.FASTQ_FORMAT_KEY);
    map.put("Condition", SampleMetadata.CONDITION_KEY);
    map.put("RepTechGroup", SampleMetadata.REP_TECH_GROUP_KEY);
    map.put("Reference", SampleMetadata.REFERENCE_KEY);
    map.put("UUID", SampleMetadata.UUID_KEY);
    map.put("Operator", SampleMetadata.OPERATOR_KEY);

    return unmodifiableMap(map);
  }

  @Override
  public Design read() throws IOException {

    final List<String> fieldnames = new ArrayList<>();
    final Design design = DesignFactory.createEmptyDesign();

    try (final BufferedReader br = new BufferedReader(
        new InputStreamReader(this.is, Globals.DEFAULT_CHARSET))) {

      String line = null;

      boolean firstLine = true;
      // String ref = null;

      final Map<String, String> designMetadataFields =
          defineDesignMetadataFields();
      final Map<String, String> sampleMetadataFields =
          defineSampleMetadataFields();

      int idFieldIndex = -1;
      int nameFieldIndex = -1;
      int experimentFieldIndex = -1;

      while ((line = br.readLine()) != null) {

        final String empty = line.trim();
        if ("".equals(empty) || empty.startsWith("#")) {
          continue;
        }

        final String[] fields = line.split(TAB_SEPARATOR);

        if (firstLine) {

          for (int i = 0; i < fields.length; i++) {

            String field = fields[i].trim();

            if ("".equals(field)) {
              throw new IOException(
                  "Found an empty field name in design file header.");
            }

            // Compatibility with old design files
            if (field.equals(FILENAME_FIELD)) {
              field = SampleMetadata.READS_KEY;
              fields[i] = field;
            }

            if (fieldnames.contains(field)) {
              throw new IOException("There is two or more field \""
                  + field + "\" in design file header.");
            }

            fieldnames.add(field);

            switch (field) {

            case SAMPLE_NUMBER_FIELD:
              idFieldIndex = i;
              break;

            case SAMPLE_NAME_FIELD:
              nameFieldIndex = i;
              break;

            case EXPERIMENT_FIELD:
              experimentFieldIndex = i;
              break;

            default:
              break;
            }

          }

          if (idFieldIndex != 0) {
            throw new IOException("Invalid file format: "
                + "The \"" + SAMPLE_NUMBER_FIELD
                + "\" field is not the first field.");
          }

          if (nameFieldIndex != 1) {
            throw new IOException("Invalid file format: "
                + "The \"" + SAMPLE_NAME_FIELD
                + "\" field is not the second field.");
          }

          firstLine = false;
        } else {

          if (fields.length != fieldnames.size()) {
            throw new IOException("Invalid file format: "
                + "Found " + fields.length + " fields whereas "
                + fieldnames.size() + " are required in line: " + line);
          }

          Sample sample = null;

          for (int i = 0; i < fields.length; i++) {

            final String value = fields[i].trim();

            final String fieldName = fieldnames.get(i);

            if (i == idFieldIndex) {

              // Do nothing for the SampleNumber field
              continue;
            } else if (i == nameFieldIndex) {

              // The Name filed
              sample = design.addSample(Naming.toValidName(value));
              sample.setName(value);
            } else if (i == experimentFieldIndex) {

              // The Experiment field
              final String experimentId = Naming.toValidName(value);
              if (!design.containsExperiment(experimentId)) {
                design.addExperiment(experimentId);
                design.getExperiment(experimentId).setName(value);
              }
              design.getExperiment(experimentId).addSample(sample);
            } else {

              // Other fields

              if (designMetadataFields.containsKey(fieldName)) {

                final String mdKey = designMetadataFields.get(fieldName);

                if (!design.getMetadata().contains(mdKey)) {
                  design.getMetadata().set(mdKey, value);
                }
              } else {

                String mdKey;

                if (sampleMetadataFields.containsKey(fieldName)) {
                  mdKey = sampleMetadataFields.get(fieldName);
                } else {
                  mdKey = fieldName;
                }

                sample.getMetadata().set(mdKey, value);
              }
            }
          }
        }
      }
    }

    if (!fieldnames.contains(READS_FIELD)) {
      throw new IOException("Invalid file format: No Reads field");
    }

    return design;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param file file to read
   * @throws FileNotFoundException if the file cannot be found
   */
  public Eoulsan1DesignReader(final File file) throws FileNotFoundException {

    checkNotNull(file, "the file argument cannot be null");

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor.
   * @param is Input stream to read
   * @throws IOException if an error occurs while opening the file
   */
  public Eoulsan1DesignReader(final InputStream is) throws IOException {

    checkNotNull(is, "the is argument cannot be null");

    this.is = is;
  }

  /**
   * Public constructor.
   * @param file file to read
   * @throws IOException if an error occurs while opening the file
   */
  public Eoulsan1DesignReader(final DataFile file) throws IOException {

    checkNotNull(file, "the file argument cannot be null");

    this.is = file.open();
  }

  /**
   * Public constructor.
   * @param filename File to read
   * @throws FileNotFoundException if the file doesn't exist
   */
  public Eoulsan1DesignReader(final String filename)
      throws FileNotFoundException {

    checkNotNull(filename, "the filename argument cannot be null");

    this.is = new FileInputStream(filename);
  }
}
