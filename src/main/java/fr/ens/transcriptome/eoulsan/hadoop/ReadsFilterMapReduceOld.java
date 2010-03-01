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

package fr.ens.transcriptome.eoulsan.hadoop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.hadoop.filterreads.ValidReadMapper;
import fr.ens.transcriptome.eoulsan.hadoop.mapreads.SOAPCombiner;
import fr.ens.transcriptome.eoulsan.hadoop.mapreads.SOAPMapper;
import fr.ens.transcriptome.eoulsan.hadoop.mapreads.SOAPReducer;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.MapReduceUtils;
import fr.ens.transcriptome.eoulsan.util.PathUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.FileUtils.SuffixFilenameFilter;

public class ReadsFilterMapReduceOld {

  private static final String WORKING_PATH = "hdfs://localhost/user/jourdren";

  private static final String READS_SUBDIR = "reads";
  private static final String FILTER_READS_OUTPUT_DIR_EXTENSION =
      ".filterreads.temp.dir";
  private static final int CHECK_COMPLETION_TIME = 5000;

  public static void main(final String[] args) throws Exception {

    System.out.println(Arrays.toString(args));

    if (args.length != 2) {

      System.err.printf("Usage: %s [generic options] <input> <output>\n",
          ReadsFilterMapReduceOld.class.getSimpleName());

      System.exit(-1);
    }

    // Create the configuration
    Configuration conf = new Configuration();

    // Set readFile
    final File readsFile = new File(args[0]);

    // Set genome sequence file
    final File genomeFile =
        new File("/home/jourdren/tmp/mapreduce/genome/Ca21_chromosomes.fasta");

    // Make SOAP index
    final File genomeIndexFile =
        new File(genomeFile.getParentFile(), StringUtils.basename(genomeFile
            .getName())
            + ".soapindex.zip");
    
    final File tempGenomeIndexFile = SOAPWrapper.makeIndex(genomeFile, true);
    System.out.println("Move "
        + tempGenomeIndexFile.getAbsolutePath() + " to "
        + genomeIndexFile.getAbsolutePath());

    if (!FileUtils.moveFile(tempGenomeIndexFile, genomeIndexFile))
      throw new IOException("Can't move "
          + tempGenomeIndexFile.getAbsolutePath() + " to "
          + genomeIndexFile.getAbsolutePath());

    // Import reads file and genome index file to hdfs
    final Path baseDir =
        importDataToHSDF(new File(args[0]), genomeFile, genomeIndexFile, conf);

    // Set the path on hdfs to genome index
    conf.set(Globals.PARAMETER_PREFIX + ".soap.genomereferencepath",
        new Path(baseDir, "/").toString());

    Path inputDir = new Path(baseDir, READS_SUBDIR);
    Path outputFilteredReads = new Path(baseDir, "outputFilteredReads");
    Path outputUnMapReads = new Path(baseDir, "outputUnmapReads");
    Path outputPath = new Path(baseDir, "output");

    final boolean result = filterReads(conf, inputDir);
    // && mapReads(conf, outputFilteredReads, outputPath, outputUnMapReads);

    System.exit(result ? 0 : 1);
  }

  private static Path importDataToHSDF(final File readsFileOrReadDir,
      final File genomeFile, final File genomeIndexFile,
      final Configuration conf) throws IOException {

    //
    // Check parameters
    //
    FileUtils.checkExistingStandardFileOrDirectory(readsFileOrReadDir,
        "input file/directory");
    FileUtils.checkExistingStandardFile(genomeFile, "genome file");
    FileUtils.checkExistingStandardFile(genomeIndexFile,
        "genome index zip file");

    //
    // Create working directory tree
    //

    final String uri =
        WORKING_PATH
            + "/" + Globals.TEMP_PREFIX
            + StringUtils.currentDateTimeToEasySortedDateTime();

    // Create the filesystem object
    final FileSystem fs = FileSystem.get(URI.create(WORKING_PATH), conf);

    // Create the directory for the process
    final Path basePath = new Path(uri);
    if (!fs.mkdirs(basePath))
      new IOException("Unable to create directory: " + uri);

    // Create the input directory
    final Path readsDirPath = new Path(uri + "/" + READS_SUBDIR);
    if (!fs.mkdirs(readsDirPath))
      new IOException("Unable to create directory: " + uri);

    //
    // Import data in tree
    //

    if (readsFileOrReadDir.isDirectory()) {

      // Copy all file of the input directory
      final File[] listFiles =
          readsFileOrReadDir.listFiles(new SuffixFilenameFilter(".fq"));

      for (File f : listFiles)
        if (!PathUtils.copyLocalFileToPath(f, readsDirPath, conf))
          throw new IOException("Unable to copy to hdfs the  file: "
              + f.getAbsolutePath());
    } else if (!PathUtils.copyLocalFileToPath(readsFileOrReadDir, readsDirPath,
        conf))
      throw new IOException("Unable to copy to hdfs the  file: "
          + readsFileOrReadDir);

    // import genome index file to hdfs
    if (!PathUtils.copyLocalFileToPath(genomeIndexFile, new Path(basePath,
        genomeIndexFile.getName()), conf))
      throw new IOException("Unable to copy to hdfs the  file: "
          + genomeIndexFile);

    // import genome sequence file to hdfs
    if (!FileUtil.copy(genomeFile, fs, new Path(basePath
        + "/" + genomeFile.getName()), false, conf))
      throw new IOException("Unable to copy to hdfs the  file: " + genomeFile);

    return basePath;
  }

