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

package fr.ens.transcriptome.eoulsan.bio.io.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * This class define a RecordReader for FASTQ files for the Hadoop MapReduce
 * framework.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FastqRecordReader extends RecordReader<Text, Text> {

  private Text key = new Text();
  private Text value = new Text();

  private final String[] lines = new String[4];
  private final long[] pos = new long[4];

  private FastqLineRecordReader lrr;

  // private LineRecordReader lrr;

  @Override
  public void close() throws IOException {

    this.lrr.close();
  }

  @Override
  public Text getCurrentKey() throws IOException, InterruptedException {

    return this.key;
  }

  @Override
  public Text getCurrentValue() throws IOException, InterruptedException {

    return this.value;
  }

  @Override
  public float getProgress() throws IOException, InterruptedException {

    return this.lrr.getProgress();
  }

  @Override
  public void initialize(final InputSplit inputSplit,
      final TaskAttemptContext taskAttemptContext) throws IOException,
      InterruptedException {

    this.lrr = new FastqLineRecordReader();
    // this.lrr = new LineRecordReader();
    this.lrr.initialize(inputSplit, taskAttemptContext);
  }

  @Override
  public boolean nextKeyValue() throws IOException, InterruptedException {

    int count = 0;
    boolean found = false;

    while (!found) {

      if (!this.lrr.nextKeyValue(count != 0)) {
        return false;
        // if (!this.lrr.nextKeyValue())
        // return false;
      }

      final String s = this.lrr.getCurrentValue().toString().trim();

      // Prevent empty lines
      if (s.length() == 0) {
        continue;
      }

      this.lines[count] = s;
      this.pos[count] = this.lrr.getCurrentKey().get();

      if (count < 3) {
        count++;
      } else {

        if (this.lines[0].charAt(0) == '@' && this.lines[2].charAt(0) == '+') {
          found = true;
        } else {

          // Shift lines
          this.lines[0] = this.lines[1];
          this.lines[1] = this.lines[2];
          this.lines[2] = this.lines[3];

          // Shift positions
          this.pos[0] = this.pos[1];
          this.pos[1] = this.pos[2];
          this.pos[2] = this.pos[3];
        }
      }

    }

    // Set key

    // this.key = new LongWritable(this.pos[0]);
    this.key = new Text(memberId(this.lines[0].substring(1)));

    // Set value
    this.value =
        new Text(this.lines[0].substring(1)
            + '\t' + this.lines[1] + '\t' + this.lines[3]);

    // Clean array
    this.lines[0] = this.lines[1] = this.lines[2] = this.lines[3] = null;

    return true;
  }

  /**
   * Get the member id of a sequence Id
   * @param s sequence id
   * @return the member of the sequence id
   */
  private static String memberId(final String s) {

    if (s == null) {
      return null;
    }

    // New Illumina Id
    final int pos1 = s.indexOf(' ');
    if (pos1 != -1) {
      return s.substring(0, pos1);
    }

    // Old Illumina Id
    final int pos2 = s.indexOf('/');
    if (pos2 != -1) {
      return s.substring(0, pos2);
    }

    // Other, do nothing
    return s;
  }

}
