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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * This class define a RecordWriter for SAM files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SAMRecordWriter extends RecordWriter<Text, Text> {

  final Writer writer;
  final StringBuilder sb = new StringBuilder();

  @Override
  public void close(final TaskAttemptContext context) throws IOException,
      InterruptedException {

    this.writer.close();
  }

  @Override
  public void write(final Text key, final Text value) throws IOException,
      InterruptedException {

    final StringBuilder sb = this.sb;

    sb.setLength(0);

    sb.append(key.toString());
    sb.append('\t');
    sb.append(value.toString());

    if (sb.charAt(sb.length() - 2) == '\t') {
      sb.setLength(sb.length() - 1);
    }

    sb.append('\n');

    this.writer.write(sb.toString());
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param os output stream
   */
  public SAMRecordWriter(final OutputStream os) {

    this.writer = new OutputStreamWriter(os);
  }

}
