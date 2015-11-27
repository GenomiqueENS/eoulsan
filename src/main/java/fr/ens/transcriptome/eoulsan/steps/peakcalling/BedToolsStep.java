package fr.ens.transcriptome.eoulsan.steps.peakcalling;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataMetadata;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
* This class uses tools from the BEDTools suite.
* @author Celine Hernandez - CSB lab - ENS - Paris
*/
@LocalOnly
public class BedToolsStep extends AbstractStep {

    private static final String SOFTWARE_LABEL = "bedtools";
    private static final String SHIPPED_PACKAGE_VERSION = "2.24.0";
    private static final String PACKAGE_ARCHIVE = "bedtools-2.24.0.tar.gz";

    private static DataFormat PEAK = DataFormatRegistry.getInstance().getDataFormatFromName("peaks");


    /**
    * Settings
    */

    private String bedtoolsPath = "";
    
    
    /**
    * Methods
    */
    
    
    @Override
    public String getName() {
        return "bedtools";
    }

    @Override
    public String getDescription() {
        return "This step runs a tool from the BEDTools suite.";
    }

    @Override
    public boolean isCreateLogFiles() {
        return true;
    }

    @Override
    public Version getVersion() {
        return Globals.APP_VERSION;
    }

    @Override
    public InputPorts getInputPorts() {
        final InputPortsBuilder builder = new InputPortsBuilder();
        builder.addPort("inputlist", true, this.PEAK);
        return builder.create();
    }

    @Override
    public OutputPorts getOutputPorts() {
        final OutputPortsBuilder builder = new OutputPortsBuilder();
        builder.addPort("outputlist", true, this.PEAK);
        return builder.create();
    }


    /**
    * Set the parameters of the step to configure the step.
    * @param stepParameters parameters of the step
    * @throws EoulsanException if a parameter is invalid
    */
    @Override
    public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
        throws EoulsanException {
        
        for (Parameter p : stepParameters) {
        
            getLogger().info("BEDTools parameter: " + p.getName() + " : " + p.getStringValue());
            
                throw new EoulsanException("Unknown parameter for "
                    + getName() + " step: " + p.getName());
        }
                
        // Install softwares
        this.install();
                
    }

