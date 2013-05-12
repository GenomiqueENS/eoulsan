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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Logger;

import com.google.common.base.Charsets;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
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
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

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

        // define final output SAM file
        final File samFile =
            context.getOutputDataFile(MAPPER_RESULTS_SAM, s).toFile();

        if (inFileCount < 1)
          throw new IOException("No reads file found.");

        if (inFileCount > 2)
          throw new IOException(
              "Cannot handle more than 2 reads files at the same time.");

        String logMsg = "";

        // Initialize the mapper
        final SequenceReadsMapper mapper =
            initMapper(context, s.getMetadata().getFastqFormat(),
                archiveIndexFile, indexDir, reporter);

        // Single end mode
        if (inFileCount == 1) {

          // Get the source
          final File inFile =
              context.getInputDataFile(FILTERED_READS_FASTQ, s, 0).toFile();

          LOGGER.info("Map file: "
              + inFile + ", Fastq format: " + s.getMetadata().getFastqFormat()
              + ", use " + mapper.getMapperName() + " with "
              + mapper.getThreadsNumber() + " threads option");

          // Single read mapping
          parseSAMResults(mapper.mapSE(inFile, genomeDescription), samFile,
              reporter);

          logMsg =
              "Mapping reads in "
                  + s.getMetadata().getFastqFormat() + " with "
                  + mapper.getMapperName() + " (" + s.getName() + ", "
                  + inFile.getName() + ")";
        }

        // Paired end mode
        else if (inFileCount == 2) {

          // Get the source
          final File inFile1 =
              context.getInputDataFile(FILTERED_READS_FASTQ, s, 0).toFile();

          final File inFile2 =
              context.getInputDataFile(FILTERED_READS_FASTQ, s, 1).toFile();

          LOGGER.info("Map files: "
              + inFile1 + "," + inFile2 + ", Fastq format: "
              + s.getMetadata().getFastqFormat() + ", use "
              + mapper.getMapperName() + " with " + mapper.getThreadsNumber()
              + " threads option");

          // Single read mapping
          parseSAMResults(mapper.mapPE(inFile1, inFile2, genomeDescription),
              samFile, reporter);

          logMsg =
              "Mapping reads in "
                  + s.getMetadata().getFastqFormat() + " with "
                  + mapper.getMapperName() + " (" + s.getName() + ", "
                  + inFile1.getName() + "," + inFile2.getName() + ")";
        } else
          throw new IllegalStateException();

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
   * Initialize the mapper to use.
   * @param context Eoulsan context
   * @param format FASTQ format
   * @param archiveIndexFile genome index for the mapper as a ZIP file
   * @param archiveIndexDir uncompressed directory for the genome index
   * @param reporter reporter
   * @throws IOException
   */
  private SequenceReadsMapper initMapper(final Context context,
      final FastqFormat format, final File archiveIndexFile,
      final File indexDir, Reporter reporter) throws IOException {

    final SequenceReadsMapper mapper = getMapper();

    // Init mapper
    mapper.init(archiveIndexFile, indexDir, reporter, COUNTER_GROUP);

    // Set FASTQ format
    mapper.setFastqFormat(format);

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
    mapper.setTempDirectory(context.getSettings().getTempDirectoryFile());

    return mapper;
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
        new OutputStreamWriter(new FileOutputStream(samFile),
            Charsets.ISO_8859_1);

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      writer.write(line);
      writer.write('\n');

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
    writer.close();

    LOGGER.info(entriesParsed
        + " entries parsed in " + getMapperName() + " output file");
  }

}
