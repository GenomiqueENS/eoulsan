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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.READS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.util.MapReduceUtilsNewAPI.parseKeyValue;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.MultiReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.PairEndReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.QualityReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.TrimReadFilter;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.ValidReadFilter;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsFilterStep;
import fr.ens.transcriptome.eoulsan.util.HadoopReporterIncrementer;

public class ReadsFilterMapper extends Mapper<LongWritable, Text, Text, Text> {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  // Parameters keys
  static final String PAIR_END_KEY = Globals.PARAMETER_PREFIX + ".pairend";
  static final String LENGTH_THRESHOLD_KEY =
      Globals.PARAMETER_PREFIX + ".filter.reads.length.threshold";
  static final String QUALITY_THRESHOLD_KEY =
      Globals.PARAMETER_PREFIX + ".filter.reads.quality.threshold";

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

    // Set pair end mode
    final boolean pairEnd =
        Boolean.parseBoolean(context.getConfiguration().get(PAIR_END_KEY));

    // Get length threshold
    final int lengthThreshold =
        Integer.parseInt(conf.get(LENGTH_THRESHOLD_KEY, ""
            + AbstractReadsFilterStep.LENGTH_THRESHOLD));

    // Get quality threshold
    final double qualityThreshold =
        Double.parseDouble(conf.get(QUALITY_THRESHOLD_KEY, ""
            + AbstractReadsFilterStep.QUALITY_THRESHOLD));

    // Counter group
    this.counterGroup = conf.get(CommonHadoop.COUNTER_GROUP_KEY);
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }

    LOGGER.info("pairend=" + pairEnd);
    LOGGER.info("lengththreshold=" + lengthThreshold);
    LOGGER.info("qualitythreshold=" + qualityThreshold);

    // Set the filters
    filter =
        new MultiReadFilter(new HadoopReporterIncrementer(context),
            this.counterGroup);
    filter.addFilter(new PairEndReadFilter(pairEnd));
    filter.addFilter(new ValidReadFilter());
    filter.addFilter(new TrimReadFilter(lengthThreshold));
    filter.addFilter(new QualityReadFilter(qualityThreshold));

    LOGGER.info("End of setup()");
  }

  //
  // Map
  // 

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
      // Pair end
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
            + this.read2.getSequence() + "\t" + this.read2.getQuality());

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
