package fr.ens.biologie.genomique.eoulsan.modules.chipseq.peakcalling;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.biologie.genomique.eoulsan.modules.chipseq.ChIPSeqDataFormats.GAPPED_PEAK;
import static fr.ens.biologie.genomique.eoulsan.modules.chipseq.ChIPSeqDataFormats.MACS2_RMODEL;
import static fr.ens.biologie.genomique.eoulsan.modules.chipseq.ChIPSeqDataFormats.PEAK;
import static fr.ens.biologie.genomique.eoulsan.modules.chipseq.ChIPSeqDataFormats.PEAK_XLS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.util.process.DockerManager;
import fr.ens.biologie.genomique.eoulsan.util.process.SimpleProcess;

/**
 * This class defines the macs2 peak-calling step. Handle multiple experiments
 * with one control per experiment.
 * @author Pierre-Marie Chiaroni - CSB lab - ENS - Paris
 * @author Celine Hernandez - CSB lab - ENS - Paris
 * @author Cedric Michaud - CSB lab - ENS - Paris
 */
@LocalOnly
public class MACS2Module extends AbstractModule {

  private static final String CALLER_NAME = "MACS2";
  private static final String CALLER_EXECUTABLE = "macs2";

  // Group for Hadoop counters.
  protected static final String COUNTER_GROUP = "peak_calling";

  /**
   * Settings for macs2
   */

  // Parameters and arguments for MACS2 command line
  private boolean isBroad = false;
  private String genomeSize = "hs";
  private double qvalue = 0;
  private double pvalue = 0;
  private boolean makeBdg = false;
  private String extraArgs = "";
  private boolean isPairedEnd = false;

  private Requirement requirement;

  final String dockerImage = "genomicpariscentre/macs2:latest";

  @Override
  public String getName() {
    return CALLER_EXECUTABLE;
  }

