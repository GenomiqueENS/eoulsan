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

package fr.ens.biologie.genomique.eoulsan.design;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.design.io.DefaultDesignReader;
import fr.ens.biologie.genomique.eoulsan.design.io.DesignReader;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * Utils methods for Design.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Xavier Bauquet
 */
public final class DesignUtils {

  /**
   * Show a design
   * @param design Design to show
   */
  public static void showDesign(final Design design) {

    checkNotNull(design, "design argument cannot be null");

    final StringBuilder sb = new StringBuilder();

    // Print the name and number of the design
    sb.append("Design: ");
    sb.append(design.getName());
    sb.append(" (");
    sb.append(design.getNumber());
    sb.append(")\n");

    // Print design metadata
    sb.append("Design metadata:\n");
    for (Map.Entry<String, String> e : design.getMetadata().entrySet()) {
      sb.append('\t');
      sb.append(e.getKey());
      sb.append('=');
      sb.append(e.getValue());
      sb.append('\n');
    }
    sb.append('\n');

    // Print experiment metadata
    sb.append("Experiments:\n");
    for (Experiment e : design.getExperiments()) {
      final String expId = e.getId();
      for (Map.Entry<String, String> m : e.getMetadata().entrySet()) {
        sb.append('\t');
        sb.append("Exp.");
        sb.append(expId);
        sb.append(".");
        sb.append(m.getKey());
        sb.append('=');
        sb.append(m.getValue());
        sb.append('\n');
      }
      sb.append('\n');
    }
    sb.append('\n');

    //
    // Print column names
    //
    sb.append("SampleId");
    sb.append('\t');
    sb.append("SampleNumber");
    sb.append('\t');
    sb.append("SampleName");

    final List<String> sampleMDKeys = getAllSamplesMetadataKeys(design);

    // Print common column names
    for (String key : sampleMDKeys) {
      sb.append('\t');
      sb.append(key);
    }

    // Print experiments column names
    for (Experiment experiment : design.getExperiments()) {

      final String prefix = "Exp." + experiment.getId() + ".";

      final List<String> experimentMDKeys =
          getExperimentSampleAllMetadataKeys(experiment);
      for (String key : experimentMDKeys) {

        sb.append('\t');
        sb.append(prefix);
        sb.append(key);
      }
    }

    sb.append('\n');

    // Print samples metadata
    for (Sample sample : design.getSamples()) {

      sb.append(sample.getId());
      sb.append('\t');
      sb.append(sample.getNumber());
      sb.append('\t');
      sb.append(sample.getName());

      final SampleMetadata smd = sample.getMetadata();

      for (String key : sampleMDKeys) {

        sb.append('\t');

        if (smd.contains(key)) {
          sb.append(smd.get(key));
        }
      }

      for (Experiment experiment : design.getExperiments()) {

        final ExperimentSampleMetadata expSampleMetadata =
            experiment.getExperimentSample(sample).getMetadata();

        final List<String> experimentMDKeys =
            getExperimentSampleAllMetadataKeys(experiment);

        for (String key : experimentMDKeys) {

          sb.append('\t');

          if (expSampleMetadata.contains(key)) {
            sb.append(expSampleMetadata.get(key));
          }
        }

      }
      sb.append('\n');
    }

    System.out.println(sb.toString());
  }

