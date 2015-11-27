package fr.ens.transcriptome.eoulsan.steps.peakcalling;

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;

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
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.data.DataMetadata;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.Version;

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepOutputDataFile;
import fr.ens.transcriptome.eoulsan.core.workflow.Workflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.InputPort;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
* This class defines the macs2 peak-calling step.
* Handle multiple experiments with one control per experiment.
* @author Pierre-Marie Chiaroni - CSB lab - ENS - Paris
* @author Celine Hernandez - CSB lab - ENS - Paris
*/
@LocalOnly
public class MACS2Step extends AbstractStep {

    private static final String CALLER_NAME = "MACS2";
    private static final String SHIPPED_PACKAGE_VERSION = "2.0.10.20131216";
    private static final String PACKAGE_ARCHIVE = "MACS2-2.0.10.20131216.tar.gz";
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

    
    private boolean isMACS2installed = false;
    private String macs2BinPath = "";
    private String macs2LibPath = "";

    private static DataFormat MACS2_RMODEL = DataFormatRegistry.getInstance().getDataFormatFromName("macs2rmodel");
    private static DataFormat GAPPED_PEAK = DataFormatRegistry.getInstance().getDataFormatFromName("gappedpeaks");
    private static DataFormat PEAK_XLS = DataFormatRegistry.getInstance().getDataFormatFromName("peaksxls");
    private static DataFormat PEAK = DataFormatRegistry.getInstance().getDataFormatFromName("peaks");

    @Override
    public String getName() {
        return this.CALLER_EXECUTABLE;
    }

    @Override
    public String getDescription() {
        return "This step performs peak calling using macs2.";
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
        builder.addPort("input", true, MAPPER_RESULTS_BAM);
        return builder.create();
    }

    @Override
    public OutputPorts getOutputPorts() {
        final OutputPortsBuilder builder = new OutputPortsBuilder();
        builder.addPort("outputr", true, this.MACS2_RMODEL);
        builder.addPort("outputgap", true, this.GAPPED_PEAK);
        builder.addPort("outputxls", true, this.PEAK_XLS);
        builder.addPort("outputpeak", true, this.PEAK);
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
        
            getLogger().info("MACS2 parameter: " + p.getName() + " : " + p.getStringValue());
            
            if ("is.broadpeak".equals(p.getName())) {
                this.isBroad = p.getBooleanValue();
            }
            else if ("genome.size".equals(p.getName())) {
                this.genomeSize = p.getStringValue();
            }
            else if ("q.value".equals(p.getName())) {
                this.qvalue = p.getDoubleValue();
            }
            else if ("p.value".equals(p.getName())) {
                this.pvalue = p.getDoubleValue();
            }
            else if ("make.bedgraph".equals(p.getName())) {
                this.makeBdg = p.getBooleanValue();
            }
            else if ("extra.args".equals(p.getName())) {
                this.extraArgs = p.getStringValue();
            }
            else
                throw new EoulsanException("Unknown parameter for "
                    + getName() + " step: " + p.getName());
        }
        
        // Coherence checks between pvalue and qvalue
        if(this.pvalue!=0 && this.qvalue!=0) {
            getLogger().warning("As p-value threshold is provided, q-value threshold will be ignored by macs2.");
        }
        if(this.pvalue==0 && this.qvalue==0) {
            getLogger().warning("Neither p-value nor q-value threshold was provided. Macs2 defaults to q-value thresold = 0.01.");
        }
        
        // Check if MACS2 needs to be installed
        this.install();
                
