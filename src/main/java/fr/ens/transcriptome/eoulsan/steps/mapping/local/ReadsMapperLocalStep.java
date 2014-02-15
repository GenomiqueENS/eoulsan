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

import static fr.ens.transcriptome.eoulsan.core.SampleStepException.reThrow;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.ProcessSampleExecutor;
import fr.ens.transcriptome.eoulsan.core.SampleStep;
import fr.ens.transcriptome.eoulsan.core.SampleStepContext;
import fr.ens.transcriptome.eoulsan.core.SampleStepException;
import fr.ens.transcriptome.eoulsan.core.SampleStepStatus;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
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
public class ReadsMapperLocalStep extends AbstractReadsMapperStep implements
    SampleStep {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private boolean firstSample = true;
  private GenomeDescription genomeDescription;

  @Override
  public InputPorts getInputFormats() {

    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("reads", READS_FASTQ);
    builder.addPort("mapper_index", getMapper().getArchiveFormat());
    builder.addPort("genome_description", GENOME_DESC_TXT);

    return builder.create();
  }

  @Override
  public StepResult execute(final Design design, final StepContext context,
      final StepStatus status) {

    return ProcessSampleExecutor.processAllSamples(context, design, status, 1,
        this);
  }

  @Override
  public void processSample(final SampleStepContext context,
      final SampleStepStatus status) throws SampleStepException {

    try {

      // Load genome description object
      if (this.firstSample) {
        if (this.genomeDescription == null) {
          genomeDescription =
              GenomeDescription.load(context.getInputDataFile(
                  DataFormats.GENOME_DESC_TXT).open());
        } else
          genomeDescription = null;
        this.firstSample = false;
      }

      // Get the mapper
      final SequenceReadsMapper mapper = getMapper();

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      final File archiveIndexFile =
          context.getInputDataFile(getMapper().getArchiveFormat()).toFile();

      final File indexDir =
          new File(StringUtils.filenameWithoutExtension(archiveIndexFile
              .getPath()));

      // get input file count for the sample
      final int inFileCount =
          context.getInputDataFileCount(DataFormats.READS_FASTQ);

      if (inFileCount < 1)
        throw new IOException("No reads file found.");

      if (inFileCount > 2)
        throw new IOException(
            "Cannot handle more than 2 reads files at the same time.");

      String logMsg = "";

      // Single end mode
      if (inFileCount == 1) {

        // Get the source
        final File inFile = context.getInputDataFile(READS_FASTQ, 0).toFile();

        // Single read mapping
        mapSingleEnd(context, mapper, inFile, archiveIndexFile, indexDir,
            reporter);

        logMsg =
            "Mapping reads in "
                + context.getSample().getMetadata().getFastqFormat() + " with "
                + mapper.getMapperName() + " (" + context.getSample().getName()
                + ", " + inFile.getName() + ")";
      }

      // Paired end mode
      if (inFileCount == 2) {

        // Get the source
        final File inFile1 = context.getInputDataFile(READS_FASTQ, 0).toFile();

        final File inFile2 = context.getInputDataFile(READS_FASTQ, 1).toFile();

        // Single read mapping
        mapPairedEnd(context, mapper, inFile1, inFile2, archiveIndexFile,
            indexDir, reporter);

        logMsg =
            "Mapping reads in "
                + context.getSample().getMetadata().getFastqFormat() + " with "
                + mapper.getMapperName() + " (" + context.getSample().getName()
                + ", " + inFile1.getName() + "," + inFile2.getName() + ")";
      }

      final File samOutputFile = mapper.getSAMFile(genomeDescription);

      // Parse SAM output file to get information for reporter object
      parseSAMResults(samOutputFile, reporter);

      // Clean mapper temporary files
      mapper.clean();

      // define final output SAM file
      final File outFile =
          context.getOutputDataFile(MAPPER_RESULTS_SAM).toFile();

      // Rename the output SAM file to its final name
      Files.move(samOutputFile, outFile);

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP, logMsg);

    } catch (FileNotFoundException e) {

      reThrow(e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      reThrow(e, "Error while filtering: " + e.getMessage());
    }
  }

  /**
   * map a fastq file in single end mode.
   * @param context Eoulsan context object
   * @param mapper mapper object
   * @param inFile fastq input file
   * @param archiveIndexFile genome index for the mapper in a zip archive
   * @param indexDir output directory for the uncompressed genome index for the
   *          mapper
   * @param reporter Eoulsan reporter object
   * @throws IOException if an error occurs while the mapping
   */
  private void mapSingleEnd(final SampleStepContext context,
      final SequenceReadsMapper mapper, final File inFile,
      final File archiveIndexFile, final File indexDir, final Reporter reporter)
      throws IOException {

    // Get FASTQ format
    final FastqFormat format =
        context.getSample().getMetadata().getFastqFormat();

    // Init mapper
    mapper.init(false, format, archiveIndexFile, indexDir, reporter,
        COUNTER_GROUP);

    // Set mapper arguments
    final int mapperThreads =
        initMapperArguments(mapper, context.getSettings()
            .getTempDirectoryFile());

    LOGGER
        .info("Map file: "
            + inFile + ", Fastq format: " + format + ", use "
            + mapper.getMapperName() + " with " + mapperThreads
            + " threads option");

    // Process to mapping
    mapper.map(inFile);

  }

  /**
   * map two fastq files in paired end mode.
   * @param context Eoulsan context object
   * @param mapper mapper object
   * @param inFile1 fastq input file for the first end
   * @param inFile2 fastq input file for the second end
   * @param archiveIndexFile genome index for the mapper in a zip archive
   * @param indexDir output directory for the uncompressed genome index for the
   *          mapper
   * @param reporter Eoulsan reporter object
   * @throws IOException if an error occurs while the mapping
   */
  private void mapPairedEnd(final SampleStepContext context,
      final SequenceReadsMapper mapper, final File inFile1, final File inFile2,
      final File archiveIndexFile, final File indexDir, final Reporter reporter)
      throws IOException {

    // Get FASTQ format
    final FastqFormat format =
        context.getSample().getMetadata().getFastqFormat();

    // Init mapper
    mapper.init(true, format, archiveIndexFile, indexDir, reporter,
        COUNTER_GROUP);

    // Set mapper arguments
    final int mapperThreads =
        initMapperArguments(mapper, context.getSettings()
            .getTempDirectoryFile());

    LOGGER
        .info("Map files: "
            + inFile1 + "," + inFile2 + ", Fastq format: " + format + ", use "
            + mapper.getMapperName() + " with " + mapperThreads
            + " threads option");

    // Process to mapping
    mapper.map(inFile1, inFile2);
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

    LOGGER.info(entriesParsed
        + " entries parsed in " + getMapperName() + " output file");
  }

}
