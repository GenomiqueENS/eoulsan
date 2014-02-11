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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.bio.io.hadoop.FastQFormatNew;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.InputPort;
import fr.ens.transcriptome.eoulsan.core.OutputPort;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractReadsFilterStep;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class is the main class for the filter reads program in hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@HadoopOnly
public class ReadsFilterHadoopStep extends AbstractReadsFilterStep {

  //
  // Step methods
  //

  @Override
  public Set<OutputPort> getOutputFormats() {
    return singleOutputPort(READS_TFQ);
  }

  @Override
  public Set<InputPort> getInputFormats() {

    return allPortsRequiredInWorkingDirectory(super.getInputFormats());
  }

  @Override
  public StepResult execute(Design design, final StepContext context,
      final StepStatus status) {

    // Create configuration object
    final Configuration conf = new Configuration();

    try {

      final List<Job> jobsPairedEnd = new ArrayList<Job>();
      for (Sample s : design.getSamples()) {
        if (context.getInputDataFileCount(READS_FASTQ, s) == 2)
          jobsPairedEnd.add(createJobConfPairedEnd(conf, context, s));
      }

      // Submit job paired end if needed
      MapReduceUtils.submitAndWaitForJobs(jobsPairedEnd,
          CommonHadoop.CHECK_COMPLETION_TIME);

      // Create the list of jobs to run
      final Map<Job, Sample> jobs = Maps.newHashMap();
      for (Sample sample : design.getSamples())
        jobs.put(createJobConf(conf, context, sample), sample);

      MapReduceUtils.submitAndWaitForJobs(jobs,
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

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
   * @param basePath base path
   * @param sample Sample to filter
   * @return a JobConf object
   * @throws IOException
   */
  private Job createJobConf(final Configuration parentConf,
      final StepContext context, final Sample sample) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Get input DataFile
    DataFile inputDataFile = null;
    inputDataFile = context.getInputDataFile(READS_TFQ, sample);
    if (inputDataFile == null)
      inputDataFile = context.getInputDataFile(READS_FASTQ, sample);

    if (inputDataFile == null)
      throw new IOException("No input file found.");

    // Setm input path
    final Path inputPath = new Path(inputDataFile.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set fastq format
    jobConf.set(ReadsFilterMapper.FASTQ_FORMAT_KEY, sample.getMetadata()
        .getFastqFormat().getName());

    // Set read filter parameters
    addParametersToJobConf(getReadFilterParameters(),
        READ_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    // Set Job name
    // Create the job and its name
    final Job job =
        new Job(jobConf, "Filter reads ("
            + sample.getName() + ", " + inputDataFile.getSource() + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set the input format
    if (READS_FASTQ.equals(inputDataFile.getDataFormat()))
      job.setInputFormatClass(FastQFormatNew.class);

    // Set the Mapper class
    job.setMapperClass(ReadsFilterMapper.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set the number of reducers
    job.setNumReduceTasks(0);

    // Set output path
    FileOutputFormat.setOutputPath(job,
        new Path(context.getOutputDataFile(DataFormats.READS_TFQ, sample)
            .getSource()));

    return job;
  }

  /**
   * Create a job for the pretreatment step in case of paired-end data.
   * @param basePath base path
   * @param sample Sample to filter
   * @return a JobConf object
   * @throws IOException
   */
  private Job createJobConfPairedEnd(final Configuration parentConf,
      final StepContext context, final Sample sample) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // get input file count for the sample
    final int inFileCount =
        context.getInputDataFileCount(DataFormats.READS_FASTQ, sample);

    if (inFileCount < 1)
      throw new IOException("No input file found.");

    if (inFileCount > 2)
      throw new IOException(
          "Cannot handle more than 2 reads files at the same time.");

    // Get the source
    final DataFile inputDataFile1 =
        context.getInputDataFile(DataFormats.READS_FASTQ, sample, 0);
    final DataFile inputDataFile2 =
        context.getInputDataFile(DataFormats.READS_FASTQ, sample, 1);

    // Set input path
    final Path inputPath1 = new Path(inputDataFile1.getSource());
    final Path inputPath2 = new Path(inputDataFile2.getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set fastq format
    jobConf.set(PreTreatmentMapper.FASTQ_FORMAT_KEY, sample.getMetadata()
        .getFastqFormat().getName());

    // Set Job name
    // Create the job and its name
    final Job job =
        new Job(jobConf, "Pretreatment ("
            + sample.getName() + ", " + inputDataFile1.getSource() + ", "
            + inputDataFile2.getSource() + ")");

    // Set the jar
    job.setJarByClass(ReadsFilterHadoopStep.class);

    // Set input path : paired-end mode so two input files
    FileInputFormat.addInputPath(job, inputPath1);
    FileInputFormat.addInputPath(job, inputPath2);

    // Set the input format
    if (READS_FASTQ.equals(inputDataFile1.getDataFormat())
        && READS_FASTQ.equals(inputDataFile2.getDataFormat()))
      job.setInputFormatClass(FastQFormatNew.class);

    // Set the Mapper class
    job.setMapperClass(PreTreatmentMapper.class);

    // Set the Reducer class
    job.setReducerClass(PreTreatmentReducer.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Output name
    String outputName =
        StringUtils.filenameWithoutExtension(inputPath2.getName());
    outputName = outputName.substring(0, outputName.length() - 1);
    outputName += ".tfq";

    // Set output path
    FileOutputFormat.setOutputPath(job, new Path(inputPath2.getParent(),
        outputName));

    return job;
  }
}
