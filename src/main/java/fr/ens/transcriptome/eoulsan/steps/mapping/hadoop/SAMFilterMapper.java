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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_INVALID_SAM_FORMAT;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.GOOD_QUALITY_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.MAPPER_WRITING_ERRORS;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.UNMAP_READS_COUNTER;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtilsNewAPI;

public class SAMFilterMapper extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  // Parameters keys
  static final String MAPPING_QUALITY_THRESOLD_KEY = Globals.PARAMETER_PREFIX
      + ".samfilter.mapping.quality.threshold";
  static final String GENOME_DESC_PATH_KEY = Globals.PARAMETER_PREFIX
      + ".samfilter.genome.desc.file";

  private static final int MAX_MAPPING_QUALITY_THRESHOLD = 255;

  private int mappingQualityThreshold;

  private final SAMParser parser = new SAMParser();
  private String counterGroup;

  private final Text outKey = new Text();
  private final Text outValue = new Text();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of configure()");

    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan DataProtocols
    if (!EoulsanRuntime.isRuntime()) {
      HadoopEoulsanRuntime.newEoulsanRuntime(conf);
    }

    // Counter group
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    // Get the number of threads to use
    this.mappingQualityThreshold =
        Integer.parseInt(conf.get(MAPPING_QUALITY_THRESOLD_KEY, "-1"));

    // Get the genome description filename
    final String genomeDescFile = conf.get(GENOME_DESC_PATH_KEY);

    if (genomeDescFile == null) {
      throw new IOException("No genome desc file set");
    }

    // Load genome description object
    final GenomeDescription genomeDescription =
        GenomeDescription.load(new DataFile(genomeDescFile).open());

    // Set the chromosomes sizes in the parser
    this.parser.setGenomeDescription(genomeDescription);

    if (this.mappingQualityThreshold < 0
        || this.mappingQualityThreshold > MAX_MAPPING_QUALITY_THRESHOLD) {
      throw new IOException("Invalid MAQ threshold: "
          + this.mappingQualityThreshold);
    }

    LOGGER.info("mappingQualityThreshold=" + this.mappingQualityThreshold);

    LOGGER.info("End of setup()");
  }

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    context.getCounter(this.counterGroup,
        INPUT_ALIGNMENTS_COUNTER.counterName()).increment(1);

    final String line = value.toString();

    try {
      final SAMRecord samRecord = this.parser.parseLine(line);

      if (samRecord.getReadUnmappedFlag()) {
        context
            .getCounter(this.counterGroup, UNMAP_READS_COUNTER.counterName())
            .increment(1);
      } else {

        if (samRecord.getMappingQuality() >= mappingQualityThreshold) {

          final boolean parseResult =
              MapReduceUtilsNewAPI.parseKeyValue(line, this.outKey,
                  this.outValue);

          if (parseResult) {
            context.getCounter(this.counterGroup,
                GOOD_QUALITY_ALIGNMENTS_COUNTER.counterName()).increment(1);
            context.write(this.outKey, this.outValue);
          } else {
            context.getCounter(this.counterGroup,
                MAPPER_WRITING_ERRORS.counterName()).increment(1);
          }

        } else {

          context.getCounter(this.counterGroup,
              ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName())
              .increment(1);
        }

      }

    } catch (SAMFormatException e) {

      context.getCounter(this.counterGroup,
          ALIGNMENTS_WITH_INVALID_SAM_FORMAT.counterName()).increment(1);
    }
  }

  @Override
  protected void cleanup(Context context) throws IOException,
      InterruptedException {
  }
}