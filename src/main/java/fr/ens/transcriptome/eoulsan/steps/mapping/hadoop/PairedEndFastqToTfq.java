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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.base.Joiner;

import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.data.DataFile;

/**
 * This class allow to convert two FASTQ file in one TFQ file.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class PairedEndFastqToTfq {

  /**
   * This class define the reducer required to convert FASTQ files into TFQ
   * file.
   * @author Laurent Jourdren
   * @since 2.0
   */
  public static final class FastqPairedEndReducer extends
      Reducer<Text, Text, Text, Text> {

    private static final Joiner JOINER = Joiner.on('\t');

    @Override
    protected void reduce(final Text key, final Iterable<Text> values,
        final Context context) throws IOException, InterruptedException {

      final List<String> list = new ArrayList<String>();
      for (Text t : values) {
        list.add(t.toString());
      }

      Collections.sort(list);

      context.write(key, new Text(JOINER.join(list)));
    }

  }

  /**
   * Create the job to convert FASTQ files in a TFQ file.
   * @param parentConf Hadoop configuration
   * @param fastqFile1 Path of the first FASTQ file
   * @param fastqFile2 Path of the second FASTQ file
   * @param outputFile Path of the output TFQ file
   * @return an Hadoop Job
   * @throws IOException if an error occurs while creating the Job
   */
  public static Job convert(final Configuration parentConf,
      final DataFile fastqFile1, final DataFile fastqFile2,
      final DataFile outputFile) throws IOException {

    checkNotNull(parentConf, "parentConf argument cannot be null");
    checkNotNull(fastqFile1, "fastqFile1 argument cannot be null");
    checkNotNull(fastqFile2, "fastqFile2 argument cannot be null");
    checkNotNull(outputFile, "outputFile argument cannot be null");

    return convert(parentConf, new Path(fastqFile1.getSource()), new Path(
        fastqFile2.getSource()), new Path(outputFile.getSource()));
  }

  /**
   * Create the job to convert FASTQ files in a TFQ file.
   * @param parentConf Hadoop configuration
   * @param fastqFile1 Path of the first FASTQ file
   * @param fastqFile2 Path of the second FASTQ file
   * @param outputFile Path of the output TFQ file
   * @return an Hadoop Job
   * @throws IOException if an error occurs while creating the Job
   */
  public static Job convert(final Configuration parentConf,
      final Path fastqFile1, final Path fastqFile2, final Path outputFile)
      throws IOException {

    checkNotNull(parentConf, "parentConf argument cannot be null");
    checkNotNull(fastqFile1, "fastqFile1 argument cannot be null");
    checkNotNull(fastqFile2, "fastqFile2 argument cannot be null");
    checkNotNull(outputFile, "outputFile argument cannot be null");

    final Configuration jobConf = new Configuration(parentConf);

    // Set Job name
    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf, "Convert FASTQ paired files in TFQ ("
            + fastqFile1.getName() + ", " + fastqFile2.getName() + ", "
            + outputFile.getName() + ")");

    // Set the jar
    job.setJarByClass(PairedEndFastqToTfq.class);

    // Set input path
    FileInputFormat.addInputPath(job, fastqFile1);
    FileInputFormat.addInputPath(job, fastqFile2);

    // Set the input format
    job.setInputFormatClass(FastQFormatNew.class);

    // Set the Reducer class
    job.setReducerClass(FastqPairedEndReducer.class);

    // Set the Combiner class
    job.setCombinerClass(FastqPairedEndReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set output path
    FileOutputFormat.setOutputPath(job, outputFile);

    return job;
  }

}
