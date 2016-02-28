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
 * of the Institut de Biologie de l'École Normale Supérieure and
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
package fr.ens.biologie.genomique.eoulsan.modules.fastqc;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import uk.ac.babraham.FastQC.Modules.AbstractQCModule;
import uk.ac.babraham.FastQC.Modules.AdapterContent;
import uk.ac.babraham.FastQC.Modules.BasicStats;
import uk.ac.babraham.FastQC.Modules.KmerContent;
import uk.ac.babraham.FastQC.Modules.NContent;
import uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs;
import uk.ac.babraham.FastQC.Modules.PerBaseQualityScores;
import uk.ac.babraham.FastQC.Modules.PerBaseSequenceContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceGCContent;
import uk.ac.babraham.FastQC.Modules.PerSequenceQualityScores;
import uk.ac.babraham.FastQC.Modules.PerTileQualityScores;
import uk.ac.babraham.FastQC.Modules.QCModule;
import uk.ac.babraham.FastQC.Modules.SequenceLengthDistribution;
import uk.ac.babraham.FastQC.Report.HTMLReportArchive;
import uk.ac.babraham.FastQC.Sequence.Sequence;
import uk.ac.babraham.FastQC.Sequence.SequenceFile;
import uk.ac.babraham.FastQC.Sequence.SequenceFormatException;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class define a step that compute QC report using FastQC.
 * @author Sandrine Perrin
 * @since 2.0
 */
@HadoopCompatible
public class FastQCModule extends AbstractModule {

  /** Name step. */
  private static final String MODULE_NAME = "fastqc";

  /** Input format key in step parameters. */
  private static final String INPUT_FORMAT_PARAMETER_NAME = "input.format";

  /** Collector FastQC kmer size */
  public static final String FASTQC_KMER_SIZE_PARAMETER_NAME =
      "fastqc.kmer.size";
  /** Collector FastQC nogroup */
  public static final String FASTQC_NOGROUP_PARAMETER_NAME = "fastqc.nogroup";
  /** Use exponential base groups in graph */
  public static final String FASTQC_EXPGROUP_PARAMETER_NAME = "fastqc.expgroup";
  /** Format fastq type casava/Illumina */
  public static final String FASTQC_CASAVA_PARAMETER_NAME = "fastqc.casava";
  /** Option for filter fastq file if casava=true for all modules */
  public static final String FASTQC_NOFILTER_PARAMETER_NAME = "fastqc.nofilter";

  /** The input format per default */
  private DataFormat inputFormat = DataFormats.READS_FASTQ;

  //
  // Module methods
  //

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "This step launch FastQC on FASTQ or SAM files and generate an html report";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    if (this.inputFormat == DataFormats.READS_FASTQ) {
      builder.addPort(DEFAULT_SINGLE_INPUT_PORT_NAME, DataFormats.READS_FASTQ);
    } else {
      builder.addPort(DEFAULT_SINGLE_INPUT_PORT_NAME,
          DataFormats.MAPPER_RESULTS_SAM);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return singleOutputPort(DataFormats.FASTQC_REPORT_HTML);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    // Parse step parameters to initialize step
    for (final Parameter p : stepParameters) {

      switch (p.getName()) {

      case INPUT_FORMAT_PARAMETER_NAME:

        // Set inputPort fastq/sam from parameters
        DataFormat format = DataFormatRegistry.getInstance()
            .getDataFormatFromNameOrAlias(p.getLowerStringValue());

        if (!(MAPPER_RESULTS_SAM.equals(format)
            || READS_FASTQ.equals(format))) {
          Modules.badParameterValue(context, p,
              "Unknown or format not supported as input format for FastQC");
        }

        this.inputFormat = format;

        break;

      case FASTQC_KMER_SIZE_PARAMETER_NAME:

        // Kmer Size, default FastQC value is 7
        System.setProperty("fastqc.kmer_size",
            "" + p.getIntValueGreaterOrEqualsTo(1));
        break;

      case FASTQC_NOGROUP_PARAMETER_NAME:

        // Set fastQC nogroup, default FastQC value false
        System.setProperty("fastqc.nogroup", "" + p.getBooleanValue());
        break;

      case FASTQC_EXPGROUP_PARAMETER_NAME:

        // Set fastQC expgroup, default FastQC value false
        System.setProperty("fastqc.expgroup", "" + p.getBooleanValue());
        break;

      case FASTQC_CASAVA_PARAMETER_NAME:

        // Set fastQC format fastq, default FastQC value false
        System.setProperty("fastqc.casava", "" + p.getBooleanValue());
        break;

      case FASTQC_NOFILTER_PARAMETER_NAME:

        // Default FastQC value true
        // Set fastQC nofilter default false, if casava=true, filter fastq file
        System.setProperty("fastqc.nofilter", "" + p.getBooleanValue());
        break;

      default:
        Modules.unknownParameter(context, p);
      }
    }

  }

