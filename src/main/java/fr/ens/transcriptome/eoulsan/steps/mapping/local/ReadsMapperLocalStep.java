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
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.design.SampleMetadata.FASTQ_FORMAT_FIELD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a step for reads mapping.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@LocalOnly
public class ReadsMapperLocalStep extends AbstractReadsMapperStep {

  private boolean firstSample = true;
  private GenomeDescription genomeDescription;

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort(READS_PORT_NAME, READS_FASTQ);
    builder.addPort(MAPPER_INDEX_PORT_NAME, getMapper().getArchiveFormat());
    builder.addPort(GENOME_DESCRIPTION_PORT_NAME, GENOME_DESC_TXT);

    return builder.create();
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {

      // Load genome description object
      if (this.firstSample) {
        if (this.genomeDescription == null) {
          genomeDescription =
              GenomeDescription.load(context
                  .getInputData(DataFormats.GENOME_DESC_TXT).getDataFile()
                  .open());
        } else
          genomeDescription = null;
        this.firstSample = false;
      }

      // Get the mapper
      final SequenceReadsMapper mapper = getMapper();

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      final File archiveIndexFile =
          context.getInputData(getMapper().getArchiveFormat()).getDataFile()
              .toFile();

      final File indexDir =
          new File(StringUtils.filenameWithoutExtension(archiveIndexFile
              .getPath()));

      // get input file count for the sample
      final Data inData = context.getInputData(DataFormats.READS_FASTQ);

      // Get FASTQ format
      // TODO create a DataMetaData class that contains standard methods like
      // SampleMeData
      FastqFormat fastqFormat =
          FastqFormat.getFormatFromName(inData.getMetadata().get(
              FASTQ_FORMAT_FIELD));
      if (fastqFormat == null) {
        fastqFormat = FastqFormat.FASTQ_ILLUMINA;
      }

      if (inData.getDataFileCount() < 1)
        throw new IOException("No reads file found.");

      if (inData.getDataFileCount() > 2)
        throw new IOException(
            "Cannot handle more than 2 reads files at the same time.");

      String logMsg = "";

      // Single end mode
      if (inData.getDataFileCount() == 1) {

        // Get the source
        final File inFile =
            context.getInputData(READS_FASTQ).getDataFile(0).toFile();

        // Single read mapping
        mapSingleEnd(context, mapper, inData, fastqFormat, archiveIndexFile,
            indexDir, reporter);

        logMsg =
            "Mapping reads in "
                + fastqFormat + " with " + mapper.getMapperName() + " ("
                + inData.getName() + ", " + inFile.getName() + ")";
      }

      // Paired end mode
      if (inData.getDataFileCount() == 2) {

        // Get the source
        final File inFile1 =
            context.getInputData(READS_FASTQ).getDataFile(0).toFile();

        final File inFile2 =
            context.getInputData(READS_FASTQ).getDataFile(1).toFile();

        // Single read mapping
        mapPairedEnd(context, mapper, inData, fastqFormat, archiveIndexFile,
            indexDir, reporter);

        logMsg =
            "Mapping reads in "
                + fastqFormat + " with " + mapper.getMapperName() + " ("
                + inData.getName() + ", " + inFile1.getName() + ","
                + inFile2.getName() + ")";
      }

      final File samOutputFile = mapper.getSAMFile(genomeDescription);

      // Parse SAM output file to get information for reporter object
      parseSAMResults(samOutputFile, reporter);

      // Clean mapper temporary files
      mapper.clean();

      // define final output SAM file
      final File outFile =
          context.getOutputData(MAPPER_RESULTS_SAM, inData).getDataFile()
              .toFile();

      // Rename the output SAM file to its final name
      Files.move(samOutputFile, outFile);

      // Set the description of the context
      status.setDescription(logMsg);

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

    } catch (FileNotFoundException e) {

      return status.createStepResult(e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return status.createStepResult(e,
          "Error while filtering: " + e.getMessage());
    }

    return status.createStepResult();
  }

