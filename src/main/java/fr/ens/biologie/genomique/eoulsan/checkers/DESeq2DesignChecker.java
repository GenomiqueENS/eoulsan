package fr.ens.biologie.genomique.eoulsan.checkers;

import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getAllSamplesMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.DesignUtils.getExperimentSampleAllMetadataKeys;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.CONDITION_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REFERENCE_KEY;
import static fr.ens.biologie.genomique.eoulsan.design.SampleMetadata.REP_TECH_GROUP_KEY;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentMetadata;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;

/**
 * This class define a Checker on the design for DESeq2 analyzes.
 * @since 2.4
 * @author Charlotte Berthelier
 */
public class DESeq2DesignChecker implements Checker, Serializable {

  private static final long serialVersionUID = -7079642248159153890L;

  @Override
  public String getName() {
    return "deseq2_design_checker";
  }

  @Override
  public boolean isDesignChecker() {
    return true;
  }

  @Override
  public DataFormat getFormat() {
    return null;
  }

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {
  }

  @Override
  public boolean check(final Data data, final CheckStore checkInfo)
      throws EoulsanException {

    // Get the design object
    Design design = (Design) checkInfo.get("design");

    // Check all experiments of the design
    for (Experiment e : design.getExperiments()) {

      if (!checkExperimentDesign(e, true)) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Set<DataFormat> getCheckersRequired() {
    return Collections.emptySet();
  }

  //
  // DESeq2 check methods
  //

  /**
   * Check experiment design.
   * @param experiment experiment to check
   * @return true if check pass
   * @throws EoulsanException if the experiment design is not correct
   */
  public static boolean checkExperimentDesign(final Experiment experiment)
      throws EoulsanException {

    return checkExperimentDesign(experiment, true);
  }

  /**
   * Check experiment design.
   * @param experiment experiment to check
   * @param throwsException if true throw an exception
   * @throws EoulsanException if the experiment design is not correct
   */
  static boolean checkExperimentDesign(Experiment experiment,
      boolean throwsException) throws EoulsanException {

    requireNonNull(experiment, "Experiment argument cannot be null");
    final ExperimentMetadata emd = experiment.getMetadata();
    final Design design = experiment.getDesign();

    // Get the name of all keys
    List<String> esColumnNames = getExperimentSampleAllMetadataKeys(experiment);
    List<String> sColumnNames = getAllSamplesMetadataKeys(design);
    List<String> allColumnNames = new ArrayList<>(esColumnNames);
    allColumnNames.addAll(sColumnNames);

    /*
     * Check if there is an empty cell in the experiment
     */
    for (String key : allColumnNames) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {
        String value = DesignUtils.getMetadata(es, key);
        if (Strings.isNullOrEmpty(value)) {
          return error("There is an empty cell" + experiment.getName(),
              throwsException);
        }
      }
    }

    /*
     * Check if the comparison string is correct
     */
    if (emd.containsComparisons()) {
      Set<String> comparisionNames = new HashSet<>();

      // Check if the comparison structure is correct
      for (String c :  Splitter.on(';').splitToList(emd.getComparisons())) {
        List<String> splitC =
            Splitter.on(':').omitEmptyStrings().trimResults().splitToList(c);

        // Check if there is not more than one value per comparison
        if (splitC.size() != 2) {
          return error("Error in "
              + experiment.getName()
              + " experiment, comparison cannot have more than 1 value: " + c,
              throwsException);
        }

        // Get the name of each comparison
        String comparison = splitC.get(1);
        if (comparison.equals("vs")
            || comparison.startsWith("_vs") || comparison.endsWith("vs_")) {
          return error("Error in "
              + experiment.getName()
              + " experiment, the comparison string is badly written : " + c,
              throwsException);
        } else if (!comparison.contains("_vs_")) {
          comparisionNames.add(comparison);
        }

        // Get each condition in the comparison string
        Set<String> conditionsInComparisonString =
            new HashSet<>(asList(comparison.split("(%)|(_vs_)")));

        // Get every sample value for each key, and get every possible condition
        // by merging the strings
        Set<String> possibleConditions = new HashSet<>();
        for (String key : allColumnNames) {
          for (ExperimentSample es : experiment.getExperimentSamples()) {
            String value = DesignUtils.getMetadata(es, key);
            possibleConditions.add(key + value);
          }
        }

        // Check if each conditions in the comparison string exist in the
        // Condition column
        for (String condi : conditionsInComparisonString) {
          Boolean exist = false;
          for (String s : possibleConditions) {
            if (s.equals(condi)) {
              exist = true;
            }
          }
          if (!exist) {
            return error("Error in "
                + experiment.getName() + " experiment, one comparison (" + condi
                + ") does not exist: " + c, throwsException);
          }
        }
      }

      // Check if the name of each comparison is unique
      Set<String> set = new HashSet<>();
      Set<String> duplicateElements = new HashSet<>();
      for (String element : comparisionNames) {
        if (!set.add(element)) {
          duplicateElements.add(element);
        }
      }
      if (!duplicateElements.isEmpty()) {
        return error("Error in "
            + experiment.getName() + " experiment, there is one or more "
            + "duplicates in the comparison string names", throwsException);
      }
    }

    /*
     * Check if there is no numeric character at the begin of a row in all
     * metakeys columns for a complex design model
     */
    for (String columnName : DesignUtils.getModelColumns(experiment)) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {

        String columnValue = DesignUtils.getMetadata(es, columnName);
        if (!columnValue.isEmpty()
            && Character.isDigit(columnValue.charAt(0))) {
          return error("The value of the \""
              + columnName + "\" column start with a numeric character for \""
              + es.getSample().getId() + "\" sample: " + columnValue,
              throwsException);
        }
      }
    }

    /*
     * Check if there is no undesirable special characters in the metakeys
     * columns or in the Condition column when the contrast mode is activate
     */
    for (String key : esColumnNames) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {
        String s = DesignUtils.getMetadata(es, key);
        if (!Pattern.matches("[a-zA-Z0-9\\_]+", s) && emd.isContrast()) {
          return error(
              "There is an undesirable special character in the column "
                  + key + " : " + s,
              throwsException);
        }
      }
    }
    if (sColumnNames.contains(CONDITION_KEY)) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {
        String s = DesignUtils.getMetadata(es, CONDITION_KEY);
        if (s.indexOf('-') != -1 && emd.isContrast()) {
          return error("There is a - character in the column "
              + CONDITION_KEY + " : " + s, throwsException);
        }
      }
    }
    /*
     * Check if there is no undesirable special characters in the metakeys
     * columns or in the Condition column when the contrast mode is not activate
     * and for a non complex design model
     */
    for (String key : esColumnNames) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {
        String s = DesignUtils.getMetadata(es, key);
        if (!emd.isContrast()
            && !emd.containsComparisons()
            && !Pattern.matches("^[a-zA-Z0-9\\+\\-\\&\\_\\/\\.\\[\\]]+$", s)) {
          return error(
              "There is a special character in the column " + key + " : " + s,
              throwsException);
        }
      }
    }
    //
    if (sColumnNames.contains(CONDITION_KEY)) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {
        String s = DesignUtils.getMetadata(es, CONDITION_KEY);
        if (!emd.isContrast()
            && !emd.containsComparisons()
            && !Pattern.matches("^[a-zA-Z0-9\\+\\-\\&\\_\\/\\.\\[\\]]+$", s)) {
          return error("There is a special character in the column "
              + CONDITION_KEY + " : " + s, throwsException);
        }
      }
    }

    /*
     * Verify consistency between the values in the columns Reference and
     * Condition for non complex mode
     */
    if (!emd.containsComparisons()) {
      // If Exp.exp1.Condition and Exp.exp1.Reference exist
      if (esColumnNames.contains(REFERENCE_KEY)
          && esColumnNames.contains(CONDITION_KEY)) {
        Map<String, String> lhm = new HashMap<>();

        // Get all samples values of the Condition and Reference columns
        for (ExperimentSample es : experiment.getExperimentSamples()) {
          String condition = DesignUtils.getMetadata(es, CONDITION_KEY);
          String reference = DesignUtils.getMetadata(es, REFERENCE_KEY);

          // Check if condition and reference are not null or empty
          if (condition == null
              || reference == null || condition.isEmpty()
              || reference.isEmpty()) {
            return error("There is an empty condition or reference "
                + experiment.getName(), throwsException);
          }

          // If one condition is associated with more than one reference, error
          for (Map.Entry<String, String> e : lhm.entrySet()) {
            if (e.getKey().equals(condition)
                && !e.getValue().equals(reference)) {
              return error(
                  "There is an inconsistency between the conditions "
                      + "and the references: " + experiment.getName(),
                  throwsException);
            }
          }
          lhm.put(condition, reference);
        }
      }
    }

    /*
     * Check if there is no combination column-name that is equal to another
     * column-name or to a column name
     */
    // Multimap containing every key and all sample values for each
    Multimap<String, String> mapPossibleCombination =
        ArrayListMultimap.create();
    for (String key : allColumnNames) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {
        String value = DesignUtils.getMetadata(es, key);
        mapPossibleCombination.put(key, value);
      }
    }

    // Test the coherence between the key and the values names
    for (String key : mapPossibleCombination.keySet()) {
      for (String value : mapPossibleCombination.values()) {
        for (String keybis : mapPossibleCombination.keySet()) {
          // keyValue contains all possible key+value combinations
          String keyValue = keybis + value;
          // Error if: key = value or key+value = key
          if (key.equals(value) || keyValue.equals(key)) {
            return error(
                "There is an incoherence between the key and the values names"
                    + experiment.getName(),
                throwsException);
          }
        }
      }
    }

    /*
     * Check if the column Condition is missing for the experiment
     */
    if (!esColumnNames.contains(CONDITION_KEY)
        && !sColumnNames.contains(CONDITION_KEY)) {
      return error(
          "Condition column missing for experiment: " + experiment.getName(),
          throwsException);
    }

    /*
     * Check if the column RepTechGroup is missing for the experiment
     */
    if (!esColumnNames.contains(REP_TECH_GROUP_KEY)
        && !sColumnNames.contains(REP_TECH_GROUP_KEY)) {
      return error(
          "RepTechGroup column missing for experiment: " + experiment.getName(),
          throwsException);
    }

    return true;
  }

  /**
   * Throw or not an exception.
   * @param message exception message
   * @param throwsException if true an exception will be thrown
   * @return always false
   * @throws EoulsanException if throwsException argument is set to true
   */
  private static boolean error(final String message,
      final boolean throwsException) throws EoulsanException {

    if (throwsException) {
      throw new EoulsanException(message);
    }

    return false;
  }

}
