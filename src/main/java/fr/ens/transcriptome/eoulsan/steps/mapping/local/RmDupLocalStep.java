package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

import java.io.IOException;
import java.io.File;
import java.util.Set;

/**
* This class removes PCR duplicates from a SAM file.
* It uses Picard's MarkDuplicates to either mark or remove PCR duplicates.
* @author Celine Hernandez - CSB lab - ENS - Paris
*/
@LocalOnly
public class RmDupLocalStep extends AbstractStep {

    /**
     *
     */
    private static final String STEP_NAME = "rmdup";

    /**
     * Group for hadoop counters.
     */
    protected static final String COUNTER_GROUP = "rmdup";

    //
    // Settings for rmdup 
    //

    /**
     * Should duplicates be removed or only annotated?
     */
    private boolean delete = true;

    /**
     * Should input file be sorted before?
     */
    private boolean sort = true;

    //
    // Overridden methods 
    //

    /**
     * Name of the Step.
     */    
    @Override
    public String getName() {
        return this.STEP_NAME;
    }

    /**
     * A short description of the tool and what is done in the step.
     */    
    @Override
    public String getDescription() {
        return "This step removes PCR duplicates from a SAM file. It uses Picard's MarkDuplicates to either mark or remove them.";
    }

    /**
     * Should a log file be created?
     */    
    @Override
    public boolean isCreateLogFiles() {
        return true;
    }

    /**
     * Version.
     */    
    @Override
    public Version getVersion() {
        return Globals.APP_VERSION;
    }

    /**
     * Define input port.
     */    
    @Override
    public InputPorts getInputPorts() {
        return singleInputPort(DataFormats.MAPPER_RESULTS_SAM);
    }

    /**
     * Define output port.
     */    
    @Override
    public OutputPorts getOutputPorts() {
        return singleOutputPort(DataFormats.MAPPER_RESULTS_SAM);
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
        
            getLogger().info("Rmdup parameter: " + p.getName() + " : " + p.getStringValue());
            
            if ("remove.marked".equals(p.getName())) {
                this.delete = p.getBooleanValue();
            }
            else if ("sort".equals(p.getName())) {
                this.sort = p.getBooleanValue();
            }
            else
                throw new EoulsanException("Unknown parameter for "
                    + getName() + " step: " + p.getName());
        }
        
    }

    
    /**
    * Run Picard's MarkDuplicates.
    * @throws EoulsanException if temp file can't be created.
    */
    @Override
    public StepResult execute(final StepContext context, final StepStatus status) {
        
        // Get input data (SAM format)
        final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_SAM);
        // Get output data (SAM format)
        final Data outData = context.getOutputData(DataFormats.MAPPER_RESULTS_SAM, inData);
        
        // Get the input sam file
        final DataFile samFile = inData.getDataFile();
        // Get the output file
        final DataFile outputFile = outData.getDataFile();
        
        // This will be the input file 
        File inputFile = samFile.toFile();
        
        // If a sort is necessary
        if(this.sort) {
            try {
                // Create a temp file
                final File tmpSortFile = FileUtils.createTempFile(context.getRuntime().getSettings().getTempDirectoryFile(), "rmdupsort", ".sam");

                getLogger().info("Running Picard's SortSam on " + inputFile + " with output " + tmpSortFile);

                // Set up arguments
                String[] sortArguments = new String[4];
                sortArguments[0]= "INPUT=" + inputFile;
                sortArguments[1]= "OUTPUT=" + tmpSortFile;
                sortArguments[2]= "SORT_ORDER=coordinate";
                sortArguments[3]= "QUIET=true";
                // Start SortSam
                new picard.sam.SortSam().instanceMain(sortArguments);
                
                // Set newly created output file as input for next step
                inputFile = tmpSortFile;
                
            } catch(IOException ioe) {
                ioe.printStackTrace();
                return status.createStepResult();
            }
        }
        
        getLogger().info("Running Picard's MarkDuplicates on " + inputFile + " with output " + outputFile);

        String[] arguments = new String[6];
        arguments[0] = "INPUT="+inputFile;
        arguments[1] = "OUTPUT="+outputFile;
        arguments[2] = "REMOVE_DUPLICATES="+this.delete;
        arguments[3] = "ASSUME_SORTED=true";
        arguments[4] = "METRICS_FILE="+outputFile+".picard_metrics";
        arguments[5] = "QUIET=true";
        
        // picard.sam.MarkDuplicates.main(arguments); // includes a System.exit()
        new picard.sam.MarkDuplicates().instanceMain(arguments);

        // If a temp file was created, delete it
        if(sort) {
            inputFile.delete();
        }
        
        return status.createStepResult();
        
    }

}
