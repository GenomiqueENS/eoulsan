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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.INVALID_INPUT_PRETREATMENT_READS_COUNTER;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * This class define a reducer for the pretreatment of paired-end data before the reads filtering
 * step.
 *
 * @since 1.2
 * @author Claire Wallon
 */
public class PreTreatmentReducer extends Reducer<Text, Text, Text, Text> {

  private String counterGroup;
  private ReadSequence read1 = null, read2 = null;
  private String completeId1, completeId2;

  @Override
  protected void setup(final Context context) throws IOException, InterruptedException {

    final Configuration conf = context.getConfiguration();

    // Counter group
    this.counterGroup = conf.get(Globals.PARAMETER_PREFIX + ".counter.group");
    if (this.counterGroup == null) {
      throw new IOException("No counter group defined");
    }
  }

  /**
   * 'key': the identifier of the read without the integer indicating the member of the pair.
   * 'values': the rest of the paired TFQ lines (the member '1' and then the member '2').
   */
  @Override
  protected void reduce(final Text key, final Iterable<Text> values, final Context context)
      throws IOException, InterruptedException {

    String[] fields;
    String stringVal;

    for (Text val : values) {

      stringVal = val.toString();

      if (stringVal.charAt(0) != '1' && stringVal.charAt(0) != '2') {
        context
            .getCounter(this.counterGroup, INVALID_INPUT_PRETREATMENT_READS_COUNTER.counterName())
            .increment(1);
        return;
      }

      fields = stringVal.split("\t");

      // Illumina technology and Casava 1.8 format for the '@' line
      if (stringVal.charAt(1) == ':') {
        if (stringVal.charAt(0) == '1') {
          this.read1 = new ReadSequence();
          this.read1.setSequence(fields[1]);
          this.read1.setQuality(fields[2]);
          this.completeId1 = key.toString() + " " + fields[0];
        } else {
          this.read2 = new ReadSequence();
          this.read2.setSequence(fields[1]);
          this.read2.setQuality(fields[2]);
          this.completeId2 = key.toString() + " " + fields[0];
        }
      }

      // Before Casava 1.8 or technology other than Illumina
      else {
        if (stringVal.charAt(0) == '1') {
          this.read1 = new ReadSequence();
          this.read1.setSequence(fields[1]);
          this.read1.setQuality(fields[2]);
          this.completeId1 = key.toString() + fields[0];
        } else {
          this.read2 = new ReadSequence();
          this.read2.setSequence(fields[1]);
          this.read2.setQuality(fields[2]);
          this.completeId2 = key.toString() + fields[0];
        }
      }
    }

    if (this.read1 == null || this.read2 == null) {
      context
          .getCounter(this.counterGroup, INVALID_INPUT_PRETREATMENT_READS_COUNTER.counterName())
          .increment(1);
      return;
    }

    final Text outKey = new Text(this.completeId1);

    // Write results
    final Text outValue =
        new Text(
            this.read1.getSequence()
                + "\t"
                + this.read1.getQuality()
                + "\t"
                + this.completeId2
                + "\t"
                + this.read2.getSequence()
                + "\t"
                + this.read2.getQuality());
    context.write(outKey, outValue);
  }
}
