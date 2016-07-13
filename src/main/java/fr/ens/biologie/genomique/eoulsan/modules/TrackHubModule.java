package fr.ens.biologie.genomique.eoulsan.modules;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.BIGWIG;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.BIGBED;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;

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
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.BinariesInstaller;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement;
import fr.ens.biologie.genomique.eoulsan.util.docker.DockerSimpleProcess;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Experiment;
import fr.ens.biologie.genomique.eoulsan.design.ExperimentSample;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;

/**
 * This class construct TrackHub for genome browser visualisation.
 * @author Cédric Michaud
 */
@LocalOnly
public class TrackHubModule extends AbstractModule {

  private static final String CALLER_NAME = "TRACKHUB";
  private static final String CALLER_EXECUTABLE = "trackhub";

  /**
   * Settings for TrackHub generator
   */

  // Parameters and arguments for MACS2 command line
  private String shortLabel = "shortLabel";
  private String longLabel = "longLabel";
  private String email = "email";
  private String genome = "genome";
  //For bigDataUrl : the complete path where your files are stored.
  private String HTTPserver = "http://Serveur/";
  private String bigDataUrl = "URLPath";
  private String multiWIG = "false";

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

    for(Parameter p : stepParameters) {
      
      getLogger().info("TrackHub parameter: " + p.getName() + " : " + p.getStringValue());

      if("shortlabel.name".equals(p.getName())){
        this.shortLabel = p.getStringValue();
      } else if("longlabel.name".equals(p.getName())){
        this.longLabel = p.getStringValue();
      } else if("e.mail".equals(p.getName())){
        this.email = p.getStringValue();
      } else if("genome.name".equals(p.getName())){
        this.genome = p.getStringValue();
      } else if("data.path".equals(p.getName())){
        this.bigDataUrl = p.getStringValue();
      } else if("multi.wig".equals(p.getName())){
	this.multiWIG = p.getStringValue();
      } else if("server.name".equals(p.getName())){
	this.HTTPserver = p.getStringValue();
      } else
        throw new EoulsanException("Unknown parameter for " + getName() + " step: " + p.getName());
    }

  }

  /**
   * Run trackhub generator.
   */
  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final Design design = context.getWorkflow().getDesign();

    Date current = new Date();
    DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
    String dat = dateFormat.format(current);
    getLogger().info("The current date : " + dat);
    
    // Get input data (BIGWIG format)
    final Data BigWigData = context.getInputData(BIGWIG);

    // Get input data (BIGBED format)
    final Data BigBedData = context.getInputData(BIGBED);

    // Construct a HashMap containing the SampleName as String corresponding to a specific BIGWIG data
    HashMap<String, Data> nameMapBIGWIG = new HashMap<String, Data>(BigWigData.getListElements().size() / 2);
    for(Data anInputData : BigWigData.getListElements()){
      String name = anInputData.getMetadata().getSampleName();
      nameMapBIGWIG.put(name, anInputData);
    }

    // Construct a HashMap containing the SampleName as String corresponding to a specific BIGBED data
    HashMap<String, Data> nameMapBIGBED = new HashMap<String, Data>(BigBedData.getListElements().size() / 2);
    for(Data anInputData : BigBedData.getListElements()){
      String name = anInputData.getMetadata().getSampleName();
      nameMapBIGBED.put(name, anInputData);
    }

    //Trackhub folder creation. Use of the current date and the shortLabel.name parameter to
    //name the folder.
    File dir = new File(dat + "_" + shortLabel);

    //If the folder already exist the step is stopped.
    if(dir.exists()) {
      getLogger().info("The folder already exist, error : " + dir.getAbsolutePath());
      try{
        final int exitvalue = 1;
        ProcessUtils.throwExitCodeException(exitvalue, "ERROR : The folder " + dir.getAbsolutePath() + " already exists.");
      } catch (IOException err) {
        return status.createTaskResult(err);
      }
      
    } else {
      if (dir.mkdir()) {
        getLogger().info("Add the folder : " + dir.getAbsolutePath());
      } else {
        getLogger().info("Fail on the folder : " + dir.getAbsolutePath());
      }
    }

    //In this folder, create a hub.txt file where we write all the necessary informations.
    try{
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dat + "_" + shortLabel + "/hub.txt")));
      writer.write("hub " + dat + "_" + shortLabel + "\n");
      writer.write("shortLabel " + shortLabel + "\n");
      writer.write("longLabel " + longLabel + "\n");
      writer.write("genomesFile genomes.txt\n");
      writer.write("email " + email + "\n");

      writer.close();
    }catch (IOException err){
      err.printStackTrace();
    }

    //Again in this folder, create a genome.txt file where we write all the necessary informations.
    try{
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dat + "_" + shortLabel + "/genomes.txt")));
      writer.write("genome " + genome + "\n");
      writer.write("trackDb ./" + genome + "/trackDb.txt");

      writer.close();
    }catch (IOException err){
      err.printStackTrace();
    }

    //Create a folder with the name of the genome.
    File dirGenome = new File(dat + "_" + shortLabel + "/" + genome);
    if(dirGenome.exists()) {
      getLogger().info("The folder already exist : " + dirGenome.getAbsolutePath());
    } else {
      if (dirGenome.mkdir()) {
        getLogger().info("Add the folder : " + dirGenome.getAbsolutePath());
      } else {
        getLogger().info("Fail on the folder : " + dirGenome.getAbsolutePath());
      }
    }

    //Part to write all informations in the trackDb file.

    //variable for the priority of tacks.
    int priorityTrack = 1;

    // First loop on Experiments
    for(Experiment e : design.getExperiments()){

      //If it is a multiWIG format prepare the trackDb file for it.
      if(multiWIG.equals("true")){
	try{
	  BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dat + "_" + shortLabel + "/" + genome + "/trackDb.txt"), true));
	  writer.write("track " + e.getName() + "\n");
	  writer.write("container multiWig\n");
	  writer.write("shortLabel " + e.getName() + "\n");
	  writer.write("longLabel " + e.getName() + "\n");
	  writer.write("type bigWig\n");
	  writer.write("configurable on\n");
	  writer.write("aggregate transparentOverlay\n");
	  writer.write("showSubtrackColorOnUi on\n");
	  writer.write("visibility full\n");
	  writer.write("autoScale off\n");
	  writer.write("viewLimits 0:1\n");
	  writer.write("maxHeightPixels 50:50:11\n");
	  writer.write("priority " + priorityTrack + "\n");
          priorityTrack = priorityTrack + 1;
	  writer.write("\n");

	  writer.close();
	}catch (IOException err){
	  err.printStackTrace();
	}
      }

      //First loop on ExperimentSamples
      for(ExperimentSample expSam : e.getExperimentSamples()){

        //Test for bigwig data file. When mergin files with RepTechGroup, all the samples in the 
	//design are not set in the hashmap. This test allow to use only samples that are present in 
	//the hashmap.
        if(nameMapBIGWIG.get(expSam.getSample().getName()) != null){

	  if(multiWIG.equals("true")){
	    
	    //Write informations in the trackDb file. (The track, shortLabel, and longLabel lignes use the condition, reptechgroup
            //and experiment name of the samples)
	    try{
	      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dat + "_" + shortLabel + "/" + genome + "/trackDb.txt"), true));
	      writer.write("\ttrack " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_" + e.getName() + "\n");
	      writer.write("\tparent " + e.getName() + "\n");
	      writer.write("\tbigDataUrl " + HTTPserver + bigDataUrl + nameMapBIGWIG.get(expSam.getSample().getName()).getDataFilename() + "\n");
	      writer.write("\tshortLabel " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "\n");
	      writer.write("\tlongLabel " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_" + e.getName() + "\n");
	      writer.write("\ttype bigWig\n");
	      if(DesignUtils.getReference(expSam).equals("true")){
                writer.write("\tcolor 210,210,210\n");
              }else{
                writer.write("\tcolor 244,195,165\n");
              }
	      writer.write("\n");

	      writer.close();

	    }catch (IOException err){
	      err.printStackTrace();
	    }
	  }else{

	    //Write informations in the trackDb file. (The track, shortLabel, and longLabel lignes use the condition, reptechgroup
	    //and experiment name of the samples)
	    try{
	      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dat + "_" + shortLabel + "/" + genome + "/trackDb.txt"), true));
	      getLogger().info("BigWig écriture de : " + expSam.getSample().getName());
	      writer.write("track " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_" + e.getName() + "\n");
	      writer.write("bigDataUrl " + HTTPserver + bigDataUrl + nameMapBIGWIG.get(expSam.getSample().getName()).getDataFilename() + "\n");
	      writer.write("shortLabel " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "\n");
	      writer.write("longLabel " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_" + e.getName() + "\n");
	      writer.write("type bigWig\n");
	      writer.write("visibility full\n");
	      writer.write("autoScale off\n");
	      writer.write("viewLimits 0:1\n");
	      if(DesignUtils.getReference(expSam).equals("true")){
	        writer.write("color 210,210,210\n");
	      }else{
	        writer.write("color 244,195,165\n");
	      }
	      writer.write("priority " + priorityTrack + "\n");
	      priorityTrack = priorityTrack + 1;
	      writer.write("\n");

	      writer.close();

	    }catch (IOException err){
              err.printStackTrace();
            }
          }
	}else{
	  continue;
	}
      }

      //Second loop on ExperimentSamples
      for(ExperimentSample expSam : e.getExperimentSamples()){

    	//Test for bigbed data file. Same explanation than for the bigwig data file test.
	if(nameMapBIGBED.get(expSam.getSample().getName()) != null){
	
	  //Write informations in the trackDb file.
	  try{
	    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dat + "_" + shortLabel + "/" + genome + "/trackDb.txt"), true));  
	    getLogger().info("BigBed écriture de : " + expSam.getSample().getName());
            writer.write("track " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_" + e.getName() + "_Peak" + "\n");
            writer.write("bigDataUrl " + HTTPserver + bigDataUrl + nameMapBIGBED.get(expSam.getSample().getName()).getDataFilename() + "\n");
            writer.write("shortLabel " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_Peak" + "\n");
            writer.write("longLabel " + DesignUtils.getCondition(expSam) + "_" + DesignUtils.getRepTechGroup(expSam) + "_" + e.getName() + "_Peak" + "\n");
            writer.write("type bigBed\n");
            writer.write("visibility dense\n");
            writer.write("autoScale off\n");
            writer.write("color 90,90,255\n");
            writer.write("priority " + priorityTrack + "\n");
            priorityTrack = priorityTrack + 1;
	    writer.write("\n");

	    writer.close();

	  }catch (IOException err){
            err.printStackTrace();
          }
	}else{
	  continue;
	}
      }
    }
    return status.createTaskResult();

  }
}
