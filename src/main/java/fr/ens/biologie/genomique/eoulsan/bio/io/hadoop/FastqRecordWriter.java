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

package fr.ens.biologie.genomique.eoulsan.bio.io.hadoop;

import static fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.Counters.ENTRIES_WRITTEN;
import static fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.Counters.INPUT_ENTRIES;

import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * This class define a RecordWriter for FASTQ files.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FastqRecordWriter extends RecordWriter<Text, Text> {

  private static final String COUNTERS_GROUP = "FASTQ Output Format Counters";

  private static final byte[] newline;

  static {
    newline = "\n".getBytes(StandardCharsets.UTF_8);
  }

  private final DataOutputStream out;
  private final TaskAttemptContext context;
  private final ReadSequence read = new ReadSequence();

  @Override
  public synchronized void write(final Text key, final Text value) throws IOException {

    this.context.getCounter(COUNTERS_GROUP, INPUT_ENTRIES).increment(1);

    if (value == null) {
      return;
    }

    this.read.parse(value.toString());

    this.out.write(this.read.toFastQ().getBytes(StandardCharsets.UTF_8));
    this.out.write(newline);

    this.context.getCounter(COUNTERS_GROUP, ENTRIES_WRITTEN).increment(1);
  }

  @Override
  public synchronized void close(final TaskAttemptContext context) throws IOException {

    this.out.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   *
   * @param context the context
   * @param out data output stream
   */
  public FastqRecordWriter(final TaskAttemptContext context, final DataOutputStream out) {

    this.context = context;
    this.out = out;
  }
}
