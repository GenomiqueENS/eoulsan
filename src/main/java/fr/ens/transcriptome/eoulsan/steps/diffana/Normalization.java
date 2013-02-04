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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.r.RSConnectionNewImpl;

/**
 * This class create and launch an R script to compute normalisation of
 * expression data
 * @since 1.2
 * @author Vivien Deshaies
 */

public class Normalization {

  /** Logger. */
  protected static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  protected static final String TARGET_CREATION = "/targetCreation.Rnw";
  protected static final String NORMALIZATION_FUNCTIONS =
      "/normalization_anaDiff_RNAseq_Functions.R";

  private static final String NORMALISATION_PART1_WHITH_TECHREP =
      "/normalisationPart1WithTechRep.Rnw";
  private static final String NORMALIZATION_PART1_WHITHOUT_TECHREP =
      "/normalisationPart1WithoutTechRep.Rnw";
  private static final String CLUSTERING_PCA_RAW = "/clusteringAndPCARaw.Rnw";
  private static final String CLUSTERING_PCA_NORM = "/clusteringAndPCANorm.Rnw";
  private static final String NORMALIZATION_PART2 = "/normalizationPart2.Rnw";

  protected final Design design;
  protected final File expressionFilesDirectory;
  protected final File outPath;
  protected final String expressionFilesPrefix;
  protected final String expressionFilesSuffix;
  protected final RSConnectionNewImpl rConnection;

  //
  // Public methods
  //

  /**
   * Run normalisation step
   * @throws EoulsanException
   */
  public void run(final Context context) throws EoulsanException {

    if (context.getSettings().isRServeServerEnabled()) {
      getLogger().info("Normalization : Rserve mode");
      runRserveRnwScript();
    } else {
      getLogger().info("Normalization : local mode");
      runLocalRnwScript();
    }
  }

  // Getters
  /**
   * get Rserve connection
   * @return rConnection
   */
  protected RSConnectionNewImpl getRConnection() {
    return this.rConnection;
  }

  /**
   * get Logger
   * @return LOGGER
   */
  static protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Test if there is Technical replicates into rRepTechGroup field
   * @param rRepTechGroup
   * @return
   */
  protected boolean isTechnicalReplicates(List<String> rRepTechGroup) {

    Map<String, String> rtgMap = Maps.newHashMap();

    for (String repTechGroup : rRepTechGroup) {

      if (rtgMap.containsKey(repTechGroup)) {
        return true;
      }
      rtgMap.put(repTechGroup, "");
    }

    return false;
  }