  private static final boolean filterReads(final Configuration conf,
      final Path inputDirPath) throws IOException, InterruptedException,
      ClassNotFoundException {

    System.out.println("inputPath=" + inputDirPath);

    final FileSystem fs = PathUtils.getFileSystem(inputDirPath, conf);
    final FileStatus[] filesStatus =
        fs.listStatus(inputDirPath, new PathUtils.SuffixPathFilter(".fq"));

    System.out.println("Status: " + Arrays.toString(filesStatus));

    List<Job> jobs = new ArrayList<Job>();

    // Run the filter
    for (FileStatus fst : filesStatus) {
      System.out.println(fst.getPath());

      final Path inputPath = fst.getPath();
      final Path outputPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              FILTER_READS_OUTPUT_DIR_EXTENSION);

      jobs.add(createFilterReadJob(conf, inputPath, outputPath));
    }

    // Runs the jobs
    MapReduceUtils.submitandWaitForJobs(jobs, CHECK_COMPLETION_TIME);

    // Rename the outputs of map reduce
    for (FileStatus fst : filesStatus) {
      final Path inputPath = fst.getPath();
      final Path outputPath =
          PathUtils.newPathWithOtherExtension(inputPath,
              FILTER_READS_OUTPUT_DIR_EXTENSION);
      final Path resultPath =
          PathUtils.newPathWithOtherExtension(inputPath, ".filter");

      MapReduceUtils.moveMapredOutput(outputPath, resultPath, true, conf);
    }

    return true;
  }

  // private static final boolean filterReads(final Configuration conf,
  // final Path inputPath, final Path outputPath) throws IOException,
  // InterruptedException, ClassNotFoundException {
  //
  // System.out.println("inputPath=" + inputPath);
  //
  // // Create the job and its name
  // Job job = new Job(conf, "Filter reads");
  //
  // // job.setJarByClass(FilterReadsMapReduce.class);
  //
  // // Set the input format
  // job.setInputFormatClass(FastQFormat.class);
  // // job.setInputFormatClass(TextInputFormat.class);
  //
  // // Set the mapper class
  // job.setMapperClass(ValidReadMapper.class);
  //
  // // Set the map key/values types
  // job.setMapOutputKeyClass(Text.class);
  // job.setMapOutputValueClass(Text.class);
  //
  // // Set the inputs files
  // FileSystem fs = FileSystem.get(conf);
  //
  // FileStatus[] status = fs.globStatus(inputPath, new PathFilter() {
  //
  // @Override
  // public boolean accept(Path path) {
  //
  // if (path.getName().endsWith(".fq"))
  // return true;
  //
  // return false;
  // }
  // });
  //
  // System.out.println("Status: " + Arrays.toString(status));
  //
  // for (Path p : FileUtil.stat2Paths(status))
  // FileInputFormat.addInputPath(job, p);
  //
  // // Set the output Path
  // FileOutputFormat.setOutputPath(job, outputPath);
  //
  // // Run Hadoop
  // return job.waitForCompletion(true);
  // }

  private static Job createFilterReadJob(final Configuration conf,
      final Path inputPath, final Path outputPath) throws IOException,
      InterruptedException, ClassNotFoundException {

    final Configuration jobConf = new Configuration(conf);

    // Create the job and its name
    Job job = new Job(jobConf, "Filter reads in " + inputPath.getName());

    // Set the jar of the map reduce classes
    job.setJarByClass(ReadsFilterMapReduceOld.class);

    // Set the input format
    job.setInputFormatClass(FastQFormat.class);
    // job.setInputFormatClass(TextInputFormat.class);

    // Set the mapper class
    job.setMapperClass(ValidReadMapper.class);

    // Set the map key/values types
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    FileInputFormat.addInputPath(job, inputPath);

    // Set the output Path
    FileOutputFormat.setOutputPath(job, outputPath);

    return job;
  }

  private static boolean mapReads(final Configuration conf,
      final Path inputDir, final Path outputPath, final Path unmapFilePath)
      throws IOException, InterruptedException, ClassNotFoundException {

    conf.set(Globals.PARAMETER_PREFIX + ".soap.unmap.path",
        unmapFilePath.toUri().toString());

    // Create the job and its name
    Job job = new Job(conf, "Map reads");

    // Set the jar of the map reduce classes
    job.setJarByClass(ReadsFilterMapReduceOld.class);

    // Set the input format
    // job.setInputFormatClass(KeyValueTextInputFormat.class);

    // Set the mapper class
    job.setMapperClass(SOAPMapper.class);

    // Set the map key/values types
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);

    FileInputFormat.addInputPath(job, inputDir);

    // Set combiner
    job.setCombinerClass(SOAPCombiner.class);

    // Set Reducer
    job.setReducerClass(SOAPReducer.class);

    // Set the output Path
    FileOutputFormat.setOutputPath(job, outputPath);

    // Run Hadoop
    return job.waitForCompletion(true);
  }

}
