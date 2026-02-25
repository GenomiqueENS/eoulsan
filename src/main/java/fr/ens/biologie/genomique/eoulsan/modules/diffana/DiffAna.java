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

package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class create and launch a R script to compute differential analysis.
 *
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
public class DiffAna extends Normalization {

  private static final String DISPERSION_ESTIMATION = "/DESeq1/dispersionEstimation.Rnw";
  private static final String ANADIFF_WITH_REFERENCE = "/DESeq1/anadiffWithReference.Rnw";
  private static final String ANADIFF_WITHOUT_REFERENCE = "/DESeq1/anadiffWithoutReference.Rnw";

  // dispersion estimation parameters
  private DispersionMethod dispEstMethod;
  private DispersionFitType dispEstFitType;
  private DispersionSharingMode dispEstSharingMode;

  //
  // enums
  //
  /** Dispersion estimation method enum for DESeq differential analysis */
  public enum DispersionMethod {
    POOLED("pooled"),
    PER_CONDITION("per-condition"),
    BLIND("blind");

    private final String name;

    /**
     * Get the dispersion estimation method
     *
     * @return a string with the dispersion estimation method
     */
    public String getName() {

      return this.name;
    }

    /**
     * Get the Dispersion estimation method form its name
     *
     * @param name dispersion estimation method name
     * @return a DispersionMethod or null if no DispersionMethod found for the name
     */
    public static DispersionMethod getDispEstMethodFromName(final String name) {

      if (name == null) {
        return null;
      }

      final String lowerName = name.trim().toLowerCase(Globals.DEFAULT_LOCALE);

      for (DispersionMethod dem : DispersionMethod.values()) {

        if (dem.getName().toLowerCase(Globals.DEFAULT_LOCALE).equals(lowerName)) {
          return dem;
        }
      }

      return null;
    }

    /**
     * Constructor
     *
     * @param method dispersion estimation method
     */
    DispersionMethod(final String method) {

      this.name = method;
    }
  }

  /** Dispersion estimation sharingMode enum for DESeq differential analysis */
  public enum DispersionSharingMode {
    FIT_ONLY("fit-only"),
    MAXIMUM("maximum"),
    GENE_EST_ONLY("gene-est-only");

    private final String name;

    /**
     * Get the dispersion estimation sharingMode name
     *
     * @return a string with the dispersion estimation sharingMode name
     */
    public String getName() {

      return this.name;
    }

    /**
     * Get the Dispersion estimation sharing mode form its name
     *
     * @param name dispersion estimation sharing mode name
     * @return a DispersionSharingMode or null if no DispersionSharingMode found for the name
     */
    public static DispersionSharingMode getDispEstSharingModeFromName(final String name) {

      if (name == null) {
        return null;
      }

      final String lowerName = name.trim().toLowerCase(Globals.DEFAULT_LOCALE);

      for (DispersionSharingMode desm : DispersionSharingMode.values()) {

        if (desm.getName().toLowerCase(Globals.DEFAULT_LOCALE).equals(lowerName)) {
          return desm;
        }
      }

      return null;
    }

    /**
     * Constructor
     *
     * @param name dispersion estimation sharingMode name
     */
    DispersionSharingMode(final String name) {

      this.name = name;
    }
  }

  /** Dispersion estimation fitType enum for DESeq differential analysis */
  public enum DispersionFitType {
    PARAMETRIC("parametric"),
    LOCAL("local");

    private final String name;

    /**
     * Get the dispersion estimation fitType name
     *
     * @return a string with the dispersion estimation fitType name
     */
    public String getName() {

      return this.name;
    }

    /**
     * Get the Dispersion estimation fit type form its name
     *
     * @param name dispersion estimation fit type name
     * @return a DispersionFitType or null if no DispersionFitType found for the name
     */
    public static DispersionFitType getDispEstFitTypeFromName(final String name) {

      if (name == null) {
        return null;
      }

      final String lowerName = name.trim().toLowerCase(Globals.DEFAULT_LOCALE);

      for (DispersionFitType deft : DispersionFitType.values()) {

        if (deft.getName().toLowerCase(Globals.DEFAULT_LOCALE).equals(lowerName)) {
          return deft;
        }
      }

      return null;
    }

    /**
     * Constructor
     *
     * @param name dispersion estimation fitType name
     */
    DispersionFitType(final String name) {

      this.name = name;
    }
  }

  //
  // Protected methods
  //

