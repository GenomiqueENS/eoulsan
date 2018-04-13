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
package fr.ens.biologie.genomique.eoulsan.modules.fastqc;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLStreamException;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
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

/**
 * This class define a module that compute QC report using FastQC.
 * @author Sandrine Perrin
 * @since 2.0
 */
@HadoopCompatible
public class FastQCModule extends AbstractModule {

  /** Module name */
  private static final String MODULE_NAME = "fastqc";

  /** Input format key in parameters. */
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

    return "This module launch FastQC on FASTQ or SAM files and generate an html report";
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

    final OutputPortsBuilder builder = new OutputPortsBuilder();
    builder.addPort("htmlreport", DataFormats.FASTQC_REPORT_HTML);
    builder.addPort("zipreport", DataFormats.FASTQC_REPORT_ZIP);

    return builder.create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Define parameters of FastQC
    System.setProperty("java.awt.headless", "true");
    System.setProperty("fastqc.unzip", "true");

    // Parse step parameters to initialize module
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
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Patch FastQC code on sequenceFile to make hadoop compatible
    try {
      FastQCRuntimePatcher.patchFastQC();
    } catch (EoulsanException e1) {
      return status.createTaskResult(e1);
    }

    // Get input data
    final Data inData = context.getInputData(this.inputFormat);

    // Get output data
    final Data htmlOutData =
        context.getOutputData(DataFormats.FASTQC_REPORT_HTML, inData);
    final Data zipOutData =
        context.getOutputData(DataFormats.FASTQC_REPORT_ZIP, inData);

    // Define the list of input files
    final List<DataFile> inputFiles = new ArrayList<>();
    if (inData.getFormat().getMaxFilesCount() > 1) {

      for (int i = 0; i < inData.getDataFileCount(); i++) {
        inputFiles.add(inData.getDataFile(i));
      }
    } else {
      inputFiles.add(inData.getDataFile());
    }

    // Process input files
    try {

      int i = 0;
      for (DataFile inputFile : inputFiles) {

        // Define the report output files
        final DataFile htmlReportFile = htmlOutData.getDataFile(i);
        final DataFile zipReportFile = zipOutData.getDataFile(i++);

        // Launch FastQC analysis
        processFile(inputFile, this.inputFormat == READS_FASTQ, htmlReportFile,
            zipReportFile, context.getLocalTempDirectory(), status);
      }

      return status.createTaskResult();

    } catch (final SequenceFormatException e) {
      return status.createTaskResult(e,
          "Error with sequence file format: " + e.getMessage());

    } catch (final IOException e) {
      return status.createTaskResult(e,
          "Error while parsing file: " + e.getMessage());

    } catch (final XMLStreamException e) {
      return status.createTaskResult(e,
          "Error while writing final report: " + e.getMessage());
    }

  }

  /**
   * Process an input file by FastQC.
   * @param inputFile the input file
   * @param fastqFormat true if the format of the input file is FASTQ
   * @param htmlOutputFile the HTML report output file
   * @param zipOutputFile the ZIP report output file
   * @param tmpDir the temporary directory
   * @param status the task status
   * @throws SequenceFormatException if an error occurs while processing
   *           sequences
   * @throws IOException if an error occurs while processing sequences
   * @throws XMLStreamException if an error occurs while creating report
   */
  private void processFile(DataFile inputFile, final boolean fastqFormat,
      final DataFile htmlOutputFile, final DataFile zipOutputFile,
      final File tmpDir, final TaskStatus status)
      throws SequenceFormatException, IOException, XMLStreamException {

    // Set the description of the context
    status.setDescription("Process sequence of " + inputFile + " for FastQC");

    // Get the SequenceFile object
    final CounterSequenceFile seqFile;
    if (fastqFormat) {

      seqFile = new FastqSequenceFile(inputFile);
    } else {

      seqFile = new SAMSequenceFile(inputFile);
    }

    // Define modules list
    final OverRepresentedSeqs os = new OverRepresentedSeqs();

    final List<AbstractQCModule> modules =
        new ArrayList<>(Arrays.asList(new BasicStats(),
            new PerBaseQualityScores(), new PerTileQualityScores(),
            new PerSequenceQualityScores(), new PerBaseSequenceContent(),
            new PerSequenceGCContent(), new NContent(),
            new SequenceLengthDistribution(), os.duplicationLevelModule(), os,
            new AdapterContent(), new KmerContent()));

    // Process sequences
    processSequences(modules, seqFile);

    // If no entries in the input file use a dedicated module
    final List<AbstractQCModule> reportModules = seqFile.getCount() > 0
        ? modules
        : singletonList((AbstractQCModule) new EmptyFileQC(inputFile));

    // Set the description of the context
    status.setDescription("Create FastQC report on "
        + inputFile + " in " + htmlOutputFile.getName());

    // Create the report
    createReport(reportModules, seqFile, htmlOutputFile, zipOutputFile, tmpDir);

    // Keep module data is now unnecessary
    modules.clear();
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
   * @param htmlReportFile the report file
   * @param zipReportFile the report file
   * @param tempDirectory temporary directory
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws XMLStreamException the XML stream exception
   */
  private void createReport(final List<AbstractQCModule> modules,
      final SequenceFile seqFile, final DataFile htmlReportFile,
      final DataFile zipOutputFile, final File tempDirectory)
      throws IOException, XMLStreamException {

    // Get the report extension
    final String reportExtension =
        DataFormats.FASTQC_REPORT_HTML.getDefaultExtension();

    // Define the temporary output file
    final File reportTempFile =
        File.createTempFile("reportfile-", reportExtension, tempDirectory);

    // Create the output report
    new HTMLReportArchive(seqFile,
        modules.toArray(new QCModule[modules.size()]), reportTempFile);

    // Report zip filename
    final String baseFilename =
        StringUtils.filenameWithoutExtension(reportTempFile.getName());

    // Define the path of the ZIP directory
    final File zipDir = new File(reportTempFile.getParentFile(), baseFilename);

    // Define the path of the ZIP file
    final File zipFile =
        new File(zipDir.getParentFile(), baseFilename + ".zip");

    // Remove original zip file
    if (!zipFile.delete()) {
      getLogger().warning(
          "Unable to remove original FastQC output zip file: " + zipFile);
    }

    // Recreate the zip file
    zipDirectory(zipDir, zipFile,
        StringUtils.filenameWithoutExtension(htmlReportFile.getName()));

    // Remove directory file
    if (!FileUtils.recursiveDelete(zipDir)) {
      getLogger()
          .warning("Unable to remove FastQC output directory: " + zipDir);
    }

    // Copy the temporary file to the real output file
    DataFiles.copy(new DataFile(reportTempFile), htmlReportFile);
    DataFiles.copy(new DataFile(zipFile), zipOutputFile);

    // Remove rezipped zip file
    if (!zipFile.delete()) {
      getLogger().warning(
          "Unable to remove rezipped FastQC output zip file: " + zipFile);
    }

    // Remove the temporary file
    if (!reportTempFile.delete()) {
      getLogger().warning(
          "Unable to remove FastQC temporary output file: " + reportTempFile);
    }

  }

  /**
   * Zip a directory.
   * @param directory the directory to zip
   * @param zipFile the ZIP file
   * @param subdirName the name of the directory to zip in the ZIP file
   * @throws IOException if an error occurs while zipping the file
   */
  private static void zipDirectory(final File directory, final File zipFile,
      final String subdirName) throws IOException {

    final ZipOutputStream out =
        new ZipOutputStream(new FileOutputStream(zipFile));

    FileUtils.zipFolder(directory, subdirName + '/', out, false);
    out.close();
  }

}
