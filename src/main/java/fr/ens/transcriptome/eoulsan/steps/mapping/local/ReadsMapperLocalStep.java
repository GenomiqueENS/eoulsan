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

import fr.ens.transcriptome.eoulsan.Globals;
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
 * Main class for mapping reads.
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

      final SequenceReadsMapper mapper = getMapper();

      // Load genome description object
      final GenomeDescription genomeDescription;
      if (design.getSampleCount() > 0) {
        genomeDescription =
            GenomeDescription.load(context.getDataFile(
                DataFormats.GENOME_DESC_TXT, design.getSample(0)).open());
      } else
        genomeDescription = null;

      for (Sample s : design.getSamples()) {

        // Create the reporter
        final Reporter reporter = new Reporter();

        final File archiveIndexFile =
            new File(context.getDataFile(getMapper().getArchiveFormat(), s)
                .getSource());

        final File indexDir =
            new File(StringUtils.filenameWithoutExtension(archiveIndexFile
                .getPath()));

        final File inFile =
            new File(context.getDataFilename(FILTERED_READS_FASTQ, s));

        final File outFile =
            new File(context.getDataFilename(MAPPER_RESULTS_SAM, s));

        // Init mapper
        mapper.init(false, reporter, COUNTER_GROUP);

        if (getMapperArguments() != null) {
          mapper.setMapperArguments(getMapperArguments());
        }

        // Get the number of threads to use
        int mapperThreads = getMapperThreads();

        if (mapperThreads > Runtime.getRuntime().availableProcessors()
            || mapperThreads < 1) {
          mapperThreads = Runtime.getRuntime().availableProcessors();
        }

        // Set the number of threads
        mapper.setThreadsNumber(mapperThreads);
        LOGGER.info("Use "
            + mapper.getMapperName() + " with " + mapperThreads
            + " threads option");

        // Set mapper temporary directory
        mapper.setTempDirectory(context.getSettings().getTempDirectoryFile());

        // Process to mapping
        mapper.map(inFile, archiveIndexFile, indexDir);

        final File samOutputFile = mapper.getSAMFile(genomeDescription);
        parseSAMResults(samOutputFile, reporter);

        // Clean mapper temporary files
        mapper.clean();

        Files.move(samOutputFile, outFile);

        // Add counters for this sample to log file
        log.append(reporter.countersValuesToString(COUNTER_GROUP,
            "Mapping reads with "
                + mapper.getMapperName() + " (" + s.getName() + ", "
                + inFile.getName() + ")"));
      }

      return new StepResult(context, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }
  }

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
