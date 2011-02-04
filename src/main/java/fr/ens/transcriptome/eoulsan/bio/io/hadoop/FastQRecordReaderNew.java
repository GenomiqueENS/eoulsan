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

package fr.ens.transcriptome.eoulsan.bio.io.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import fr.ens.transcriptome.eoulsan.Globals;

public class FastQRecordReaderNew extends RecordReader<LongWritable, Text> {


  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  
  private long end;
  private boolean stillInChunk = true;

  private LongWritable key = new LongWritable();
  private Text value = new Text();

  private InputStream fsin;
  private DataOutputBuffer buffer = new DataOutputBuffer();

  private byte[] endTag = "\n@".getBytes();
  private static final Pattern PATTERN = Pattern.compile("\n");
  private static final StringBuilder sb = new StringBuilder();

  // TODO
  private boolean startPos = true;
  

  private long pos;
  
  public void initialize(InputSplit inputSplit,
      TaskAttemptContext taskAttemptContext) throws IOException,
      InterruptedException {

    
    FileSplit split = (FileSplit) inputSplit;
    Configuration conf = taskAttemptContext.getConfiguration();
    Path path = split.getPath();
    FileSystem fs = path.getFileSystem(conf);

    final CompressionCodecFactory compressionCodecs =
        new CompressionCodecFactory(conf);
    final CompressionCodec codec = compressionCodecs.getCodec(path);

    // open the file and seek to the start of the split
    FSDataInputStream fileIn = fs.open(split.getPath());
    
    long start = split.getStart();
    
    if (codec != null) {
      this.fsin = codec.createInputStream(fileIn);
      end = Long.MAX_VALUE;
      
      LOGGER.info("Compressed mode. " + this.fsin.getClass().getName());
    } 
    else {
      
      LOGGER.info("Non Compressed mode. " + this.fsin.getClass().getName());
      if (start != 0) {
//        skipFirstLine = true;
//        --start;
//        fileIn.seek(start);
        end = split.getStart() + split.getLength();
        fileIn.seek(start);
        this.pos=start;
        readUntilMatch(endTag, false);
        startPos=false;
      }
      fsin = fileIn;;
    }

    

//    fsin = fs.open(path);
//    long start = split.getStart();
//    end = split.getStart() + split.getLength();
//    fsin.seek(start);

//    if (start != 0) {
//      readUntilMatch(endTag, false);
//    }
  }

  public boolean nextKeyValue() throws IOException {
    if (!stillInChunk)
      return false;

    //final long startPos = fsin.getPos();



    boolean status = readUntilMatch(endTag, true);

    final String data;

    // If start of the file, ignore first '@'
    if (startPos) {
      data = new String(buffer.getData(), 1, buffer.getLength());
      startPos=false;
    }
    else
      data = new String(buffer.getData(), 0, buffer.getLength());

    final String[] lines = PATTERN.split(data);

    String id = "";
    int count = 0;

    for (int i = 0; i < lines.length; i++) {

      final String line = lines[i].trim();

      if ("".equals(line))
        continue;

      if (count == 0)
        id = line;

      if (count == 2 && !id.equals(line.substring(1)))
        throw new IOException("Invalid record: id not equals "
            + id + " " + line.substring(1));

      if (count != 2 && count < 4) {
        if (count > 0)
          sb.append("\t");
        sb.append(line);
      }
      count++;
    }

    // if (count != 5)
    // throw new IOException("Invalid record: found " + count + " lines");

    value = new Text(sb.toString());
    // value.set(buffer.getData(), 0, buffer.getLength());
    sb.setLength(0);

    key = new LongWritable(pos);
    buffer.reset();

    if (!status) {
      stillInChunk = false;
    }

    return true;
  }

  public LongWritable getCurrentKey() throws IOException, InterruptedException {
    return key;
  }

  public Text getCurrentValue() throws IOException, InterruptedException {
    return value;
  }

  public float getProgress() throws IOException, InterruptedException {
    return 0;
  }

  public void close() throws IOException {
    fsin.close();
  }

  private boolean readUntilMatch( byte[] match, boolean withinBlock)
      throws IOException {
    int i = 0;
    
    while (true) {
      int b = fsin.read();
      if (b == -1)
        return false;
      pos++;
      if (withinBlock)
        buffer.write(b);
      if (b == match[i]) {
        i++;
        if (i >= match.length) {
          return pos < end;
        }
      } else
        i = 0;
    }
  }
}