  /**
   * Get all the sample metadata keys of the samples of a design.
   * @param design the design
   * @return a list with the sample metadata keys of the samples of a design
   */
  public static List<String> getAllSamplesMetadataKeys(final Design design) {

    checkNotNull(design, "design argument cannot be null");

    final List<String> result = new ArrayList<>();
    final Set<String> keys = new HashSet<>();

    for (Sample sample : design.getSamples()) {
      for (String key : sample.getMetadata().keySet()) {

        if (keys.contains(key)) {
          continue;
        }

        keys.add(key);
        result.add(key);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Get all the experiment metadata keys of the samples of a design.
   * @param experiment the experiment
   * @return a list with the experiment metadata keys of the samples of a design
   */
  public static List<String> getExperimentSampleAllMetadataKeys(
      final Experiment experiment) {

    checkNotNull(experiment, "design argument cannot be null");

    final List<String> result = new ArrayList<>();
    final Set<String> keys = new HashSet<>();

    for (ExperimentSample sample : experiment.getExperimentSamples()) {
      for (String key : sample.getMetadata().keySet()) {

        if (keys.contains(key)) {
          continue;
        }

        keys.add(key);
        result.add(key);
      }
    }

    return Collections.unmodifiableList(result);
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

        if (samplesSources.contains(fileSource)) {
          return false;
        }
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

        if (samplesSources.contains(fileSource)) {
          throw new EoulsanException(
              "Error: The design contains one or more duplicate sample sources: "
                  + fileSource + " (sample " + s.getId() + ")");
        }

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

    return design.getMetadata().containsGenomeFile();
  }

  /**
   * Check if there is more than one annotation in the design
   * @param design Design to test
   * @return true if there is more than one annotation in the genome
   */
  public static boolean checkAnnotations(final Design design) {

    return design.getMetadata().containsGffFile();
  }

  /**
   * Read and Check design
   * @param is InputStream for the design
   * @return a Design object
   * @throws EoulsanException if an error occurs while reading the design
   */
  public static Design readAndCheckDesign(final InputStream is)
      throws EoulsanException {

    try {
      final DesignReader dr = new DefaultDesignReader(is);
      final Design design = dr.read();

      DesignUtils.checkSamplesWithException(design);

      if (!DesignUtils.checkGenomes(design)) {
        throw new EoulsanException(
            "Warning: The design contains more than one genome file.");
      }

      if (!DesignUtils.checkAnnotations(design)) {
        throw new EoulsanException(
            "Warning: The design contains more than one annotation file.");
      }

      return design;

    } catch (IOException e) {
      throw new EoulsanException(e);
    }
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

    removeSampleMedataIfExists(design, SampleMetadata.COMMENT_KEY);
    removeSampleMedataIfExists(design, SampleMetadata.DATE_KEY);
    removeSampleMedataIfExists(design, SampleMetadata.OPERATOR_KEY);

    if (removeReplicateInformation) {
      removeExperimentSampleMedataIfExists(design,
          ExperimentSampleMetadata.CONDITION_KEY);
      removeExperimentSampleMedataIfExists(design,
          ExperimentSampleMetadata.REP_TECH_GROUP_KEY);
      removeExperimentSampleMedataIfExists(design,
          ExperimentSampleMetadata.REFERENCE_KEY);
    }

    final Map<Experiment, Integer> mapExperiment = new HashMap<>();
    final Map<String, Integer> mapCondition = new HashMap<>();
    final Map<String, Integer> mapRepTechGroup = new HashMap<>();
    int countExperiment = 0;
    int countCondition = 0;
    int countRepTechGroup = 0;

    for (Experiment exp : design.getExperiments()) {

      if (!mapExperiment.containsKey(exp)) {
        mapExperiment.put(exp, ++countExperiment);
      }

      exp.setName("e" + mapExperiment.get(exp));

      for (ExperimentSample es : exp.getExperimentSamples()) {

        ExperimentSampleMetadata esmd = es.getMetadata();

        // Obfuscate Condition field
        if (esmd.containsCondition()) {
          final String condition = esmd.getCondition();

          if (!mapCondition.containsKey(condition)) {
            mapCondition.put(condition, ++countCondition);
          }

          esmd.setCondition("c" + mapCondition.get(condition));
        }

        // Obfuscate RepTechGroup field
        if (esmd.containsRepTechGroup()) {
          final String rtg = esmd.getRepTechGroup();

          if (!mapRepTechGroup.containsKey(rtg)) {
            mapRepTechGroup.put(rtg, ++countRepTechGroup);
          }

          esmd.setRepTechGroup("g" + mapRepTechGroup.get(rtg));
        }

      }

    }

    for (Sample s : design.getSamples()) {

      final String newSampleName = "s" + s.getId();
      if (!newSampleName.equals(s.getName())) {
        s.setName(newSampleName);
      }
    }

  }

  private static void removeSampleMedataIfExists(final Design design,
      final String fieldName) {

    if (design == null || fieldName == null) {
      return;
    }

    for (Sample sample : design.getSamples()) {

      SampleMetadata smd = sample.getMetadata();

      if (smd.contains(fieldName)) {
        smd.remove(fieldName);
      }
    }

  }

  private static void removeExperimentSampleMedataIfExists(final Design design,
      final String fieldName) {

    if (design == null || fieldName == null) {
      return;
    }

    for (Experiment experiment : design.getExperiments()) {

      for (ExperimentSample expSample : experiment.getExperimentSamples()) {

        ExperimentSampleMetadata esmd = expSample.getMetadata();

        if (esmd.contains(fieldName)) {
          esmd.remove(fieldName);
        }
      }
    }
  }

  /**
   * Replace the local paths in the design by paths to symbolic links in a
   * directory.
   * @param design Design object to modify
   * @param symlinksDir path to the directory where create symbolic links
   * @throws IOException if an error occurs while creating symbolic links of if
   *           a path the design file does not exists
   */
  public static void replaceLocalPathBySymlinks(final Design design,
      final DataFile symlinksDir) throws IOException {

    if (design == null) {
      return;
    }

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();
    final Set<String> createdLinks = new HashSet<>();

    //
    // Design metadata
    //

    final List<String> designKeysToModify = new ArrayList<>();

    for (String field : design.getMetadata().keySet()) {
      if (registry.getDataFormatForDesignMetadata(field) != null) {
        designKeysToModify.add(field);
      }
    }

    final DesignMetadata dmd = design.getMetadata();
    for (final String field : designKeysToModify) {
      dmd.set(field, replaceLocalPathBySymlinks(dmd.getAsList(field),
          symlinksDir, createdLinks));
    }

    //
    // Sample metadata
    //

    final Set<String> sampleKeysToModify = new HashSet<>();

    for (final Sample s : design.getSamples()) {
      for (String field : s.getMetadata().keySet()) {
        if (registry.getDataFormatForSampleMetadata(field) != null) {
          sampleKeysToModify.add(field);
        }
      }
    }

    for (final Sample s : design.getSamples()) {

      final SampleMetadata smd = s.getMetadata();
      for (final String field : sampleKeysToModify) {
        smd.set(field, replaceLocalPathBySymlinks(smd.getAsList(field),
            symlinksDir, createdLinks));
      }
    }
  }

  /**
   * Replace values with the path of a symbolic link that will be created by
   * this method.
   * @param values the values to change
   * @param symlinksDir the directory of the symbolic links
   * @param createdLinks a set with the name of the created symbolic link
   * @return a list with the new values
   * @throws IOException if the link cannot be created
   */
  private static List<String> replaceLocalPathBySymlinks(List<String> values,
      final DataFile symlinksDir, Set<String> createdLinks) throws IOException {

    final List<String> result = new ArrayList<>();

    for (String inputPath : values) {

      final DataFile inFile = new DataFile(inputPath);

      if (inFile.isLocalFile()) {

        String linkName = findLinkFilename(createdLinks, inFile.getName());

        final DataFile outFile = new DataFile(symlinksDir, linkName);

        if (!inFile.exists()) {
          throw new IOException("File not exists: " + inFile);
        }

        if (outFile.exists()) {
          throw new IOException(
              "The symlink to create, already exists: " + outFile);
        }

        try {
          inFile.symlink(outFile);
        } catch (IOException e) {
          throw new IOException("Cannot create symlink: " + outFile, e);
        }

        createdLinks.add(linkName);
        result.add(outFile.getName());
      } else {
        result.add(inputPath);
      }
    }

    return result;
  }

  /**
   * Find a link filename that has not been yet used.
   * @param createdLinks the created links
   * @param filename the filename to create
   * @return the filename to create
   */
  private static String findLinkFilename(Set<String> createdLinks,
      String filename) {

    if (!createdLinks.contains(filename)) {
      return filename;
    }

    // Get the basename of the file and its extensions
    String compressionExtension = StringUtils.compressionExtension(filename);
    String extension =
        StringUtils.extensionWithoutCompressionExtension(filename);
    String basename = filename.substring(0,
        filename.length() - compressionExtension.length() - extension.length());

    int count = 1;
    String newName;

    // Find a non used filename
    do {

      count++;
      newName = basename + '_' + count + extension + compressionExtension;

    } while (createdLinks.contains(newName));

    return newName;
  }

  /**
   * Get the Condition metadata value for an experimentSample. First look in
   * @param experiment the experiment
   * @param sample the sample
   * @return the Condition value
   */
  public static String getCondition(final Experiment experiment,
      final Sample sample) {

    checkNotNull(experiment, "experiment argument cannot be null");
    checkNotNull(sample, "sample argument cannot be null");

    final ExperimentSample es = experiment.getExperimentSample(sample);

    return getCondition(es);
  }

  /**
   * Get the Condition metadata value for an experimentSample. First look in
   * @param experimentSample the experiment sample
   * @return the Condition value
   */
  public static String getCondition(final ExperimentSample experimentSample) {

    checkNotNull(experimentSample, "experimentSample argument cannot be null");

    final ExperimentSampleMetadata esm = experimentSample.getMetadata();

    if (esm.containsCondition()) {
      return esm.getCondition();
    }

    final SampleMetadata sm = experimentSample.getSample().getMetadata();

    final String result = sm.getCondition();

    return result == null ? null : result.trim();
  }

  /**
   * Get the RepTechGroup metadata value for an experimentSample. First look in
   * @param experiment the experiment
   * @param sample the sample
   * @return the Condition value
   */
  public static String getRepTechGroup(final Experiment experiment,
      final Sample sample) {

    checkNotNull(experiment, "experiment argument cannot be null");
    checkNotNull(sample, "sample argument cannot be null");

    final ExperimentSample es = experiment.getExperimentSample(sample);

    return getRepTechGroup(es);
  }

  /**
   * Get the Condition metadata value for an experimentSample. First look in
   * @param experimentSample the experiment sample
   * @return the Condition value
   */
  public static String getRepTechGroup(
      final ExperimentSample experimentSample) {

    checkNotNull(experimentSample, "experimentSample argument cannot be null");

    final ExperimentSampleMetadata esm = experimentSample.getMetadata();

    if (esm.containsRepTechGroup()) {
      return esm.getRepTechGroup();
    }

    final SampleMetadata sm = experimentSample.getSample().getMetadata();

    final String result = sm.getRepTechGroup();

    return result == null ? null : result.trim();
  }

  /**
   * Test if an experiement is skipped.
   * @param experiment the experiment
   * @return true if the experiment must be skipped
   */
  public static boolean isSkipped(final Experiment experiment) {

    checkNotNull(experiment, "experiment argument cannot be null");

    final ExperimentMetadata emd = experiment.getMetadata();

    return emd.containsSkip() && emd.isSkip();
  }

  /**
   * Test if an experiment contains reference fields
   * @return true if an experiment contains reference fields
   */
  public static boolean containsReferenceField(final Experiment experiment) {

    checkNotNull(experiment, "experiment argument cannot be null");

    for (ExperimentSample es : experiment.getExperimentSamples()) {

      final ExperimentSampleMetadata esmd = es.getMetadata();

      if (esmd.containsReference()) {
        return true;
      }

      final SampleMetadata smd = es.getSample().getMetadata();

      if (smd.containsReference()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Get the reference of a sample.
   * @param experiment the experiment
   * @param sample the sample
   * @return the reference of a sample
   */
  public static String getReference(final Experiment experiment,
      final Sample sample) {

    checkNotNull(experiment, "experiment argument cannot be null");
    checkNotNull(sample, "sample argument cannot be null");

    final ExperimentSample es = experiment.getExperimentSample(sample);

    return getReference(es);
  }

  /**
   * Get the reference of a sample.
   * @param experimentSample the experiment sample
   * @return the reference of a sample
   */
  public static String getReference(final ExperimentSample experimentSample) {

    checkNotNull(experimentSample, "experimentSample argument cannot be null");

    final ExperimentSampleMetadata esmd = experimentSample.getMetadata();

    if (esmd.containsReference()) {
      return esmd.getReference();
    }

    final SampleMetadata smd = experimentSample.getSample().getMetadata();

    if (smd.containsReference()) {
      return smd.getReference();
    }

    return null;
  }

  /**
   * Convert a reference value to an integer.
   * @param value the reference value
   * @return an integer
   */
  public static int referenceValueToInt(final String value,
      final String experiementReference) {

    if (value == null) {
      return 0;
    }

    final String s = value.trim();

    if (s.equals(experiementReference)) {
      return 1;
    }

    switch (s.toLowerCase()) {
    case "t":
    case "true":
    case "y":
    case "yes":
      return 1;

    default:

      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
  }

}
