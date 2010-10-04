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
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.programs.mapping.FilterReadsConstants;
import fr.ens.transcriptome.eoulsan.programs.mapping.ReadsFilter;

/**
 * This class define a mapper that filter and map reads with SOAP in the same
 * time.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public final class FilterAndSoapMapReadsMapper extends SoapMapReadsMapper {

  public static final String COUNTER_GROUP = "Filter and map reads with SOAP";

  private final String counterGroup = getCounterGroup();
  private final ReadSequence read = new ReadSequence();
  private int lengthThreshold;
  private double qualityThreshold;

  protected String getCounterGroup() {

    return COUNTER_GROUP;
  }

  @Override
  public void configure(final JobConf conf) {

    this.lengthThreshold =
        Integer.parseInt(conf.get(Globals.PARAMETER_PREFIX
            + ".filter.reads.length.threshold", ""
            + FilterReadsConstants.LENGTH_THRESHOLD));

    this.qualityThreshold =
        Double.parseDouble(conf.get(Globals.PARAMETER_PREFIX
            + ".filter.reads.quality.threshold", ""
            + FilterReadsConstants.QUALITY_THRESHOLD));

    super.configure(conf);
  }

  @Override
  public void map(final LongWritable key, final Text value,
      final OutputCollector<Text, Text> collector, final Reporter reporter)
      throws IOException {

    // Fill the ReadSequence object
    this.read.parse(value.toString());

    // Create an id if does not exists
    if ("".equals(this.read.getName()))
      this.read.setName("ID_" + Long.toHexString(key.get()));

    reporter.incrCounter(counterGroup, "input fastq", 1);

    if (!this.read.check()) {

      reporter.incrCounter(counterGroup, "input fastq not valid", 1);
      return;
    }
    reporter.incrCounter(counterGroup, "input fastq valid", 1);

    // Trim the sequence with polyN as tail
    ReadsFilter.trimReadSequence(read);

    // Filter bad sequence
    if (ReadsFilter.isReadValid(read, this.lengthThreshold,
        this.qualityThreshold)) {

      reporter.incrCounter(this.counterGroup, "reads after filtering", 1);
      super.writeRead(this.read, collector, reporter);
    } else
      reporter.incrCounter(this.counterGroup, "reads rejected by filter", 1);
  }

}
