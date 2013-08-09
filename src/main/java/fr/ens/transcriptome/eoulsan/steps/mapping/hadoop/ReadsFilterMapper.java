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

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.READS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.HadoopMappingUtils.jobConfToParameters;
import static fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils.parseKeyValue;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.HadoopEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilterBuilder;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.util.hadoop.HadoopReporterIncrementer;

/**
 * This class defines a read filter mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class ReadsFilterMapper extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  // Parameters keys
  static final String FASTQ_FORMAT_KEY = Globals.PARAMETER_PREFIX
      + ".filter.reads.fastq.format";

  static final String READ_FILTER_PARAMETER_KEY_PREFIX =
      Globals.PARAMETER_PREFIX + ".filter.reads.parameter.";

  private static final Splitter TAB_SPLITTER = Splitter.on('\t').trimResults();
  private List<String> fields = newArrayList();
  private MultiReadFilter filter;
  private String counterGroup;

  private final ReadSequence read1 = new ReadSequence();
  private final ReadSequence read2 = new ReadSequence();

  private final Text outKey = new Text();
  private final Text outValue = new Text();

  //
  // Setup
  //

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    LOGGER.info("Start of configure()");

    // Get configuration object
    final Configuration conf = context.getConfiguration();

    // Initialize Eoulsan Settings
    if (!EoulsanRuntime.isRuntime()) {
      HadoopEoulsanRuntime.newEoulsanRuntime(conf);
    }

    // Set the FastqFormat
    final FastqFormat fastqFormat =
        FastqFormat.getFormatFromName(conf.get(FASTQ_FORMAT_KEY, ""
            + EoulsanRuntime.getSettings().getDefaultFastqFormat()));
    this.read1.setFastqFormat(fastqFormat);
    this.read2.setFastqFormat(fastqFormat);

    // Counter group
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    LOGGER.info("Fastq format: " + fastqFormat);

    // Set the filters
    try {
      final MultiReadFilterBuilder mrfb = new MultiReadFilterBuilder();

      // Add the parameters from the job configuration to the builder
      mrfb.addParameters(jobConfToParameters(conf,
          READ_FILTER_PARAMETER_KEY_PREFIX));

      this.filter =
          mrfb.getReadFilter(new HadoopReporterIncrementer(context), this.counterGroup);

      LOGGER.info("Reads filters to apply: "
          + Joiner.on(", ").join(this.filter.getFilterNames()));

    } catch (EoulsanException e) {
      throw new IOException(e.getMessage());
    }

    LOGGER.info("End of setup()");
  }

  //
  // Map
  //

  /**
   * 'key': offset of the beginning of the line from the beginning of the TFQ
   * file. 'value': the TFQ line.
   */
  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    context
        .getCounter(this.counterGroup, INPUT_RAW_READS_COUNTER.counterName())
        .increment(1);

    final String line = value.toString();

    fields.clear();
    for (String e : TAB_SPLITTER.split(line)) {
      fields.add(e);
    }

    final int fieldsSize = fields.size();

    if (fieldsSize == 3) {
      // Single end
      this.read1.setName(fields.get(0));
      this.read1.setSequence(fields.get(1));
      this.read1.setQuality(fields.get(2));

      if (this.filter.accept(this.read1)) {

        parseKeyValue(line, this.outKey, this.outValue);

        this.outKey.set(this.read1.getName());
        this.outValue.set(this.read1.getSequence()
            + "\t" + this.read1.getQuality());

        context.write(this.outKey, this.outValue);
        context.getCounter(this.counterGroup,
            OUTPUT_FILTERED_READS_COUNTER.counterName()).increment(1);
      } else {
        context.getCounter(this.counterGroup,
            READS_REJECTED_BY_FILTERS_COUNTER.counterName()).increment(1);
      }

    } else if (fieldsSize == 6) {
      // Paired-end
      this.read1.setName(fields.get(0));
      this.read1.setSequence(fields.get(1));
      this.read1.setQuality(fields.get(2));

      this.read2.setName(fields.get(3));
      this.read2.setSequence(fields.get(4));
      this.read2.setQuality(fields.get(5));

      if (this.filter.accept(this.read1, this.read2)) {

        this.outKey.set(this.read1.getName());
        this.outValue.set(this.read1.getSequence()
            + "\t" + this.read1.getQuality() + "\t" + this.read2.getName()
            + "\t" + this.read2.getSequence() + "\t" + this.read2.getQuality());

        context.write(this.outKey, this.outValue);
        context.getCounter(this.counterGroup,
            OUTPUT_FILTERED_READS_COUNTER.counterName()).increment(1);
      } else {
        context.getCounter(this.counterGroup,
            READS_REJECTED_BY_FILTERS_COUNTER.counterName()).increment(1);
      }
    }

  }

  @Override
  protected void cleanup(Context context) throws IOException,
      InterruptedException {
  }

}
