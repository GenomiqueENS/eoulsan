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

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.HadoopMappingUtils.addParametersToJobConf;
import static fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.ReadsFilterMapper.READ_FILTER_PARAMETER_KEY_PREFIX;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsFilterStep;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class is the main class for the filter reads program in hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@HadoopOnly
public class ReadsFilterHadoopStep extends AbstractReadsFilterStep {

  static final String OUTPUT_FILE1_KEY = Globals.PARAMETER_PREFIX
      + ".filter.reads.output.file1";
  static final String OUTPUT_FILE2_KEY = Globals.PARAMETER_PREFIX
      + ".filter.reads.output.file1";

  //
  // Step methods
  //

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(READS_FASTQ);
  }

  @Override
  public InputPorts getInputPorts() {

    return allPortsRequiredInWorkingDirectory(super.getInputPorts());
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create configuration object
    final Configuration conf = new Configuration();

    try {

      // Get input and output data
      final Data inData = context.getInputData(READS_FASTQ);
      final Data outData = context.getOutputData(READS_FASTQ, inData);

      // Get FASTQ format
      final FastqFormat fastqFormat = inData.getMetadata().getFastqFormat();

      // Create the job to run
      final Job job;

      DataFile tfqFile = null;

      // Preprocess paired-end files
      if (inData.getDataFileCount() == 1) {
        job =
            createJobConf(conf, inData.getDataFile(0), READS_FASTQ,
                fastqFormat, outData.getDataFile(0));
      } else {

        final DataFile inFile1 = inData.getDataFile(0);
        final DataFile inFile2 = inData.getDataFile(1);

        tfqFile =
            new DataFile(inFile1.getParent(), inFile1.getBasename()
                + READS_TFQ.getDefaultExtension());

        // Convert FASTQ files to TFQ
        MapReduceUtils.submitAndWaitForJob(
            PairedEndFastqToTfq.convert(conf, inFile1, inFile2, tfqFile),
            CommonHadoop.CHECK_COMPLETION_TIME);

        job =
            createJobConf(conf, tfqFile, READS_TFQ, fastqFormat,
                outData.getDataFile(0), outData.getDataFile(1));
      }

      // Submit main job
      MapReduceUtils.submitAndWaitForJob(job, inData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      // Cleanup paired-end
      if (inData.getDataFileCount() > 1) {

        final DataFile outFile1 = outData.getDataFile(0);
        final DataFile outFile2 = outData.getDataFile(1);

        // TODO implement DataFile.renameTo(DataFile)
        // TODO implement DataFile.delete(DataFile,true)

        final FileSystem fs = FileSystem.get(conf);
        fs.rename(
            new Path(outFile1.getSource() + ".tmp/" + outFile1.getName()),
            new Path(outFile1.getSource()));
        fs.rename(
            new Path(outFile1.getSource() + ".tmp/" + outFile2.getName()),
            new Path(outFile2.getSource()));

        fs.delete(new Path(outFile1.getSource() + ".tmp"), true);
        fs.delete(new Path(tfqFile.getSource()), true);
      }

      return status.createStepResult();

    } catch (IOException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    } catch (InterruptedException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    } catch (ClassNotFoundException e) {

      return status.createStepResult(e,
          "Error while running job: " + e.getMessage());
    }

  }

  /**
   * Create a filter reads job
   * @return a JobConf object
   * @throws IOException
   */
  private Job createJobConf(final Configuration parentConf,
      final DataFile inFile, final DataFormat inputFormat,
      final FastqFormat fastqFormat, final DataFile... outFiles)
      throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Set input path
    final Path inputPath = new Path(inFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set fastq format
    jobConf.set(ReadsFilterMapper.FASTQ_FORMAT_KEY, fastqFormat.getName());

    // Set read filter parameters
    addParametersToJobConf(getReadFilterParameters(),
        READ_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    // Set outputs
    if (outFiles.length > 1) {
      jobConf.set(OUTPUT_FILE1_KEY, outFiles[0].getName());
      jobConf.set(OUTPUT_FILE2_KEY, outFiles[1].getName());
    }

    // Set Job name
    // Create the job and its name
    final Job job =
        Job.getInstance(jobConf, "Filter reads ("
            + inFile.getName() + ", " + inFile.getSource() + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (inputFormat == READS_FASTQ) {
      job.setInputFormatClass(FastqInputFormat.class);
    } else {
      job.setInputFormatClass(KeyValueTextInputFormat.class);
    }

    // Set the Mapper class
    job.setMapperClass(ReadsFilterMapper.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(0);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outFiles[0].getSource()
        + (outFiles.length > 1 ? ".tmp" : "")));

    return job;
  }
}
