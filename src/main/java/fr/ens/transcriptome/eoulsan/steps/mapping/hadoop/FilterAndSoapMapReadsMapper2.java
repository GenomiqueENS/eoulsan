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

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.steps.mapping.FilterReadsConstants;
import fr.ens.transcriptome.eoulsan.steps.mapping.ReadsFilter;

public class FilterAndSoapMapReadsMapper2 extends SoapMapReadsMapper2 {

  public static final String COUNTER_GROUP = "Filter and map reads with SOAP";

  private final String counterGroup = getCounterGroup();
  private final ReadSequence read = new ReadSequence();
  private int lengthThreshold;
  private double qualityThreshold;

  protected String getCounterGroup() {

    return COUNTER_GROUP;
  }
  
  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {
    
    // Get configuration
    final Configuration conf = context.getConfiguration();
    
    this.lengthThreshold =
      Integer.parseInt(conf.get(Globals.PARAMETER_PREFIX
          + ".filter.reads.length.threshold", ""
          + FilterReadsConstants.LENGTH_THRESHOLD));

  this.qualityThreshold =
      Double.parseDouble(conf.get(Globals.PARAMETER_PREFIX
          + ".filter.reads.quality.threshold", ""
          + FilterReadsConstants.QUALITY_THRESHOLD));

    super.setup(context);
  }
  
  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    // Fill the ReadSequence object
    this.read.parse(value.toString());
    
    // Set a sequence name if does not exist
    if ("".equals(this.read.getName()))
      this.read.setName("s" + key);
    
    
    context.getCounter(counterGroup, "input fastq").increment(1);

    if (!this.read.check()) {

      context.getCounter(counterGroup, "input fastq not valid").increment(1);
      return;
    }
    context.getCounter(counterGroup, "input fastq valid").increment(1);

    // Trim the sequence with polyN as tail
    ReadsFilter.trimReadSequence(read);

    // Filter bad sequence
    if (ReadsFilter.isReadValid(read, this.lengthThreshold,
        this.qualityThreshold)) {

      context.getCounter(this.counterGroup, "reads after filtering").increment(1);
      super.map(key,value,context);
    } else
      context.getCounter(this.counterGroup, "reads rejected by filter").increment(1);
  }
  
}
