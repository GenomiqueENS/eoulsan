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

package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.StatusReporter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

/**
 * This mapper simulate the ChainMapper for the new map reduce API.
 * @author Laurent Jourdren
 */
public class FilterAndMapMapper extends Mapper<LongWritable, Text, Text, Text> {

  private final LongWritable inKey = new LongWritable(0);
  private final Text inValue = new Text();

  private final ReadsFilterMapper filterReadsMapper = new ReadsFilterMapper();
  private final ReadsMapperMapper mapReadsMapper = new ReadsMapperMapper();
  private final SAMFilterMapper samFilterMapper = new SAMFilterMapper();

  private Context contextSAMFilter;
  private Context contextMapReads;
  private Context contextFilterReads;

  private abstract class ContextWrapper {

    private final Context context;

    public abstract void internalWrite(final Text arg0, final Text arg1)
        throws IOException, InterruptedException;

    public Context getContext() throws IOException, InterruptedException {

      final RecordWriter<Text, Text> rw = new RecordWriter<Text, Text>() {

        @Override
        public void write(final Text arg0, final Text arg1) throws IOException,
            InterruptedException {

          // results.add(arg0 + "\t" + arg1);
          internalWrite(arg0, arg1);
        }

        @Override
        public void close(TaskAttemptContext arg0) throws IOException,
            InterruptedException {
        }
      };

      final RecordReader<LongWritable, Text> rr =
          new RecordReader<LongWritable, Text>() {

            @Override
            public void close() throws IOException {
            }

            @Override
            public LongWritable getCurrentKey() throws IOException,
                InterruptedException {
              return null;
            }

            @Override
            public Text getCurrentValue() throws IOException,
                InterruptedException {
              return null;
            }

            @Override
            public float getProgress() throws IOException, InterruptedException {
              return 0;
            }

            @Override
            public boolean nextKeyValue() throws IOException,
                InterruptedException {
              return false;
            }

            @Override
            public void initialize(InputSplit arg0, TaskAttemptContext arg1)
                throws IOException, InterruptedException {
            }

          };

      final StatusReporter sr = new StatusReporter() {

        @Override
        public void setStatus(String status) {
          context.setStatus(status);

        }

        @Override
        public void progress() {
          context.progress();

        }

        @Override
        public Counter getCounter(final String arg0, final String arg1) {

          return context.getCounter(arg0, arg1);
        }

        @Override
        public Counter getCounter(final Enum<?> arg0) {

          return context.getCounter(arg0);
        }
      };

      return new Context(context.getConfiguration(),
          context.getTaskAttemptID(), rr, rw, context.getOutputCommitter(), sr,
          context.getInputSplit()) {
      };

    }

    //
    // Constructor
    //

    public ContextWrapper(final Context context) {

      this.context = context;
    }

  }

  //
  // Mapper methods
  //

  @Override
  protected void setup(final Context context) throws IOException,
      InterruptedException {

    this.contextSAMFilter = new ContextWrapper(context) {

      @Override
      public void internalWrite(final Text arg0, final Text arg1)
          throws IOException, InterruptedException {

        context.write(arg0, arg1);
      }

    }.getContext();

   this.contextMapReads = new ContextWrapper(context) {

      @Override
      public void internalWrite(final Text arg0, final Text arg1)
          throws IOException, InterruptedException {

        inValue.set(arg0 + "\t" + arg1);
        samFilterMapper.map(inKey, inValue, contextSAMFilter);
      }

    }.getContext();

    this.contextFilterReads = new ContextWrapper(context) {

      @Override
      public void internalWrite(final Text arg0, final Text arg1)
          throws IOException, InterruptedException {

        inValue.set(arg0 + "\t" + arg1);
        mapReadsMapper.map(inKey, inValue, contextMapReads);
      }

    }.getContext();

    this.filterReadsMapper.setup(contextFilterReads);
    this.mapReadsMapper.setup(contextMapReads);
    this.samFilterMapper.setup(contextSAMFilter);
  }

  @Override
  protected void map(final LongWritable key, final Text value,
      final Context context) throws IOException, InterruptedException {

    this.filterReadsMapper.map(key, value, contextFilterReads);

  }

  @Override
  protected void cleanup(Context context) throws IOException,
      InterruptedException {

    this.filterReadsMapper.cleanup(contextFilterReads);
    this.mapReadsMapper.cleanup(contextMapReads);
    this.samFilterMapper.cleanup(contextSAMFilter);
  }

}
