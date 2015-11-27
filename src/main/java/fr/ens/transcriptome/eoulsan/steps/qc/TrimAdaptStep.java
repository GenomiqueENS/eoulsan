package fr.ens.transcriptome.eoulsan.steps.qc;

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
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
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

import java.io.File;
import java.util.Set;

/**
* This class removes adapter sequences using Trim galore! and cutadapt.
* @author Celine Hernandez - CSB lab - ENS - Paris
*/
@LocalOnly
public class TrimAdaptStep extends AbstractStep {

    private static final String SOFTWARE_LABEL_CA = "cutadapt";
    private static final String SHIPPED_PACKAGE_VERSION_CA = "1.8.1";
    private static final String PACKAGE_ARCHIVE_CA = "cutadapt-1.8.1.tar.gz";
    
    private static final String SOFTWARE_LABEL_TG = "trimgalore";
    private static final String SHIPPED_PACKAGE_VERSION_TG = "0.4.0";
    private static final String PACKAGE_ARCHIVE_TG = "trim_galore_v0.4.0.zip";


    /**
    * Settings
    */

    private String cutadaptPath = "";
    private String trimGalorePath = "";
    
    // Parameters and arguments for the command line

    // Quality Phred score cutoff: 20 by default
    private int phredCutoff = 20;
    
    // Quality encoding type selected: ASCII+33 (default) or ASCII+64
    private String phredType = " --phred33";
    
    // Adapter sequence: 
    // auto                  try to auto-detect adapter sequence, if it can't it defaults to illumina
    // illumina              Adapter sequence to be trimmed is the first 13bp of the Illumina universal adapter
    //                        'AGATCGGAAGAGC' instead of the default auto-detection of adapter sequence.
    // nextera               Adapter sequence to be trimmed is the first 12bp of the Nextera adapter
    //                        'CTGTCTCTTATA' instead of the default auto-detection of adapter sequence.
    // small_rna             Adapter sequence to be trimmed is the first 12bp of the Illumina Small RNA Adapter
    //                        'ATGGAATTCTCG' instead of the default auto-detection of adapter sequence.
    // [ACGTacgt]            Any base sequence
    private String adapter = "";
    
    // Maximum trimming error rate: 0.1 (default)
    private double errorRate = 0.1;
    
    // Minimum required adapter overlap (stringency): 1 bp
    private int stringency = 1;

    // Minimum required sequence length before a sequence gets removed: 20 bp
    private int minSeqLength = 20;

    
    /**
    * Methods
    */
    
    
    @Override
    public String getName() {
        return "trimadapt";
    }

    @Override
    public String getDescription() {
        return "This step cuts adapter sequences.";
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
        return singleInputPort(READS_FASTQ);
    }

