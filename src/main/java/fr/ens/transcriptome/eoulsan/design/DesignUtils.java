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

package fr.ens.transcriptome.eoulsan.design;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * Utils methods for Design.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class DesignUtils {

  /**
   * Show a design
   * @param design Design to show
   */
  public static void showDesign(final Design design) {

    List<String> metadataFields = design.getMetadataFieldsNames();

    StringBuilder sb = new StringBuilder();

    // Write header
    sb.append(Design.SAMPLE_NUMBER_FIELD);
    sb.append("\t");
    sb.append(Design.NAME_FIELD);
    sb.append("\t");

    for (String f : metadataFields) {

      sb.append("\t");
      sb.append(f);
    }

    System.out.println(sb.toString());

    // Write data
    List<Sample> slides = design.getSamples();

    for (Sample s : slides) {

      sb.setLength(0);

      sb.append(s.getId());
      sb.append("\t");

      sb.append(s.getName());
      sb.append("\t");

      for (String f : metadataFields) {

        sb.append("\t");
        sb.append(s.getMetadata().getField(f));
      }

      System.out.println(sb.toString());
    }

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DesignUtils() {
  }

  /**
   * Check if there are duplicate samples in the design.
   * @param design Design to test
   * @return if there are no duplicate
   */
  public static boolean checkSamples(final Design design) {

    final Set<String> samplesSources = new HashSet<>();

    for (Sample s : design.getSamples()) {
      for (String fileSource : s.getMetadata().getReads()) {

        if (samplesSources.contains(fileSource))
          return false;
        samplesSources.add(fileSource);
      }
    }

    return true;
  }

  /**
   * Check if there are duplicate samples in the design.
   * @param design Design to test
   * @return if there are no duplicate
   * @throws EoulsanException if a source is a duplicate
   */
  private static boolean checkSamplesWithException(final Design design)
      throws EoulsanException {

    final Set<String> samplesSources = new HashSet<>();

    for (Sample s : design.getSamples()) {
      for (String fileSource : s.getMetadata().getReads()) {

        if (samplesSources.contains(fileSource))
          throw new EoulsanException(
              "Error: The design contains one or more duplicate sample sources: "
                  + fileSource + " (sample " + s.getId() + ")");

        samplesSources.add(fileSource);
      }
    }

    return true;
  }

  /**
   * Check if there is more than one genome in the design
   * @param design Design to test
   * @return true if there is more than one genome in the genome
   */
  public static boolean checkGenomes(final Design design) {

    if (!design.isMetadataField(SampleMetadata.GENOME_FIELD))
      return true;

    final Set<String> genomes = new HashSet<>();

    for (Sample s : design.getSamples()) {

      String genome = s.getMetadata().getGenome();

      if (genomes.size() == 1 && !genomes.contains(genome))
        return false;

      if (genomes.size() == 0)
        genomes.add(genome);
    }

    return true;
  }

  /**
   * Check if there is more than one annotation in the design
   * @param design Design to test
   * @return true if there is more than one annotation in the genome
   */
  public static boolean checkAnnotations(final Design design) {

    if (!design.isMetadataField(SampleMetadata.GENOME_FIELD))
      return true;

    final Set<String> annotations = new HashSet<>();

    for (Sample s : design.getSamples()) {

      String annotation = s.getMetadata().getAnnotation();

      if (annotations.size() == 1 && !annotations.contains(annotation))
        return false;

      if (annotations.size() == 0)
        annotations.add(annotation);
    }

    return true;
  }

  /**
   * Read and Check design
   * @param is InputStream for the design
   * @return a Design object
   * @throws EoulsanException if an error occurs while reading the design
   */
  public static Design readAndCheckDesign(final InputStream is)
      throws EoulsanException {

    final DesignReader dr = new SimpleDesignReader(is);
    final Design design = dr.read();

    DesignUtils.checkSamplesWithException(design);

    if (!DesignUtils.checkGenomes(design))
      throw new EoulsanException(
          "Warning: The design contains more than one genome file.");

    if (!DesignUtils.checkAnnotations(design))
      throw new EoulsanException(
          "Warning: The design contains more than one annotation file.");

    return design;
  }

  /**
   * Remove optional description fields and obfuscate condition field.
   * @param design design object to obfuscate
   * @param removeReplicateInformation if replicate information must be removed
   */
  public static void obfuscate(final Design design,
      final boolean removeReplicateInformation) {

    if (design == null) {
      return;
    }

    removeFieldIfExists(design, SampleMetadata.COMMENT_FIELD);
    removeFieldIfExists(design, SampleMetadata.DATE_FIELD);
    removeFieldIfExists(design, SampleMetadata.OPERATOR_FIELD);

    if (removeReplicateInformation) {
      removeFieldIfExists(design, SampleMetadata.EXPERIMENT_FIELD);
      removeFieldIfExists(design, SampleMetadata.CONDITION_FIELD);
      removeFieldIfExists(design, SampleMetadata.REP_TECH_GROUP_FIELD);
      removeFieldIfExists(design, SampleMetadata.REFERENCE_FIELD);
    }

    final Map<String, Integer> mapExperiment = new HashMap<>();
    final Map<String, Integer> mapCondition = new HashMap<>();
    final Map<String, Integer> mapRepTechGroup = new HashMap<>();
    int countExperiment = 0;
    int countCondition = 0;
    int countRepTechGroup = 0;

    for (Sample s : design.getSamples()) {

      final String newSampleName = "s" + s.getId();
      if (!newSampleName.equals(s.getName()))
        s.setName(newSampleName);

      // Obfuscate Experiment field
      if (design.isMetadataField(SampleMetadata.EXPERIMENT_FIELD)) {
        final String exp = s.getMetadata().getExperiment();

        if (!mapExperiment.containsKey(exp)) {
          mapExperiment.put(exp, ++countExperiment);
        }

        s.getMetadata().setExperiment("e" + mapExperiment.get(exp));
      }

      // Obfuscate Condition field
      if (design.isMetadataField(SampleMetadata.CONDITION_FIELD)) {
        final String cond = s.getMetadata().getCondition();

        if (!mapCondition.containsKey(cond)) {
          mapCondition.put(cond, ++countCondition);
        }

        s.getMetadata().setCondition("c" + mapCondition.get(cond));
      }

      // Obfuscate RepTechGroup field
      if (design.isMetadataField(SampleMetadata.REP_TECH_GROUP_FIELD)) {
        final String rtg = s.getMetadata().getRepTechGroup();

        if (!mapRepTechGroup.containsKey(rtg)) {
          mapRepTechGroup.put(rtg, ++countRepTechGroup);
        }

        s.getMetadata().setRepTechGroup("g" + mapRepTechGroup.get(rtg));
      }

    }
  }

  private static final void removeFieldIfExists(final Design design,
      final String fieldName) {

    if (design == null || fieldName == null) {
      return;
    }

    if (design.isMetadataField(fieldName)) {
      design.removeMetadataField(fieldName);
    }

  }

  /**
   * Replace the local paths in the design by paths to symbolic links in a
   * directory.
   * @param design Design object to modify
   * @param symlinksDir path to the directory where create symbolic links
   * @throws EoulsanIOException if an error occurs while creating symbolic links
   *           of if a path the design file does not exists
   */
  public static void replaceLocalPathBySymlinks(final Design design,
      final File symlinksDir) throws EoulsanIOException {

    if (design == null)
      return;

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    final List<String> fieldsToModify = new ArrayList<>();

    for (String field : design.getMetadataFieldsNames())
      if (registry.getDataFormatForDesignField(field) != null)
        fieldsToModify.add(field);

    for (final Sample s : design.getSamples())
      for (final String field : fieldsToModify) {

        final List<String> values =
            new ArrayList<>(s.getMetadata().getFieldAsList(field));
        for (int i = 0; i < values.size(); i++) {

          final DataFile df = new DataFile(values.get(i));

          if (df.isLocalFile()) {

            final File inFile = df.toFile();
            final File outFile = new File(symlinksDir, df.getName());

            if (!inFile.exists())
              throw new EoulsanIOException("File not exists: " + df);

            if (outFile.exists())
              throw new EoulsanIOException(
                  "The symlink to create, already exists: " + outFile);

            if (!FileUtils.createSymbolicLink(df.toFile(), outFile))
              throw new EoulsanIOException("Cannot create symlink: " + outFile);

            values.set(i, df.getName());
          }

        }
        s.getMetadata().setField(field, values);

      }

  }

}
