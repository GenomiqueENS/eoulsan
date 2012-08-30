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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.r.RSConnectionNewImpl;

/**
 * This class create and launch a R script to compute differential analysis.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
public class DiffAna {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String NORMALISATION_WHITH_TECHREP =
      "/normalisationWithTechRep.Rnw";
  private static final String NORMALISATION_WHITHOUT_TECHREP =
      "/normalisationWithoutTechRep.Rnw";
  private static final String NORMALISATION_FUNCTIONS =
      "/normalizationRNAseqFunctions.R";
  private static final String DISPERSION_ESTIMATION_WITH_REPLICATES =
      "/dispersionEstimationWithReplicates.Rnw";
  private static final String DISPERSION_ESTIMATION_WITHOUT_REPLICATES =
      "/dispersionEstimationWithoutReplicates.Rnw";
  private static final String KINETIC_ANADIFF = "/kineticAnadiff.Rnw";
  private static final String NOT_KINETIC_ANADIFF = "/notKineticAnadiff.Rnw";

  private Design design;
  private File expressionFilesDirectory;
  private File outPath;
  private String expressionFilesPrefix;
  private String expressionFilesSuffix;
  private RSConnectionNewImpl rConnection;

  //
  // Public methods
  //

  public void run() throws EoulsanException {

    try {
      // create an experiment map
      HashMap<String, List<Sample>> experiments = experimentsSpliter();
      // create an iterator on the map
      Set<String> cles = experiments.keySet();
      Iterator<String> itr = cles.iterator();
      while (itr.hasNext()) {
        String cle = itr.next();
        List<Sample> experiment = experiments.get(cle);

        if (EoulsanRuntime.getSettings().isRServeServerEnabled())
          putExpressionFiles(experiment);

        String rScript = writeScript(experiment);
        runRnwScript(rScript);

        if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
          removeExpressionFiles(experiment);
          this.rConnection.removeFile(rScript);
          this.rConnection.getAllFiles(outPath.toString() + "/");
        }
      }

    } catch (REngineException e) {
      throw new EoulsanException("Error while running differential analysis: "
          + e.getMessage());
    } catch (REXPMismatchException e) {
      throw new EoulsanException("Error while getting file : " + e.getMessage());

    } finally {
      try {
        if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
          this.rConnection.removeAllFiles();
          this.rConnection.disConnect();
        }
      } catch (Exception e) {
        throw new EoulsanException("Error while removing files on server : "
            + e.getMessage());
      }
    }

  }

  /**
   * Write the R script
   * @return rScript a String containing script to run
   * @throws EoulsanException
   */
  public String writeScript(List<Sample> experiment) throws EoulsanException {

    final Map<String, List<Integer>> conditionsMap =
        new HashMap<String, List<Integer>>();

    final List<Integer> rSampleIds = new ArrayList<Integer>();
    final List<String> rSampleNames = new ArrayList<String>();
    final List<String> rCondNames = new ArrayList<String>();
    final List<String> rRepTechGroup = new ArrayList<String>();
    int i = 0;

    // Get samples ids, conditions names/indexes and replicate types
    for (Sample s : experiment) {

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

      if (!conditionsMap.containsKey(condition)) {
        List<Integer> index = new ArrayList<Integer>();
        index.add(i);
        conditionsMap.put(condition, index);
      } else {
        conditionsMap.get(condition).add(i);
      }

      rSampleIds.add(s.getId());
      rSampleNames.add(s.getName());
      rCondNames.add(condition);

      i++;
    }

    // Determine if there is biological replicates
    boolean biologicalReplicate = false;
    for (String condition : rCondNames) {
      List<Integer> condPos = conditionsMap.get(condition);
      for (i = 0; i < condPos.size() - 1; i++) {
        int pos1 = condPos.get(i);
        int pos2 = condPos.get(i + 1);
        if (!rRepTechGroup.get(pos1).equals(rRepTechGroup.get(pos2))) {
          biologicalReplicate = true;
        }
        if (biologicalReplicate)
          break;
      }
      if (biologicalReplicate)
        break;
    }

    // Check repTechGroup field coherence
    for (i = 0; i < rRepTechGroup.size(); i++) {
      String repTechGroup1 = rRepTechGroup.get(i);
      String condition = rCondNames.get(i);
      for (int j = 0; j < rRepTechGroup.size(); j++) {
        String repTechGroup2 = rRepTechGroup.get(j);
        if (!repTechGroup2.equals(repTechGroup1)) {
          if (rCondNames.get(j).equals(condition)) {
            throw new EoulsanException(
                "There is a mistake in RepTechGroup field of design file : "
                    + "two condition have the same repTechGroup");
          }
        }
      }
    }

    final StringBuilder sb = new StringBuilder();

    sb.append("\\documentclass[a4paper,10pt]{article}\n");
    sb.append("\\usepackage[utf8]{inputenc}\n");
    sb.append("\\usepackage{lmodern}\n");
    sb.append("\\usepackage{a4wide}\n");
    sb.append("\\usepackage{marvosym}\n");
    sb.append("\\usepackage{graphicx}\n\n");

    sb.append("\\SweaveOpts{eps = FALSE, pdf = TRUE}\n");
    sb.append("\\setkeys{Gin}{width=0.95\textwidth}\n\n");

    sb.append("\\title{"
        + experiment.get(1).getMetadata().getExperiment() + " analysis}\n\n");

    sb.append("\\begin{document}\n");

    sb.append("\\maketitle\n\n");

    // Add function part of the script
    sb.append("<<echo=FALSE>>=\n");

    sb.append("### Auto generated by ");
    sb.append(Globals.APP_NAME);
    sb.append(" ");
    sb.append(Globals.APP_VERSION_STRING);
    sb.append(" on ");
    sb.append(new Date(System.currentTimeMillis()));
    sb.append(" ###\n\n");

    // add function part to string builder
    sb.append(readStaticScript(NORMALISATION_FUNCTIONS));
    sb.append("@\n\n");

    sb.append("\\section{Initialization}\n");
    sb.append("<<>>=\n");
    // determine if there is technical replicates
    boolean rep = false;
    for (int j = 0; j < rSampleIds.size(); j++) {
      if (!rRepTechGroup.get(j).toLowerCase().equals("na")) {
        rep = true;
      } else {
        // replace "na" values of repTechGroup by unique sample ids to avoid
        // pooling problem while executing R script
        rRepTechGroup.set(j, rSampleIds.get(j).toString());
      }
    }

    // Test if there is a reference field for kinetic experiments
    if (isReference(experiment)) {
      for (Sample s : experiment) {
        String refval = s.getMetadata().getReference().trim().toLowerCase();
        if (refval.equals("true")) {
          // add reference to R script
          sb.append("ref <- "
              + "\"" + s.getMetadata().getCondition() + "\"\n\n");
          break;
        }
      }
    }

    // Add normalization part
    if (rep)
      writeWithTechnicalReplicate(sb, rSampleIds, rSampleNames, rCondNames,
          rRepTechGroup, experiment.get(1).getMetadata().getExperiment());
    else
      writeWithoutTechnicalReplicates(sb, rSampleIds, rSampleNames, rCondNames,
          experiment.get(1).getMetadata().getExperiment());

    // add dispersion estimation part
    if (biologicalReplicate) {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITH_REPLICATES));
    } else {
      sb.append(readStaticScript(DISPERSION_ESTIMATION_WITHOUT_REPLICATES));
    }

    if (isReference(experiment)) {
      sb.append(readStaticScript(KINETIC_ANADIFF));
    } else {
      sb.append(readStaticScript(NOT_KINETIC_ANADIFF));
    }

    String rScript = null;
    try {
      rScript =
          experiment.get(1).getMetadata().getExperiment()
              + "_" + "diffAna" + ".Rnw";
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
   * Put all expression files needed for the analysis on the R server
   * @throws REngineException
   */
  public void putExpressionFiles(List<Sample> experiment)
      throws REngineException {

    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // put file on rserve server
      this.rConnection.putFile(new File(expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix),
          this.expressionFilesPrefix + i + this.expressionFilesSuffix);
    }
  }

  public void removeExpressionFiles(List<Sample> experiment)
      throws REngineException {
    int i;

    for (Sample s : experiment) {
      i = s.getId();

      // remove file from rserve server
      this.rConnection.removeFile(expressionFilesDirectory
          + "/" + this.expressionFilesPrefix + i + this.expressionFilesSuffix);
    }
  }

  //
  // Private methods
  //

  private HashMap<String, List<Sample>> experimentsSpliter() {
    String exp = this.design.getSample(0).getMetadata().getExperiment();
    List<Sample> samples = this.design.getSamples();
    // create design HashMap
    HashMap<String, List<Sample>> experimentTab =
        new HashMap<String, List<Sample>>();
    List<Sample> sampleList = new ArrayList<Sample>();
    for (Sample s : samples) {
      String expName = s.getMetadata().getExperiment();

      if (exp.equals(expName)) {
        sampleList.add(s);
      }
    }
    // put first experiment
    experimentTab.put(exp, sampleList);

    // add other experiments
    for (Sample s1 : samples) {
      String expName = s1.getMetadata().getExperiment();
      // reinitialize sampleList
      sampleList = new ArrayList<Sample>();

      exp = s1.getMetadata().getExperiment();

      if (!experimentTab.containsKey(exp)) {
        for (Sample s2 : this.design.getSamples()) {
          expName = s2.getMetadata().getExperiment();
          if (exp.equals(expName)) {
            sampleList.add(s2);
          }
        }
        experimentTab.put(exp, sampleList);
      }
    }

    return experimentTab;
  }

  /**
   * Execute the analysis.
   * @param rScript
   * @throws IOException
   * @throws REngineException
   * @throws EoulsanException
   */
  private void runRnwScript(String rnwScript) throws REngineException,
      EoulsanException {

    if (EoulsanRuntime.getSettings().isRServeServerEnabled()) {
      this.rConnection.executeRnwCode(rnwScript);

    } else {

      try {

        final ProcessBuilder pb =
            new ProcessBuilder("/usr/bin/R", "CMD", "Sweave",
                StringUtils.bashEscaping(rnwScript));

        // Set the temporary directory for R
        pb.environment().put("TMPDIR", this.outPath.getAbsolutePath());

        ProcessUtils.logEndTime(pb.start(), pb.toString(),
            System.currentTimeMillis());

        if (!new File(rnwScript).delete())
          LOGGER.warning("Unable to remove R script: " + rnwScript);

      } catch (IOException e) {

        throw new EoulsanException(
            "Error while executing R script in anadiff: " + e.getMessage());
      }

    }

  }

  /**
   * Read a static part of the generated script.
   * @return a String with the static part of the script
   */
  private String readStaticScript(String ST) {

    final StringBuilder sb = new StringBuilder();

    final InputStream is = DiffAna.class.getResourceAsStream(ST);

    try {
      final BufferedReader br = FileUtils.createBufferedReader(is);

      String line;

      while ((line = br.readLine()) != null) {

        sb.append(line);
        sb.append('\n');
      }
    } catch (IOException e) {
    }

    return sb.toString();
  }

  /**
   * Write code with technical replicates.
   * @param sb StringBuilder to use
   * @param rSampleIds R samples ids
   * @param rSampleNames R samples names
   * @param rCondIndexes R conditions indexes
   * @param rCondNames R conditions names
   * @param rRepTechGroup R technical replicate group
   */
  private void writeWithTechnicalReplicate(final StringBuilder sb,
      final List<Integer> rSampleIds, final List<String> rSampleNames,
      final List<String> rCondNames, final List<String> rRepTechGroup,
      final String experimentName) {

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

    // put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append("" + id);
      if (i < rSampleIds.size())
        sb.append(",");
    }
    sb.append(")\n\n");

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"" + expressionFilesPrefix + '\"' + ',');
    sb.append("sampleIds"
        + ',' + '\"' + expressionFilesSuffix + '\"' + ',' + "sep=\"\"" + ")"
        + "\n\n");

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

    // Add exp, projectPath, outPath and projectName
    sb.append("# create vector of comparision to proceed\n");
    sb.append("exp <- c()\n");
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\"" + experimentName + "\"" + "\n");
    sb.append("@\n\n");

    // add not variable part of the analysis
    sb.append(readStaticScript(NORMALISATION_WHITH_TECHREP));

  }

  /**
   * Write normalization code without replicates
   * @param sb A StringBuilder
   * @param rSampleIds
   * @param rSampleNames
   * @param rCondNames
   */
  private void writeWithoutTechnicalReplicates(final StringBuilder sb,
      final List<Integer> rSampleIds, final List<String> rSampleNames,
      final List<String> rCondNames, String experimentName) {

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

    // put sample ids into R vector
    sb.append("sampleIds <- c(");
    int i = 0;
    for (int id : rSampleIds) {
      i++;
      sb.append("" + id);
      if (i < rSampleIds.size())
        sb.append(",");
    }
    sb.append(")\n\n");

    // Add file names vector
    sb.append("#create file names vector\n");
    sb.append("fileNames <- paste(\"" + expressionFilesPrefix + '\"' + ',');
    sb.append("sampleIds"
        + ',' + '\"' + expressionFilesSuffix + '\"' + ',' + "sep=\"\"" + ")"
        + "\n\n");

    // Add repTechGroup vector equal to sampleNames to avoid error in R
    // function buildTarget
    sb.append("# create technical replicates groups vector\n");
    sb.append("repTechGroup <- sampleNames\n\n");

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

    // Add exp, projectPath, outPath and projectName
    sb.append("# create vector of comparision to proceed\n");
    sb.append("exp <- c()\n");
    sb.append("# projectPath : path of count files directory\n");
    sb.append("projectPath <- \"\"\n");
    sb.append("# outPath path of the outputs\n");
    sb.append("outPath <- \"./\"\n");
    sb.append("projectName <- ");
    sb.append("\"" + experimentName + "\"" + "\n");
    sb.append("@\n\n");

    // add not variable part of the analysis
    sb.append(readStaticScript(NORMALISATION_WHITHOUT_TECHREP));

  }

  /**
   * Test if there is reference in an experiment
   * @param experiment
   * @return boolean isRef
   */
  private boolean isReference(List<Sample> experiment) {
    boolean isRef = false;
    if (experiment.get(1).getMetadata().isReference()) {
      for (Sample s : experiment) {
        if (s.getMetadata().getReference().toLowerCase().equals("true")) {
          isRef = true;
          break;
        }
      }
    }
    return isRef;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param design Design to set
   */
  public DiffAna(final Design design, final File expressionFilesDirectory,
      final String expressionFilesPrefix, final String expressionFilesSuffix,
      final File outPath, final String rServerName) {

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
