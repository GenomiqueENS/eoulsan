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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
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

  private final static String COUNT_MATRIX_FILE_PREFIX = "anaDiff_";
  private final static String COUNT_MATRIX_FILE_SUFFIX = "_rawCountMatrix.txt";

  //
  // Protected methods
  //

  @Override
  protected void runLocalRnwScript() throws EoulsanException {

    try {

      LOGGER.info("Differential analysis : local mode");
      // create an experiment map
      Map<String, List<Sample>> experiments = experimentsSpliter();
      // create an iterator on the map
      Set<String> cles = experiments.keySet();
      Iterator<String> itr = cles.iterator();
      while (itr.hasNext()) {
        String cle = itr.next();
        List<Sample> experimentSampleList = experiments.get(cle);

        LOGGER.info("Experiment : "
            + experimentSampleList.get(0).getMetadata().getExperiment());

        String rScript = writeScript(experimentSampleList);
        runRnwScript(rScript, false);
      }

    } catch (REngineException e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage());
    }
  }

  @Override
  protected void runRserveRnwScript() throws EoulsanException {

    try {

      // print lof info
      getLogger().info("Differential analysis : Rserve mode");
      getLogger().info(
          "Rserve server name : " + getRConnection().getServerName());

      // create an experiment map
      Map<String, List<Sample>> experiments = experimentsSpliter();
      // create an iterator on the map
      Set<String> cles = experiments.keySet();
      Iterator<String> itr = cles.iterator();
      while (itr.hasNext()) {
        String cle = itr.next();
        List<Sample> experimentSampleList = experiments.get(cle);

        LOGGER.info("Experiment : "
            + experimentSampleList.get(0).getMetadata().getExperiment());

        putRawMatrix(experimentSampleList);

        String rScript = writeScript(experimentSampleList);
        runRnwScript(rScript, true);

        this.rConnection.removeFile(rScript);
        this.rConnection.getAllFiles(outPath.toString() + "/");
      }

    } catch (REngineException e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage());
    } catch (REXPMismatchException e) {
      throw new EoulsanException("Error while getting file : " + e.getMessage());

    } finally {

      try {

        this.rConnection.removeAllFiles();
        this.rConnection.disConnect();

      } catch (Exception e) {
        throw new EoulsanException("Error while removing files on server : "
            + e.getMessage());
      }
    }
  }

  @Override
  protected StringBuilder writeRnwpreamble(List<Sample> experimentSamplesList) {

    StringBuilder sb = new StringBuilder();
    // Add packages to the LaTeX stringbuilder
    sb.append("\\documentclass[a4paper,10pt]{article}\n");
    sb.append("\\usepackage[utf8]{inputenc}\n");
    sb.append("\\usepackage{lmodern}\n");
    sb.append("\\usepackage{a4wide}\n");
    sb.append("\\usepackage{marvosym}\n");
    sb.append("\\usepackage{graphicx}\n\n");
    // Set Sweave options
    sb.append("\\SweaveOpts{eps = FALSE, pdf = TRUE}\n");
    sb.append("\\setkeys{Gin}{width=0.95\textwidth}\n\n");
    // Add document title
    sb.append("\\title{"
        + experimentSamplesList.get(0).getMetadata().getExperiment()
        + " differential analysis}\n\n");

    // Begin document...
    sb.append("\\begin{document}\n");
    sb.append("\\maketitle\n\n");

    // Add a begin R code chunck mark
    sb.append("<<functions, echo=FALSE>>=\n");

    // Add the auto generate info
    sb.append("### Auto generated by ");
    sb.append(Globals.APP_NAME);
    sb.append(" ");
    sb.append(Globals.APP_VERSION_STRING);
    sb.append(" on ");
    sb.append(new Date(System.currentTimeMillis()));
    sb.append(" ###\n\n");
    // Add function part to string builder
    try {
      sb.append(readStaticScript(NORMALISATION_FUNCTIONS));
    } catch (EoulsanException e) {
      e.printStackTrace();
    }

    // Add a end R code chunck mark
    sb.append("@\n\n");

    // Add initialization part
    sb.append("\\section{Initialization}\n");
    sb.append("<<>>=\n");

    return sb;
  }

  //
  // Private methods
  //

  /**
   * write differential analysis script
   * @param experimentSamplesList a list of sample
   * @return String r
   * @throws EoulsanException
   */
  private String writeScript(final List<Sample> experimentSamplesList)
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

    // Create Rnw script stringbuilder with preamble
    final StringBuilder sb = writeRnwpreamble(experimentSamplesList);

    // Add raw count matrix reading part
    sb.append("matrix <- readCountMatrix(\"");
    sb.append(COUNT_MATRIX_FILE_PREFIX
        + experimentSamplesList.get(0).getMetadata().getExperiment()
        + COUNT_MATRIX_FILE_SUFFIX);
    sb.append("\")\n");

    // Add reference if there is one
    addReferenceField(experimentSamplesList, sb);

    if (isTechnicalReplicates(rRepTechGroup))
      writeWithTechnicalReplicate(sb, rSampleNames, rCondNames, rRepTechGroup,
          experimentSamplesList.get(0).getMetadata().getExperiment());
    else
      writeWithoutTechnicalReplicates(sb, rCondNames);

    // Add dispersion estimation part
    if (isBiologicalReplicates(conditionsMap, rCondNames, rRepTechGroup)) {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITH_REPLICATES));
    } else {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITHOUT_REPLICATES));
    }

    if (isReference(experimentSamplesList)) {
      sb.append(readStaticScript(KINETIC_ANADIFF));
    } else {
      sb.append(readStaticScript(NOT_KINETIC_ANADIFF));
    }

    // end document
    sb.append("\\end{document}");

    // create file
    String rScript = null;
    try {
      rScript =
          "anaDiff_"
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

  /**
   * Write Rnw script body with technical replicates
   * @param sb StringBuilder to use
   * @param rSampleIds R samples ids
   * @param rSampleNames R samples names
   * @param rCondIndexes R conditions indexes
   * @param rCondNames R conditions names
   * @param rRepTechGroup R technical replicate group
   */
  private void writeWithTechnicalReplicate(final StringBuilder sb,
      final List<String> rSampleNames, final List<String> rCondNames,
      final List<String> rRepTechGroup, final String experimentName) {

    // Add samples names to R script
    sb.append("# create sample names vector\n");
    sb.append("sampleNames <- c(");
    boolean first = true;
    for (String r : rSampleNames) {

      if (first)
        first = false;
      else
        sb.append(',');
      sb.append('\"');
      sb.append(r.toLowerCase());
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Add repTechGroup vector
    sb.append("# create technical replicates groups vector\n");
    sb.append("repTechGroup <- c(");
    first = true;
    for (String r : rRepTechGroup) {

      if (first)
        first = false;
      else
        sb.append(',');

      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Add condition to R script
    sb.append("# create condition vector\n");
    sb.append("condition <- c(");
    first = true;
    for (String r : rCondNames) {

      if (first)
        first = false;
      else
        sb.append(',');
      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

    // Add projectPath, outPath and projectName
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\"" + experimentName + "\"" + "\n");

    // Add preprocessing part
    sb.append("target <- list()\n");
    sb.append("target$counts <- matrix\n");
    sb.append("target$sampleLabel <- sampleNames\n");
    sb.append("target$repTechGroup <- repTechGroup\n");
    sb.append("target$condition <- condition\n");
    sb.append("target$projectName <- projectName\n");
    sb.append("\n\n");

    // Pool technical replicates
    sb.append("target <- poolTechRep( target )\n");

    sb.append("countDataSet <- normDESeq(target$counts, target$condition)\n");
    sb.append("@\n\n");
  }

  /**
   * Write normalization code without replicates
   * @param sb A StringBuilder
   * @param rSampleIds
   * @param rSampleNames
   * @param rCondNames
   */
  private void writeWithoutTechnicalReplicates(final StringBuilder sb,
      final List<String> rCondNames) {

    // Add condition to R script
    sb.append("# create condition vector\n");
    sb.append("condition <- c(");
    boolean first = true;
    for (String r : rCondNames) {

      if (first)
        first = false;
      else
        sb.append(',');

      sb.append('\"');
      sb.append(r);
      sb.append('\"');

    }
    sb.append(")\n\n");

    // Add projectPath, outPath and projectName
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");

    // create countdataset
    sb.append("countDataSet <- newCountDataSet(matrix, condition)\n");
    sb.append("countDataSet <- estimateSizeFactors(countDataSet)\n");
    sb.append("@\n\n");

  }

  /**
   * Put the raw count matrix on Rserve server
   * @param experimentListSample
   * @throws REngineException
   */
  private void putRawMatrix(List<Sample> experimentListSample) {

    try {
      String matrixFileName =
          COUNT_MATRIX_FILE_PREFIX
              + experimentListSample.get(0).getMetadata().getExperiment()
              + COUNT_MATRIX_FILE_SUFFIX;

      File matrixFile = new File(matrixFileName);
      
      rConnection.putFile(matrixFile, matrixFileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

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
   * @return
   */
  private StringBuilder addReferenceField(List<Sample> experimentSamplesList,
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

    return sb;
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
      String rServerName) {
    super(design, expressionFilesDirectory, expressionFilesPrefix,
        expressionFilesSuffix, outPath, rServerName);
  }

}
