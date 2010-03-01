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

package fr.ens.transcriptome.eoulsan.hadoop.filterreads;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.ReadSequence;
import fr.ens.transcriptome.eoulsan.ReadsFilter;

public class ValidReadMapper extends Mapper<LongWritable, Text, Text, Text> {

  private static final int THRESHOLD = 15;

  private Text outKey = new Text();
  private Text outValue = new Text();
  private final ReadSequence read = new ReadSequence();
  private int threshold;

  @Override
  protected void setup(Context context) throws IOException,
      InterruptedException {

    // Get the threshold value
    this.threshold =
        Integer.parseInt(context.getConfiguration().get(
            Globals.PARAMETER_PREFIX + ".validreadsmapper.theshold",
            "" + THRESHOLD));

    super.setup(context);
  }

  @Override
  protected void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {

    // Fill the ReadSequence object
    read.parseFastQ(value.toString());

    // Trim the sequence with polyN as tail
    ReadsFilter.trimReadSequence(read);

    // Filter bad sequence
    if (ReadsFilter.isReadValid(read, this.threshold)) {
      this.outKey.set(read.toOutKey());
      this.outValue.set(read.toOutValue());
      context.write(this.outKey, this.outValue);
    }

  }

  public static final Pattern PATTERN = Pattern.compile("NN+$");

  /**
   * calcul le score moyen numérique d'une chaine de caractère de qualité
   * Illumina
   */
  private static final double calcNumScore(final String quality) {

    int score = 0;
    final int len = quality.length();
    for (int i = 0; i < len; i++)
      score += quality.codePointAt(i) - 64;

    return score / len;
  }

  public static void main(String[] args) {

    final String data =
        "HWI-EAS285:7:100:1056:415#0/1\tCTCAAACCTATGCGGAATACGGTGGGTTGACTTAGC\taaaaaaaaaaa[aUN^aaBBBBBBBBBBBBBBBBBB";

    // if (true) throw new IOException("data=" + data);

    final int indexFirstTab = data.indexOf('\t');
    final int indexSecondTab = data.indexOf('\t', indexFirstTab + 1);

    // final String id = data.substring(0, indexFirstTab);
    String sequence = data.substring(indexFirstTab + 1, indexSecondTab);
    String quality = data.substring(indexSecondTab + 1);

    sequence = PATTERN.split(sequence)[0];

    final int len = sequence.length();

    if (len != quality.length())
      quality = quality.substring(0, len);

    System.out.println("Sequence len=" + sequence.length());

    if (sequence.length() > 15 && calcNumScore(quality) > 15.0) {

      System.out.println("ok");
    } else
      System.out.println("ko");

  }

}
