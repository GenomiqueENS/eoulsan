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

package fr.ens.transcriptome.eoulsan.hadoop.gmorse;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import fr.ens.transcriptome.eoulsan.hadoop.Parameter;

/**
 * This class implements the reducer for the build covtigs phase. It is base on
 * Gmorse source code.
 * @author Laurent Jourdren
 * @author Jean-Marc Aury
 * @author France Denoeud
 */
public class BuildCovtigsReducer extends
    Reducer<Text, Text, NullWritable, Text> {

  private static final int DSEUIL = 1;

  private int threshold;
  private int dseuil = DSEUIL;
  private Text outValue = new Text();

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    final Configuration conf = context.getConfiguration();

    this.threshold =
        Parameter.getInt(conf, ".gmorse.buildcovtigs.threshold",
            "Invalid or not found buildcovtigs threshold");
  }

  @Override
  protected void reduce(final Text key, final Iterable<Text> values,
      final Context context) throws IOException, InterruptedException {

    // Parse key
    final String sk = key.toString();   
    //System.out.println("Reducer key='"+sk+"'");
    final int spaceKeyIndex = sk.indexOf(' ');

    // Get chromomose
    final String chr = sk.substring(0, spaceKeyIndex);
    System.out.println("Chromosome='"+chr+"'");

    // Set threshold as final
    final int threshold = this.threshold;
    final int dseuil = this.dseuil;

    int debContigCur = 0;
    int endContigCur = 0;
    int lcontigcur = 0;
    int couvContigCur = 0;
    int couvMinCur = 0;
    int couvMaxCur = 0;
    int posprec = 0;

    boolean first = true;

    for (Text v : values) {

      // Parse value
      final String sv = v.toString();

      final int spaceIndex = sv.indexOf(' ');

      // Get postion
      final int pos = Integer.parseInt(sv.substring(0, spaceIndex));

      // Get coverage
      final int cov = Integer.parseInt(sv.substring(spaceIndex + 1));

      if (first) {

        debContigCur = pos;
        lcontigcur = 1;
        couvContigCur = cov;
        couvMinCur = cov;
        couvMaxCur = cov;
        first = false;
      } else {

        if (pos <= (posprec + dseuil)) {

          endContigCur = pos;
          lcontigcur++;
          couvContigCur = couvContigCur + cov;

          // Test if max
          if (cov > couvMaxCur)
            couvMaxCur = cov;

          // Test if min
          if (cov < couvMinCur)
            couvMinCur = cov;

        }

        if (pos > posprec + dseuil) {
          endContigCur = posprec;

          // Write output id CouvMax is > threshold
          if (couvMaxCur > threshold) {
            // final int intVal =
            // ((int) ((double) couvContigCur / (double) lcontigcur));
             final int intVal =
             (int) Math
             .round(((double) couvContigCur / (double) lcontigcur));
//            final String intVal =
//                String.format("%1.0f",
//                    ((double) couvContigCur / (double) lcontigcur));
            outValue.set(chr
                + "\t" + debContigCur + "\t" + endContigCur + "\t" + intVal+"\t-");
            context.write(null, outValue);
          }

          debContigCur = pos;
          lcontigcur = 1;
          couvContigCur = cov;
          couvMaxCur = cov;
          couvMinCur = cov;
        }

      }

      posprec = pos;
    }

    // Write output id CouvMax is > threshold
    if (couvMaxCur > threshold) {
      endContigCur = posprec;
      // final int intVal = ((int) ((double) couvContigCur / (double)
      // lcontigcur));
       final int intVal =
       (int) Math.round(((double) couvContigCur / (double) lcontigcur));
//      final String intVal =
//          String.format("%1.0f", ((double) couvContigCur / (double) lcontigcur));
      outValue.set(chr
          + "\t" + debContigCur + "\t" + endContigCur + "\t" + intVal+ "\t+");
      context.write(null, outValue);
    }
  } // end reduce method 
}