  /**
   * map a fastq file in single end mode.
   * @param context Eoulsan context object
   * @param mapper mapper object
   * @param inData fastq input data
   * @param fastqFormat FASTQ format
   * @param archiveIndexFile genome index for the mapper in a zip archive
   * @param indexDir output directory for the uncompressed genome index for the
   *          mapper
   * @param reporter Eoulsan reporter object
   * @throws IOException if an error occurs while the mapping
   */
  private void mapSingleEnd(final StepContext context,
      final SequenceReadsMapper mapper, final Data inData,
      final FastqFormat fastqFormat, final File archiveIndexFile,
      final File indexDir, final Reporter reporter) throws IOException {

    // Init mapper
    mapper.init(false, fastqFormat, archiveIndexFile, indexDir, reporter,
        COUNTER_GROUP);

    // Set mapper arguments
    final int mapperThreads =
        initMapperArguments(mapper, context.getSettings()
            .getTempDirectoryFile());

    getLogger().info(
        "Map file: "
            + inData.getDataFile() + ", Fastq format: " + fastqFormat
            + ", use " + mapper.getMapperName() + " with " + mapperThreads
            + " threads option");

    // Process to mapping
    mapper.map(inData.getDataFile().toFile());

  }

  /**
   * map two fastq files in paired end mode.
   * @param context Eoulsan context object
   * @param mapper mapper object
   * @param inData fastq input data
   * @param fastqFormat FASTQ format
   * @param archiveIndexFile genome index for the mapper in a zip archive
   * @param indexDir output directory for the uncompressed genome index for the
   *          mapper
   * @param reporter Eoulsan reporter object
   * @throws IOException if an error occurs while the mapping
   */
  private void mapPairedEnd(final StepContext context,
      final SequenceReadsMapper mapper, final Data inData,
      final FastqFormat fastqFormat, final File archiveIndexFile,
      final File indexDir, final Reporter reporter) throws IOException {

    // Init mapper
    mapper.init(true, fastqFormat, archiveIndexFile, indexDir, reporter,
        COUNTER_GROUP);

    // Set mapper arguments
    final int mapperThreads =
        initMapperArguments(mapper, context.getSettings()
            .getTempDirectoryFile());

    getLogger().info(
        "Map files: "
            + inData.getDataFile(0) + "," + inData.getDataFile(1)
            + ", Fastq format: " + fastqFormat + ", use "
            + mapper.getMapperName() + " with " + mapperThreads
            + " threads option");

    // Process to mapping
    mapper.map(inData.getDataFile(0).toFile(), inData.getDataFile(1).toFile());
  }

  /**
   * Initialize the mapper to use.
   * @param mapper mapper object
   * @param tempDirectory temporary directory
   * @return the number of threads to use
   */
  private int initMapperArguments(final SequenceReadsMapper mapper,
      final File tempDirectory) {

    if (getMapperArguments() != null) {
      mapper.setMapperArguments(getMapperArguments());
    }

    // Get the number of threads to use
    int mapperThreads = getMapperLocalThreads();

    if (mapperThreads > Runtime.getRuntime().availableProcessors()
        || mapperThreads < 1) {
      mapperThreads = Runtime.getRuntime().availableProcessors();
    }

    // Set the number of threads
    mapper.setThreadsNumber(mapperThreads);

    // Set mapper temporary directory
    mapper.setTempDirectory(tempDirectory);

    return mapperThreads;
  }

  /**
   * Parse the output the mapper (in SAM format).
   * @param samFile output file from the mapper (in SAM format)
   * @param reporter Eoulsan reporter for the step
   * @throws IOException if an error occurs while reading the sAM file
   */
  private void parseSAMResults(final File samFile, final Reporter reporter)
      throws IOException {

    String line;

    // Parse SAM result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(samFile);

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("@"))
        continue;

      final int tabPos = trimmedLine.indexOf('\t');

      if (tabPos != -1) {

        entriesParsed++;

        reporter.incrCounter(COUNTER_GROUP,
            MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName(), 1);
      }

    }

    readerResults.close();

    getLogger().info(
        entriesParsed
            + " entries parsed in " + getMapperName() + " output file");
  }

}
