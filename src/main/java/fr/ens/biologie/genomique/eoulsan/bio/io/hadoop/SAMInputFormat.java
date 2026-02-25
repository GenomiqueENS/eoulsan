package fr.ens.biologie.genomique.eoulsan.bio.io.hadoop;

import java.io.IOException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

/**
 * This class define an InputFormat for SAM files for the Hadoop MapReduce framework.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
public class SAMInputFormat extends FileInputFormat<Text, Text> {

  @Override
  public RecordReader<Text, Text> createRecordReader(
      InputSplit inputSplit, TaskAttemptContext context) throws IOException, InterruptedException {

    return new SAMRecordReader(context);
  }

  @Override
  protected boolean isSplitable(JobContext context, Path file) {

    final CompressionCodec codec =
        new CompressionCodecFactory(context.getConfiguration()).getCodec(file);

    if (null == codec) {
      return true;
    }

    return codec instanceof SplittableCompressionCodec;
  }
}
