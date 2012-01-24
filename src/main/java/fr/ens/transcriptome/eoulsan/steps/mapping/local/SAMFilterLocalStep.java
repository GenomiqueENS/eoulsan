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

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.GOOD_QUALITY_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.UNMAP_READS_COUNTER;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSample;
import fr.ens.transcriptome.eoulsan.steps.ProcessSampleExecutor;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAMFilterStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

@LocalOnly
public class SAMFilterLocalStep extends AbstractSAMFilterStep {

  @Override
  public StepResult execute(final Design design, final Context context) {

    final GenomeDescription genomeDescription;

    // Load genome description object
    try {

      if (design.getSampleCount() > 0)
        genomeDescription =
            GenomeDescription.load(context.getInputDataFile(
                DataFormats.GENOME_DESC_TXT, design.getSample(0)).open());
      else
        genomeDescription = null;

    } catch (FileNotFoundException e) {

      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(context, e, "error while filtering: "
          + e.getMessage());
    }

    // Get threshold
    final int mappingQualityThreshold = getMappingQualityThreshold();

    // Process all samples
    return ProcessSampleExecutor.processAllSamples(context, design,
        new ProcessSample() {

          @Override
          public String processSample(Context context, Sample sample)
              throws ProcessSampleException {

            try {

              // Create parser object
              final SAMParser parser = new SAMParser();
              parser.setGenomeDescription(genomeDescription);

              // Filter alignments
              return filterSAMFile(context, sample, parser,
                  mappingQualityThreshold);
            } catch (FileNotFoundException e) {

              throwException(e, "File not found: " + e.getMessage());
            } catch (IOException e) {

              throwException(e, "error while filtering: " + e.getMessage());
            }
            return null;
          }

        });
  }

  /**
   * Parse a sam file.
   * @param context context object
   * @param sample sample to use
   * @param parser parse with genome description
   * @param mappingQualityThreshold mapping quality threshold
   * @return a String with log information about the filtering of alignments of
   *         the sample
   * @throws IOException if an error occurs while reading SAM input file or
   *           writing filtered SAM file
   */
  private String filterSAMFile(final Context context, final Sample sample,
      final SAMParser parser, final int mappingQualityThreshold)
      throws IOException {

    // Create the reporter
    final Reporter reporter = new Reporter();

    // Get reader
    final SAMFileReader inputSam =
        new SAMFileReader(context.getInputDataFile(
            DataFormats.MAPPER_RESULTS_SAM, sample).open());

    // Get Writer
    final SAMFileWriter outputSam =
        new SAMFileWriterFactory().makeSAMWriter(
            inputSam.getFileHeader(),
            false,
            context.getInputDataFile(DataFormats.FILTERED_MAPPER_RESULTS_SAM,
                sample).create());

    String lastId = null;
    SAMRecord lastRecord = null;
    int lastIdCount = 0;

    for (SAMRecord samRecord : inputSam) {

      try {
        reporter.incrCounter(COUNTER_GROUP,
            INPUT_ALIGNMENTS_COUNTER.counterName(), 1);

        if (samRecord.getReadUnmappedFlag()) {
          reporter.incrCounter(COUNTER_GROUP,
              UNMAP_READS_COUNTER.counterName(), 1);
        } else {

          if (samRecord.getMappingQuality() >= mappingQualityThreshold) {

            reporter.incrCounter(COUNTER_GROUP,
                GOOD_QUALITY_ALIGNMENTS_COUNTER.counterName(), 1);

            final String id = samRecord.getReadName();

            if (id.equals(lastId)) {
              lastIdCount++;
            } else {

              if (lastIdCount == 1) {

                outputSam.addAlignment(samRecord);
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

          lastRecord = samRecord;
        }

      } catch (SAMFormatException e) {

        reporter.incrCounter(COUNTER_GROUP,
            ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName(), 1);
      }

    }

    if (lastIdCount == 1) {

      outputSam.addAlignment(lastRecord);
      reporter.incrCounter(COUNTER_GROUP,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);

    } else if (lastIdCount > 1) {

      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName(), 1);
      reporter.incrCounter(COUNTER_GROUP,
          ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName(), 1);
    }

    // Close files
    inputSam.close();
    outputSam.close();

    return reporter.countersValuesToString(
        COUNTER_GROUP,
        "Filter SAM files ("
            + sample.getName()
            + ", "
            + context.getOutputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample)
                .getName() + ")");
  }

}