  @Override
  protected String generateScript(final Experiment experiment, final TaskContext context)
      throws EoulsanException {

    final String comparison = experiment.getMetadata().getComparisons();

    // Check if user use the comparison property of the experiment
    if (comparison != null && !comparison.trim().isEmpty()) {
      throw new EoulsanException(
          "The diffana step do not actually handle the comparison property of an experiment");
    }

    final Map<String, List<Integer>> conditionsMap = new HashMap<>();

    final List<Integer> rSampleIds = new ArrayList<>();
    final List<String> rSampleNames = new ArrayList<>();
    final List<String> rCondNames = new ArrayList<>();
    final List<String> rRepTechGroup = new ArrayList<>();
    int i = 0;

    // Get samples ids, conditions names/indexes and repTechGoups
    for (Sample s : experiment.getSamples()) {

      final String condition = DesignUtils.getCondition(experiment, s);

      if (condition == null) {
        throw new EoulsanException("No condition field found in design file.");
      }

      if ("".equals(condition)) {
        throw new EoulsanException(
            "No value for condition in sample: " + s.getName() + " (" + s.getId() + ")");
      }

      final String repTechGroup = DesignUtils.getRepTechGroup(experiment, s);

      if (repTechGroup != null && !"".equals(repTechGroup)) {
        rRepTechGroup.add(repTechGroup);
      }

      final List<Integer> index;
      if (!conditionsMap.containsKey(condition)) {
        index = new ArrayList<>();
        conditionsMap.put(condition, index);
      } else {
        index = conditionsMap.get(condition);
      }
      index.add(i);

      rSampleIds.add(s.getNumber());
      rSampleNames.add(s.getName());
      rCondNames.add(condition);

      i++;
    }

    checkRepTechGroupCoherence(rRepTechGroup, rCondNames);

    // Create Rnw script stringbuilder with preamble
    String pdfTitle = escapeUnderScore(experiment.getName()) + " differential analysis";
    String filePrefix = "diffana_" + escapeUnderScore(experiment.getName());

    final StringBuilder sb = generateRnwpreamble(experiment.getSamples(), pdfTitle, filePrefix);

    /*
     * Replace "na" values of repTechGroup by unique sample ids to avoid pooling
     * problem while executing R script
     */
    replaceRtgNA(rRepTechGroup, rSampleNames);

    // Add reference if there is one
    writeReferenceField(experiment, sb);

    // Add sampleNames vector
    generateSampleNamePart(rSampleNames, sb);

    // Add SampleIds vector
    generateSampleIdsPart(rSampleIds, sb);

    // Add file names vector
    generateExpressionFileNamesPart(sb);

    // Add repTechGroupVector
    generateRepTechGroupPart(rRepTechGroup, sb);

    // Add condition to R script
    generateConditionPart(rCondNames, sb);

    // Add projectPath, outPath and projectName
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- \"");
    sb.append(experiment.getName());
    sb.append("\"\n@\n\n");

    sb.append(readStaticScript(TARGET_CREATION));

    sb.append("\\section{Analysis}\n\n");
    sb.append("<<beginAnalysis>>=\n");

    // Add delete unexpressed gene call
    sb.append("target$counts <- deleteUnexpressedGene(target$counts)\n");

    if (isTechnicalReplicates(rRepTechGroup)) {
      sb.append("target <- poolTechRep(target)\n\n");
    }

    sb.append("target <- sortTarget(target)\n");
    sb.append("countDataSet <- normDESeq(target$counts, target$condition)\n");

    sb.append("@\n");

    // generate dispersion estimation part
    String dispersionEstimation = readStaticScript(DISPERSION_ESTIMATION);
    if (!isBiologicalReplicates(conditionsMap, rCondNames, rRepTechGroup)) {

      if (!(this.dispEstMethod == DispersionMethod.BLIND)
          || !(this.dispEstSharingMode == DispersionSharingMode.FIT_ONLY)) {
        throw new EoulsanException(
            "There is no replicates in this experiment, you have to use "
                + "disp_est_method=blind and disp_est_sharingMode=fit-only in "
                + "diffana parameters");
      }
    }

    dispersionEstimation = dispersionEstimation.replace("${METHOD}", this.dispEstMethod.getName());
    dispersionEstimation =
        dispersionEstimation.replace("${SHARINGMODE}", this.dispEstSharingMode.getName());
    dispersionEstimation =
        dispersionEstimation.replace("${FITTYPE}", this.dispEstFitType.getName());

    // Add dispersion estimation part to stringbuilder
    sb.append(dispersionEstimation);

    // Add plot dispersion
    if (this.dispEstMethod.equals(DispersionMethod.PER_CONDITION)) {

      List<String> passedConditionName = new ArrayList<>();
      for (String cond : rCondNames) {

        if (passedConditionName.indexOf(cond) == -1) {
          sb.append("<<dispersionPlot_");
          sb.append(cond);
          sb.append(", fig=TRUE>>=\n");
          sb.append("fitInfo <- fitInfo(countDataSet, name = \"");
          sb.append(cond);
          sb.append("\")\n");
          sb.append("plotDispEsts(countDataSet, fitInfo, \"");
          sb.append(cond);
          sb.append("\")\n");
          sb.append("@\n");

          passedConditionName.add(cond);
        }
      }
    } else {

      sb.append("<<dispersionPlot, fig=TRUE>>=\n");
      sb.append("fitInfo <- fitInfo(countDataSet)\n");
      sb.append("plotDispEsts(countDataSet, fitInfo)\n");
      sb.append("@\n");
    }

