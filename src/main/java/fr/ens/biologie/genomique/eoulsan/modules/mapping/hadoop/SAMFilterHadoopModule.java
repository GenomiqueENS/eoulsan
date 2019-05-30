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

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.HadoopMappingUtils.addParametersToJobConf;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SAMFilterReducer.MAP_FILTER_PARAMETER_KEY_PREFIX;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.SAMInputFormat;
import fr.ens.biologie.genomique.eoulsan.bio.io.hadoop.SAMOutputFormat;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractSAMFilterModule;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define a filter alignment step in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class SAMFilterHadoopModule extends AbstractSAMFilterModule {

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

      final Data inData = context.getInputData(MAPPER_RESULTS_SAM);
      final Data outData = context.getOutputData(MAPPER_RESULTS_SAM, inData);

      // Create the job to run
      final Job job = createJob(conf, inData, outData);

      // Launch jobs
      MapReduceUtils.submitAndWaitForJob(job, inData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);

      return status.createTaskResult();
    } catch (IOException | EoulsanException e) {

      return status.createTaskResult(e,
          "Error while running job: " + e.getMessage());
    }

  }

  /**
   * Create the JobConf object for a sample
   * @param inData input data
   * @param outData output data
   * @return a new JobConf object
   * @throws IOException if an error occurs creating the job
   */
  private Job createJob(final Configuration parentConf, final Data inData,
      final Data outData) throws IOException {

    final Configuration jobConf = new Configuration(parentConf);

    // Set input path
    final Path inputPath = new Path(inData.getDataFile().getSource());

    // Set counter group
    jobConf.set(CommonHadoop.COUNTER_GROUP_KEY, COUNTER_GROUP);

    // Set SAM filter parameters
    addParametersToJobConf(getAlignmentsFilterParameters(),
        MAP_FILTER_PARAMETER_KEY_PREFIX, jobConf);

    // timeout
    jobConf.set("mapreduce.task.timeout", "" + 30 * 60 * 1000);

    // Create the job and its name
    final Job job = Job.getInstance(jobConf, "Filter SAM file ("
        + inData.getName() + ", " + inputPath.getName() + ")");

    // Set the jar
    job.setJarByClass(ReadsMapperHadoopModule.class);

    // Set input path
    FileInputFormat.addInputPath(job, inputPath);

    // Set input format
    job.setInputFormatClass(SAMInputFormat.class);

    // Set the Mapper class
    job.setMapperClass(SAMFilterMapper.class);

    // Set the reducer class
    job.setReducerClass(SAMFilterReducer.class);

    // Set the reducer task count
    if (getReducerTaskCount() > 0) {
      job.setNumReduceTasks(getReducerTaskCount());
    }

    // job.setPartitionerClass(SAMRecordsPartitioner.class);
    // job.setSortComparatorClass(SAMRecordsKeyComparator.class);
    // job.setGroupingComparatorClass(SAMRecordsGroupComparator.class);

    // Set the output format
    job.setOutputFormatClass(SAMOutputFormat.class);

    // Set the output key class
    job.setOutputKeyClass(Text.class);

    // Set the output value class
    job.setOutputValueClass(Text.class);

    // Set output path
    FileOutputFormat.setOutputPath(job,
        new Path(outData.getDataFile().getSource()));

    return job;
  }

}
