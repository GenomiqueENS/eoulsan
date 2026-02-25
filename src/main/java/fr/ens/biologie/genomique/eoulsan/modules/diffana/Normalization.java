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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.datetoString;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toCompactTime;
import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;
import fr.ens.biologie.genomique.eoulsan.util.r.RSConnection;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class create and launch an R script to compute normalisation of
 * expression data
 * @since 1.2
 * @author Vivien Deshaies
 */

public class Normalization {

  protected static final String TARGET_CREATION = "/DESeq1/targetCreation.Rnw";
  protected static final String NORMALIZATION_FUNCTIONS =
      "/DESeq1/normalization_anaDiff_RNAseq_Functions.R";

  private static final String NORMALISATION_PART1_WHITH_TECHREP =
      "/DESeq1/normalisationPart1WithTechRep.Rnw";
  private static final String NORMALIZATION_PART1_WHITHOUT_TECHREP =
      "/DESeq1/normalisationPart1WithoutTechRep.Rnw";
  private static final String CLUSTERING_PCA_RAW =
      "/DESeq1/clusteringAndPCARaw.Rnw";
  private static final String CLUSTERING_PCA_NORM =
      "/DESeq1/clusteringAndPCANorm.Rnw";
  private static final String NORMALIZATION_PART2 =
      "/DESeq1/normalizationPart2.Rnw";

  protected final Design design;
  protected final String expressionFilesPrefix;
  protected final String expressionFilesSuffix;
  protected RSConnection rConnection = null;
  protected final RExecutor executor;

  //
  // Run methods
  //

  /**
   * Run normalisation step.
   * @param context task context
   * @param data data to process
   * @throws EoulsanException if the number of sample to analyze if lower than
   *           one
   */
  public void run(final TaskContext context, final Data data)
      throws EoulsanException {

    // Check if there more than one file to launch the analysis
    if (data.size() < 2) {
      throw new EoulsanException(
          "Cannot run the analysis with less than 2 input files");
    }

    runRExecutor(context, data);
  }

  /**
   * Execute Rnw script.
   * @param context Step context
   * @param data data to process
   * @throws EoulsanException if an error occurs while executing the script
   */
  protected void runRExecutor(final TaskContext context, final Data data)
      throws EoulsanException {

    final boolean saveRScript = context.getSettings().isSaveRscripts();
    final DataFile workflowOutputDir = context.getOutputDirectory();

    try {

      // create an iterator on the map values
      for (Experiment experiment : this.design.getExperiments()) {

        // Skip experiment if required in design
        if (DesignUtils.isSkipped(experiment)) {
          continue;
        }

        getLogger().info("Experiment : " + experiment.getName());

        // Open executor connection
        executor.openConnection();

        // Put input input files
        for (Data d : data.getListElements()) {

          final int sampleId = d.getMetadata().getSampleNumber();

          // Check if the sample ID exists
          if (sampleId == -1) {
            throw new EoulsanException(
                "No sample Id found for input file: " + d.getDataFile());
          }

          final String linkFilename = this.expressionFilesPrefix
              + sampleId + this.expressionFilesSuffix;

          executor.putInputFile(d.getDataFile(), linkFilename);
        }

        // Generate the R script
        final String rScript = generateScript(experiment, context);

        // Set the description of the analysis
        final String description = context.getCurrentStep().getId()
            + '_' + experiment.getId() + '-'
            + toCompactTime(System.currentTimeMillis());

        // Set the Sweave output
        final String sweaveOutput = context.getCurrentStep().getId()
            + '_' + experiment.getId() + ".tex";

        // Execute the R script
        executor.executeRScript(rScript, true, sweaveOutput, saveRScript,
            description, workflowOutputDir);

        // Remove input files
        executor.removeInputFiles();

        // Retrieve output files
        executor.getOutputFiles();

        // Close executor connection
        executor.closeConnection();
      }

    } catch (IOException e) {
      throw new EoulsanException(
          "Error while running differential analysis: " + e.getMessage(), e);
    }
  }

  //
  // Getters
  //

  /**
   * Test if there is Technical replicates into rRepTechGroup field.
   * @param rRepTechGroup list of the technical replicate group
   */
  protected boolean isTechnicalReplicates(final List<String> rRepTechGroup) {

    Map<String, String> rtgMap = new HashMap<>();

    for (String repTechGroup : rRepTechGroup) {

      if (rtgMap.containsKey(repTechGroup)) {
        return true;
      }
      rtgMap.put(repTechGroup, "");
    }

    return false;
  }

  //
  // R code generation methods
  //

  /**
   * Read a static part of the generated script.
   * @param staticFile the name of a file containing a part of the script
   * @return A String with the static part of the script
   * @throws EoulsanException if an error occurs while reading the script
   */
  protected String readStaticScript(final String staticFile)
      throws EoulsanException {

    final StringBuilder sb = new StringBuilder();

    final InputStream is = DiffAna.class.getResourceAsStream(staticFile);

    try {
      final BufferedReader br = FileUtils.createBufferedReader(is);

      String line;

      while ((line = br.readLine()) != null) {

        sb.append(line);
        sb.append('\n');
      }
    } catch (IOException e) {
      throw new EoulsanException("Error while reading a file" + e.getMessage());
    }

    return sb.toString();
  }

