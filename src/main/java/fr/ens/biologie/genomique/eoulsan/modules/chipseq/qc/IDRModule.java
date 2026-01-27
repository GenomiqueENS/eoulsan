package fr.ens.biologie.genomique.eoulsan.modules.chipseq.qc;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.util.BinariesInstaller;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;

/**
 * This class defines the IDR quality check step. This QC step determines the
 * reproducibility of peaks between replicates.
 * @author Pierre-Marie Chiaroni - CSB lab - ENS - Paris
 * @author Celine Hernandez - CSB lab - ENS - Paris
 */
@LocalOnly
public class IDRModule extends AbstractModule {

  private static final String TOOL_NAME = "IDR";

  private static final String SHIPPED_PACKAGE_VERSION = "20120922_patched";

  /**
   * Name of the archive containing IDR.
   */
  private static final String PACKAGE_ARCHIVE =
      "idrCode_20120922_patched.tar.gz";

  /**
   * DataFormat of the input peak file.
   */
  private static final DataFormat PEAK =
      DataFormatRegistry.getInstance().getDataFormatFromName("peaks");

  private static final List<String> ACCEPTED_ASSEMBLIES =
      Arrays.asList("human.hg19", "human.hg18", "mm9", "worm.ws220");

  private static final Map<String, String> ACCEPTED_ASSEMBLIES_FILES;

  static {
    Map<String, String> tempMap = new HashMap<>(4);
    tempMap.put("human.hg19", "./genome_tables/genome_table.human.hg19.txt");
    tempMap.put("human.hg18", "./genome_tables/genome_table.human.hg18.txt");
    tempMap.put("mm9", "./genome_tables/genome_table.mm9.txt");
    tempMap.put("worm.ws220", "./genome_tables/genome_table.worm.ws220.txt");
    ACCEPTED_ASSEMBLIES_FILES = Collections.unmodifiableMap(tempMap);
  }

  //
  // Parameters of IDR execution
  //

  /**
   * Truncate input peak list. As written in IDR documentation, concerning
   * parameter peak.half.width : "IMPORTANT: Currrently this parameter does not
   * work properly so please pre-truncate your peaks if desired before feeding
   * to IDR. Always set this parameter to -1." Consequently, this parameter is
   * always -1 and cannot be modified.
   */
  private int peakHalfWidth = -1;

  /**
   * Overlap ratio between two peaks.
   */
  private int minOverlapRatio = 0;

  /**
   * If the input peak file contains narrow or broad peaks.
   */
  private boolean isBroadpeak = false;

  /**
   * Value from the (narrow/broad)peak file used to rank the peaks. Can take
   * three values: "signal.value", "p.value" or "q.value".
   */
  private String rankingMeasure = "p.value";

  /**
   * Genome assembly used for the mapping step. IDR needs to know the size of
   * the chromosomes. Consequently to this default initialization, if no
   * 'assembly' parameter is set in the param.xml file, hg19 will be used by
   * default. A specific log message will warn the user.
   */
  private String assembly = "human.hg19";

  /**
   * Path where IDR was installed.
   */
  private String idrPath = "";

  //
  // Overriden methods.
  //

  /**
   * Name of the Step.
   */
  @Override
  public String getName() {
    return "idr";
  }

  /**
   * A short description of the tool and what is done in the step.
   */
  @Override
  public String getDescription() {
    return "This step performs a quality control using IDR algorithm, as implemented by idrCode (https://sites.google.com/site/anshulkundaje/projects/idr).";
  }

  /**
   * Version.
   */
  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  /**
   * Define input ports.
   */
  @Override
  public InputPorts getInputPorts() {
    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("inputpeaklists", true, PEAK);
    return builder.create();
  }

  //
  // Step
  //