    /**
    * Check whether bedtools are already installed. If not decompress 
    * Eoulsan's included archives and install them.
    */
    private void install() {

//         // Install 
//         if(!BinariesInstaller.check(this.SOFTWARE_LABEL, this.SHIPPED_PACKAGE_VERSION, "bedtools")) {
        
            // If not installed, install it
            getLogger().info("BEDTools not installed. Running installation....");      
            
            try {
                // Get the shipped archive
                String binaryFile = BinariesInstaller.install(this.SOFTWARE_LABEL,
                    this.SHIPPED_PACKAGE_VERSION, this.PACKAGE_ARCHIVE, 
                    EoulsanRuntime.getSettings().getTempDirectoryFile().getAbsolutePath());
                getLogger().info("Archive location for BEDTools : " + binaryFile);
                DataFile archive = new DataFile(binaryFile);
                
                // Extract full archive
                String cmd = String.format("tar -xzf %s -C %s",
                    archive.getSource(), archive.getParent().getSource());
                getLogger().info("Extract archive : " + cmd);
                ProcessUtils.system(cmd);
                
                // Compile
                final String folder = String.format("%s/bedtools2/",
                    archive.getParent().getSource());
                getLogger().info("Running make in : " + folder);
//                 final Process p = Runtime.getRuntime().exec("make", null, new File(folder));
                final Process p = new ProcessBuilder("make").directory(new File(folder)).start();
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    throw new java.io.IOException(e.getMessage());
                }
                
                // Memorize path
                this.bedtoolsPath = archive.getParent() + "/bedtools2/bin/bedtools";

            } catch (java.io.IOException e) {
                getLogger().warning("Error during bedtools installation : " + e.toString());
                return;
            }
//         }

    }

    /**
    * Run bedtools multiinter. Installation (if needed) was made during configuration.
    */
    @Override
    public StepResult execute(final StepContext context, final StepStatus status) {
        
        getLogger().info("Running BEDTools " + this.bedtoolsPath ); 

        // Get input data (PEAK format, as generated by )
        final Data inData = context.getInputData(this.PEAK);

        // First sort data into experiments/replicate groups before we can concatenate what is inside each group
        HashMap<String, ArrayList<Data>> expMap = new HashMap<String, ArrayList<Data>>(inData.getListElements().size()/2);
        for(Data anInputData : inData.getListElements()) {
        
            getLogger().finest("Input file. ref : " + anInputData.getMetadata().get("Reference") + 
                             "| exp : " + anInputData.getMetadata().get("Experiment") + 
                             "| rep : " + anInputData.getMetadata().get("RepTechGroup"));

            boolean isReference = anInputData.getMetadata().get("Reference").toLowerCase().equals("true");

            // if we have a control
            
            // Should we add its peaks also ? Not, for now.
            if(isReference) {
                getLogger().finest("Reference file, not treated.");
                continue;
            }
            getLogger().finest("Not a reference file. Proceeding.");
            
            // if we have a sample
            
            // Access the sample metadata and concatenate values 
            // in order to create a key used to sort samples by category 
            // (experiment and replicate group)
            String experimentName = anInputData.getMetadata().get("Experiment");
            String sortingKey = experimentName;
            
            // Store current sample in expmap
            // If it's the first in its category, create a new container before
            if (expMap.get(sortingKey) == null) {
                ArrayList<Data> tmpList = new ArrayList<Data>();
                tmpList.add(anInputData);
                expMap.put(sortingKey, tmpList);
            }
            else {
                expMap.get(sortingKey).add(anInputData);
            }
            getLogger().finest("Now " + expMap.get(sortingKey).size() + " samples for experiment " + sortingKey);
        }
        
        final Data peakDataList = context.getOutputData(this.PEAK, "mergedPeaklist");

        // Loop through each sorted category
        // to apply bedtools mergeinter
        for (String experimentName : expMap.keySet()) {
            
            // Get all samples of current category
            ArrayList<Data> expDataList = expMap.get(experimentName);
            
            // Do not merge if we have only one file
            // Copy and change name to make it available in Eoulsan for the rest of the analysis
            if(expDataList.size()<2) {
                getLogger().info("Data list contains only 1 sample for experiment " + experimentName);

                Data anInputData = expDataList.get(0);
                
                final Data outputData = peakDataList.addDataToList(anInputData.getMetadata().get("Name").replaceAll("[^a-zA-Z0-9]", ""));
                outputData.getMetadata().set(anInputData.getMetadata());
                
                try {
                    anInputData.getDataFile().symlink(outputData.getDataFile());
                } catch (IOException ioe) {
                    getLogger().severe("Could not create symlink from " + anInputData.getDataFile() + " to " + outputData.getDataFile());
                    return status.createStepResult();
                }
                
                continue;
            }
            
            // Create command line
            
            // Executable
            String cmd = bedtoolsPath;
            cmd += " multiinter";
            
            // Add all files to be merged
            cmd += " -i";
            for (Data sample : expDataList) {
                cmd += String.format(" %s", sample.getDataFile().getSource());
            }
            
            // Run bedtools
            try {
                // Get metadata of one peak file
                DataMetadata metadata = expDataList.get(0).getMetadata();
                // Peaks
                final Data peakData = peakDataList.addDataToList(metadata.get("Name").replaceAll("[^a-zA-Z0-9]", "")+"Mergedpeaks");
                peakData.getMetadata().set(metadata);
                File outputFile = peakData.getDataFile().toFile();
                
                getLogger().info(String.format("Running : %s with output: %s", cmd, outputFile));
                ProcessUtils.execWriteOutput(cmd, outputFile);
                
            } catch (java.io.IOException e){
                getLogger().severe(e.toString());
            }
            
            
            
        }
        
        
        return status.createStepResult();
        
    }

}
