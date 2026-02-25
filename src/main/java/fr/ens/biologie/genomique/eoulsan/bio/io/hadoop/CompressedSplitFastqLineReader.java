/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.ens.biologie.genomique.eoulsan.bio.io.hadoop;

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;
import org.apache.hadoop.mapreduce.lib.input.SplitLineReader;

/**
 * This class define a split line reader for compressed FASTQ files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CompressedSplitFastqLineReader extends SplitLineReader {

  SplitCompressionInputStream scin;
  private boolean usingCRLF;
  private boolean needAdditionalRecord = false;
  private boolean finished = false;
  private boolean cont = false;

  /**
   * Constructor.
   * @param in input stream
   * @param conf Hadoop configuration
   * @param recordDelimiterBytes bytes
   * @throws IOException if an error occurs while reading data
   */
  public CompressedSplitFastqLineReader(SplitCompressionInputStream in,
      Configuration conf, byte[] recordDelimiterBytes) throws IOException {
    super(in, conf, recordDelimiterBytes);
    scin = in;
    usingCRLF = (recordDelimiterBytes == null);
  }

  @Override
  protected int fillBuffer(InputStream in, byte[] buffer, boolean inDelimiter)
      throws IOException {
    int bytesRead = in.read(buffer);

    // If the split ended in the middle of a record delimiter then we need
    // to read one additional record, as the consumer of the next split will
    // not recognize the partial delimiter as a record.
    // However if using the default delimiter and the next character is a
    // linefeed then next split will treat it as a delimiter all by itself
    // and the additional record read should not be performed.
    if (inDelimiter && bytesRead > 0) {
      if (usingCRLF) {
        needAdditionalRecord = (buffer[0] != '\n');
      } else {
        needAdditionalRecord = true;
      }
    }
    return bytesRead;
  }

  @Override
  public int readLine(Text str, int maxLineLength, int maxBytesToConsume)
      throws IOException {
    int bytesRead = 0;
    if (!finished) {
      // only allow at most one more record to be read after the stream
      // reports the split ended
      if (scin.getPos() > scin.getAdjustedEnd() && !this.cont) {
        finished = true;
      }

      bytesRead = super.readLine(str, maxLineLength, maxBytesToConsume);
    }
    return bytesRead;
  }

  @Override
  public boolean needAdditionalRecordAfterSplit() {
    return !finished && needAdditionalRecord;
  }

  void setContinue(final boolean cont) {
    this.cont = cont;
  }
}
