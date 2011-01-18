/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.common.io.Files;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.SequenceReadsMapper;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsMapperStep;
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
        getMapper().getArchiveFormat()};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final SequenceReadsMapper mapper = getMapper();

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

        // Process to mapping
        mapper.map(inFile, archiveIndexFile, indexDir);

        final File samOutputFile = mapper.getSAMFile();
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

      return new StepResult(this, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(this, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(this, e, "error while filtering: " + e.getMessage());
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

        reporter.incrCounter(COUNTER_GROUP, "output mapping alignments", 1);
      }

    }

    readerResults.close();

    LOGGER.info(entriesParsed
        + " entries parsed in " + getMapperName() + " output file");

  }

}
