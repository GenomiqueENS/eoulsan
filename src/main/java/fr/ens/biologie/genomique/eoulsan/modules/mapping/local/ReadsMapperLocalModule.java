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

package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.DEFAULT_SINGLE_OUTPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode.OWN_PARALLELIZATION;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_LOG;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.FileMapping;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.Mapper;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperIndex;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperInstance;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperProcess;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.STARMapperProvider;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsMapperModule;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.UnSynchronizedBufferedWriter;

/**
 * This class define a module for reads mapping.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@LocalOnly
public class ReadsMapperLocalModule extends AbstractReadsMapperModule {

  @Override
  public ParallelizationMode getParallelizationMode() {

    return OWN_PARALLELIZATION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort(READS_PORT_NAME, READS_FASTQ);
    builder.addPort(MAPPER_INDEX_PORT_NAME, getMapper().getArchiveFormat());

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    OutputPortsBuilder builder = new OutputPortsBuilder();

    builder.addPort(DEFAULT_SINGLE_OUTPUT_PORT_NAME, MAPPER_RESULTS_SAM);
    builder.addPort("log", MAPPER_RESULTS_LOG);

    return builder.create();
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      final DataFile archiveIndexFile =
          context.getInputData(getMapper().getArchiveFormat()).getDataFile();

      final File indexDir = new File(StringUtils
          .filenameWithoutExtension(archiveIndexFile.toUri().getPath()));

      // Get input data
      final Data inData = context.getInputData(READS_FASTQ);

      // Get output data
      final Data outData = context.getOutputData(MAPPER_RESULTS_SAM, inData);

      // Define final output SAM file
      final File samFile = outData.getDataFile().toFile();

      // Get error log data
      final Data logData = context.getOutputData(MAPPER_RESULTS_LOG, inData);

      // Define mapper error file
      final File errorFile = logData.getDataFile().toFile();

      // If the mapper is STAR, the log extension must be empty
      final String logExtension =
          STARMapperProvider.MAPPER_NAME.equals(getMapperName()) ? "." : ".log";

      // Define mapper log file
      final File logFile = new File(samFile.getParentFile(),
          StringUtils.filenameWithoutExtension(errorFile.getName())
              + logExtension);

      // Get FASTQ format
      final FastqFormat fastqFormat = inData.getMetadata().getFastqFormat();

      // Initialize the mapper
      final FileMapping mapper = initMapper(context, fastqFormat,
          archiveIndexFile, indexDir, reporter);

      if (inData.getDataFileCount() < 1) {
        throw new IOException("No reads file found.");
      }

      if (inData.getDataFileCount() > 2) {
        throw new IOException(
            "Cannot handle more than 2 reads files at the same time.");
      }

      String logMsg = "";

      // Single end mode
      if (inData.getDataFileCount() == 1) {

        // Get the source
        final DataFile inFile =
            context.getInputData(READS_FASTQ).getDataFile(0);

        getLogger().info("Map file: "
            + inFile + ", Fastq format: " + fastqFormat + ", use "
            + mapper.getName() + " with " + mapper.getThreadNumber()
            + " threads option");

        // Single read mapping
        final MapperProcess process = mapper.mapSE(inFile, errorFile, logFile);

        // Set executed command line in status
        status.setCommandLine(process.getCommandLine());

        // Parse output of the mapper
        parseSAMResults(process.getStout(), samFile, reporter);

        // Wait the end of the process and do cleanup
        process.waitFor();

        logMsg = "Mapping reads in "
            + fastqFormat + " with " + mapper.getName() + " ("
            + inData.getName() + ", " + inFile.getName() + ")";

      }

      // Paired end mode
      if (inData.getDataFileCount() == 2) {

        // Get the source
        final DataFile inFile1 =
            context.getInputData(READS_FASTQ).getDataFile(0);

        final DataFile inFile2 =
            context.getInputData(READS_FASTQ).getDataFile(1);

        getLogger().info("Map files: "
            + inFile1 + "," + inFile2 + ", Fastq format: " + fastqFormat
            + ", use " + mapper.getName() + " with " + mapper.getThreadNumber()
            + " threads option");

        // Single read mapping
        final MapperProcess process =
            mapper.mapPE(inFile1, inFile2, errorFile, logFile);

        // Parse output of the mapper
        parseSAMResults(process.getStout(), samFile, reporter);

        // Wait the end of the process and do cleanup
        process.waitFor();

        logMsg = "Mapping reads in "
            + fastqFormat + " with " + mapper.getName() + " ("
            + inData.getName() + ", " + inFile1.getName() + ","
            + inFile2.getName() + ")";
      }

      // Throw an exception if an exception has occurred while mapping
      mapper.throwMappingException();

      // Set the description of the context
      status.setDescription(logMsg);

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

    } catch (FileNotFoundException e) {

      return status.createTaskResult(e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return status.createTaskResult(e,
          "Error while mapping reads: " + e.getMessage());
    }

    return status.createTaskResult();
  }

  /**
   * Initialize the mapper to use.
   * @param context Eoulsan context
   * @param format FASTQ format
   * @param archiveIndexFile genome index for the mapper as a ZIP file
   * @param indexDir uncompressed directory for the genome index
   * @param reporter reporter
   * @throws IOException if an error while initializing the mapper
   */
  private FileMapping initMapper(final TaskContext context,
      final FastqFormat format, final DataFile archiveIndexFile,
      final File indexDir, final Reporter reporter) throws IOException {

    // Get the mapper object
    final Mapper mapper = getMapper();

    // Set mapper temporary directory
    mapper.setTempDirectory(context.getLocalTempDirectory());

    // Set mapper executable temporary directory
    mapper.setExecutablesTempDirectory(
        context.getSettings().getExecutablesTempDirectoryFile());

    // Create the mapper instance
    final MapperInstance mapperInstance =
        mapper.newMapperInstance(getMapperVersion(), getMapperFlavor(),
            isUseBundledBinaries(), getMapperDockerImage());

    // Create the MapperIndex object
    final MapperIndex mapperIndex =
        mapperInstance.newMapperIndex(archiveIndexFile.open(), indexDir);

    // Get the number of threads to use
    int mapperThreads = getMapperLocalThreads();
    if (mapperThreads > Runtime.getRuntime().availableProcessors()
        || mapperThreads < 1) {
      mapperThreads = Runtime.getRuntime().availableProcessors();
    }

    final FileMapping mapping = mapperIndex.newFileMapping(format,
        getMapperArguments(), mapperThreads, false, reporter, COUNTER_GROUP);

    // Delete the index directory at the end of the workflow
    context.getWorkflow()
        .deleteOnExit(new DataFile(mapperIndex.getIndexDirectory()));

    return mapping;
  }

  /**
   * Parse the output the mapper (in SAM format).
   * @param samFileInputStream SAM input stream
   * @param samFile output file to be written
   * @param reporter Eoulsan reporter for the step
   * @throws IOException if an error occurs while reading the sAM file
   */
  private void parseSAMResults(final InputStream samFileInputStream,
      final File samFile, final Reporter reporter) throws IOException {

    String line;

    // Parse SAM result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(samFileInputStream);
    final Writer writer =
        new UnSynchronizedBufferedWriter(new OutputStreamWriter(
            new FileOutputStream(samFile), StandardCharsets.ISO_8859_1));

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      writer.write(line);
      writer.write('\n');

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("@")) {
        continue;
      }

      final int tabPos = trimmedLine.indexOf('\t');

      if (tabPos != -1) {

        entriesParsed++;
        reporter.incrCounter(COUNTER_GROUP,
            MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName(), 1);
      }
    }

    readerResults.close();
    writer.close();

    getLogger().info(entriesParsed
        + " entries parsed in " + getMapperName() + " output file");
  }

}
