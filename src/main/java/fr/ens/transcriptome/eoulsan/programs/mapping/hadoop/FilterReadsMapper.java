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

package fr.ens.transcriptome.eoulsan.programs.mapping.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.programs.mapping.FilterReadsConstants;
import fr.ens.transcriptome.eoulsan.programs.mapping.ReadsFilter;

/**
 * This class is the mapper for filtering reads
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class FilterReadsMapper implements
    Mapper<LongWritable, Text, Text, Text> {

  public static final String COUNTER_GROUP = "Filter reads";

  private final Text outKey = new Text();
  private final Text outValue = new Text();
  private final ReadSequence read = new ReadSequence();
  private int lengthThreshold;
  private double qualityThreshold;

  @Override
  public void map(final LongWritable key, final Text value,
      final OutputCollector<Text, Text> output, final Reporter reporter)
      throws IOException {

    // Fill the ReadSequence object
    read.parse(value.toString());

    // Create an id if does not exists
    if ("".equals(this.read.getName()))
      this.read.setName("ID_" + Long.toHexString(key.get()));

    reporter.incrCounter(COUNTER_GROUP, "input fastq", 1);

    if (!this.read.check()) {

      reporter.incrCounter(COUNTER_GROUP, "input fastq not valid", 1);
      return;
    }
    reporter.incrCounter(COUNTER_GROUP, "input fastq valid", 1);

    // Trim the sequence with polyN as tail
    ReadsFilter.trimReadSequence(read);

    // Filter bad sequence
    if (ReadsFilter.isReadValid(read, this.lengthThreshold,
        this.qualityThreshold)) {
      this.outKey.set(read.toOutKey());
      this.outValue.set(read.toOutValue());
      output.collect(this.outKey, this.outValue);
      reporter.incrCounter(COUNTER_GROUP, "reads after filtering", 1);
    } else
      reporter.incrCounter(COUNTER_GROUP, "reads rejected by filter", 1);

  }

  @Override
  public void configure(final JobConf conf) {

    this.lengthThreshold =
        Integer.parseInt(conf.get(Globals.PARAMETER_PREFIX
            + ".validreadsmapper.length.threshold", ""
            + FilterReadsConstants.LENGTH_THRESHOLD));

    this.qualityThreshold =
        Double.parseDouble(conf.get(Globals.PARAMETER_PREFIX
            + ".validreadsmapper.quality.threshold", ""
            + FilterReadsConstants.QUALITY_THRESHOLD));

  }

  @Override
  public void close() throws IOException {
  }

}