    String anadiffPart = "";

    // check if there is a reference
    if (isReference(experiment)) {
      anadiffPart = readStaticScript(ANADIFF_WITH_REFERENCE);
    } else {
      anadiffPart = readStaticScript(ANADIFF_WITHOUT_REFERENCE);
    }
    anadiffPart = anadiffPart.replace("${METHOD}", this.dispEstMethod.getName());
    sb.append(anadiffPart);

    // end document
    sb.append("\\end{document}");

    return sb.toString();
  }

  //
  // Private methods
  //

  /**
   * Determine if there is biological replicates in an experiment
   *
   * @param conditionsMap map of the conditions
   * @param rCondNames r condition names
   * @param rRepTechGroup replicate tech group
   * @return a boolean
   */
  private boolean isBiologicalReplicates(
      final Map<String, List<Integer>> conditionsMap,
      final List<String> rCondNames,
      final List<String> rRepTechGroup) {

    for (String condition : rCondNames) {
      List<Integer> condPos = conditionsMap.get(condition);

      for (int i = 0; i < condPos.size() - 1; i++) {
        int pos1 = condPos.get(i);
        int pos2 = condPos.get(i + 1);
        if (!rRepTechGroup.get(pos1).equals(rRepTechGroup.get(pos2))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Test if there is reference in an experiment
   *
   * @param experiment the experiment
   * @return boolean isRef
   */
  private boolean isReference(final Experiment experiment) {

    if (experiment == null || experiment.getSamples().isEmpty()) {
      return false;
    }

    if (DesignUtils.containsReferenceField(experiment)) {

      // Check if one ore more value of the reference field enable a reference
      for (ExperimentSample es : experiment.getExperimentSamples()) {

        // Search the reference value is all the possible fields
        final String ref = DesignUtils.getReference(es);

        if (DesignUtils.referenceValueToInt(ref, null) == 1) {
          return true;
        }
      }

      return false;
    }

    // Get the experiment reference
    final String refExp = experiment.getMetadata().getReference();

    // Check if the sample condition equals to the experiement condition
    if (refExp != null) {
      for (ExperimentSample es : experiment.getExperimentSamples()) {

        final String condition = DesignUtils.getCondition(es);

        if (refExp.equals(condition)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Add the reference to R script if there is one
   *
   * @param experiment the experiment
   * @param sb the StringBuilder to append
   */
  private void writeReferenceField(final Experiment experiment, final StringBuilder sb)
      throws EoulsanException {

    // Get experiment reference if exists
    final String refExp = experiment.getMetadata().getReference();

    String refValue = null;

    for (ExperimentSample es : experiment.getExperimentSamples()) {

      final int ref = DesignUtils.referenceValueToInt(DesignUtils.getReference(es), refExp);

      switch (ref) {
        case -1:
          throw new EoulsanException(
              "Reference value lower than 0 and not handled by the Diffana step (sample: "
                  + es.getSample().getId()
                  + ")");

        case 0:
          break;

        case 1:
          String newRefValue = DesignUtils.getCondition(es);

          if (newRefValue != null) {
            newRefValue = newRefValue.trim();

            if (refValue != null && !refValue.equals(newRefValue)) {
              throw new EoulsanException(
                  "Found a reference value ("
                      + newRefValue
                      + ") that is not the current reference value ("
                      + refValue
                      + ") in the Diffana step (sample: "
                      + es.getSample().getId()
                      + ")");
            }

            refValue = newRefValue;
          }

          break;

        default:
          throw new EoulsanException(
              "Reference value greater than 1 and not handled by the Diffana step (sample: "
                  + es.getSample().getId()
                  + ")");
      }
    }

    // Add reference to R script if found
    if (refValue != null) {
      sb.append("ref <- \"");
      sb.append(refValue);
      sb.append("\"\n\n");
    }
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   *
   * @param executor executor to use to execute the differential analysis
   * @param design The design object
   * @param dispEstMethod dispersion estimation method
   * @param dispEstSharingMode dispersion estimation sharing mode
   * @param dispEstFitType dispersion estimation fit type
   * @throws EoulsanException if an error occurs if connection to RServe server cannot be
   *     established
   */
  public DiffAna(
      final RExecutor executor,
      final Design design,
      final DispersionMethod dispEstMethod,
      final DispersionSharingMode dispEstSharingMode,
      final DispersionFitType dispEstFitType)
      throws EoulsanException {

    super(executor, design);

    if (dispEstMethod == null || dispEstFitType == null || dispEstSharingMode == null) {
      throw new NullPointerException(
          "dispersion estimation fit type or method or sharing mode is null");
    } else {
      this.dispEstMethod = dispEstMethod;
      this.dispEstFitType = dispEstFitType;
      this.dispEstSharingMode = dispEstSharingMode;
    }
  }
}
