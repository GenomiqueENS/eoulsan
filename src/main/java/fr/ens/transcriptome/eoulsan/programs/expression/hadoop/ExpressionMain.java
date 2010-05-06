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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.TextInputFormat;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.hadoop.CommonHadoop;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.programs.expression.GeneAndExonFinder;
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
public class ExpressionMain {

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
    final JobConf conf = new JobConf(ExpressionMain.class);

    final int sampleId = CommonHadoop.getSampleId(sample);
    final int genomeId =
        CommonHadoop.getSampleId(sample.getMetadata().getGenome());

    final Path inputPath =
        CommonHadoop.selectDirectoryOrFile(new Path(basePath,
            CommonHadoop.SOAP_ALIGNMENT_FILE_PREFIX + sampleId),
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

    // Set the parent type
    conf
        .set(Globals.PARAMETER_PREFIX + ".expression.parent.type",
            parentType == null
                ? sample.getMetadata().getGenomicType() : parentType);

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
        CommonHadoop.EXPRESSION_FILE_PREFIX + sampleId));

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
    final List<JobConf> jobconfs =
        new ArrayList<JobConf>(design.getSampleCount());
    for (Sample s : design.getSamples())
      jobconfs.add(createJobConf(basePath, s, parentType));

    try {
      final long startTime = System.currentTimeMillis();
      CommonHadoop.writeLog(new Path(basePath, "expression.log"), startTime,
          MapReduceUtils.submitandWaitForJobs(jobconfs,
              CommonHadoop.CHECK_COMPLETION_TIME,
              ExpressionMapper.COUNTER_GROUP));

    } catch (IOException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (InterruptedException e) {
      CommonHadoop.error("Error while running job: ", e);
    } catch (ClassNotFoundException e) {
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

    final GeneAndExonFinder ef =
        new GeneAndExonFinder(gffPath, conf, expressionType);
    final File exonIndexFile =
        FileUtils.createFileInTempDir(StringUtils.basename(gffPath.getName())
            + SERIALIZED_DATA_EXTENSION);
    ef.save(exonIndexFile);

    PathUtils.copyLocalFileToPath(exonIndexFile, exonsIndexPath, conf);
    exonIndexFile.delete();

    return exonsIndexPath;
  }

}