  /**
   * Install IDR archive. Installation path will be memorized. R needs to be
   * installed and available.
   */
  private void install() {
    try {

      BinariesInstaller installer =
          new BinariesInstaller(Globals.APP_NAME, Globals.APP_VERSION_STRING);

      // Get the shipped archive
      String binaryFile = installer.install(TOOL_NAME, SHIPPED_PACKAGE_VERSION,
          PACKAGE_ARCHIVE, EoulsanRuntime.getSettings().getTempDirectoryFile()
              .getAbsolutePath());
      getLogger().info("Archive location : " + binaryFile);
      DataFile idrArchive = new DataFile(binaryFile);

      this.idrPath = idrArchive.getParent().getSource();
      String cmd =
          String.format("tar -xzf %s -C %s", idrArchive.getSource(), idrPath);
      getLogger().info("Unpacking archive : " + cmd);
      ProcessUtils.exec(cmd, false);

    } catch (java.io.IOException e) {
      getLogger()
          .warning("Error during IDR file installation : " + e.toString());
    }
  }

  /**
   * Set IDR parameters to configure the step. As written in IDR documentation,
   * concerning parameter peak.half.width : "IMPORTANT: Currrently this
   * parameter does not work properly so please pre-truncate your peaks if
   * desired before feeding to IDR. Always set this parameter to -1."
   * @param context step configuration context
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      getLogger()
          .info("IDR parameter: " + p.getName() + " : " + p.getStringValue());

      if ("min.overlap.ratio".equals(p.getName())) {
        this.minOverlapRatio = p.getIntValue();
      } else if ("peak.half.width".equals(p.getName())) {
        getLogger().warning(
            "As written in IDR documentation, concerning parameter peak.half.width :\nIMPORTANT: Currrently this parameter does not work properly so please pre-truncate your peaks if desired before feeding to IDR. Always set this parameter to -1.\nSee https://sites.google.com/site/anshulkundaje/projects/idr#TOC-IDR-CODE-README or IDR's README file.");
      } else if ("is.broadpeak".equals(p.getName())) {
        this.isBroadpeak = p.getBooleanValue();
      } else if ("ranking.measure".equals(p.getName())) {
        String tmpRanking = p.getStringValue();
        if ("signal.value".equals(tmpRanking)
            || "p.value".equals(tmpRanking) || "q.value".equals(tmpRanking)) {
          this.rankingMeasure = tmpRanking;
        } else {
          throw new EoulsanException("Unknown value ("
              + p.getStringValue() + ") for ranking.measure parameter (step: "
              + getName() + ").");
        }
      } else if ("assembly".equals(p.getName())) {
        if (ACCEPTED_ASSEMBLIES.contains(p.getStringValue())) {
          this.assembly = p.getStringValue();
        } else {
          throw new EoulsanException(
              "Unknow value for 'assembly' parameter (IDR step) : "
                  + p.getStringValue());
        }
      } else {
        throw new EoulsanException(
            "Unknown parameter for " + getName() + " step: " + p.getName());
      }
    }

    // If assembly was not provided by user, hg19 is set by default.
    if (this.assembly.equals("")) {
      getLogger().warning(
          "No assembly specified. Will use human genome (version 19) by default");
    }

    // TODO: check if already installed
    this.install();

  }

  /**
   * Install all the files necessary in the tmp folder, then run idr.
   */
  @Override
  // public StepResult execute(final Design design, final Context context) {
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Get input data (PEAK format, as generated by )
    final Data inData = context.getInputData(PEAK);
    // getLogger().info("All input data : " + inData.getMetadata()); // empty?

    // If we don't have sufficient input files
    if (!inData.isList() || inData.getListElements().size() < 2) {
      getLogger().severe(
          "Not enough data to run IDR. Need a list of at least two samples.");
      return status.createTaskResult();
    }

