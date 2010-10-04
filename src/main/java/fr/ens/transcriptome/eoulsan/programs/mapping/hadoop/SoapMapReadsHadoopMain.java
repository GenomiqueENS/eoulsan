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

package fr.ens.transcriptome.eoulsan.programs.mapping.hadoop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.programs.mapping.MapReadsStep;
import fr.ens.transcriptome.eoulsan.programs.mgmt.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.programs.mgmt.StepResult;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class is the main class for the mapping program of the reads in hadoop
 * mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class SoapMapReadsHadoopMain extends MapReadsStep {

  private static final String UNMAP_CHUNK_PREFIX = "soap-unmap-";

  /**
   * Create the JobConf object for a sample
   * @param basePath base path of data
   * @param sample sample to process
   * @return a new JobConf object
   * @throws IOException if an error occurs while creating input path
   */
  private static JobConf createJobConf(final Path basePath, final Sample sample)
      throws IOException {

    final JobConf conf = new JobConf(FilterReadsHadoopMain.class);

    final int sampleId = sample.getId();
    final int genomeId =
        CommonHadoop.getSampleId(sample.getMetadata().getGenome());

    final Path inputPath =
        CommonHadoop.selectDirectoryOrFile(new Path(basePath,
            CommonHadoop.SAMPLE_FILTERED_PREFIX + sampleId),
            CommonHadoop.FASTQ_EXTENSION);

    // Set Job name
    conf.setJobName("Map reads with SOAP ("
        + sample.getName() + ", " + inputPath.getName() + ")");

    // Set genome reference path
    conf
        .set(Globals.PARAMETER_PREFIX + ".soap.indexzipfilepath", new Path(
            basePath, CommonHadoop.GENOME_SOAP_INDEX_FILE_PREFIX
                + genomeId + CommonHadoop.GENOME_SOAP_INDEX_FILE_SUFFIX)
            .toString());

    // Set unmap chuck dir path
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix.dir",
        new Path(basePath, CommonHadoop.SAMPLE_SOAP_UNMAP_ALIGNMENT_PREFIX
            + sampleId).toString());

    // Set unmap chuck prefix
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.chunk.prefix",
        UNMAP_CHUNK_PREFIX);

    // Set unmap output file path
    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.path", PathUtils
        .newPathWithOtherExtension(new Path(basePath, sample.getSource()),
            CommonHadoop.UNMAP_EXTENSION).toString());

    // Set the number of threads for soap
    conf.set(Globals.PARAMETER_PREFIX + ".soap.nb.threads", "1");

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set the jar
    conf.setJarByClass(SoapMapReadsHadoopMain.class);

    // Set input path

    FileInputFormat.setInputPaths(conf, inputPath);

    // Set the input format
    conf.setInputFormat(TextInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(SoapMapReadsMapper.class);

    // Set the reducer class
    conf.setReducerClass(IdentityReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, new Path(basePath,
        CommonHadoop.SAMPLE_SOAP_ALIGNMENT_PREFIX + sampleId));

    return conf;
  }

  //
  // Step methods
  // 

  @Override
  public String getLogName() {

    return "soapmapreads";
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    try {

      final Path basePath = new Path(info.getBasePathname());

      // Create the list of jobs to run
      final List<JobConf> jobconfs =
          new ArrayList<JobConf>(design.getSampleCount());
      for (Sample s : design.getSamples())
        jobconfs.add(createJobConf(basePath, s));

      final long startTime = System.currentTimeMillis();

      final String log =
          MapReduceUtils.submitAndWaitForRunningJobs(jobconfs,
              CommonHadoop.CHECK_COMPLETION_TIME,
              SoapMapReadsMapper.COUNTER_GROUP);

      return new StepResult(this, startTime, log);

    } catch (IOException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    } catch (InterruptedException e) {

      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    } catch (ClassNotFoundException e) {
      return new StepResult(this, e, "Error while running job: "
          + e.getMessage());
    }

  }

}
