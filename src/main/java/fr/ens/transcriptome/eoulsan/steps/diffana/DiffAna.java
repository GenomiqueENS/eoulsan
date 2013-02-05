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

package fr.ens.transcriptome.eoulsan.steps.diffana;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.rosuda.REngine.REngineException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class create and launch a R script to compute differential analysis.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
public class DiffAna extends Normalization {

  private static final String DISPERSION_ESTIMATION_WITH_REPLICATES =
      "/dispersionEstimationWithReplicates.Rnw";
  private static final String DISPERSION_ESTIMATION_WITHOUT_REPLICATES =
      "/dispersionEstimationWithoutReplicates.Rnw";
  private static final String ANADIFF_WITH_REFERENCE =
      "/anadiffWithReference.Rnw";
  private static final String ANADIFF_WITHOUT_REFERENCE =
      "/anadiffWithoutReference.Rnw";

  private final boolean forceBlindDispersionEstimation;

  //
  // Public methods
  //

  @Override
  public void run(final Context context) throws EoulsanException {

    if (context.getSettings().isRServeServerEnabled()) {
      getLogger().info("Differential analysis : Rserve mode");
      runRserveRnwScript();
    } else {
      getLogger().info("Differential analysis : local mode");
      runLocalRnwScript();
    }
  }

  //
  // Protected methods
  //

  @Override
  protected String generateScript(final List<Sample> experimentSamplesList)
      throws EoulsanException {

    final Map<String, List<Integer>> conditionsMap = Maps.newHashMap();

    final List<Integer> rSampleIds = Lists.newArrayList();
    final List<String> rSampleNames = Lists.newArrayList();
    final List<String> rCondNames = Lists.newArrayList();
    final List<String> rRepTechGroup = Lists.newArrayList();
    int i = 0;

    // Get samples ids, conditions names/indexes and repTechGoups
    for (Sample s : experimentSamplesList) {

      if (!s.getMetadata().isConditionField())
        throw new EoulsanException("No condition field found in design file.");

      final String condition = s.getMetadata().getCondition().trim();

      if ("".equals(condition))
        throw new EoulsanException("No value for condition in sample: "
            + s.getName() + " (" + s.getId() + ")");

      final String repTechGroup = s.getMetadata().getRepTechGroup().trim();

      if (!"".equals(repTechGroup)) {
        rRepTechGroup.add(repTechGroup);
      }

      final List<Integer> index;
      if (!conditionsMap.containsKey(condition)) {
        index = Lists.newArrayList();
        conditionsMap.put(condition, index);
      } else {
        index = conditionsMap.get(condition);
      }
      index.add(i);

      rSampleIds.add(s.getId());
      rSampleNames.add(s.getName());
      rCondNames.add(condition);

      i++;
    }

    checkRepTechGroupCoherence(rRepTechGroup, rCondNames);

    // Create Rnw script stringbuilder with preamble
    String pdfTitle =
        escapeUnderScore(experimentSamplesList.get(0).getMetadata()
            .getExperiment())
            + " differential analysis";
    final StringBuilder sb = writeRnwpreamble(experimentSamplesList, pdfTitle);

    /*
     * Replace "na" values of repTechGroup by unique sample ids to avoid pooling
     * problem while executing R script
     */
    replaceRtgNA(rRepTechGroup, rSampleNames);

    // Add reference if there is one
    writeReferenceField(experimentSamplesList, sb);

    // Add sampleNames vector
    writeSampleName(rSampleNames, sb);

    // Add SampleIds vector
    writeSampleIds(rSampleIds, sb);

    // Add file names vector
    writeExpressionFileNames(sb);

    // Add repTechGroupVector
    writeRepTechGroup(rRepTechGroup, sb);

    // Add condition to R script
    writeCondition(rCondNames, sb);

    // Add projectPath, outPath and projectName
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\""
        + experimentSamplesList.get(0).getMetadata().getExperiment() + "\""
        + "\n");
    sb.append("@\n\n");

    sb.append(readStaticScript(TARGET_CREATION));

    sb.append("\\section{Analysis}\n\n");
    sb.append("<<beginAnalysis>>=\n");

    // Add delete unexpressed gene call
    sb.append("target$counts <- deleteUnexpressedGene(target$counts)\n");

    if (isTechnicalReplicates(rRepTechGroup))
      sb.append("target <- poolTechRep(target)\n\n");

    sb.append("target <- sortTarget(target)\n");
    sb.append("countDataSet <- normDESeq(target$counts, target$condition)\n");

    sb.append("@\n");

    // Add dispersion estimation part
    if (isBiologicalReplicates(conditionsMap, rCondNames, rRepTechGroup)
        && !forceBlindDispersionEstimation) {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITH_REPLICATES));
    } else {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITHOUT_REPLICATES));
    }

    if (isReference(experimentSamplesList)) {
      sb.append(readStaticScript(ANADIFF_WITH_REFERENCE));
    } else {
      sb.append(readStaticScript(ANADIFF_WITHOUT_REFERENCE));
    }

    // end document
    sb.append("\\end{document}");

    // create file
    String rScript = null;
    try {
      rScript =
          "diffana_"
              + experimentSamplesList.get(0).getMetadata().getExperiment()
              + ".Rnw";
      if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
        this.rConnection.writeStringAsFile(rScript, sb.toString());
      } else {
        Writer writer = FileUtils.createFastBufferedWriter(rScript);
        writer.write(sb.toString());
        writer.close();
      }
    } catch (REngineException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return rScript;
  }

  //
  // Private methods
  //

  /**
   * Determine if there is biological replicates in an experiment
   * @param conditionsMap
   * @param rCondNames
   * @param rRepTechGroup
   * @return a boolean
   */
  private boolean isBiologicalReplicates(
      Map<String, List<Integer>> conditionsMap, List<String> rCondNames,
      List<String> rRepTechGroup) {

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
   * @param experiment
   * @return boolean isRef
   */
  private boolean isReference(List<Sample> experiment) {

    if (experiment == null
        || experiment.size() == 0
        || !experiment.get(0).getMetadata().isReferenceField())
      return false;

    for (Sample s : experiment) {
      if (s.getMetadata().isReference()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add the reference to R script if there is one
   * @param experimentSamplesList
   * @param sb
   */
  private void writeReferenceField(List<Sample> experimentSamplesList,
      StringBuilder sb) {

    if (experimentSamplesList.get(0).getMetadata().isReferenceField()) {

      for (Sample s : experimentSamplesList) {

        if (s.getMetadata().isReference()) {
          // Add reference to R script
          sb.append("ref <- "
              + "\"" + s.getMetadata().getCondition() + "\"\n\n");
          break;
        }
      }
    }
  }

  /*
   * Constructor
   */

  /**
   * Public constructor
   * @param design
   * @param expressionFilesDirectory
   * @param expressionFilesPrefix
   * @param expressionFilesSuffix
   * @param outPath
   * @param rServerName
   */
  public DiffAna(Design design, File expressionFilesDirectory,
      String expressionFilesPrefix, String expressionFilesSuffix, File outPath,
      String rServerName, boolean fbde) {

    super(design, expressionFilesDirectory, expressionFilesPrefix,
        expressionFilesSuffix, outPath, rServerName);

    this.forceBlindDispersionEstimation = fbde;
  }

}
