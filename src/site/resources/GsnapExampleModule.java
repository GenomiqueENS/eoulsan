package com.example;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters;
import fr.ens.biologie.genomique.eoulsan.util.BinariesInstaller;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// The "@LocalOnly" annotation means that the Eoulsan workflow engine will
// only use this module in local mode. The two other annotations are "@HadoopOnly"
// and "@HadoopCompatible" when a module can be executed in local or Hadoop mode.
@LocalOnly
public class GsnapExampleModule extends AbstractModule {

  private static final String COUNTER_GROUP = "reads_mapping";
  private String mapperArguments = "-N 1";

  @Override
  public String getName() {
    // This method return the name of the module
    return "gsnapexample";
  }

  @Override
  public String getDescription() {
    // This method return a description of the module. This method is optional
    return "This step map reads using gsnap";
  }

  @Override
  public Version getVersion() {
    // This method return the version of the module
    return new Version(0, 1, 0);
  }

  @Override
  public ParallelizationMode getParallelizationMode() {
    // The mapper programs can use multithreading, so we don't let here Eoulsan
    // run several mapping at the same time by using OWN_PARALLELIZATION mode
    // instead of STANDARD parallelization mode
    return ParallelizationMode.OWN_PARALLELIZATION;
  }

  @Override
  public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
      throws EoulsanException {

    // This method allow to configure the module
    for (Parameter p : stepParameters) {

      switch (p.getName()) {
        case "mapper.arguments":
          this.mapperArguments = p.getStringValue();
          break;

        default:
          Modules.unknownParameter(context, p);
          break;
      }
    }
  }

  @Override
  public InputPorts getInputPorts() {

    // This method define the 3 input ports of the module
    // This method is called by the workflow after the configure() method. So
    // the number and type of the input port can change against the
    // configuration of the module

    final InputPortsBuilder builder = new InputPortsBuilder();

    builder.addPort("reads", DataFormats.READS_FASTQ);
    builder.addPort("gsnapindex", DataFormats.GSNAP_INDEX_ZIP);
    builder.addPort("genomedesc", DataFormats.GENOME_DESC_TXT);

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    // This method define the output ports of the module
    // This method is called by the workflow after the configure() method. So
    // the number and type of the output port can change against the
    // configuration of the module

    return OutputPortsBuilder.singleOutputPort(DataFormats.MAPPER_RESULTS_SAM);
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    // The context object had many useful method for writing a Module
    // (e.g. access to file to process, the workflow description, the
    // logger...).

    // The status object contains methods to inform the workflow about the
    // progress of the task. The status object is also used to create the
    // TaskResult objects.

    try {

      // Create the reporter. The reporter collect information about the
      // process of the data (e.g. the number of reads, the number of
      // alignments generated...)
      final Reporter reporter = new LocalReporter();

      // Each input port of a module are filled by a Data object when executing
      // a task.

      // To get an input file, you need first the get the data of the requested
      // port. To do this use the TaskContext.getInputData() and the name of the
      // port as argument (you can use the format of the port as argument if no
      // other input port use the same format).
      // Here we get the data related to the archive that contains the GSNAP
      // genome index
      final Data indexData = context.getInputData(DataFormats.GSNAP_INDEX_ZIP);

      // A Data object contains one or more file and metadata (e.g. FASTQ
      // format, sample name...).
      // To get a file we use Data.getDatFile(). This method return a DataFile
      // object.
      // The DataFile object allow to support file on the local filesystem and
      // file on the network (e.g. http, ftp, hdfs...)
      // If you are sure that the DataFile is local file, you can use the
      // toFile() method to get a Java File object..
      final File archiveIndexFile = indexData.getDataFile().toFile();

      // Get input file count for the sample
      // It could have one or two fastq files by sample (single end or
      // paired-end data)
      final Data readData = context.getInputData(DataFormats.READS_FASTQ);
      final int inFileCount = readData.getDataFileCount();

      // Throw error if no reads file found.
      if (inFileCount < 1) throw new IOException("No reads file found.");

      // Throw error if more that 2 reads files found.
      if (inFileCount > 2)
        throw new IOException("Cannot handle more than 2 reads files at the same time.");

      // Get the path to the output SAM file
      final File outSamFile =
          context.getOutputData(DataFormats.MAPPER_RESULTS_SAM, readData).getDataFile().toFile();

      // Single end mode
      if (inFileCount == 1) {

        // Get the source
        // For data format with more that one file (e.g. FASTQ format),
        // You must must add an argument to Data.getDataFile() method with the
        // number of the requested file. With single end fastq the value is
        // always 0.
        // In paired-end mode, the number of the second end is 1.
        final File inFile = readData.getDataFile(0).toFile();

        // Single read mapping
        mapSingleEnd(
            context,
            inFile,
            readData.getMetadata().getFastqFormat(),
            archiveIndexFile,
            outSamFile,
            reporter);
      }

      // Paired end mode
      if (inFileCount == 2) {

        // Get the path of the first end
        // The argument of Data.getDataFile() is 0 like in single end mode.
        final File inFile1 = readData.getDataFile(0).toFile();

        // Get the path of the second end
        // The third argument of Data.getDataFile() is 1.
        final File inFile2 = readData.getDataFile(1).toFile();

        // Single read mapping
        mapPairedEnd(
            context,
            inFile1,
            inFile2,
            readData.getMetadata().getFastqFormat(),
            archiveIndexFile,
            outSamFile,
            reporter);
      }

      // Add counters for this sample to step result file
      status.setCounters(reporter, COUNTER_GROUP);

      // Create a success TaskResult object and return this object to the
      // workflow
      return status.createTaskResult();

    } catch (IOException | InterruptedException e) {

      // If an exception occurs while running Gsnap, return a error TaskResult
      // object with the exception that cause the error
      return status.createTaskResult(e);
    }
  }

