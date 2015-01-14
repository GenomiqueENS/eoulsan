package fr.ens.transcriptome.eoulsan.steps.generators;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.Generator;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.STARReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step that generate a STAR mapper index.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@Generator
public class STARIndexGenerator extends AbstractStep {

  private final SequenceReadsMapper mapper = new STARReadsMapper();

  private Integer overhang = 100;
  private boolean gtfFile = false;
  private String chrStartEndFilename;
  private String gtfFeatureExon;
  private String gtfTagExonParentTranscript;
  private Integer genomeSAindexNbases;
  private Integer genomeChrBinNbits;
  private boolean useExpressionStepParameters;

  @Override
  public String getName() {

    return "starindexgenerator";
  }

  @Override
  public String getDescription() {

    return "Generate Mapper index";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("genome", GENOME_FASTA).addPort("genomedescription",
        GENOME_DESC_TXT);

    if (this.gtfFile) {
      builder.addPort("annotation", ANNOTATION_GFF,
          EnumSet.of(CompressionType.NONE), true);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return OutputPortsBuilder.singleOutputPort(this.mapper.getArchiveFormat());
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    if (stepParameters == null) {
      throw new EoulsanException("No parameters set in "
          + getName() + " generator");
    }

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "overhang":
        this.overhang = p.getIntValue();
        break;

      case "gtf.file":
        this.gtfFile = p.getBooleanValue();
        break;

      case "file.chr.start.end":
        this.chrStartEndFilename = p.getStringValue();
        break;

      case "gtf.feature.exon":
        this.gtfFeatureExon = p.getStringValue();
        break;

      case "gtf.tag.exon.parent.transcript":
        this.gtfTagExonParentTranscript = p.getStringValue();
        break;

      case "genome.sa.index.nbases":
        this.genomeSAindexNbases = p.getIntValue();
        break;

      case "genome.chr.bin.nbits":
        this.genomeChrBinNbits = p.getIntValue();
        break;

      case "use.expression.step.parameters":
        this.useExpressionStepParameters = p.getBooleanValue();
        break;

      default:
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());
      }
    }
  }

  /**
   * Set the "gtf.feature.exon" and "gtf.tag.exon.parent.transcript" parameter
   * from the expression step parameters.
   * @param context the context of the task
   * @throws EoulsanException if more than one expression step exists
   */
  private void searchExpressionStepParameters(final StepContext context)
      throws EoulsanException {

    int count = 0;

    for (WorkflowStep step : context.getWorkflow().getSteps()) {

      if (AbstractExpressionStep.STEP_NAME.equals(step.getStepName())) {

        for (Parameter p : step.getParameters()) {

          switch (p.getName()) {

          case AbstractExpressionStep.GENOMIC_TYPE_PARAMETER_NAME:
            gtfFeatureExon = p.getStringValue();
            break;

          case AbstractExpressionStep.ATTRIBUTE_ID_PARAMETER_NAME:
            gtfTagExonParentTranscript = p.getStringValue();
            break;

          default:
            break;
          }
        }
        count++;
      }
    }

    if (count > 1) {
      throw new EoulsanException(
          "Found more than one expression step in the workflow");
    }
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {

      final StringBuilder additionalArguments = new StringBuilder();
      final Map<String, String> additionalDescription = new HashMap<>();

      // Search expression parameter is needed
      if (this.useExpressionStepParameters) {
        searchExpressionStepParameters(context);
      }

      if (this.gtfFile) {

        // Get the annotation data
        final Data annotationData = context.getInputData(ANNOTATION_GFF);

        // Get the annotation DataFile
        final File genomeFile = annotationData.getDataFile().toFile();

        additionalArguments.append("--sjdbGTFfile");
        additionalArguments.append(' ');
        additionalArguments.append(genomeFile);
        additionalArguments.append(' ');
        additionalDescription.put("sjdbGTFfile", computeMD5SumFile(genomeFile));
      }

      if (this.overhang != null) {

        additionalArguments.append("--sjdbOverhang");
        additionalArguments.append(' ');
        additionalArguments.append(this.overhang.toString());
        additionalArguments.append(' ');
        additionalDescription.put("sjdbOverhang", this.overhang.toString());
      }

      if (this.gtfTagExonParentTranscript != null) {

        additionalArguments.append("--sjdbGTFtagExonParentTranscript");
        additionalArguments.append(' ');
        additionalArguments.append(this.gtfTagExonParentTranscript);
        additionalArguments.append(' ');
        additionalDescription.put("sjdbGTFtagExonParentTranscript",
            this.gtfTagExonParentTranscript);
      }

      if (this.gtfFeatureExon != null) {

        additionalArguments.append("--sjdbGTFfeatureExon");
        additionalArguments.append(' ');
        additionalArguments.append(this.gtfFeatureExon);
        additionalArguments.append(' ');
        additionalDescription.put("sjdbGTFfeatureExon", this.gtfFeatureExon);
      }

      if (this.chrStartEndFilename != null) {

        File chrStartEndFile = new File(this.chrStartEndFilename);

        if (!chrStartEndFile.exists() || !chrStartEndFile.isFile()) {
          throw new IOException("Unable to read chromosome startend file: "
              + chrStartEndFile);
        }

        additionalArguments.append("--sjdbFileChrStartEnd");
        additionalArguments.append(' ');
        additionalArguments.append(chrStartEndFile);
        additionalArguments.append(' ');
        additionalDescription.put("sjdbFileChrStartEnd",
            computeMD5SumFile(chrStartEndFile));
      }

      if (this.genomeSAindexNbases != null) {

        additionalArguments.append("--genomeSAindexNbases");
        additionalArguments.append(' ');
        additionalArguments.append(this.genomeSAindexNbases.toString());
        additionalArguments.append(' ');
        additionalDescription.put("genomeSAindexNbases",
            this.genomeSAindexNbases.toString());
      }

      if (this.genomeChrBinNbits != null) {
        additionalArguments.append("--genomeChrBinNbits");
        additionalArguments.append(' ');
        additionalArguments.append(this.genomeChrBinNbits.toString());
        additionalArguments.append(' ');
        additionalDescription.put("genomeChrBinNbits",
            this.genomeChrBinNbits.toString());
      }

      status.setMessage(this.mapper.getMapperName() + " index creation");

      // Create the index
      GenomeMapperIndexGeneratorStep.execute(this.mapper, context,
          additionalArguments.toString(), additionalDescription);

    } catch (IOException | EoulsanException e) {

      return status.createStepResult(e);
    }

    return status.createStepResult();
  }

  //
  // Other methods
  //

  /**
   * Compute the md5 sum of a file.
   * @param file the file
   * @return a string with the md5sum of a file
   * @throws IOException if an error occurs while computing the md5sum
   */
  private static final String computeMD5SumFile(File file) throws IOException {

    MessageDigest md5Digest;
    try {
      md5Digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      md5Digest = null;
    }
    try (InputStream is = Files.newInputStream(file.toPath())) {
      new DigestInputStream(is, md5Digest);
    }

    return new BigInteger(1, md5Digest.digest()).toString(16);
  }

}
