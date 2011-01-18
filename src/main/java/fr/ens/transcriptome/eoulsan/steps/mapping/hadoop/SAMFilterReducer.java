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

import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.Globals;

public class SAMFilterReducer extends Reducer<Text, Text, Text, Text> {

  private String counterGroup;

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    final Configuration conf = context.getConfiguration();

    // Counter group
    this.counterGroup = conf.get(Globals.PARAMETER_PREFIX + ".counter.group");
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }
  }

  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    int count = 0;
    Text firstValue = null;

    for (Text val : values) {

      if (count == 0) {
        firstValue = val;
        count++;
      } else {
        count++;
        break;
      }
    }

    if (count == 1) {
      context.write(key, firstValue);
      context.getCounter(this.counterGroup,
          OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName()).increment(1);
    } else {

      context.getCounter(this.counterGroup,
          ALIGNMENTS_REJECTED_BY_FILTERS_COUNTER.counterName()).increment(1);
      context.getCounter(this.counterGroup,
          ALIGNMENTS_WITH_MORE_ONE_HIT_COUNTER.counterName()).increment(1);
    }

  }

}