  @Override
  public StepResult execute(final TaskContext context,
      final TaskStatus status) {

    // Patch FastQC code on sequenceFile to make hadoop compatible
    try {
      FastQCRuntimePatcher.patchFastQC();
    } catch (EoulsanException e1) {
      return status.createStepResult(e1);
    }

    // Get input data
    final Data inData = context.getInputData(this.inputFormat);

    // Get output data
    final Data outData =
        context.getOutputData(DataFormats.FASTQC_REPORT_HTML, inData);

    // Extract data file
    final DataFile inFile;
    if (inData.getFormat().getMaxFilesCount() > 1) {
      inFile = inData.getDataFile(0);
    } else {
      inFile = inData.getDataFile();
    }

    final DataFile reportFile = outData.getDataFile();

    try {

      // Get the SequenceFile object
      final SequenceFile seqFile;
      if (this.inputFormat == READS_FASTQ) {

        seqFile = new FastqSequenceFile(inFile);
      } else {

        seqFile = new SAMSequenceFile(inFile);
      }

      // Define modules list
      final OverRepresentedSeqs os = new OverRepresentedSeqs();

      final List<AbstractQCModule> modules =
          Lists.newArrayList(new BasicStats(), new PerBaseQualityScores(),
              new PerTileQualityScores(), new PerSequenceQualityScores(),
              new PerBaseSequenceContent(), new PerSequenceGCContent(),
              new NContent(), new SequenceLengthDistribution(),
              os.duplicationLevelModule(), os, new AdapterContent(),
              new KmerContent());

      // Process sequences
      processSequences(modules, seqFile);

      // Create the report
      createReport(modules, seqFile, reportFile,
          context.getLocalTempDirectory());

      // Set the description of the context
      status.setDescription("Create FastQC report on "
          + inFile + " in " + reportFile.getName() + ")");

      // Keep module data is now unnecessary
      modules.clear();

      return status.createStepResult();

    } catch (final SequenceFormatException e) {
      return status.createStepResult(e,
          "Error with sequence file format: " + e.getMessage());

    } catch (final IOException e) {
      return status.createStepResult(e,
          "Error while parsing file: " + e.getMessage());

    } catch (final XMLStreamException e) {
      return status.createStepResult(e,
          "Error while writing final report: " + e.getMessage());
    }

  }

  /**
   * Process sequences.
   * @param modules the modules
   * @param seqFile the sequence file
   * @throws SequenceFormatException the sequence format exception
   */
  private void processSequences(final List<AbstractQCModule> modules,
      final SequenceFile seqFile) throws SequenceFormatException {

    while (seqFile.hasNext()) {

      final Sequence seq = seqFile.next();

      for (final QCModule module : modules) {

        module.processSequence(seq);
      }
    }

  }

  /**
   * Creates the report.
   * @param modules the modules
   * @param seqFile the sequence file
   * @param reportFile the report file
   * @param tempDirectory temporary directory
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws XMLStreamException the XML stream exception
   */
  private void createReport(final List<AbstractQCModule> modules,
      final SequenceFile seqFile, final DataFile reportFile,
      final File tempDirectory) throws IOException, XMLStreamException {

    // Get the report extension
    final String reportExtension =
        DataFormats.FASTQC_REPORT_HTML.getDefaultExtension();

    // Define the temporary output file
    final File reportTempFile =
        File.createTempFile("reportfile-", reportExtension, tempDirectory);

    // Create the output report
    new HTMLReportArchive(seqFile, modules.toArray(new QCModule[] {}),
        reportTempFile);

    // Report zip filename
    final String baseFilename = reportFile.getName().substring(0,
        reportFile.getName().length() - reportExtension.length());

    // Remove zip file

    final File zipFile =
        new File(reportTempFile.getParent(), baseFilename + ".zip");
    if (!zipFile.delete()) {
      getLogger()
          .warning("Unable to remove FastQC output zip file: " + zipFile);
    }

    // Remove directory file
    final File zipDir = new File(reportTempFile.getParent(), baseFilename);

    if (!FileUtils.recursiveDelete(zipDir)) {
      getLogger()
          .warning("Unable to remove FastQC output directory: " + zipDir);
    }

    // Copy the temporary file to the real output file
    DataFiles.copy(new DataFile(reportTempFile), reportFile);

    // Remove the temporary file
    if (!reportTempFile.delete()) {
      getLogger().warning(
          "Unable to remove FastQC temporary output file: " + reportTempFile);
    }

  }
}