    @Override
    public OutputPorts getOutputPorts() {
        return singleOutputPort(READS_FASTQ);
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
        
            getLogger().info("TrimAdapt parameter: " + p.getName() + " : " + p.getStringValue());
            
            // TODO: Trimming mode: single-end/paired-end
            
            // Quality Phred score cutoff: 20 by default
            if ("phred.cutoff".equals(p.getName())) {
            
                // Check value is positive and integer
                if (p.getDoubleValue()<0 && (p.getDoubleValue() % 1) != 0) {
                    throw new EoulsanException("Invalid Phred score cutoff for parameter " + p.getName() +
                            " step: "+ getName());
                }

                this.phredCutoff = (int)p.getDoubleValue();
            }
            // Whether quality encoding type ASCII+64 should be used. 
            // By default, if not present, ASCII+33 is used.
            else if ("phred.type64".equals(p.getName())) {
            
                this.phredType = (p.getBooleanValue()?" --phred64":" --phred33");
                
            }
            // Adapter sequence: 
            // auto                  try to auto-detect adapter sequence, defaults to illumina
            // illumina              Adapter sequence to be trimmed is the first 13bp of the Illumina universal adapter
            //                        'AGATCGGAAGAGC' instead of the default auto-detection of adapter sequence.
            // nextera               Adapter sequence to be trimmed is the first 12bp of the Nextera adapter
            //                        'CTGTCTCTTATA' instead of the default auto-detection of adapter sequence.
            // small_rna             Adapter sequence to be trimmed is the first 12bp of the Illumina Small RNA Adapter
            //                        'ATGGAATTCTCG' instead of the default auto-detection of adapter sequence.
            // [ACGTacgt]            Any base sequence
            else if ("adapter".equals(p.getName())) {
                final String adaptVal = p.getStringValue();
                
                if(adaptVal.equals("auto")) { // auto-detect. Default behavior.
                    this.adapter = "";
                }                
                else if(adaptVal.equals("illumina")) {
                    this.adapter = " --illumina";
                }
                else if(adaptVal.equals("nextera")) {
                    this.adapter = " --nextera";
                }
                else if(adaptVal.equals("small_rna")) {
                    this.adapter = " --small_rna";
                }
                else {
                    boolean valid = true;
                    char[] a = adaptVal.toCharArray();
                    for (char c: a) {
                        valid = (c == 'a') || (c == 'c') || (c == 'g') || (c == 't') ||
                                (c == 'A') || (c == 'C') || (c == 'G') || (c == 'T');
                        if (!valid) {
                            throw new EoulsanException("Invalid character " + c + " in adapter sequence for parameter " + p.getName() +
                                 " step: "+ getName());
                        }
                    }
                    this.adapter = " --adapter " + adaptVal;
                }
            }
            // Maximum trimming error rate: 0.1 (default)
            else if ("error.rate".equals(p.getName())) {
            
                // Check value is positive
                if (p.getDoubleValue()<0) {
                    throw new EoulsanException("Invalid maximum trimming error rate for parameter " + p.getName() +
                            " step: "+ getName());
                }
                
                this.errorRate = p.getDoubleValue();
                
            }
            // Minimum required adapter overlap (stringency): 1 bp
            else if ("stringency".equals(p.getName())) {

                // Check value is positive and integer
                if (p.getDoubleValue()<0 && (p.getDoubleValue() % 1) != 0) {
                    throw new EoulsanException("Invalid minimum required adapter overlap (stringency) for parameter " + p.getName() +
                            " step: "+ getName());
                }

                this.stringency = (int)p.getDoubleValue();
            }
            // Minimum required sequence length before a sequence gets removed: 20 bp
            else if ("min.length".equals(p.getName())) {
            
                // Check value is positive and integer
                if (p.getDoubleValue()<0 && (p.getDoubleValue() % 1) != 0) {
                    throw new EoulsanException("Invalid minimum required sequence length for parameter " + p.getName() +
                            " step: "+ getName());
                }
                this.minSeqLength = (int)p.getDoubleValue();
                
            }
            else
                throw new EoulsanException("Unknown parameter for "
                    + getName() + " step: " + p.getName());
        }
                
