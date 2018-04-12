/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.generators;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GTF;
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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.Generator;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.Mapper;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.STARMapperProvider;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocol;
import fr.ens.biologie.genomique.eoulsan.data.protocols.StorageDataProtocol;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.modules.expression.AbstractExpressionModule;

/**
 * This class define a module that generate a STAR mapper index.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@Generator
public class STARIndexGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "starindexgenerator";

  private final Mapper mapper =
      Mapper.newMapper(STARMapperProvider.MAPPER_NAME);

  private Integer overhang = null;
  private boolean gtfFile;
  private boolean gtfFormat;
  private String chrStartEndFilename;
  private String gtfFeatureExon;
  private String gtfTagExonParentTranscript;
  private Integer genomeSAindexNbases;
  private Integer genomeChrBinNbits;
  private boolean useExpressionStepParameters;

  @Override
  public String getName() {

    return MODULE_NAME;
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
      builder.addPort("annotation",
          this.gtfFormat ? ANNOTATION_GTF : ANNOTATION_GFF);
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
        Modules.renamedParameter(context, p, "use.gtf.file");
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
        Modules.removedParameter(context, p);
        break;

      case "max.local.threads":
        Modules.removedParameter(context, p);
        break;

      case "features.file.format":

        switch (p.getLowerStringValue()) {

        case "gtf":
          this.gtfFormat = true;
          break;

        case "gff":
        case "gff3":
          this.gtfFormat = false;
          break;

        default:
          Modules.badParameterValue(context, p,
              "Unknown annotation file format");
          break;
        }

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
  private void searchExpressionStepParameters(final TaskContext context)
      throws EoulsanException {

    int count = 0;

    for (Step step : context.getWorkflow().getSteps()) {

      if (AbstractExpressionModule.MODULE_NAME.equals(step.getModuleName())) {

        for (Parameter p : step.getParameters()) {

          switch (p.getName()) {

          case AbstractExpressionModule.OLD_GENOMIC_TYPE_PARAMETER_NAME:
          case AbstractExpressionModule.GENOMIC_TYPE_PARAMETER_NAME:
            gtfFeatureExon = p.getStringValue();
            break;

          case AbstractExpressionModule.OLD_ATTRIBUTE_ID_PARAMETER_NAME:
          case AbstractExpressionModule.ATTRIBUTE_ID_PARAMETER_NAME:
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
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

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
        final Data annotationData = context
            .getInputData(this.gtfFormat ? ANNOTATION_GTF : ANNOTATION_GFF);

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

      status.setProgressMessage(this.mapper.getName() + " index creation");

      // Create the index
      GenomeMapperIndexGeneratorModule.execute(this.mapper, context,
          additionalArguments.toString(), additionalDescription,
          context.getCurrentStep().getRequiredProcessors());

      // Remove temporary files
      for (File temporaryFile : temporaryFiles) {

        if (!temporaryFile.delete()) {
          context.getLogger()
              .warning("Cannot remove temporary file: " + temporaryFile);
        }

      }

    } catch (IOException | EoulsanException e) {

      return status.createTaskResult(e);
    }

    return status.createTaskResult();
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
  private File uncompressFileIfNecessary(final TaskContext context,
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
  private File uncompressFile(final TaskContext context, final DataFile file)
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
            MODULE_NAME + "-", realFile.getExtension()).toFile();

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
  private static String computeMD5SumFile(File file) throws IOException {

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
