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

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
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

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {FILTERED_READS_FASTQ,
        getMapper().getArchiveFormat(), GENOME_DESC_TXT};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final SequenceReadsMapper mapper = getMapper();

      // Load genome description object
      final GenomeDescription genomeDescription;
      if (design.getSampleCount() > 0) {
        genomeDescription =
            GenomeDescription.load(context.getInputDataFile(
                DataFormats.GENOME_DESC_TXT, design.getSample(0)).open());
      } else
        genomeDescription = null;

      for (Sample s : design.getSamples()) {

        // Create the reporter
        final Reporter reporter = new Reporter();

        final File archiveIndexFile =
            context.getInputDataFile(getMapper().getArchiveFormat(), s)
                .toFile();

        final File indexDir =
            new File(StringUtils.filenameWithoutExtension(archiveIndexFile
                .getPath()));

        // get input file count for the sample
        final int inFileCount =
            context.getDataFileCount(DataFormats.READS_FASTQ, s);

        if (inFileCount < 1)
          throw new IOException("No reads file found.");

        if (inFileCount > 2)
          throw new IOException(
              "Cannot handle more than 2 reads files at the same time.");

        String logMsg = "";

        // Single end mode
        if (inFileCount == 1) {

          // Get the source
          final File inFile =
              context.getInputDataFile(FILTERED_READS_FASTQ, s, 0).toFile();

          // Single read mapping
          mapSingleEnd(context, s, mapper, inFile, archiveIndexFile, indexDir,
              reporter);

          logMsg =
              "Mapping reads in "
                  + s.getMetadata().getFastqFormat() + " with "
                  + mapper.getMapperName() + " (" + s.getName() + ", "
                  + inFile.getName() + ")";
        }

        // Paired end mode
        if (inFileCount == 2) {

          // Get the source
          final File inFile1 =
              context.getInputDataFile(FILTERED_READS_FASTQ, s, 0).toFile();

          final File inFile2 =
              context.getInputDataFile(FILTERED_READS_FASTQ, s, 1).toFile();

          // Single read mapping
          mapPairedEnd(context, s, mapper, inFile1, inFile2, archiveIndexFile,
              indexDir, reporter);

          logMsg =
              "Mapping reads in "
                  + s.getMetadata().getFastqFormat() + " with "
                  + mapper.getMapperName() + " (" + s.getName() + ", "
                  + inFile1.getName() + "," + inFile2.getName() + ")";
        }

        final File samOutputFile = mapper.getSAMFile(genomeDescription);

        // Parse SAM output file to get information for reporter object
        parseSAMResults(samOutputFile, reporter);

        // Clean mapper temporary files
        mapper.clean();

        // define final output SAM file
        final File outFile =
            context.getOutputDataFile(MAPPER_RESULTS_SAM, s).toFile();

        // Rename the output SAM file to its final name
        Files.move(samOutputFile, outFile);

        // Add counters for this sample to log file

        log.append(reporter.countersValuesToString(COUNTER_GROUP, logMsg));

      }
      return new StepResult(context, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }
  }

  /**
   * map a fastq file in single end mode.
   * @param context Eoulsan context object
   * @param s sample to process
   * @param mapper mapper object
   * @param inFile fastq input file
   * @param archiveIndexFile genome index for the mapper in a zip archive
   * @param indexDir output directory for the uncompressed genome index for the
   *          mapper
   * @param reporter Eoulsan reporter object
   * @throws IOException if an error occurs while the mapping
   */
  private void mapSingleEnd(final Context context, final Sample s,
      final SequenceReadsMapper mapper, final File inFile,
      final File archiveIndexFile, final File indexDir, final Reporter reporter)
      throws IOException {

    // Init mapper
    mapper.init(false, s.getMetadata().getFastqFormat(), archiveIndexFile,
        indexDir, reporter, COUNTER_GROUP);

    // Set mapper arguments
    final int mapperThreads =
        initMapperArguments(mapper, context.getSettings()
            .getTempDirectoryFile());

    LOGGER.info("Map file: "
        + inFile + ", Fastq format: " + s.getMetadata().getFastqFormat()
        + ", use " + mapper.getMapperName() + " with " + mapperThreads
        + " threads option");

    // Process to mapping
    mapper.map(inFile);

  }

  /**
   * map two fastq files in paired end mode.
   * @param context Eoulsan context object
   * @param s sample to process
   * @param mapper mapper object
   * @param inFile1 fastq input file for the first end
   * @param inFile2 fastq input file for the second end
   * @param archiveIndexFile genome index for the mapper in a zip archive
   * @param indexDir output directory for the uncompressed genome index for the
   *          mapper
   * @param reporter Eoulsan reporter object
   * @throws IOException if an error occurs while the mapping
   */
  private void mapPairedEnd(final Context context, final Sample s,
      final SequenceReadsMapper mapper, final File inFile1, final File inFile2,
      final File archiveIndexFile, final File indexDir, final Reporter reporter)
      throws IOException {

    // Init mapper
    mapper.init(true, s.getMetadata().getFastqFormat(), archiveIndexFile,
        indexDir, reporter, COUNTER_GROUP);

    // Set mapper arguments
    final int mapperThreads =
        initMapperArguments(mapper, context.getSettings()
            .getTempDirectoryFile());

    LOGGER.info("Map files: "
        + inFile1 + "," + inFile2 + ", Fastq format: "
        + s.getMetadata().getFastqFormat() + ", use " + mapper.getMapperName()
        + " with " + mapperThreads + " threads option");

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
