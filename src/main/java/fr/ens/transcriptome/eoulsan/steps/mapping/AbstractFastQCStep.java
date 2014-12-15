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

    // TODO
    System.out.println("define input ports");
    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("fastq", DataFormats.READS_FASTQ);

    if (!this.inputFormatParameter.equals("fastq")) {
      builder.addPort("sam", DataFormats.MAPPER_RESULTS_SAM);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    // TODO
    System.out.println("define input ports");
    return singleOutputPort(DataFormats.REPORT_HTML);
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    // TODO
    System.out.println("start configure with "
        + stepParameters.size() + " parameters define.");
    for (final Parameter p : stepParameters) {

      System.out.println("parameter " + p.getName() + "-" + p.getValue());

      switch (p.getName()) {

      case INPUT_FORMAT_KEY:
        // Set inputPort fastq/sam from parameters
        this.inputFormatParameter = p.getValue();

        if (this.inputFormatParameter.trim()
            .toUpperCase(Globals.DEFAULT_LOCALE).equals("sam")) {

          this.inputFormat = DataFormats.MAPPER_RESULTS_SAM;

        }

        break;

      default:
      }
    }

    // TODO
    System.out.println("End configure, input param "
        + this.inputFormatParameter + " input format "
        + getInputFormat().getDescription());

  }

  protected DataFormat getInputFormat() {

    return this.inputFormat;
  }

}