    // List all samples per experiment, skip reference files
    HashMap<String, ArrayList<Data>> expMap =
        new HashMap<>(inData.getListElements().size() / 2);
    for (Data anInputData : inData.getListElements()) {

      // getLogger().info("One input file : " +
      // anInputData.getMetadata().toString());
      getLogger().info("One input file. ref : "
          + anInputData.getMetadata().get("Reference") + "| exp : "
          + anInputData.getMetadata().get("Experiment") + "| rep : "
          + anInputData.getMetadata().get("RepTechGroup"));

      boolean isReference = anInputData.getMetadata().get("Reference")
          .toLowerCase(Globals.DEFAULT_LOCALE).equals("true");
      String experimentName = anInputData.getMetadata().get("Experiment");

      // if we have a control, add it along with the experiment name
      if (isReference) {
        getLogger().info("Reference file, not treated.");
        continue;
      }

      // if we have a sample
      getLogger().info("Not a reference file. Proceeding.");
      if (expMap.get(experimentName) == null) {
        ArrayList<Data> tmpList = new ArrayList<>();
        tmpList.add(anInputData);
        expMap.put(experimentName, tmpList);
      } else {
        expMap.get(experimentName).add(anInputData);
      }
      getLogger().info("Now "
          + expMap.get(experimentName).size() + " samples for experiment "
          + experimentName);
    }

    // Loop through each experiment
    for (String experimentName : expMap.keySet()) {

      // Get all samples of current experiment
      ArrayList<Data> expDataList = expMap.get(experimentName);

      // Get all Data split into different replicate groups
      HashMap<String, ArrayList<Data>> replicatesMap =
          new HashMap<>(expDataList.size() / 2);
      for (Data sample : expDataList) {

        String replicateGroupName = sample.getMetadata().get("RepTechGroup");

        if (replicatesMap.get(replicateGroupName) == null) {
          ArrayList<Data> tmpList = new ArrayList<>();
          tmpList.add(sample);
          replicatesMap.put(replicateGroupName, tmpList);
        } else {
          replicatesMap.get(replicateGroupName).add(sample);
        }
      }

      // For each list of replicates
      // Treat two by two all data from a set of replicates
      for (Map.Entry<String, ArrayList<Data>> entry : replicatesMap
          .entrySet()) {

        ArrayList<Data> replicatesList = entry.getValue();
        // Shouldn't have no but... we never know.
        if (replicatesList == null || replicatesList.size() == 0) {
          getLogger().severe("Incoherence : experiment "
              + entry.getKey() + " has no corresponding sample.");
        }

        int nbSameReplicates = replicatesList.size();

        // Case where we have no replicate, just one sample
        if (nbSameReplicates == 1) {
          Data tmp = replicatesList.get(0);
          getLogger().info("Will not call IDR on 1 sample, for experiment "
              + tmp.getMetadata().get("Experiment") + " in replicate group "
              + tmp.getMetadata().get("RepTechGroup"));
          continue;
        }

        String outputDir = "";
        try {
          outputDir =
              replicatesList.get(0).getDataFile().getParent().getSource();
        } catch (java.io.IOException e) {
          getLogger().severe("Error while accessing parent folder of "
              + replicatesList.get(0).getDataFile().getSource() + ". "
              + e.toString());
        }

        // Loop through all samples and run the analysis for each couple
        ArrayList<String> outputs = new ArrayList<>();
        for (int i = 0; i < nbSameReplicates; i++) {
          for (int j = i + 1; j < nbSameReplicates; j++) {

            int retVal =
                runAnalysis(replicatesList.get(i), replicatesList.get(j));
            if (retVal == 0) {
              outputs.add(String.format("%s/idr_output_%s_vs_%s", outputDir,
                  replicatesList.get(i).getMetadata().get("Name"),
                  replicatesList.get(j).getMetadata().get("Name")));
            }
          }
        }

        // Call IDR on all replicates together, if we have more than 2
        if (outputs.size() > 1) {
          runAnalysisPlot(outputs.size(),
              String.format("%s/idrplot_output_%s", outputDir, entry.getKey()),
              outputs);
        }

      }
    }