  // This method launch the computation in single end mode.
  private void mapSingleEnd(
      final TaskContext context,
      final File inFile,
      final FastqFormat format,
      final File archiveIndexFile,
      final File outSamFile,
      final Reporter reporter)
      throws IOException, InterruptedException {

    // Build the command line
    final List<String> cmdArgs = new ArrayList<>();

    for (String s : this.mapperArguments.split(" ")) {
      if (!s.isEmpty()) {
        cmdArgs.add(s);
      }
    }

    // Path to the FASTQ file
    cmdArgs.add(inFile.getAbsolutePath());

    map(context, cmdArgs, format, archiveIndexFile, outSamFile, reporter);
  }

  // This method launch the computation in paired-end mode
  private void mapPairedEnd(
      final TaskContext context,
      final File inFile1,
      final File inFile2,
      final FastqFormat format,
      final File archiveIndexFile,
      final File outSamFile,
      final Reporter reporter)
      throws IOException, InterruptedException {

    // Build the command line
    final List<String> cmdArgs = new ArrayList<>();

    for (String s : this.mapperArguments.split(" ")) {
      if (!s.isEmpty()) {
        cmdArgs.add(s);
      }
    }

    // Path to the FASTQ files
    cmdArgs.add(inFile1.getAbsolutePath());
    cmdArgs.add(inFile2.getAbsolutePath());

    map(context, cmdArgs, format, archiveIndexFile, outSamFile, reporter);
  }

  // This method execute the mapping
  private void map(
      final TaskContext context,
      final List<String> cmdArgs,
      final FastqFormat format,
      final File archiveIndexFile,
      final File outSamFile,
      final Reporter reporter)
      throws IOException, InterruptedException {

    // Extract and install the gsnap binary for eoulsan jar archive
    final String gsnapPath =
        BinariesInstaller.install(
            "gsnap", "2012-07-20", "gsnap", context.getSettings().getTempDirectory());

    // Get the path to the uncommpressed genome index
    final File archiveIndexDir =
        new File(
            archiveIndexFile.getParent(),
            StringUtils.filenameWithoutExtension(archiveIndexFile.getName()));

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Select the argument for the FASTQ format
    final String formatArg;
    switch (format) {
      case FASTQ_ILLUMINA:
        formatArg = "--quality-protocol=illumina";
        break;
      case FASTQ_ILLUMINA_1_5:
        formatArg = "--quality-protocol=illumina";
        break;
      case FASTQ_SOLEXA:
        throw new IOException("Gsnap not handle the Solexa FASTQ format.");

      case FASTQ_SANGER:
      default:
        formatArg = "--quality-protocol=sanger";
        break;
    }

    // Build the command line
    List<String> cmd =
        new ArrayList<String>(
            Arrays.asList(
                gsnapPath,
                "-A",
                "sam",
                formatArg,
                "-t",
                "" + context.getSettings().getLocalThreadsNumber(),
                "-D",
                archiveIndexDir.getAbsolutePath(),
                "-d",
                "genome"));

    // Add user arguments
    cmd.addAll(cmdArgs);

    // Log the command line to execute
    EoulsanLogger.getLogger().info(cmd.toString());

    // Create process builder
    final ProcessBuilder pb = new ProcessBuilder(cmd);

    // Redirect the output of the process to the SAM file
    pb.redirectOutput(outSamFile.getAbsoluteFile());
    // pb.redirectError(new File("/home/jourdren/toto.err"));

    EoulsanLogger.getLogger().info("pb: " + pb);
    // Execute the command line and save the exit value
    final int exitValue = pb.start().waitFor();

    // if the exit value is not success (0) throw an exception
    if (exitValue != 0) {
      throw new IOException("Bad error result for gsnap execution: " + exitValue);
    }

    // Count the number of alignment generated for the sample
    parseSAMResults(outSamFile, reporter);
  }

  // Uncompress
  private static final void unzipArchiveIndexFile(
      final File archiveIndexFile, final File archiveIndexDir) throws IOException {

    // Test if genome index file exists
    if (!archiveIndexFile.exists())
      throw new IOException("No index for the mapper found: " + archiveIndexFile);

    // Uncompress archive if necessary
    if (!archiveIndexDir.exists()) {

      if (!archiveIndexDir.mkdir())
        throw new IOException("Can't create directory for gsnap index: " + archiveIndexDir);

      EoulsanLogger.getLogger()
          .fine("Unzip archiveIndexFile " + archiveIndexFile + " in " + archiveIndexDir);
      FileUtils.unzip(archiveIndexFile, archiveIndexDir);
    }

    // Test if extracted directory exists
    FileUtils.checkExistingDirectoryFile(archiveIndexDir, "gsnap index directory");
  }

  // Count the number of alignment in a SAM file and save the result in the
  // reporter object
  private static final void parseSAMResults(final File samFile, final Reporter reporter)
      throws IOException {

    String line;

    // Parse SAM result file
    final BufferedReader readerResults = FileUtils.createBufferedReader(samFile);

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("@")) continue;

      final int tabPos = trimmedLine.indexOf('\t');

      if (tabPos != -1) {

        entriesParsed++;

        reporter.incrCounter(
            COUNTER_GROUP, MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName(), 1);
      }
    }

    readerResults.close();

    EoulsanLogger.getLogger().info(entriesParsed + " entries parsed in gsnap output file");
  }
}