  /**
   * Generate the R script.
   * @param experiment the experiment
   * @param context step context
   * @return String rScript R script to execute
   * @throws EoulsanException if an error occurs while generate the R script
   */
  protected String generateScript(final Experiment experiment,
      final TaskContext context) throws EoulsanException {

    final Map<String, List<Integer>> conditionsMap = new HashMap<>();

    final List<Integer> rSampleIds = new ArrayList<>();
    final List<String> rSampleNames = new ArrayList<>();
    final List<String> rCondNames = new ArrayList<>();
    List<String> rRepTechGroup = new ArrayList<>();
    int i = 0;

    // Get samples ids, conditions names/indexes and repTechGoups
    for (Sample s : experiment.getSamples()) {

      final String condition = DesignUtils.getCondition(experiment, s);

      if (condition == null) {
        throw new EoulsanException("No condition field found in design file.");
      }

      if ("".equals(condition)) {
        throw new EoulsanException("No value for condition in sample: "
            + s.getName() + " (" + s.getId() + ")");
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
    String pdfTitle = escapeUnderScore(experiment.getName()) + " normalisation";
    String filePrefix =
        "normalization_" + escapeUnderScore(experiment.getName());

    final StringBuilder sb =
        generateRnwpreamble(experiment.getSamples(), pdfTitle, filePrefix);

    /*
     * Replace "na" values of repTechGroup by unique sample ids to avoid pooling
     * problem while executing R script
     */
    replaceRtgNA(rRepTechGroup, rSampleNames);

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

    // Add target creation
    sb.append(readStaticScript(TARGET_CREATION));

    sb.append("\\section{Analysis}\n\n");
    sb.append("\t\\subsection{Normalization}\n\n");
    sb.append("\\begin{itemize}\n\n");

    if (experiment.getSamples().size() > 2) {
      sb.append(readStaticScript(CLUSTERING_PCA_RAW));
    }

    // Add normalization part
    if (isTechnicalReplicates(rRepTechGroup)) {
      sb.append(readStaticScript(NORMALISATION_PART1_WHITH_TECHREP));
    } else {
      sb.append(readStaticScript(NORMALIZATION_PART1_WHITHOUT_TECHREP));
    }

    // Add normalise data clustering if it's possible
    if (isEnoughRepTechGroup(rRepTechGroup)) {
      sb.append(readStaticScript(CLUSTERING_PCA_NORM));
    }

    sb.append(readStaticScript(NORMALIZATION_PART2));

    // end document
    sb.append("\\end{document}\n");

    return sb.toString();
  }

  /**
   * Write Rnw preamble.
   * @param experimentSamplesList sample experiment list
   * @param title title of the document
   * @param filePrefix Sweave file prefix
   * @return a StringBuilder with Rnw preamble
   */
  protected StringBuilder generateRnwpreamble(
      final List<Sample> experimentSamplesList, final String title,
      final String filePrefix) {

    StringBuilder sb = new StringBuilder();
    // Add packages to the LaTeX StringBuilder
    sb.append("\\documentclass[a4paper,10pt]{article}\n");
    sb.append("\\usepackage[utf8]{inputenc}\n");
    sb.append("\\usepackage{lmodern}\n");
    sb.append("\\usepackage{a4wide}\n");
    sb.append("\\usepackage{marvosym}\n");
    sb.append("\\usepackage{graphicx}\n\n");
    // Set Sweave options
    sb.append("\\SweaveOpts{eps = FALSE, pdf = TRUE, prefix.string=");
    sb.append(filePrefix);
    sb.append("}\n\n");
    sb.append("\\setkeys{Gin}{width=0.95\textwidth}\n\n");
    // Add document title
    sb.append("\\title{");
    sb.append(title);
    sb.append("}\n\n");

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
    sb.append(datetoString(System.currentTimeMillis()));
    sb.append(" ###\n\n");
    // Add function part to string builder
    try {
      sb.append(readStaticScript(NORMALIZATION_FUNCTIONS));
    } catch (EoulsanException e) {
      getLogger().severe(e.getMessage());
    }

    // Add a end R code chunck mark
    sb.append("@\n\n");

    // Add initialization part
    sb.append("\\section{Initialization}\n");
    sb.append("<<>>=\n");

    return sb;
  }

  /**
   * Add sampleNames vector to R script.
   * @param rSampleNames sample names
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateSampleNamePart(final List<String> rSampleNames,
      final StringBuilder sb) {

    // Add samples names to R script
    sb.append("# create sample names vector\n");
    sb.append("sampleNames <- c(");
    boolean first = true;
    for (String r : rSampleNames) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }
      sb.append('\"');
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

  }

  /**
   * Add SampleIds vector to R script.
   * @param rSampleIds samples identifiers
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateSampleIdsPart(final List<Integer> rSampleIds,
      final StringBuilder sb) {

    // Put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append(id);
      if (i < rSampleIds.size()) {
        sb.append(",");
      }
    }
    sb.append(")\n\n");
  }

  /**
   * Add expression file name vector to R script.
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateExpressionFileNamesPart(final StringBuilder sb) {

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"");
    sb.append(this.expressionFilesPrefix);
    sb.append("\",sampleIds" + ',' + '\"');
    sb.append(this.expressionFilesSuffix);
    sb.append("\",sep=\"\")\n\n");
  }

  /**
   * Write the section of the script that handle technical replicate groups.
   * @param rRepTechGroup list of technical replicate groups
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateRepTechGroupPart(final List<String> rRepTechGroup,
      final StringBuilder sb) {

    if (isTechnicalReplicates(rRepTechGroup)) {

      // Add repTechGroup vector
      sb.append("# create technical replicates groups vector\n");
      sb.append("repTechGroup <- c(");
      boolean first = true;

      for (String r : rRepTechGroup) {

        if (first) {
          first = false;
        } else {
          sb.append(',');
        }

        sb.append('\"');
        sb.append(r);
        sb.append('\"');
      }
      sb.append(")\n\n");

    } else {
      // Add repTechGroup vector equal to sampleNames to avoid error in R
      // function buildTarget
      sb.append("# create technical replicates groups vector\n");
      sb.append("repTechGroup <- sampleNames\n\n");
    }
  }

  /**
   * Add condition vector to R script.
   * @param rCondNames condition names
   * @param sb StringBuilder where write the part of the script
   */
  protected void generateConditionPart(final List<String> rCondNames,
      final StringBuilder sb) {

    sb.append("# create condition vector\n");
    sb.append("condition <- c(");
    boolean first = true;
    for (String r : rCondNames) {

      if (first) {
        first = false;
      } else {
        sb.append(',');
      }

      sb.append('\"');
      sb.append(r);
      sb.append('\"');

    }
    sb.append(")\n\n");
  }

  //
  // Other methods
  //

  /**
   * Check if there is a problem in the repTechGroup coherence.
   * @param rRepTechGroup technical replicate group
   * @param rCondNames condition names
   * @throws EoulsanException if an error if found in the design file
   */
  protected void checkRepTechGroupCoherence(final List<String> rRepTechGroup,
      final List<String> rCondNames) throws EoulsanException {

    // Check repTechGroup field coherence
    Map<String, String> condRepTGMap = new HashMap<>();
    for (int i = 0; i < rRepTechGroup.size(); i++) {

      String repTechGroup = rRepTechGroup.get(i);
      String condition = rCondNames.get(i);

      if (!repTechGroup.toLowerCase(Globals.DEFAULT_LOCALE).equals("na")) {
        if (!condRepTGMap.containsKey(repTechGroup)) {
          condRepTGMap.put(repTechGroup, condition);
        } else if (!condRepTGMap.get(repTechGroup).equals(condition)) {
          throw new EoulsanException(
              "There is a mistake in RepTechGroup field of design file : "
                  + "two condition have the same repTechGroup value : "
                  + repTechGroup);
        }
      }
    }
  }

  /**
   * Escape underscore for LaTeX title.
   * @param s string to escape
   * @return s with escaped underscore
   */
  protected String escapeUnderScore(final String s) {

    return s.replace("_", "\\_");
  }

  /**
   * Replace na values in RepTechGroup list to avoid pooling error.
   * @param rRepTechGroup list of technical replicate groups
   * @param rSampleNames sample names
   */
  protected void replaceRtgNA(final List<String> rRepTechGroup,
      final List<String> rSampleNames) {

    for (int j = 0; j < rRepTechGroup.size(); j++) {

      if (rRepTechGroup.get(j).toLowerCase(Globals.DEFAULT_LOCALE).equals("na")) {
        rRepTechGroup.set(j, rSampleNames.get(j));
      }
    }
  }

  /*
   * Private methods
   */

  /**
   * Test if there is enough distinct repTechGroup (>2) to perform clustering.
   * @param rRepTechGroup list of technical replicate groups
   * @return true if there is enough distinct repTechGroup (>2) to perform
   *         clustering
   */
  private boolean isEnoughRepTechGroup(final List<String> rRepTechGroup) {

    List<String> repTechGroupMap = new ArrayList<>();
    for (String r : rRepTechGroup) {

      if (!repTechGroupMap.contains(r)) {
        repTechGroupMap.add(r);
      }

      if (repTechGroupMap.size() > 2) {
        return true;
      }
    }
    return false;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param executor executor to use to execute the normalization
   * @param design The design object
   * @throws EoulsanException if an error occurs if connection to RServe server
   *           cannot be established
   */
  public Normalization(final RExecutor executor, final Design design)
      throws EoulsanException {

    requireNonNull(design, "design is null.");

    this.design = design;

    final DataFormat eDF = DataFormats.EXPRESSION_RESULTS_TSV;
    this.expressionFilesPrefix = eDF.getPrefix();
    this.expressionFilesSuffix = eDF.getDefaultExtension();

    this.executor = executor;
  }

}
