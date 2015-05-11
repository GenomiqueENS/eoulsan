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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * This class define a RecordWriter for expression files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionRecordWriter extends RecordWriter<Text, LongWritable> {

  private static final String utf8 = "UTF-8";
  private static final byte[] newline;
  private static final byte[] separator;

  static {
    try {
      newline = "\n".getBytes(utf8);
      separator = "\t".getBytes(utf8);
    } catch (UnsupportedEncodingException uee) {
      throw new IllegalArgumentException("can't find " + utf8 + " encoding");
    }
  }

  private final DataOutputStream out;

  @Override
  public synchronized void write(final Text key, final LongWritable value)
      throws IOException, InterruptedException {

    if (value == null) {
      return;
    }

    out.write(key.getBytes(), 0, key.getLength());
    out.write(separator);
    out.write(value.toString().getBytes(utf8));
    out.write(newline);
  }

  @Override
  public synchronized void close(final TaskAttemptContext context)
      throws IOException {
    out.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param out output stream
   */
  public ExpressionRecordWriter(final DataOutputStream out) {

    this.out = out;
  }

}