    return status.createTaskResult();
  }

  /**
   * Compare two data files (replicates).
   */
  private int runAnalysis(Data data1, Data data2) {

    if (!data1.getDataFile().exists()) {
      getLogger().severe(
          "File " + data1.getDataFile().getSource() + " doesn't exist.");
      return -1;
    }
    if (!data2.getDataFile().exists()) {
      getLogger().severe(
          "File " + data2.getDataFile().getSource() + " doesn't exist.");
      return -1;
    }

    String outputDir = "";
    try {
      outputDir = data1.getDataFile().getParent().getSource();
    } catch (java.io.IOException e) {
      getLogger().severe("Error while accessing parent folder of "
          + data1.getDataFile().getSource() + ". " + e.toString());
      return -1;
    }

    // As IDR is "well written", it sources files that are located inside its
    // own folder. So we must move to that folder first in order for IDR to find
    // them...
    // Another problem: file genome_table.txt, also located in the same
    // folder was supposed to be copied from the folder genome_tables,
    // depending on the desired assembly to be used by IDR.
    // Consequently, IDR was patched in order to accept the path to the
    // genome table to be used as input parameter (8th argument).
    String inputPrefix = String.format("idr_output_%s_vs_%s",
        data1.getMetadata().get("Name"), data2.getMetadata().get("Name"));
    String cmd = String.format(
        "cd %s/idrCode/ ; Rscript %s/batch-consistency-analysis.r %s %s %d %s/%s %d %s %s %s; cd -",
        this.idrPath, ".", data1.getDataFile().getSource(),
        data2.getDataFile().getSource(), this.peakHalfWidth, outputDir,
        inputPrefix, this.minOverlapRatio, this.isBroadpeak ? "T" : "F",
        this.rankingMeasure, ACCEPTED_ASSEMBLIES_FILES.get(this.assembly));
    getLogger().info("Running : " + cmd);

    String outputStr = "";
    try {
      outputStr = ProcessUtils.execToString(cmd);
    } catch (java.io.IOException e) {
      getLogger().severe(e.toString());
      getLogger().severe("\nException while running IDR:\n" + outputStr);
      return -1;
    }

    // Find a way to check that everything went well from the output string?
    // Check that the output string contains the output of the "cd -" i.e. path
    // of the output folder
    // if(!outputStr.startsWith(outputDir)) {
    // getLogger().info("Output string:\n" + outputStr);
    // return -1;
    // }

    // Run plot on generated files
    String outputPrefix = String.format("idrplot_output_%s_vs_%s",
        data1.getMetadata().get("Name"), data2.getMetadata().get("Name"));
    return runAnalysisPlot(1, String.format("%s/%s", outputDir, outputPrefix),
        Collections
            .singletonList(String.format("%s/%s", outputDir, inputPrefix)));
  }

  /**
   * Buil plot from one or more analyses. usage: Rscript
   * batch-consistency-plot-merged.r [npairs] [output.file.prefix]
   * [input.file.prefix 1, 2, 3 ...] [npairs]: integer, number of consistency
   * analyses (e.g. if 2 replicates, npairs=1, if 3 replicates, npairs=3
   * [output.file.prefix]: output prefix for plot [input.file.prefix 1, 2, 3]:
   * prefix for the output from batch-consistency-analysis2. They are the input
   * files for merged analysis. It can be multiple files
   */
  private int runAnalysisPlot(int nbPairs, String outputPrefix,
      Iterable<String> inputPrefixIter) {

    // Build the concatenated list of all the prefixes to be used together
    // http://stackoverflow.com/questions/523871/best-way-to-concatenate-list-of-string-objects
    StringBuilder sb = new StringBuilder();
    String sep = "";
    String separator = " ";
    for (String s : inputPrefixIter) {
      sb.append(sep).append(s);
      sep = separator;
    }

    String cmd = String.format(
        "cd %s/idrCode/ ; Rscript %s/batch-consistency-plot.r %d %s %s; cd -",
        this.idrPath, ".", nbPairs, outputPrefix, sb.toString());
    getLogger().info("Running : " + cmd);

    // Execute command
    String outputStr = "";
    try {
      outputStr = ProcessUtils.execToString(cmd);
    } catch (java.io.IOException e) {
      getLogger().warning("Exception while running IDR plot:\n"
          + e.toString() + "\nOutput string:\n" + outputStr);
      return -1;
    }

    return 0;
  }

} // End of class IDRStep