        // Install softwares
        this.install();
                
    }

    /**
    * Check whether cutadapt and Trim_galore are already installed. If not decompress 
    * Eoulsan's included archives and install them.
    */
    private void install() {

        // Install cutadapt
        if(!BinariesInstaller.check(this.SOFTWARE_LABEL_CA, this.SHIPPED_PACKAGE_VERSION_CA, "cutadapt")) {
        
            // If cutadapt not installed, install it
            getLogger().info("Cutadapt not installed. Running installation....");      
            
            try {
                // Get the shipped archive
                String binaryFile = BinariesInstaller.install(this.SOFTWARE_LABEL_CA,
                    this.SHIPPED_PACKAGE_VERSION_CA, this.PACKAGE_ARCHIVE_CA, 
                    EoulsanRuntime.getSettings().getTempDirectoryFile().getAbsolutePath());
                getLogger().info("Archive location for cutadapt : " + binaryFile);
                DataFile cutadaptArchive = new DataFile(binaryFile);
                
                // Extract full archive
                String cmd = String.format("tar -xzf %s -C %s",
                    cutadaptArchive.getSource(), cutadaptArchive.getParent().getSource());
                getLogger().info("Extract archive : " + cmd);
                ProcessUtils.system(cmd);
                
                // Memorize path
                this.cutadaptPath = cutadaptArchive.getParent() + "/cutadapt-1.8.1/bin/cutadapt";

            } catch (java.io.IOException e) {
                getLogger().severe("Error during cutadapt installation : " + e.toString());
                return;
            }
        }

        // Install Trim galore!
        if(!BinariesInstaller.check(this.SOFTWARE_LABEL_TG, this.SHIPPED_PACKAGE_VERSION_TG, "trim_galore")) {
        
            // If trim_galore not installed, install it
            getLogger().info("trim_galore not installed. Running installation....");  
            
            try {
                // Get the shipped archive
                String binaryFile = BinariesInstaller.install(this.SOFTWARE_LABEL_TG,
                    this.SHIPPED_PACKAGE_VERSION_TG, this.PACKAGE_ARCHIVE_TG, 
                    EoulsanRuntime.getSettings().getTempDirectoryFile().getAbsolutePath());
                getLogger().info("Archive location for trim_galore : " + binaryFile);
                DataFile trimgArchive = new DataFile(binaryFile);

                // Extract full archive
                // Note that unzip must be in very quiet mode otherwise the process doesn't terminate
//                 String cmd = String.format("unzip -qq %s -d %s",
//                     trimgArchive.getSource(), trimgArchive.getParent().getSource());
                String cmd = String.format("tar -xzf %s -C %s",
                    trimgArchive.getSource(), trimgArchive.getParent().getSource());
                getLogger().info("Extract archive : " + cmd);
                ProcessUtils.system(cmd);

                // Memorize path
                this.trimGalorePath = trimgArchive.getParent() + "/trim_galore_zip/trim_galore";

            } catch (java.io.IOException e) {
                getLogger().severe("Error during trim_galore installation : " + e.toString());
                return;
            }
        
        }


    }

    /**
    * Run cutadapt through trim_galore. Installation (if needed) was made during configuration.
    */
    @Override
    public StepResult execute(final StepContext context, final StepStatus status) {
        
        getLogger().info("Running trim_galore " + this.trimGalorePath + " using cutadapt " + this.cutadaptPath); 

        //// Create command line
        
        // Executable
        String cmd = this.trimGalorePath;

        // Quality Phred score cutoff: 20 by default
        cmd += " --quality " + this.phredCutoff;
        
        // Quality encoding type selected: ASCII+33 (default) or ASCII+64
        cmd += this.phredType;
        
        // Adapter sequence: 
        // auto                  try to auto-detect adapter sequence, defaults to illumina
        // illumina              Adapter sequence to be trimmed is the first 13bp of the Illumina universal adapter
        //                        'AGATCGGAAGAGC' instead of the default auto-detection of adapter sequence.
        // nextera               Adapter sequence to be trimmed is the first 12bp of the Nextera adapter
        //                        'CTGTCTCTTATA' instead of the default auto-detection of adapter sequence.
        // small_rna             Adapter sequence to be trimmed is the first 12bp of the Illumina Small RNA Adapter
        //                        'ATGGAATTCTCG' instead of the default auto-detection of adapter sequence.
        // [ACGTacgt]            Any base sequence
        cmd += this.adapter;
        
        // Maximum trimming error rate: 0.1 (default)
        cmd += " -e " + this.errorRate;
        
        // Minimum required adapter overlap (stringency): 1 bp
        cmd += " --stringency " + this.stringency;

        // Minimum required sequence length before a sequence gets removed: 20 bp
        cmd += " --length " + this.minSeqLength;
        
        // Path to cutadapt
        cmd += " --path_to_cutadapt " + this.cutadaptPath;
        
        // If specified any output to STDOUT or STDERR will be suppressed.
        cmd += " --suppress_warn";
        
        // Get input data (FASTQ format)
        final Data inData = context.getInputData(DataFormats.READS_FASTQ);
        cmd += " " + inData.getDataFile();
        
        
        //// Execute command
        try {  
            ProcessUtils.exec(cmd, false); 
        } catch (java.io.IOException e) {
            getLogger().severe("Could not execute " + cmd + ". Error:" + e.toString());
            return status.createStepResult();
        }
        

        //// Rename the output files to be complient with the naming scheme of Eoulsan (thus making them available to further analysis steps)

        // First, create a Datafile with the output file name of trim_galore
        final DataFile tmpTrimmedFile = new DataFile(inData.getDataFile().getSourceWithoutExtension() + "_trimmed.fq");
        
        // If the file does exist, rename it to the name created by Eoulsan and stored in data.getDataFile()
        if (tmpTrimmedFile.exists()) {

            // Get file name created by Eoulsan
            final Data outputFile = context.getOutputData(DataFormats.READS_FASTQ, inData);

            // Rename output file
            tmpTrimmedFile.toFile().renameTo(outputFile.getDataFile().toFile());
            
            // Rename report
            new DataFile(inData.getDataFile().getSource() + "_trimming_report.txt").toFile().renameTo(new File(outputFile.getDataFile().getSource()+ "_trimming_report.txt"));
        }
        
        
        return status.createStepResult();
        
    }

}