  /**
   * run Rnw script on Rserve server
   * @throws EoulsanException
   */
  protected void runRserveRnwScript() throws EoulsanException {

    try {

      // print log info
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

        getLogger().info(
            "Experiment : "
                + experimentSampleList.get(0).getMetadata().getExperiment());

        putExpressionFiles(experimentSampleList);

        String rScript = writeScript(experimentSampleList);
        runRnwScript(rScript, true);

        removeExpressionFiles(experimentSampleList);
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

  /**
   * run Rnw script on local mode
   * @throws EoulsanException
   */
  protected void runLocalRnwScript() throws EoulsanException {

    try {

      // create an experiment map
      Map<String, List<Sample>> experiments = experimentsSpliter();
      // create an iterator on the map
      Set<String> cles = experiments.keySet();
      Iterator<String> itr = cles.iterator();
      while (itr.hasNext()) {
        String cle = itr.next();
        List<Sample> experimentSampleList = experiments.get(cle);

        getLogger().info(
            "Experiment : "
                + experimentSampleList.get(0).getMetadata().getExperiment());

        String rScript = writeScript(experimentSampleList);
        runRnwScript(rScript, false);
      }

    } catch (REngineException e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage());
    }
  }

  /**
   * Split design into multiple experiments Samples list
   * @return experiementMap a map of experiments
   */
  protected Map<String, List<Sample>> experimentsSpliter() {

    List<Sample> samples = this.design.getSamples();
    // Create design HashMap
    Map<String, List<Sample>> experimentMap = Maps.newHashMap();

    for (Sample s : samples) {
      String expName = s.getMetadata().getExperiment();

      if (experimentMap.containsKey(expName)) {
        experimentMap.get(expName).add(s);
      } else {
        experimentMap.put(expName, Lists.newArrayList(s));
      }
    }

    return experimentMap;
  }

  /**
   * Execute the analysis.
   * @param rScript
   * @throws IOException
   * @throws REngineException
   * @throws EoulsanException
   */
  protected void runRnwScript(String rnwScript, boolean isRserveEnable)
      throws REngineException, EoulsanException {

    if (isRserveEnable) {
      this.rConnection.executeRnwCode(rnwScript);

    } else {

      try {

        final ProcessBuilder pb =
            new ProcessBuilder("/usr/bin/R", "CMD", "Sweave", rnwScript);

        // Set the temporary directory for R
        pb.environment().put("TMPDIR", this.outPath.getAbsolutePath());

        ProcessUtils.logEndTime(pb.start(), Joiner.on(' ').join(pb.command()),
            System.currentTimeMillis());


      } catch (IOException e) {

        throw new EoulsanException(
            "Error while executing R script in anadiff: " + e.getMessage());
      }

    }

  }

  /**
   * Read a static part of the generated script.
   * @param staticFile the name of a file containing a part of the script
   * @return A String with the static part of the script
   * @throws EoulsanException
   */
  protected String readStaticScript(String staticFile) throws EoulsanException {

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
   * Write the R script
   * @param experimentSamplesList
   * @return String rScript
   * @throws EoulsanException
   */
  protected String writeScript(final List<Sample> experimentSamplesList)
      throws EoulsanException {

    final Map<String, List<Integer>> conditionsMap = Maps.newHashMap();

    final List<Integer> rSampleIds = Lists.newArrayList();
    final List<String> rSampleNames = Lists.newArrayList();
    final List<String> rCondNames = Lists.newArrayList();
    List<String> rRepTechGroup = Lists.newArrayList();
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
            + " normalisation";
    final StringBuilder sb = writeRnwpreamble(experimentSamplesList, pdfTitle);

    /*
     * Replace "na" values of repTechGroup by unique sample ids to avoid pooling
     * problem while executing R script
     */
    replaceRtgNA(rRepTechGroup, rSampleNames);

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

    // Add target creation
    sb.append(readStaticScript(TARGET_CREATION));

    sb.append("\\section{Analysis}\n\n");
    sb.append("\t\\subsection{Normalization}\n\n");
    sb.append("\\begin{itemize}\n\n");

    if (experimentSamplesList.size() > 2)
      sb.append(readStaticScript(CLUSTERING_PCA_RAW));

    // Add normalization part
    if (isTechnicalReplicates(rRepTechGroup))
      sb.append(readStaticScript(NORMALISATION_PART1_WHITH_TECHREP));
    else
      sb.append(readStaticScript(NORMALIZATION_PART1_WHITHOUT_TECHREP));

    // Add normalise data clustering if it's possible
    if (isEnoughRepTechGroup(rRepTechGroup))
      sb.append(readStaticScript(CLUSTERING_PCA_NORM));

    sb.append(readStaticScript(NORMALIZATION_PART2));

    // end document
    sb.append("\\end{document}\n");

    String rScript = null;
    try {
      rScript =
          "normalization_"
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
   * Write Rnw preamble
   * @param sb
   * @param experimentSamplesList
   * @return a stringbuilder whith Rnw preamble
   */
  protected StringBuilder writeRnwpreamble(List<Sample> experimentSamplesList,
      String title) {

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
    sb.append("\\title{" + title + "}\n\n");

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
      sb.append(readStaticScript(NORMALIZATION_FUNCTIONS));
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

  /**
   * Add sampleNames vector to R script
   * @param rSampleNames
   * @param sb
   */
  protected void writeSampleName(final List<String> rSampleNames,
      final StringBuilder sb) {

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
      sb.append(r);
      sb.append('\"');
    }
    sb.append(")\n\n");

  }

  /**
   * Add SampleIds vector to R script
   * @param rSampleIds
   * @param sb
   */
  protected void writeSampleIds(final List<Integer> rSampleIds,
      final StringBuilder sb) {

    // Put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append("" + id);
      if (i < rSampleIds.size())
        sb.append(",");
    }
    sb.append(")\n\n");
  }

  /**
   * Add expression file name vector to R script
   * @param sb
   */
  protected void writeExpressionFileNames(StringBuilder sb) {

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"" + expressionFilesPrefix + '\"' + ',');
    sb.append("sampleIds"
        + ',' + '\"' + expressionFilesSuffix + '\"' + ',' + "sep=\"\"" + ")"
        + "\n\n");
  }

  /**
   * Write
   * @param rRepTechGroup
   * @param sb
   */
  protected void writeRepTechGroup(List<String> rRepTechGroup, StringBuilder sb) {

    if (isTechnicalReplicates(rRepTechGroup)) {

      // Add repTechGroup vector
      sb.append("# create technical replicates groups vector\n");
      sb.append("repTechGroup <- c(");
      boolean first = true;

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

    } else {
      /*
       * Add repTechGroup vector equal to sampleNames to avoid error in R
       * function buildTarget
       */
      sb.append("# create technical replicates groups vector\n");
      sb.append("repTechGroup <- sampleNames\n\n");
    }
  }

  /**
   * Add condition vector to R script
   * @param rCondNames
   * @param sb
   */
  protected void writeCondition(List<String> rCondNames, StringBuilder sb) {

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
  }

  /**
   * Check if there is a problem in the repTechGroup coherence
   * @param rRepTechGroup
   * @param rCondNames
   * @throws EoulsanException
   */
  protected void checkRepTechGroupCoherence(List<String> rRepTechGroup,
      List<String> rCondNames) throws EoulsanException {
    // Check repTechGroup field coherence
    Map<String, String> condRepTGMap = Maps.newHashMap();
    for (int i = 0; i < rRepTechGroup.size(); i++) {

      String repTechGroup = rRepTechGroup.get(i);
      String condition = rCondNames.get(i);

      if (!repTechGroup.toLowerCase().equals("na")) {
        if (!condRepTGMap.containsKey(repTechGroup))
          condRepTGMap.put(repTechGroup, condition);
        else if (!condRepTGMap.get(repTechGroup).equals(condition))
          throw new EoulsanException(
              "There is a mistake in RepTechGroup field of design file : "
                  + "two condition have the same repTechGroup value : "
                  + repTechGroup);
      }
    }
  }

  /**
   * Escape underscore for LaTeX title
   * @param s
   * @return s with escaped underscore
   */
  protected String escapeUnderScore(final String s) {

    StringBuilder sb = new StringBuilder();
    String[] splittedString = s.split("");

    for (String c : splittedString) {

      if (c.equals("_"))
        sb.append("\\_");
      else
        sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Replace na values in RepTechGroup list to avoid pooling error
   * @param rRepTechGroup
   * @param rSampleNames
   * @return
   */
  protected void replaceRtgNA(List<String> rRepTechGroup,
      List<String> rSampleNames) {

    for (int j = 0; j < rRepTechGroup.size(); j++) {

      if (rRepTechGroup.get(j).toLowerCase().equals("na")) {
        rRepTechGroup.set(j, rSampleNames.get(j));
      }
    }
  }

  /*
   * Private methods
   */

  /**
   * Test if there is enough distinct repTechGroup (>2) to perform clustering
   * @param rRepTechGroup
   * @return
   */
  private boolean isEnoughRepTechGroup(List<String> rRepTechGroup) {

    List<String> repTechGroupMap = Lists.newArrayList();
    for (String r : rRepTechGroup) {

      if (!repTechGroupMap.contains(r))
        repTechGroupMap.add(r);

      if (repTechGroupMap.size() > 2)
        return true;
    }
    return false;
  }

  /**
   * Put all expression files needed for the analysis on the R server
   * @throws REngineException
   */
  private void putExpressionFiles(List<Sample> experiment)
      throws REngineException {

    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // Put file on rserve server
      this.rConnection.putFile(new File(expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix),
          this.expressionFilesPrefix + i + this.expressionFilesSuffix);
    }
  }

  /**
   * Remove all expression files from the R server after analysis
   * @param experiment
   * @throws REngineException
   */
  private void removeExpressionFiles(List<Sample> experiment)
      throws REngineException {

    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // Remove file from rserve server
      this.rConnection.removeFile(expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix);
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
  public Normalization(final Design design,
      final File expressionFilesDirectory, final String expressionFilesPrefix,
      final String expressionFilesSuffix, final File outPath,
      final String rServerName) {

    checkNotNull(design, "design is null.");
    checkNotNull(expressionFilesDirectory,
        "The path of the expression files is null.");
    checkNotNull(expressionFilesPrefix,
        "The prefix for expression files is null");
    checkNotNull(expressionFilesSuffix,
        "The suffix for expression files is null");

    this.design = design;
    this.expressionFilesPrefix = expressionFilesPrefix;
    this.expressionFilesSuffix = expressionFilesSuffix;

    if (!(expressionFilesDirectory.isDirectory() && expressionFilesDirectory
        .exists()))
      throw new NullPointerException(
          "The path of the expression files doesn't exist or is not a directory.");

    this.expressionFilesDirectory = expressionFilesDirectory;

    if (!(outPath.isDirectory() && outPath.exists()))
      throw new NullPointerException(
          "The outpath file doesn't exist or is not a directory.");

    this.outPath = outPath;

    this.rConnection = new RSConnectionNewImpl(rServerName);
  }

}
