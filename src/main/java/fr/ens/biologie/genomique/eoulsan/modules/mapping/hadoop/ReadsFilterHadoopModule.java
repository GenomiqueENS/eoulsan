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

package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import static com.google.common.collect.Lists.newArrayList;
import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_TFQ;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.HadoopMappingUtils.addParametersToJobConf;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.ReadsFilterMapper.READ_FILTER_PARAMETER_KEY_PREFIX;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.FastqInputFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.FastqOutputFormat;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractReadsFilterModule;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class is the main class for the filter reads program in hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@HadoopOnly
public class ReadsFilterHadoopModule extends AbstractReadsFilterModule {

  static final String OUTPUT_FILE1_KEY =
      Globals.PARAMETER_PREFIX + ".filter.reads.output.file1";
  static final String OUTPUT_FILE2_KEY =
      Globals.PARAMETER_PREFIX + ".filter.reads.output.file2";

  private static final String TEMP_DIR_SUFFIX = ".tmp";

  //
  // Module methods
  //

  @Override
  public InputPorts getInputPorts() {

    return allPortsRequiredInWorkingDirectory(super.getInputPorts());
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Create configuration object
    final Configuration conf = createConfiguration();

    try {

      // Get input and output data
      final Data inData = context.getInputData(READS_FASTQ);
      final Data outData = context.getOutputData(READS_FASTQ, inData);
      final String dataName = inData.getName();

      // Get FASTQ format
      final FastqFormat fastqFormat = inData.getMetadata().getFastqFormat();

      // Create the job to run
      final Job job;

      DataFile tfqFile = null;

      // Pre-process paired-end files
      if (inData.getDataFileCount() == 1) {

        // Define input and output files
        final DataFile inFile = inData.getDataFile(0);
        final DataFile outFile = outData.getDataFile(0);
        final List<String> filenames = singletonList(inFile.getName());

        job = createJobConf(conf, dataName, inFile, filenames, READS_FASTQ,
            fastqFormat, outFile);
      } else {

        // Define input and output files
        final DataFile inFile1 = inData.getDataFile(0);
        final DataFile inFile2 = inData.getDataFile(1);
        final DataFile outFile1 = outData.getDataFile(0);
        final DataFile outFile2 = outData.getDataFile(1);
        final List<String> filenames =
            newArrayList(inFile1.getName(), inFile2.getName());

        tfqFile = new DataFile(inFile1.getParent(),
            inFile1.getBasename() + READS_TFQ.getDefaultExtension());

        // Convert FASTQ files to TFQ
        MapReduceUtils.submitAndWaitForJob(
            PairedEndFastqToTfq.convert(conf, inFile1, inFile2, tfqFile,
                getReducerTaskCount()),
            inData.getName(), CommonHadoop.CHECK_COMPLETION_TIME, status,
            COUNTER_GROUP);

        job = createJobConf(conf, dataName, tfqFile, filenames, READS_TFQ,
            fastqFormat, outFile1, outFile2);
      }

      // Submit main job
      MapReduceUtils.submitAndWaitForJob(job, inData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      // Cleanup paired-end
      if (inData.getDataFileCount() > 1) {

        final DataFile outFile1 = outData.getDataFile(0);
        final DataFile outFile2 = outData.getDataFile(1);

        final DataFile tmpDir =
            new DataFile(outFile1.getSource() + TEMP_DIR_SUFFIX);

        final DataFile outTmpFile1 = new DataFile(tmpDir, outFile1.getName());
        final DataFile outTmpFile2 = new DataFile(tmpDir, outFile2.getName());

        // Rename temporary file
        outTmpFile1.renameTo(outFile1);
        outTmpFile2.renameTo(outFile2);

        // Remove TFQ temporary directory
        tmpDir.delete(true);
      }

      return status.createTaskResult();

    } catch (IOException | EoulsanException e) {

      return status.createTaskResult(e,
          "Error while running job: " + e.getMessage());
    }
  }

  /**
   * Create a filter reads job.
   * @param parentConf Hadoop configuration
   * @param dataName data name
   * @param inFile input file in FASTQ or TFQ format
   * @param filenames original input file names
   * @param inputFormat input format (FASTQ or TFQ)
   * @param fastqFormat FASTQ format
   * @param outFiles output files
   * @return a Job object
   * @throws IOException if an error occurs while creating the job
   */
  private Job createJobConf(final Configuration parentConf,
      final String dataName, final DataFile inFile,
      final List<String> filenames, final DataFormat inputFormat,
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
    final Job job = Job.getInstance(jobConf, "Filter reads ("
        + dataName + ", " + Joiner.on(", ").join(filenames) + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopModule.class);

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

    // Set the output format
    job.setOutputFormatClass(FastqOutputFormat.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(0);

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(outFiles[0].getSource()
        + (outFiles.length > 1 ? TEMP_DIR_SUFFIX : "")));

    return job;
  }
}
