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

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.GOOD_QUALITY_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;

import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAMFilterStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

@LocalOnly
public class SAMFilterLocalStep extends AbstractSAMFilterStep {

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      // Create parser object
      final SAMParser parser = new SAMParser();

      boolean first = true;

      for (Sample sample : design.getSamples()) {

        if (first) {

          // Load genome description object
          final GenomeDescription genomeDescription =
              GenomeDescription.load(context.getDataFile(
                  DataFormats.GENOME_DESC_TXT, sample).open());

          // Set the chromosomes sizes in the parser
          parser.setGenomeDescription(genomeDescription);

          first = false;
        }

        log.append(filterSAMFile(context, sample, parser));

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
   * Parse a sam file.
   * @param context context object
   * @param sample sample to use
   * @param parser parse with genome description
   * @return a String with log information about the filtering of alignments of
   *         the sample
   * @throws IOException if an error occurs while reading SAM input file or
   *           writing filtered SAM file
   */
  private String filterSAMFile(final Context context, final Sample sample,
      final SAMParser parser) throws IOException {

    // Get threshold
    final int mappingQualityThreshold = getMappingQualityThreshold();

    // Create the reporter
    final Reporter reporter = new Reporter();

    // Get reader
    final BufferedReader br =
        FileUtils.createBufferedReader(context.getDataFile(
            DataFormats.MAPPER_RESULTS_SAM, sample).open());

    // Get Writer
    final Writer writer =
        FileUtils.createFastBufferedWriter(context.getDataFile(
            DataFormats.FILTERED_MAPPER_RESULTS_SAM, sample).create());

    String line = null;

    String lastId = null;
    String lastLine = null;
    int lastIdCount = 0;

    while ((line = br.readLine()) != null) {

      try {
        final SAMRecord samRecord = parser.parseLine(line);

        reporter.incrCounter(COUNTER_GROUP,
            INPUT_ALIGNMENTS_COUNTER.counterName(), 1);

        if (samRecord.getReadUnmappedFlag()) {
          reporter.incrCounter(COUNTER_GROUP, "unmapped reads", 1);
        } else {

          if (samRecord.getMappingQuality() >= mappingQualityThreshold) {

            reporter.incrCounter(COUNTER_GROUP,
                GOOD_QUALITY_ALIGNMENTS_COUNTER.counterName(), 1);

            final String id = samRecord.getReadName();

            if (id.equals(lastId)) {
              lastIdCount++;
            } else {

              if (lastIdCount == 1) {

                writer.write(lastLine + "\n");
                reporter.incrCounter(COUNTER_GROUP,
                    OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

              } else if (lastIdCount > 1) {

                reporter.incrCounter(COUNTER_GROUP,
                    ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
                reporter.incrCounter(COUNTER_GROUP,
                    ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), 1);
              }

              lastIdCount = 1;
              lastId = id;
            }

          } else {

            reporter.incrCounter(COUNTER_GROUP,
                ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
          }

          lastLine = line;
        }

      } catch (SAMFormatException e) {

        reporter.incrCounter(COUNTER_GROUP,
            ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), 1);
      }

    }

    if (lastIdCount == 1) {

      writer.write(lastLine + "\n");
      reporter.incrCounter(COUNTER_GROUP,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

    } else if (lastIdCount > 1) {

      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), 1);
    }

    // Close files
    br.close();
    writer.close();

    return reporter.countersValuesToString(
        COUNTER_GROUP,
        "Filter SAM files ("
            + sample.getName()
            + ", "
            + context.getDataFile(DataFormats.MAPPER_RESULTS_SAM, sample)
                .getName() + ")");
  }

}
