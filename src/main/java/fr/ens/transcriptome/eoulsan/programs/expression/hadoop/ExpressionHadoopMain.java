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

package fr.ens.transcriptome.eoulsan.programs.expression.hadoop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.TextInputFormat;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.programs.expression.FinalExpressionTranscriptsCreator;
import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class is the main class for the expression program of the reads in
 * hadoop mode.
 * @author Laurent Jourdren
 */
@SuppressWarnings("deprecation")
public class ExpressionHadoopMain {

  // Configure URL handler for hdfs protocol
  static {
    URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
  }

  private static final String SERIALIZED_DATA_EXTENSION = ".data";

  /**
   * Create JobConf object.
   * @param basePath base path
   * @param sample sample of the job
   * @param parentType parent type
   */
  private static final JobConf createJobConf(final Path basePath,
      final Sample sample, final String parentType) {

    // Create JobConf
    final JobConf conf = new JobConf(ExpressionHadoopMain.class);

    final int sampleId = CommonHadoop.getSampleId(sample);
    final int genomeId =
        CommonHadoop.getSampleId(sample.getMetadata().getGenome());

    final Path inputPath =
        CommonHadoop.selectDirectoryOrFile(new Path(basePath,
            CommonHadoop.SAMPLE_SOAP_ALIGNMENT_PREFIX + sampleId),
            CommonHadoop.SOAP_RESULT_EXTENSION);

    // Set Job name
    conf.setJobName("Expression computation ("
        + sample.getName() + ", " + inputPath.getName() + ")");

    conf.set("mapred.child.java.opts", "-Xmx1024m");

    final Path exonsIndexPath =
        new Path(basePath, CommonHadoop.ANNOTATION_FILE_PREFIX
            + genomeId + SERIALIZED_DATA_EXTENSION);

    try {
      if (!PathUtils.isFile(exonsIndexPath, conf))
        createExonsIndex(new Path(basePath, sample.getMetadata()
            .getAnnotation()), sample.getMetadata().getGenomicType(),
            exonsIndexPath, conf);
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
      return null;
    }

    // Set the path to the exons index
    conf.set(Globals.PARAMETER_PREFIX + ".expression.exonsindex.path",
        exonsIndexPath.toString());

    // Debug
    // conf.set("mapred.job.tracker", "local");

    // Set input path
    FileInputFormat.setInputPaths(conf, inputPath);

    // Set the input format
    conf.setInputFormat(TextInputFormat.class);

    // Set the Mapper class
    conf.setMapperClass(ExpressionMapper.class);

    // Set the reducer class
    conf.setReducerClass(ExpressionReducer.class);

    // Set the output key class
    conf.setOutputKeyClass(Text.class);

    // Set the output value class
    conf.setOutputValueClass(Text.class);

    // Set the number of reducers
    conf.setNumReduceTasks(1);

    // Set output path
    FileOutputFormat.setOutputPath(conf, new Path(basePath,
        CommonHadoop.SAMPLE_EXPRESSION_FILE_PREFIX + sampleId));

    return conf;
  }

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) throws Exception {

    System.out.println("Expression arguments:\t" + Arrays.toString(args));

    if (args == null)
      throw new NullPointerException("The arguments of import data is null");

    if (args.length < 1)
      throw new IllegalArgumentException("Expression need one or two arguments");

    // Set the design path
    final String designPathname = args[0];

    // Set the threshold
    String parentType = null;

    if (args.length > 1)
      parentType = args[1];

    final Path designPath = new Path(designPathname);
    final Path basePath = designPath.getParent();
    Design design = null;

    // Read design file
    try {

      final DesignReader dr =
          new SimpleDesignReader(designPath.toUri().toURL().openStream());

      design = dr.read();

    } catch (IOException e) {
      CommonHadoop.error("Error while reading design file: ", e);
    } catch (EoulsanIOException e) {
      CommonHadoop.error("Error while reading design file: ", e);
    }

    // Create the list of jobs to run
    final Map<Sample, RunningJob> jobsRunning =
        new HashMap<Sample, RunningJob>();

    try {
      final long startTime = System.currentTimeMillis();

      JobClient jc = null;

      for (Sample s : design.getSamples()) {

        final JobConf jconf = createJobConf(basePath, s, parentType);

        if (jc == null)
          jc = new JobClient(jconf);
        jobsRunning.put(s, jc.submitJob(jconf));
      }

      CommonHadoop.writeLog(new Path(basePath, "expression.log"), startTime,
          MapReduceUtils.waitForJobs(jobsRunning.values(),
              CommonHadoop.CHECK_COMPLETION_TIME,
              ExpressionMapper.COUNTER_GROUP));

      createFinalExpressionTranscriptsFile(basePath, jobsRunning,
          new Configuration());

    } catch (IOException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (InterruptedException e) {
      CommonHadoop.error("Error while running job: ", e);
    }

  }

  /**
   * Create exon index.
   * @param gffPath gff path
   * @param expressionType expression type
   * @param exonsIndexPath output exon index path
   * @param conf configuration object
   * @throws IOException if an error occurs while creating the index
   */
  private static final Path createExonsIndex(final Path gffPath,
      final String expressionType, final Path exonsIndexPath,
      final Configuration conf) throws IOException {

    final FileSystem fs = FileSystem.get(conf);
    final FSDataInputStream is = fs.open(gffPath);

    final TranscriptAndExonFinder ef =
        new TranscriptAndExonFinder(is, expressionType);
    final File exonIndexFile =
        FileUtils.createFileInTempDir(StringUtils.basename(gffPath.getName())
            + SERIALIZED_DATA_EXTENSION);
    ef.save(exonIndexFile);

    PathUtils.copyLocalFileToPath(exonIndexFile, exonsIndexPath, conf);
    exonIndexFile.delete();

    return exonsIndexPath;
  }

  private static final void createFinalExpressionTranscriptsFile(
      final Path basePath, final Map<Sample, RunningJob> jobconfs,
      final Configuration conf) throws IOException {

    int lastGenomeId = -1;
    FinalExpressionTranscriptsCreator fetc = null;

    for (Map.Entry<Sample, RunningJob> e : jobconfs.entrySet()) {

      final Sample sample = e.getKey();
      final RunningJob rj = e.getValue();

      final int sampleId = CommonHadoop.getSampleId(sample);
      final int genomeId =
          CommonHadoop.getSampleId(sample.getMetadata().getGenome());
      final long readsUsed =
          rj.getCounters().getGroup(ExpressionMapper.COUNTER_GROUP).getCounter(
              "reads used");

      final FileSystem fs = PathUtils.getFileSystem(basePath, conf);

      // Load the annotation index
      if (genomeId != lastGenomeId) {

        final Path exonsIndexPath =
            new Path(basePath, CommonHadoop.ANNOTATION_FILE_PREFIX
                + genomeId + SERIALIZED_DATA_EXTENSION);

        fetc = new FinalExpressionTranscriptsCreator(fs.open(exonsIndexPath));

        lastGenomeId = genomeId;
      }

      final Path outputDirPath =
          new Path(basePath, CommonHadoop.SAMPLE_EXPRESSION_FILE_PREFIX + sampleId);
      final Path resultPath =
          new Path(basePath, CommonHadoop.SAMPLE_EXPRESSION_FILE_PREFIX
              + sampleId + Common.EXPRESSION_FILE_SUFFIX);

      fetc.initializeExpressionResults();

      for (FileStatus fstatus : fs.listStatus(outputDirPath))
        if (!fstatus.getPath().getName().startsWith("_"))
          fetc.loadPreResults(fs.open(fstatus.getPath()), readsUsed);

      fetc.saveFinalResults(fs.create(resultPath));
    }

  }
}
