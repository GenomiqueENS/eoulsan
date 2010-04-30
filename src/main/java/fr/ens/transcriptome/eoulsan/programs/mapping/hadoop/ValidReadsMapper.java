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
import fr.ens.transcriptome.eoulsan.parsers.ReadSequence;
import fr.ens.transcriptome.eoulsan.programs.mapping.FilterReadsConstants;
import fr.ens.transcriptome.eoulsan.programs.mapping.ReadsFilter;

@SuppressWarnings("deprecation")
public class ValidReadsMapper implements Mapper<LongWritable, Text, Text, Text> {

  private Text outKey = new Text();
  private Text outValue = new Text();
  private final ReadSequence read = new ReadSequence();
  private int threshold;

  @Override
  public void map(final LongWritable key, final Text value, final 
      OutputCollector<Text, Text> output, final Reporter reporter) throws IOException {
   
    
    // Fill the ReadSequence object
    read.parse(value.toString());

    // Trim the sequence with polyN as tail
    ReadsFilter.trimReadSequence(read);

    reporter.incrCounter("Filter reads", "reads total", 1);
    
    // Filter bad sequence
    if (ReadsFilter.isReadValid(read, this.threshold)) {
      this.outKey.set(read.toOutKey());
      this.outValue.set(read.toOutValue());
      output.collect(this.outKey, this.outValue);
      reporter.incrCounter("Filter reads", "reads valids", 1);
    } else
      reporter.incrCounter("Filter reads", "reads not valids", 1);

  }

  @Override
  public void configure(final JobConf conf) {

    this.threshold =
        Integer.parseInt(conf
            .get(Globals.PARAMETER_PREFIX + ".validreadsmapper.theshold", ""
                + FilterReadsConstants.THRESHOLD));

  }

  @Override
  public void close() throws IOException {
  }

}
