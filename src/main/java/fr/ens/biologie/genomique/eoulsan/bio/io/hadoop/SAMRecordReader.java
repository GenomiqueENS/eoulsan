package fr.ens.biologie.genomique.eoulsan.bio.io.hadoop;

import static fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.Counters.ENTRIES_WRITTEN;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;

/**
 * This class define a RecordReader for SAM files for the Hadoop MapReduce framework.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class SAMRecordReader extends RecordReader<Text, Text> {

  private static final String COUNTERS_GROUP = "SAM Input Format Counters";

  private final TaskAttemptContext context;
  private final LineRecordReader lrr = new LineRecordReader();
  private final Text key = new Text();
  private final Text value = new Text();

  @Override
  public synchronized void close() throws IOException {

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
  public void initialize(final InputSplit inputSplit, final TaskAttemptContext taskAttemptContext)
      throws IOException, InterruptedException {

    this.lrr.initialize(inputSplit, taskAttemptContext);
  }

  @Override
  public synchronized boolean nextKeyValue() throws IOException, InterruptedException {

    if (!this.lrr.nextKeyValue()) {
      return false;
    }

    final Text value = this.lrr.getCurrentValue();

    if (value != null) {

      final String s = value.toString();

      if (s.length() == 0) {
        this.key.set("");
        this.value.set("");
      } else {

        if (s.charAt(0) == '@') {
          this.key.set("");
        } else {

          final int posFirstTab = s.indexOf('\t');
          if (posFirstTab == -1) {
            this.key.set("");
          } else {
            this.key.set(s.substring(0, posFirstTab));
          }
        }

        this.value.set(s);
      }
    }

    this.context.getCounter(COUNTERS_GROUP, ENTRIES_WRITTEN).increment(1);

    return true;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   *
   * @param context the context
   */
  public SAMRecordReader(final TaskAttemptContext context) {

    this.context = context;
  }
}
