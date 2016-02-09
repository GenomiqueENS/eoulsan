package fr.ens.biologie.genomique.eoulsan.steps.generators;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.Common;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.Generator;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.STARReadsMapper;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.StepContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.StepStatus;
import fr.ens.biologie.genomique.eoulsan.core.workflow.WorkflowStep;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocol;
import fr.ens.biologie.genomique.eoulsan.data.protocols.StorageDataProtocol;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.steps.Steps;
import fr.ens.biologie.genomique.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class define a step that generate a STAR mapper index.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@Generator
public class STARIndexGenerator extends AbstractStep {

  public static final String STEP_NAME = "starindexgenerator";

  private static final int OVERHANG_DEFAULT = 100;

  private final SequenceReadsMapper mapper = new STARReadsMapper();

  private Integer overhang = OVERHANG_DEFAULT;
  private boolean gtfFile = false;
  private String chrStartEndFilename;
  private String gtfFeatureExon;
  private String gtfTagExonParentTranscript;
  private Integer genomeSAindexNbases;
  private Integer genomeChrBinNbits;
  private boolean useExpressionStepParameters;

  private int localThreads;
  private int maxLocalThreads;

  @Override
  public String getName() {

    return STEP_NAME;
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
      builder.addPort("annotation", ANNOTATION_GFF);
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
      throw new EoulsanException(
          "No parameters set in " + getName() + " generator");
    }

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "overhang":
        this.overhang = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case "gtf.file":
        Steps.renamedParameter(context, p, "use.gtf.file");
      case "use.gtf.file":
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
        this.genomeSAindexNbases = p.getIntValueGreaterOrEqualsTo(0);
        break;

      case "genome.chr.bin.nbits":
        this.genomeChrBinNbits = p.getIntValueGreaterOrEqualsTo(0);
        break;

      case "use.expression.step.parameters":
        this.useExpressionStepParameters = p.getBooleanValue();
        break;

      case "local.threads":
        this.localThreads = p.getIntValueGreaterOrEqualsTo(1);
        break;

      case "max.local.threads":
        this.maxLocalThreads = p.getIntValueGreaterOrEqualsTo(1);
        break;

      default:
        throw new EoulsanException(
            "Unknown parameter for " + getName() + " step: " + p.getName());
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

    if (count == 0) {
      throw new EoulsanException("No expression step found in the workflow");
    }

    if (count > 1) {
      throw new EoulsanException(
          "Found more than one expression step in the workflow");
    }
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    try {

      final StringBuilder additionalArguments = new StringBuilder();
      final Map<String, String> additionalDescription = new HashMap<>();
      final List<File> temporaryFiles = new ArrayList<>();

      // Search expression parameter is needed
      if (this.useExpressionStepParameters) {
        searchExpressionStepParameters(context);
      }

      if (this.gtfFile) {

        // Get the annotation data
        final Data annotationData = context.getInputData(ANNOTATION_GFF);

        // Get the annotation DataFile
        final DataFile gffFile = annotationData.getDataFile();
        final File gffFilePath =
            uncompressFileIfNecessary(context, temporaryFiles, gffFile);

        additionalArguments.append("--sjdbGTFfile");
        additionalArguments.append(' ');
        additionalArguments.append(gffFilePath.getAbsolutePath());
        additionalArguments.append(' ');
        additionalDescription.put("sjdbGTFfile",
            computeMD5SumFile(gffFilePath));
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

        DataFile chrStartEndFile = new DataFile(this.chrStartEndFilename);

        if (!chrStartEndFile.exists()) {
          throw new IOException(
              "Unable to read chromosome startend file: " + chrStartEndFile);
        }

        final File chrStartEndFilePath =
            uncompressFileIfNecessary(context, temporaryFiles, chrStartEndFile);

        additionalArguments.append("--sjdbFileChrStartEnd");
        additionalArguments.append(' ');
        additionalArguments.append(chrStartEndFilePath.getAbsolutePath());
        additionalArguments.append(' ');
        additionalDescription.put("sjdbFileChrStartEnd",
            computeMD5SumFile(chrStartEndFilePath));
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

      status
          .setProgressMessage(this.mapper.getMapperName() + " index creation");

      // Create the index
      GenomeMapperIndexGeneratorStep.execute(this.mapper, context,
          additionalArguments.toString(), additionalDescription,
          Common.getThreadsNumber(this.localThreads, this.maxLocalThreads));

      // Remove temporary files
      for (File temporaryFile : temporaryFiles) {

        if (!temporaryFile.delete()) {
          context.getLogger()
              .warning("Cannot remove temporary file: " + temporaryFile);
        }

      }

    } catch (IOException | EoulsanException e) {

      return status.createStepResult(e);
    }

    return status.createStepResult();
  }

  //
  // Other methods
  //

  /**
   * Uncompress a file if compressed.
   * @param context the step context
   * @param temporaryFiles the list of temporary files
   * @param file the file to process
   * @return the absolute path of the file (once uncompressed or not)
   * @throws IOException if an error occurs while uncompressing the file
   */
  private File uncompressFileIfNecessary(final StepContext context,
      List<File> temporaryFiles, final DataFile file) throws IOException {

    checkNotNull(file, "file argument cannot be null");

    final File result;

    if (file.getCompressionType() != CompressionType.NONE
        || !file.isLocalFile()) {

      // Uncompress file
      final File uncompressedFile = uncompressFile(context, file);

      // Add the temporary file to the file of the file to remove
      temporaryFiles.add(uncompressedFile);

      result = uncompressedFile;
    } else {
      result = file.toFile();
    }

    return result;
  }

  /**
   * Uncompress a file to a temporary file.
   * @param context Step context
   * @param file file to uncompress
   * @return the path to the uncompressed file
   * @throws IOException if an error occurs while creating the uncompressed file
   */
  private File uncompressFile(final StepContext context, final DataFile file)
      throws IOException {

    checkNotNull(file, "file argument cannot be null");

    final DataFile realFile;
    final DataProtocol protocol = file.getProtocol();

    // Get the underlying file if the file protocol is a storage protocol
    if (protocol instanceof StorageDataProtocol) {

      realFile = ((StorageDataProtocol) protocol).getUnderLyingData(file);
    } else {
      realFile = file;
    }

    final File outputFile =
        Files.createTempFile(context.getLocalTempDirectory().toPath(),
            STEP_NAME + "-", realFile.getExtension()).toFile();

    context.getLogger()
        .fine("Uncompress/copy " + realFile + " to " + outputFile);

    DataFiles.copy(realFile, new DataFile(outputFile));

    return outputFile;
  }

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
      throw new IOException(e);
    }
    try (InputStream is = new FileInputStream(file)) {
      new DigestInputStream(is, md5Digest);
    }

    return new BigInteger(1, md5Digest.digest()).toString(16);
  }

}