        // Display version of MACS2
        getLogger().info("Will run MACS2 version: " + getMACS2Version());
    }

    /**
    * Check whether MACS2 is already installed. If not decompress 
    * Eoulsan's included MACS2 archive and install it.
    */
    private void install() {

        // Check with python whether MACS2 is avalaible
        try{
            this.isMACS2installed = ProcessUtils.execToString("python -c 'import pkgutil; allnames = [name for (module_loader, name, ispkg) in pkgutil.iter_modules()]; print \"MACS2\" in allnames'").startsWith("True");
        } catch (java.io.IOException e) {
            getLogger().warning("Cannot determine if MACS2 is installed. Installing MACS2. e:" + e.toString());
        }

        // If MACS2 is installed, exit installation
        if(this.isMACS2installed) {
            getLogger().info("MACS2 already installed (python CL check). No installation.");
            return;
        }
        
        // If MACS2 not installed, install it
        getLogger().info("MACS2 not installed. Installing...");        
        try {
            // Get the shipped archive
            String binaryFile = BinariesInstaller.install(this.CALLER_NAME,
                this.SHIPPED_PACKAGE_VERSION, this.PACKAGE_ARCHIVE, 
                EoulsanRuntime.getSettings().getTempDirectoryFile().getAbsolutePath());
            getLogger().info("Archive location : " + binaryFile);
            DataFile macs2Archive = new DataFile(binaryFile);
            
            // Set up a few paths to be used later
            String macs2Path = macs2Archive.getParent() + "/MACS2-2.0.10.20131216/";
            this.macs2BinPath = macs2Path + "bin/macs2";
            this.macs2LibPath = macs2Path + "lib/python2.7/site-packages";
            
            // Extract full archive
            String cmd = String.format("tar -xzf %s -C %s",
                macs2Archive.getSource(), macs2Archive.getParent().getSource());
            getLogger().info("Extract archive : " + cmd);
            ProcessUtils.exec(cmd, false);
            
            // Install MACS2
            cmd = String.format("python2 setup.py install --prefix %s", macs2Path);
            getLogger().info("Installing : " + cmd + "in folder " + macs2Path);
            ProcessUtils.exec(cmd, false);
            
            // Set the flag indicating that MACS2 is now installed.
            this.isMACS2installed = true;

        } catch (java.io.IOException e) {
            getLogger().warning("Error during MACS2 file installation : " + e.toString());
            return;
        }
            
    }

    /**
    * Get current MACS2 version. 
    * @return Value returned by 'macs2 --version'. If an exception occures or if MACS2 is not installed (or install() has not been called yet), return 'Unknown'.
    */
    private String getMACS2Version() {
    
        // Access version
        try{
            if(this.isMACS2installed) {
                return ProcessUtils.execToString("macs2 --version").trim();
            }
        } catch (java.io.IOException e) {
            getLogger().warning("Cannot determine MACS2 version. Error:" + e.toString());
        }
        
        // If IO exception or MACS2 not installed, return 'Unknown'.
        return "Unknown";

    }
    
    
    /**
    * Run macs2. Installation was made during configuration.
    */
    @Override
    public StepResult execute(final StepContext context, final StepStatus status) {
        
        // Get input data (BAM format)
        final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_BAM);
        
        // If we don't have sufficient input files: end step
        if(!inData.isList() || inData.getListElements().size()<2) {
            getLogger().severe("Not enough data to run MACS2. Need at least one control and one sample.");
            return status.createStepResult();
        }
        
        // List all experiments and their corresponding controls
        // Note that if by error multiple controls are specified for an experiment, only the last one will be used
        HashMap<String, Data> expMap = new HashMap<String, Data>(inData.getListElements().size()/2);
        for(Data anInputData : inData.getListElements()) {
        
            boolean isReference = anInputData.getMetadata().get("Reference").toLowerCase().equals("true");
            String experimentName = anInputData.getMetadata().get("Experiment");
            
            // If we have a control, add it along with the experiment name
            if(isReference) {
                if(expMap.containsKey(experimentName) && expMap.get(experimentName) != null) {
                    getLogger().warning("Multiple control samples for experiment " + experimentName + ". First one will be used.");
                }
                else {
                    expMap.put(experimentName, anInputData);
                }
            }
            // if we have a sample, and no key yet set for the experiment, add it
            // this will allow us to check later that each experiment has a control, and raise a warning otherwise
            else if(!expMap.containsKey(experimentName)) {
                expMap.put(experimentName, null);
            }
        }

        // Before running MACS2 we have to create empty data list objects to hold newly created outputs files
        final Data rModelDataList = context.getOutputData(MACS2_RMODEL, "rmodellist");
        final Data gappedPeakDataList = context.getOutputData(GAPPED_PEAK, "gappedpeaklist");
        final Data peakXlsDataList = context.getOutputData(PEAK_XLS, "peakxlslist");
        final Data peakDataList = context.getOutputData(PEAK, "peaklist");
        
        // Loop through all samples
        for(Data sampleData : inData.getListElements()) {
            
            // Get metadata of current sample
            DataMetadata metadata = sampleData.getMetadata();

            // If current sample is a control, we don't call MACS2 on it
            // (Information is in the design.txt file, column Reference)
            if(metadata.get("Reference").toLowerCase().equals("true")) {
                getLogger().info("Skipping control file.");
                continue;
            }
            
            // Get experiment name of current sample
            String currentExpName = metadata.get("Experiment");
            getLogger().finest("Experiment " + currentExpName);
            
            // Check that we have a control for this experiment
            if(expMap.get(currentExpName)==null) {
                getLogger().warning("Sample " + metadata.get("Name") + " from experiment " + currentExpName + "has no control data. Will not be treated by MACS2.");
                continue;
            }       
            getLogger().finest("Control data " + expMap.get(currentExpName).getDataFile().getSource());
            

            /////////
            // Construct the command line. TODO: use StringBuilder

            String cmd = "";
            
            // Add folder where MACS2 was installed, if necessary
            if(!macs2LibPath.equals("")) { cmd += String.format("PYTHONPATH=%s:$PYTHONPATH ", macs2LibPath); }
            
            // First part of macs2 command
            cmd += macs2BinPath.equals("")? "macs2" : String.format("python2 %s", macs2BinPath);
            cmd += " callpeak";
            
            // Provide control and sample files
            cmd += String.format(" -t %s", sampleData.getDataFile().getSource());
            cmd += String.format(" -c %s", expMap.get(currentExpName).getDataFile().getSource());
            cmd += " -f BAM";
            
            // If pvalue threshold is set, qvalue threshold will be ignored by macs2
            if(this.pvalue != 0) { 
                cmd += String.format(" --pvalue %f", pvalue);
            }
            else if(this.qvalue != 0) { 
                cmd += String.format(" --qvalue %f", qvalue);
            }
            else { // Default is set by macs2 to qvalue=0.01
                cmd += " --qvalue 0.01";
            }
            
            // Options/parameters and extra arguments
            String prefixOutputFiles = String.format("macs2_ouput_%s", metadata.get("Name").replaceAll("[^a-zA-Z0-9]", ""));
            cmd += String.format(" --name %s", prefixOutputFiles);
            cmd += String.format(" --gsize %s", genomeSize);
            if(isBroad) { cmd += " --broad"; }
            if(makeBdg) { cmd += " --bdg"; }
            cmd += String.format(" %s", extraArgs);
                        
            // Run the command line.
            getLogger().info("Running : " + cmd);
            String result = "";
            try {
                result = ProcessUtils.execToString(cmd, true, false);
            } catch (java.io.IOException e) {
                getLogger().severe(e.toString());
            }

            /////////
            // Rename output files to be Eoulsan-complient

            // Create new Data objects and register them into the output lists
            // Needed because we are dealing with lists of files for each type

            // R model
            final Data rModelData = rModelDataList.addDataToList(metadata.get("Name").replaceAll("[^a-zA-Z0-9]", "")+"R");
            rModelData.getMetadata().set(metadata);

            // Gapped peak
            final Data gappedPeakData = gappedPeakDataList.addDataToList(metadata.get("Name").replaceAll("[^a-zA-Z0-9]", "")+"GP");
            gappedPeakData.getMetadata().set(metadata);
            
            // Peaks (Excel format)
            final Data peakXlsData = peakXlsDataList.addDataToList(metadata.get("Name").replaceAll("[^a-zA-Z0-9]", "")+"Xls");
            peakXlsData.getMetadata().set(metadata);
            
            // Peaks
            final Data peakData = peakDataList.addDataToList(metadata.get("Name").replaceAll("[^a-zA-Z0-9]", "")+"Peak");
            peakData.getMetadata().set(metadata);


            // Now we must rename the outputs generated by MACS2 so that they correspond to the naming scheme of Eoulsan, and thus make them available to further analysis steps
            // First, create a Datafile with one of the potential output file name of MACS2
            // If the file does exist, rename it to the name created by Eoulsan and stored in data.getDataFile()
            
            try {
                DataFile sampleDataFolder = sampleData.getDataFile().getParent();
                
                // R model
                final DataFile tmpRmodelFile = new DataFile(sampleDataFolder, prefixOutputFiles + "_model.r");
                if (tmpRmodelFile.exists()) {
                    tmpRmodelFile.toFile().renameTo(rModelData.getDataFile().toFile());
                }
                
                // Gapped peak
                final DataFile tmpGappedPeakFile = new DataFile(sampleDataFolder, prefixOutputFiles + "_peaks.gappedPeak");
                if (tmpGappedPeakFile.exists()) {
                    tmpGappedPeakFile.toFile().renameTo(gappedPeakData.getDataFile().toFile());
                }
                
                // Peaks (Excel format)
                final DataFile tmpPeakXlsFile = new DataFile(sampleDataFolder, prefixOutputFiles + "_peaks.xls");
                if (tmpPeakXlsFile.exists()) {
                    tmpPeakXlsFile.toFile().renameTo(peakXlsData.getDataFile().toFile());
                }
                
                // Peak
                // Peak file extension depends on one of the command line options
                final DataFile tmpPeakFile;
                if (isBroad) {
                    tmpPeakFile = new DataFile(sampleDataFolder, prefixOutputFiles + "_peaks.broadPeak");
                }
                else {
                    tmpPeakFile = new DataFile(sampleDataFolder, prefixOutputFiles + "_peaks.narrowPeak");
                }
                if (tmpPeakFile.exists()) {
                    tmpPeakFile.toFile().renameTo(peakData.getDataFile().toFile());
                }
            
            } catch (java.io.IOException e) {
                getLogger().severe("Could not determine folder of sample data file " + sampleData.getDataFile() + ". Error:" + e.toString() + " \nMACS2 output files will not be renamed.");
            }
            
        }
        
        return status.createStepResult();
        
    }

}
