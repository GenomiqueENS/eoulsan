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

/**
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

package fr.ens.transcriptome.eoulsan.bio.io.hadoop;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.util.LineReader;

/**
 * Treats keys as offset in file and value as line.
 */
public class FastqLineRecordReader extends RecordReader<LongWritable, Text> {

  private CompressionCodecFactory compressionCodecs = null;
  private long start;
  private long pos;
  private long end;
  private LineReader in;
  private int maxLineLength;
  private LongWritable key = null;
  private Text value = null;

  @Override
  public void initialize(final InputSplit genericSplit,
      final TaskAttemptContext context) throws IOException {
    FileSplit split = (FileSplit) genericSplit;
    Configuration job = context.getConfiguration();
    this.maxLineLength =
        job.getInt("mapreduce.input.linerecordreader.line.maxlength",
            Integer.MAX_VALUE);
    this.start = split.getStart();
    this.end = this.start + split.getLength();
    final Path file = split.getPath();
    this.compressionCodecs = new CompressionCodecFactory(job);
    final CompressionCodec codec = this.compressionCodecs.getCodec(file);

    // open the file and seek to the start of the split
    FileSystem fs = file.getFileSystem(job);
    FSDataInputStream fileIn = fs.open(split.getPath());
    boolean skipFirstLine = false;
    if (codec != null) {
      this.in = new LineReader(codec.createInputStream(fileIn), job);
      this.end = Long.MAX_VALUE;
    } else {
      if (this.start != 0) {
        skipFirstLine = true;
        --this.start;
        fileIn.seek(this.start);
      }
      this.in = new LineReader(fileIn, job);
    }
    if (skipFirstLine) { // skip first line and re-establish "start".
      this.start +=
          this.in.readLine(new Text(), 0,
              (int) Math.min(Integer.MAX_VALUE, this.end - this.start));
    }
    this.pos = this.start;
  }

  @Override
  public boolean nextKeyValue() throws IOException {

    return nextKeyValue(false);
  }

  public boolean nextKeyValue(final boolean cont) throws IOException {
    if (this.key == null) {
      this.key = new LongWritable();
    }
    this.key.set(this.pos);
    if (this.value == null) {
      this.value = new Text();
    }
    int newSize = 0;
    while (this.pos < this.end || cont) {
      newSize =
          this.in.readLine(this.value, this.maxLineLength, Math.max(
              (int) Math.min(Integer.MAX_VALUE, this.end - this.pos),
              this.maxLineLength));
      if (newSize == 0) {
        break;
      }
      this.pos += newSize;
      if (newSize < this.maxLineLength) {
        break;
      }

      // line too long. try again
      getLogger()
          .info(
              "Skipped line of size "
                  + newSize + " at pos " + (this.pos - newSize));
    }
    if (newSize == 0) {
      this.key = null;
      this.value = null;
      return false;
    } else {
      return true;
    }
  }

  @Override
  public LongWritable getCurrentKey() {
    return this.key;
  }

  @Override
  public Text getCurrentValue() {
    return this.value;
  }

  /**
   * Get the progress within the split
   */
  @Override
  public float getProgress() {
    if (this.start == this.end) {
      return 0.0f;
    } else {
      return Math.min(1.0f, (this.pos - this.start)
          / (float) (this.end - this.start));
    }
  }

  @Override
  public synchronized void close() throws IOException {
    if (this.in != null) {
      this.in.close();
    }
  }
}
