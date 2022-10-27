package fr.ens.biologie.genomique.eoulsan.modules.chipseq;

import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.BIGBED;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.BIGWIG;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;

/**
 * This class construct TrackHub for genome browser visualization.
 * @author Cédric Michaud
 */
@LocalOnly
public class TrackHubModule extends AbstractModule {

  // Parameters and arguments for MACS2 command line
  private String shortLabel = "shortLabel";
  private String longLabel = "longLabel";
  private String email = "email";
  private String genome = "genome";

  // For bigDataUrl : the complete path where your files are stored.
  private String server = "http://server/";
  private String bigDataUrl = "URLPath";
  private boolean multiWIG = false;

  @Override
  public String getName() {
    return "trackhub";
  }

  @Override
  public String getDescription() {
    return "This step construct trackhub based on the experimental design.";
  }

  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("inputbigwig", true, BIGWIG);
    builder.addPort("inputbigbed", true, BIGBED);
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

      switch (p.getLowerStringValue()) {

      case "shortlabel.name":
        this.shortLabel = p.getStringValue();
        break;

      case "longlabel.name":
        this.longLabel = p.getStringValue();
        break;

      case "e.mail":
      case "email":
        this.email = p.getStringValue();
        break;

      case "data.path":
        this.bigDataUrl = p.getStringValue();
        break;

      case "multi.wig":
        this.multiWIG = p.getBooleanValue();
        break;

      case "server.name":
        this.server = p.getStringValue();
        break;

      default:
        Modules.unknownParameter(context, p);
        break;
      }
    }

  }

  /**
   * Run trackhub generator.
   */
  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final Design design = context.getWorkflow().getDesign();

    // Define the current date as a string
    final String date = new SimpleDateFormat("yyyy_MM_dd").format(new Date());

    // Get input data (BIGWIG format)
    final Data BigWigData = context.getInputData(BIGWIG);

    // Get input data (BIGBED format)
    final Data BigBedData = context.getInputData(BIGBED);

    // Construct a HashMap containing the SampleName as String corresponding to
    // a specific BIGWIG data
    Map<String, Data> nameMapBigWig = new HashMap<>();
    for (Data anInputData : BigWigData.getListElements()) {
      String name = anInputData.getMetadata().getSampleName();
      nameMapBigWig.put(name, anInputData);
    }

    // Construct a HashMap containing the SampleName as String corresponding to
    // a specific BIGBED data
    Map<String, Data> nameMapBIGBED = new HashMap<>();
    for (Data anInputData : BigBedData.getListElements()) {
      String name = anInputData.getMetadata().getSampleName();
      nameMapBIGBED.put(name, anInputData);
    }

    // Trackhub folder creation. Use of the current date and the shortLabel.name
    // parameter to
    // name the folder.
    File dir = new File(date + '_' + this.shortLabel);

    try {
      createTrack(dir, design, date, nameMapBigWig, nameMapBIGBED);
    } catch (IOException e) {
      status.createTaskResult(e);
    }

    return status.createTaskResult();
  }

  //
  // Track creating methods
  //

  /**
   * Create track files.
   * @param dir output directory
   * @param design the design
   * @param date the current date as a string
   * @param nameMapBigWig map of BigWig data
   * @param nameMapBigBed map of BigBed data
   * @throws IOException if an error occurs while creating the output files
   */
  private void createTrack(final File dir, final Design design,
      final String date, Map<String, Data> nameMapBigWig,
      Map<String, Data> nameMapBigBed) throws IOException {

    // If the folder already exist the step is stopped.
    if (dir.exists()) {
      throw new IOException(
          "The output folder " + dir.getAbsolutePath() + " already exists");
    }

    // Create the directory
    if (!dir.mkdir()) {
      throw new IOException(
          "Fail to create the folder : " + dir.getAbsolutePath());
    }

    // Create a folder with the name of the genome.
    File genomeDirectory = new File(dir, this.genome);

    if (!genomeDirectory.mkdir()) {
      throw new IOException(
          "Fail to create the folder : " + dir.getAbsolutePath());
    }

    // In this folder, create a hub.txt file where we write all the necessary
    // informations.
    writeHubFile(dir, date);

    // Again in this folder, create a genome.txt file where we write all the
    // necessary informations.
    writeGenomeFile(dir);

    // Part to write all informations in the trackDb file.
    writeTrackDb(genomeDirectory, design, nameMapBigWig, nameMapBigBed);
  }

  /**
   * Write Hub file.
   * @param dir output directory
   * @param date the current date as a string
   * @throws IOException if an error occurs while creating the file
   */
  private void writeHubFile(final File dir, final String date)
      throws IOException {

    try (Writer w =
        new FileWriter(new File(dir, "hub.txt"), Charset.defaultCharset())) {

      w.write("hub " + date + '_' + this.shortLabel + '\n');
      w.write("shortLabel " + this.shortLabel + '\n');
      w.write("longLabel " + this.longLabel + '\n');
      w.write("genomesFile genomes.txt\n");
      w.write("email " + this.email + '\n');
    }
  }

  /**
   * Write genome file.
   * @param dir output directory
   * @throws IOException if an error occurs while creating the file
   */
  private void writeGenomeFile(final File dir) throws IOException {

    try (Writer w = new FileWriter(new File(dir, "genomes.txt"),
        Charset.defaultCharset())) {
      w.write("genome " + this.genome + '\n');
      w.write("trackDb ./" + this.genome + "/trackDb.txt");
    }
  }

  /**
   * Create TrackDb file.
   * @param genomeDirectory output directory.
   * @param design design object
   * @param nameMapBigWig map of BigWig data
   * @param nameMapBigBed map of BigBed data
   * @throws IOException if an error occurs while creating the output file
   */
  private void writeTrackDb(File genomeDirectory, Design design,
      Map<String, Data> nameMapBigWig, Map<String, Data> nameMapBigBed)
      throws IOException {

    // variable for the priority of tacks.
    int priorityTrack = 1;

    final StringBuilder sb = new StringBuilder();

    // First loop on Experiments
    for (Experiment e : design.getExperiments()) {

      // If it is a multiWIG format prepare the trackDb file for it.
      if (this.multiWIG) {
        priorityTrack = addMultiWigTrackDbHeader(sb, e, priorityTrack);
      }

      // First loop on ExperimentSamples
      for (ExperimentSample expSam : e.getExperimentSamples()) {

        // Test for bigwig data file. When mergin files with RepTechGroup, all
        // the samples in the
        // design are not set in the hashmap. This test allow to use only
        // samples that are present in
        // the hashmap.
        if (nameMapBigWig.get(expSam.getSample().getName()) != null) {

          if (this.multiWIG) {

            // Write informations in the trackDb file. (The track, shortLabel,
            // and longLabel lignes use the condition, reptechgroup
            // and experiment name of the samples)
            addMultiWigTrackDbSample(sb, e, expSam, nameMapBigWig);
          } else {

            // Write informations in the trackDb file. (The track, shortLabel,
            // and longLabel lignes use the condition, reptechgroup
            // and experiment name of the samples)
            priorityTrack = addTrackDbStandardSample(sb, e, expSam,
                nameMapBigWig, priorityTrack);
          }
        }
      }

      // Second loop on ExperimentSamples
      for (ExperimentSample expSam : e.getExperimentSamples()) {

        // Test for bigbed data file. Same explanation than for the bigwig data
        // file test.
        if (nameMapBigBed.get(expSam.getSample().getName()) != null) {

          // Write informations in the trackDb file.
          priorityTrack =
              addTrackDbSample(sb, e, expSam, nameMapBigBed, priorityTrack);
        }
      }
    }

    // Write trackDb file
    try (
        Writer writer = new FileWriter(new File(genomeDirectory, "trackDb.txt"),
            Charset.defaultCharset())) {
      writer.write(sb.toString());
    }
  }

  private int addMultiWigTrackDbHeader(StringBuilder sb, Experiment e,
      int priorityTrack) {

    sb.append("track ");
    sb.append(e.getName());
    sb.append("\ncontainer multiWig");
    sb.append("\nshortLabel ");
    sb.append(e.getName());
    sb.append("\nlongLabel ");
    sb.append(e.getName());
    sb.append("\ntype bigWig");
    sb.append("\nconfigurable on");
    sb.append("\naggregate transparentOverlay");
    sb.append("\nshowSubtrackColorOnUi on");
    sb.append("\nvisibility full");
    sb.append("\nautoScale off");
    sb.append("\nviewLimits 0:1");
    sb.append("\nmaxHeightPixels 50:50:11");
    sb.append("\npriority ");
    sb.append(priorityTrack);
    sb.append('\n');

    return priorityTrack + 1;
  }

  private void addMultiWigTrackDbSample(StringBuilder sb, Experiment experiment,
      ExperimentSample sample, Map<String, Data> nameMapBigWig) {

    sb.append("\ttrack ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('_');
    sb.append(experiment.getName());
    sb.append('\n');

    sb.append("\tparent ");
    sb.append(experiment.getName());
    sb.append('\n');

    sb.append("\tbigDataUrl ");
    sb.append(server);
    sb.append(bigDataUrl);
    sb.append(
        nameMapBigWig.get(sample.getSample().getName()).getDataFilename());
    sb.append('\n');

    sb.append("\tshortLabel ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('\n');

    sb.append("\tlongLabel ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('_');
    sb.append(experiment.getName());
    sb.append('\n');

    sb.append("\ttype bigWig\n");

    if (DesignUtils.getReference(sample).equals("true")) {
      sb.append("\tcolor 210,210,210\n");
    } else {
      sb.append("\tcolor 244,195,165\n");
    }
    sb.append('\n');

  }

  private int addTrackDbSample(StringBuilder sb, Experiment experiment,
      ExperimentSample sample, Map<String, Data> nameMapBigBed,
      int priorityTrack) {

    sb.append("track ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('_');
    sb.append(experiment.getName());
    sb.append("_Peak");
    sb.append('\n');

    sb.append("bigDataUrl ");
    sb.append(server);
    sb.append(this.bigDataUrl);
    sb.append(
        nameMapBigBed.get(sample.getSample().getName()).getDataFilename());
    sb.append('\n');

    sb.append("shortLabel ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append("_Peak");
    sb.append('\n');

    sb.append("longLabel ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('_');
    sb.append(experiment.getName());
    sb.append("_Peak");
    sb.append('\n');

    sb.append("type bigBed\n");
    sb.append("visibility dense\n");
    sb.append("autoScale off\n");
    sb.append("color 90,90,255\n");

    sb.append("priority ");
    sb.append(priorityTrack);
    sb.append('\n');

    sb.append('\n');

    return priorityTrack + 1;
  }

  private int addTrackDbStandardSample(StringBuilder sb, Experiment experiment,
      ExperimentSample sample, Map<String, Data> nameMapBigWig,
      int priorityTrack) {

    sb.append("track ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('_');
    sb.append(experiment.getName());
    sb.append('\n');

    sb.append("bigDataUrl ");
    sb.append(server);
    sb.append(bigDataUrl);
    sb.append(
        nameMapBigWig.get(sample.getSample().getName()).getDataFilename());
    sb.append('\n');

    sb.append("shortLabel ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('\n');

    sb.append("longLabel ");
    sb.append(DesignUtils.getCondition(sample));
    sb.append('_');
    sb.append(DesignUtils.getRepTechGroup(sample));
    sb.append('_');
    sb.append(experiment.getName());
    sb.append('\n');

    sb.append("type bigWig\n");
    sb.append("visibility full\n");
    sb.append("autoScale off\n");
    sb.append("viewLimits 0:1\n");

    if (DesignUtils.getReference(sample).equals("true")) {
      sb.append("color 210,210,210\n");
    } else {
      sb.append("color 244,195,165\n");
    }

    sb.append("priority ");
    sb.append(priorityTrack);
    sb.append('\n');

    sb.append('\n');

    return priorityTrack + 1;
  }

}
