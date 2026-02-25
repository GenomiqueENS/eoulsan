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

import java.io.DataOutputStream;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.ReflectionUtils;

/**
 * This class define a expression output format.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ExpressionOutputFormat extends FileOutputFormat<Text, LongWritable> {

  @Override
  public RecordWriter<Text, LongWritable> getRecordWriter(TaskAttemptContext context)
      throws IOException, InterruptedException {

    Configuration conf = context.getConfiguration();
    boolean isCompressed = getCompressOutput(context);

    CompressionCodec codec = null;
    String extension = "";

    if (isCompressed) {
      Class<? extends CompressionCodec> codecClass =
          getOutputCompressorClass(context, GzipCodec.class);
      codec = ReflectionUtils.newInstance(codecClass, conf);
      extension = codec.getDefaultExtension();
    }

    // Get the output file path
    final Path file = getDefaultWorkFile(context, extension);

    final FileSystem fs = file.getFileSystem(conf);
    if (!isCompressed) {

      FSDataOutputStream fileOut = fs.create(file, false);
      return new ExpressionRecordWriter(context, fileOut);
    } else {

      FSDataOutputStream fileOut = fs.create(file, false);
      return new ExpressionRecordWriter(
          context, new DataOutputStream(codec.createOutputStream(fileOut)));
    }
  }
}
