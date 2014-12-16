package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.util.Version;

public abstract class AbstractFastQCStep extends AbstractStep {

  private static final String STEP_NAME = "fastqc";
  private static final String INPUT_FORMAT_KEY = "input.format";

  /** Collector FastQC kmer size */
  public static final String FASTQC_KMER_SIZE_KEY = "fastqc.kmer.size";
  /** Collector FastQC nogroup */
  public static final String FASTQC_NOGROUP_KEY = "fastqc.nogroup";
  /** Use exponential base groups in graph */
  public static final String FASTQC_EXPGROUP_KEY = "fastqc.expgroup";
  /** Format fastq type casava/Illumina */
  public static final String FASTQC_CASAVA_KEY = "fastqc.casava";
  /** Option for filter fastq file if casava=true for all modules */
  public static final String FASTQC_NOFILTER_KEY = "fastqc.nofilter";

  // Default value
  private String inputFormatParameter = "fastq";
  private DataFormat inputFormat = DataFormats.READS_FASTQ;

  protected static final String COUNTER_GROUP = "fastqc";

  //
  // Step methods
  //

  @Override
  public String getName() {
    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step fastqc launch FastQC on FASTQ or SAM files, generate a html report.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("fastq", DataFormats.READS_FASTQ);

    if (!this.inputFormatParameter.equals("fastq")) {
      builder.addPort("sam", DataFormats.MAPPER_RESULTS_SAM);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return singleOutputPort(DataFormats.REPORT_HTML);
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    for (final Parameter p : stepParameters) {

      switch (p.getName()) {

      case INPUT_FORMAT_KEY:
        // Set inputPort fastq/sam from parameters
        this.inputFormatParameter = p.getValue();

        if (this.inputFormatParameter.trim()
            .toLowerCase(Globals.DEFAULT_LOCALE).equals("sam")) {

          this.inputFormat = DataFormats.MAPPER_RESULTS_SAM;

        }

        break;

      case FASTQC_KMER_SIZE_KEY:

        // Convert in int
        if (p.getIntValue() < 1) {
          throw new EoulsanException(
              "Configuration FastQC step: invalid value for parameter kmer size "
                  + p.getValue());
        }

        // Kmer Size, default FastQC value 7
        System.setProperty("fastqc.kmer_size", p.getValue());

        break;

      case FASTQC_NOGROUP_KEY:

        // Set fastQC nogroup, default FastQC value false
        System.setProperty("fastqc.nogroup", p.getBooleanValue() + "");
        break;

      case FASTQC_EXPGROUP_KEY:

        // Set fastQC expgroup, default FastQC value false
        System.setProperty("fastqc.expgroup", p.getBooleanValue() + "");
        break;

      case FASTQC_CASAVA_KEY:

        // Set fastQC format fastq, default FastQC value false
        System.setProperty("fastqc.casava", p.getBooleanValue() + "");
        break;

      case FASTQC_NOFILTER_KEY:

        // Default FastQC value true
        // Set fastQC nofilter default false, if casava=true, filter fastq file
        System.setProperty("fastqc.nofilter", p.getBooleanValue() + "");
        break;

      default:
      }
    }

  }

  protected DataFormat getInputFormat() {

    return this.inputFormat;
  }

}