  @Override
  public String getDescription() {
    return "This step performs peak calling using macs2.";
  }

  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("input", true, MAPPER_RESULTS_BAM);
    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    final OutputPortsBuilder builder = new OutputPortsBuilder();
    builder.addPort("outputr", true, MACS2_RMODEL);
    builder.addPort("outputgap", true, GAPPED_PEAK);
    builder.addPort("outputxls", true, PEAK_XLS);
    builder.addPort("outputpeak", true, PEAK);
    return builder.create();
  }

  /**
   * Set the parameters of the step to configure the step.
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      getLogger()
          .info("MACS2 parameter: " + p.getName() + " : " + p.getStringValue());

      if ("is.broadpeak".equals(p.getName())) {
        this.isBroad = p.getBooleanValue();
      } else if ("genome.size".equals(p.getName())) {
        this.genomeSize = p.getStringValue();
      } else if ("q.value".equals(p.getName())) {
        this.qvalue = p.getDoubleValue();
      } else if ("p.value".equals(p.getName())) {
        this.pvalue = p.getDoubleValue();
      } else if ("make.bedgraph".equals(p.getName())) {
        this.makeBdg = p.getBooleanValue();
      } else if ("extra.args".equals(p.getName())) {
        this.extraArgs = p.getStringValue();
      } else if ("is.paired.end".equals(p.getName())) {
        this.isPairedEnd = p.getBooleanValue();
      } else
        throw new EoulsanException(
            "Unknown parameter for " + getName() + " step: " + p.getName());
    }

    // Coherence checks between pvalue and qvalue
    if (this.pvalue != 0 && this.qvalue != 0) {
      getLogger().warning(
          "As p-value threshold is provided, q-value threshold will be ignored by macs2.");
    }
    if (this.pvalue == 0 && this.qvalue == 0) {
      getLogger().warning(
          "Neither p-value nor q-value threshold was provided. Macs2 defaults to q-value thresold = 0.01.");
      qvalue = 0.01;
    }

    this.requirement =
        DockerRequirement.newDockerRequirement(dockerImage, true);
  }

  @Override
  public Set<Requirement> getRequirements() {

    return Collections.singleton(this.requirement);
  }

  /**
   * Run macs2.
   */
  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Get input data (BAM format)
    final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_BAM);

    // Creation of a design object of the experimental design
    final Design design = context.getWorkflow().getDesign();

    // Before running MACS2 we have to create empty data list objects to hold
    // newly created outputs files
    final Data rModelDataList =
        context.getOutputData(MACS2_RMODEL, "rmodellist");
    final Data gappedPeakDataList =
        context.getOutputData(GAPPED_PEAK, "gappedpeaklist");
    final Data peakXlsDataList = context.getOutputData(PEAK_XLS, "peakxlslist");
    final Data peakDataList = context.getOutputData(PEAK, "peaklist");

    // If we don't have sufficient input files: end step
    if (!inData.isList() || inData.getListElements().size() < 2) {
      getLogger().severe(
          "Not enough data to run MACS2. Need at least one control and one sample.");
      return status.createTaskResult();
    }

    // Construction of a HashMap containing a SampleName as String corresponding
    // to a specific Data.
    HashMap<String, Data> nameMap =
        new HashMap<String, Data>(inData.getListElements().size() / 2);
    for (Data anInputData : inData.getListElements()) {
      String name = anInputData.getMetadata().getSampleName();
      nameMap.put(name, anInputData);
    }

    // The refSampleName correspond to the control (reference) of an experiment.
    String refSampleName = "null";

    // First loop on Experiments.
    for (Experiment e : design.getExperiments()) {
      // First loop on ExperimentSamples to find the input for this experiment.
      for (ExperimentSample expSam : e.getExperimentSamples()) {

        // When mergin files with RepTechGroup, all the samples in the design
        // are not set in the nameMap hashmap. This test allow to use only
        // samples that are present in the nameMap hashmap.
        if (!nameMap.containsKey(expSam.getSample().getName())) {
          continue;
        }

        // Check if there is a control for the Experiment. If not, set it to
        // null.
        if (DesignUtils.getReference(expSam).equals("true")) {
          refSampleName = expSam.getSample().getName();
          break;
        }
      }

      if (refSampleName == "null") {
        getLogger().warning("No control for experiment : " + e.getName());
      }

      // Second loop on ExperimentSamples which match the Data and launch macs2.
      for (ExperimentSample expSam2 : e.getExperimentSamples()) {
        if (!nameMap.containsKey(expSam2.getSample().getName())) {
          getLogger().info("Skipping empty sample after merge : "
              + expSam2.getSample().getName());
          continue;
        }

        if (refSampleName == expSam2.getSample().getName()) {
          getLogger().info("Skipping control file.");
          continue;
        }

        // Construct the command line
        List<String> commandLine = new ArrayList<String>();

        // First part of macs2 command
        commandLine.add("macs2");
        commandLine.add("callpeak");

        // Provide control and sample files
        commandLine.add("-t");
        getLogger()
            .info("nomSample : " + nameMap.get(expSam2.getSample().getName()));
        commandLine
            .add(nameMap.get(expSam2.getSample().getName()).getDataFilename());

        // Test if there is a control for the experiment, if not the peak
        // calling will be performed
        // without control.
        if (refSampleName != "null") {
          commandLine.add("-c");
          commandLine.add(nameMap.get(refSampleName).getDataFilename());
        }

        // If paired end
        if (this.isPairedEnd) {
          commandLine.add("-f");
          commandLine.add("BAMPE");

        } else { // If single end
          commandLine.add("-f");
          commandLine.add("BAM");
        }

        // If pvalue threshold is set, qvalue threshold will be ignored by macs2
        if (this.pvalue != 0) {
          commandLine.add("--pvalue");
          commandLine.add(String.format("%f", pvalue));
        } else { // Default was set to qvalue=0.01
          commandLine.add("--qvalue");
          commandLine.add(String.format("%f", qvalue));
        }

        // Options/parameters and extra arguments
        String prefixOutputFiles = String.format("macs2_ouput_%s",
            expSam2.getSample().getName().replaceAll("[^a-zA-Z0-9]", ""));
        commandLine.add("--name");
        commandLine.add(String.format("%s", prefixOutputFiles));
        commandLine.add("--gsize");
        commandLine.add(String.format("%s", genomeSize));
        if (isBroad) {
          commandLine.add("--broad");
        }
        if (makeBdg) {
          commandLine.add("--bdg");
        }

        commandLine.add(String.format("%s", extraArgs));

        String commandLine2 = Joiner.on(" ").join(commandLine);

        final File stdoutFile = new File("docker.out");
        final File stderrFile = new File("docker.err");

        getLogger().info("Run command line : " + commandLine2);
        try {
          final SimpleProcess process =
              DockerManager.getInstance().createImageInstance(dockerImage);
          final int exitValue = process.execute(commandLine,
              context.getStepOutputDirectory().toFile(),
              context.getLocalTempDirectory(), stdoutFile, stderrFile);

          ProcessUtils.throwExitCodeException(exitValue,
              Joiner.on(' ').join(commandLine));
        } catch (IOException err) {
          return status.createTaskResult(err);
        }

        /////////
        // Rename output files to be Eoulsan-complient

        // Create new Data objects and register them into the output lists
        // Needed because we are dealing with lists of files for each type

        // R model
        final Data rModelData = rModelDataList.addDataToList(
            expSam2.getSample().getName().replaceAll("[^a-zA-Z0-9]", "") + "R");
        rModelData.getMetadata()
            .set(nameMap.get(expSam2.getSample().getName()).getMetadata());

        // Gapped peak
        final Data gappedPeakData = gappedPeakDataList.addDataToList(
            expSam2.getSample().getName().replaceAll("[^a-zA-Z0-9]", "")
                + "GP");
        gappedPeakData.getMetadata()
            .set(nameMap.get(expSam2.getSample().getName()).getMetadata());

        // Peaks (Excel format)
        final Data peakXlsData = peakXlsDataList.addDataToList(
            expSam2.getSample().getName().replaceAll("[^a-zA-Z0-9]", "")
                + "Xls");
        peakXlsData.getMetadata()
            .set(nameMap.get(expSam2.getSample().getName()).getMetadata());

        // Peaks
        final Data peakData = peakDataList.addDataToList(
            expSam2.getSample().getName().replaceAll("[^a-zA-Z0-9]", "")
                + "Peak");
        peakData.getMetadata()
            .set(nameMap.get(expSam2.getSample().getName()).getMetadata());

        // Now we must rename the outputs generated by MACS2 so that they
        // correspond to the naming scheme of Eoulsan, and thus make them
        // available to further analysis steps
        // First, create a Datafile with one of the potential output file name
        // of
        // MACS2
        // If the file does exist, rename it to the name created by Eoulsan and
        // stored in data.getDataFile()

        try {
          DataFile sampleDataFolder = nameMap.get(expSam2.getSample().getName())
              .getDataFile().getParent();

          // R model
          final DataFile tmpRmodelFile =
              new DataFile(sampleDataFolder, prefixOutputFiles + "_model.r");
          if (tmpRmodelFile.exists()) {
            tmpRmodelFile.toFile().renameTo(rModelData.getDataFile().toFile());
          }

          // Gapped peak
          final DataFile tmpGappedPeakFile = new DataFile(sampleDataFolder,
              prefixOutputFiles + "_peaks.gappedPeak");
          if (tmpGappedPeakFile.exists()) {
            tmpGappedPeakFile.toFile()
                .renameTo(gappedPeakData.getDataFile().toFile());
          }

          // Peaks (Excel format)
          final DataFile tmpPeakXlsFile =
              new DataFile(sampleDataFolder, prefixOutputFiles + "_peaks.xls");
          if (tmpPeakXlsFile.exists()) {
            tmpPeakXlsFile.toFile()
                .renameTo(peakXlsData.getDataFile().toFile());
          }

          // Peak
          // Peak file extension depends on one of the command line options
          final DataFile tmpPeakFile;
          if (isBroad) {
            tmpPeakFile = new DataFile(sampleDataFolder,
                prefixOutputFiles + "_peaks.broadPeak");
          } else {
            tmpPeakFile = new DataFile(sampleDataFolder,
                prefixOutputFiles + "_peaks.narrowPeak");
          }
          if (tmpPeakFile.exists()) {
            tmpPeakFile.toFile().renameTo(peakData.getDataFile().toFile());
          }

        } catch (java.io.IOException err) {
          getLogger().severe("Could not determine folder of sample data file "
              + nameMap.get(expSam2.getSample().getName()).getDataFile()
              + ". Error:" + err.toString()
              + " \nMACS2 output files will not be renamed.");
        }
      }
    }

    return status.createTaskResult();

  }

}
