package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import static fr.ens.biologie.genomique.eoulsan.CommonHadoop.createConfiguration;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_INDEX_BAI;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.InputSampler;
import org.apache.hadoop.mapreduce.lib.partition.TotalOrderPartitioner;
import org.seqdoop.hadoop_bam.AnySAMInputFormat;
import org.seqdoop.hadoop_bam.AnySAMOutputFormat;
import org.seqdoop.hadoop_bam.BAMRecordReader;
import org.seqdoop.hadoop_bam.KeyIgnoringAnySAMOutputFormat;
import org.seqdoop.hadoop_bam.SAMFormat;
import org.seqdoop.hadoop_bam.SAMRecordWritable;
import org.seqdoop.hadoop_bam.util.SAMHeaderReader;

import fr.ens.biologie.genomique.eoulsan.CommonHadoop;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractSAM2BAMModule;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.SortRecordReader.IndexerMapper;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.hadoopbamcli.CLIMergingAnySAMOutputFormat;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.hadoopbamcli.ContextUtil;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop.hadoopbamcli.Utils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopJobEmergencyStopTask;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.MapReduceUtils;
import htsjdk.samtools.BAMIndexer;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

/**
 * This class define a module for converting SAM files into BAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class SAM2BAMHadoopModule extends AbstractSAM2BAMModule {

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

    // Get input and output data
    final Data samData = context.getInputData(MAPPER_RESULTS_SAM);
    final Data bamData = context.getOutputData(MAPPER_RESULTS_BAM, samData);
    final Data indexData =
        context.getOutputData(MAPPER_RESULTS_INDEX_BAI, samData);

    // Get input and output files
    final DataFile samFile = samData.getDataFile();
    final DataFile bamFile = bamData.getDataFile();
    final DataFile indexFile = indexData.getDataFile();

    final Path bamPath = new Path(bamFile.toUri());

    final Path workPath =
        new Path(bamPath.getParent(), bamPath.getName() + ".tmp");

    final Job job;
    try {

      // Create the job to run
      job = createJobConf(conf, context, samData.getName(), samFile, bamFile,
          workPath);

      // Submit main job
      MapReduceUtils.submitAndWaitForJob(job, samData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);
    } catch (IOException | ClassNotFoundException | InterruptedException
        | EoulsanException e) {
      return status.createTaskResult(e);
    }

    try {
      HadoopBamUtils.mergeSAMInto(bamPath, workPath, "", "", SAMFormat.BAM,
          job.getConfiguration(), "sort");
    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    // Create Indexing Hadoop job
    try {

      // Create the indexer submit file
      final DataFile indexerSubmitFile = createSubmitFile(bamFile, indexFile);

      // Create the indexer job
      final Job indexingJob = createIndexJob(conf, indexerSubmitFile,
          "Create " + indexFile + " index file");

      // Submit the Hadoop job
      indexingJob.submit();

      // Add the Hadoop job to the list of job to kill if workflow fails
      HadoopJobEmergencyStopTask.addHadoopJobEmergencyStopTask(indexingJob);

      // Submit the job to the Hadoop scheduler, and wait the end of the job
      // in non verbose mode
      indexingJob.waitForCompletion(false);

      // Removes the Hadoop job to the list of job to kill if workflow fails
      HadoopJobEmergencyStopTask.removeHadoopJobEmergencyStopTask(indexingJob);

      if (!indexingJob.isSuccessful()) {
        throw new IOException("Error while running Hadoop job for creating "
            + indexFile + " index file");
      }

      // Delete the indexer submit file
      indexerSubmitFile.delete();

    } catch (IOException | ClassNotFoundException | InterruptedException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }

  /**
   * Create the sam2bam job.
   * @param conf Hadoop configuration
   * @param context Step context
   * @param sampleName sample sample
   * @param samFile SAM file
   * @param bamFile BAM file
   * @param workPath work path
   * @return an Hadoop Job instance
   * @throws IOException if an error occurs while creating the job
   * @throws ClassNotFoundException if an error occurs while creating the job
   * @throws InterruptedException if an error occurs while creating the job
   */
  private Job createJobConf(final Configuration conf, final TaskContext context,
      final String sampleName, final DataFile samFile, final DataFile bamFile,
      final Path workPath)
      throws IOException, ClassNotFoundException, InterruptedException {

    final ValidationStringency stringency =
        ValidationStringency.DEFAULT_STRINGENCY;

    Path input = new Path(samFile.toUri());
    Path output = new Path(bamFile.toUri());

    context.getLogger().info("Input SAM path: " + input);
    context.getLogger().info("Output BAM path: " + output);
    context.getLogger().info("Working path: " + workPath);

    Utils.setHeaderMergerSortOrder(conf, SortOrder.coordinate);

    if (stringency != null)
      conf.set(SAMHeaderReader.VALIDATION_STRINGENCY_PROPERTY,
          stringency.toString());

    // Used by Utils.getMergeableWorkFile() to name the output files.
    final String intermediateOutName = output.getName();
    conf.set(Utils.WORK_FILENAME_PROPERTY, intermediateOutName);

    conf.set(AnySAMOutputFormat.OUTPUT_SAM_FORMAT_PROPERTY,
        SAMFormat.BAM.toString());
    conf.set(AnySAMInputFormat.TRUST_EXTS_PROPERTY, "true");
    conf.set(KeyIgnoringAnySAMOutputFormat.WRITE_HEADER_PROPERTY, "false");

    Utils.configureSampling(workPath, intermediateOutName, conf);

    final Job job = Job.getInstance(conf,
        "Sam2Bam ("
            + sampleName + ", input file: " + input + ", output file: "
            + workPath + ")");

    job.setJarByClass(SAM2BAMHadoopModule.class);
    job.setMapperClass(Mapper.class);
    job.setReducerClass(SortReducer.class);

    job.setMapOutputKeyClass(LongWritable.class);
    job.setOutputKeyClass(NullWritable.class);
    job.setOutputValueClass(SAMRecordWritable.class);

    job.setInputFormatClass(SortInputFormat.class);
    job.setOutputFormatClass(CLIMergingAnySAMOutputFormat.class);

    // Set the reducer task count
    if (getReducerTaskCount() > 0) {
      job.setNumReduceTasks(getReducerTaskCount());
    }

    // Set input paths
    FileSystem fs = input.getFileSystem(conf);
    final FileStatus status = fs.getFileStatus(input);

    if (status.isDirectory()) {

      boolean first = true;
      for (FileStatus status2 : fs.listStatus(input)) {

        Path p = status2.getPath();
        if (!p.getName().startsWith("_")) {
          FileInputFormat.addInputPath(job, p);

          if (first) {
            job.getConfiguration()
                .setStrings(Utils.HEADERMERGER_INPUTS_PROPERTY, p.toString());
          }

          context.getLogger().info("add path1: " + p);
        }
      }
    } else {
      FileInputFormat.addInputPath(job, input);
      job.getConfiguration().setStrings(Utils.HEADERMERGER_INPUTS_PROPERTY,
          input.toString());
      context.getLogger().info("add path2: " + input);
    }

    FileOutputFormat.setOutputPath(job, workPath);

    job.setPartitionerClass(TotalOrderPartitioner.class);
    context.getLogger().info(Utils.HEADERMERGER_INPUTS_PROPERTY
        + ":" + job.getConfiguration().get(Utils.HEADERMERGER_INPUTS_PROPERTY));

    InputSampler.writePartitionFile(job,
        new InputSampler.RandomSampler<LongWritable, SAMRecordWritable>(0.01,
            10000, Math.max(100, job.getNumReduceTasks())));

    return job;
  }

  private DataFile createSubmitFile(final DataFile bamFile,
      final DataFile indexFile) throws IOException {

    DataFile out = new DataFile(indexFile.getParent(),
        indexFile.getName() + ".submitfile");

    Writer writer = new OutputStreamWriter(out.create(), Charset.defaultCharset());
    writer.write(bamFile.getSource() + '\t' + indexFile.getSource());
    writer.close();

    return out;
  }

  /**
   * Create the index Hadoop job.
   * @param conf the Hadoop configuration
   * @param submitFile the path to the submit file
   * @param jobDescription the job description
   * @return a Job object
   * @throws IOException if an error occurs while creating the index
   */
  private Job createIndexJob(final Configuration conf,
      final DataFile submitFile, final String jobDescription)
      throws IOException {

    final Configuration jobConf = new Configuration(conf);

    // Set one task per map
    jobConf.set("mapreduce.input.lineinputformat.linespermap", "" + 1);

    // Set Job name
    // Create the job and its name
    final Job job = Job.getInstance(jobConf, jobDescription);

    // Set the jar
    job.setJarByClass(IndexerMapper.class);

    // Set input path
    FileInputFormat.addInputPath(job, new Path(submitFile.getSource()));

    job.setInputFormatClass(NLineInputFormat.class);

    // Set the Mapper class
    job.setMapperClass(IndexerMapper.class);

    // Set the output key class
    job.setOutputKeyClass(NullWritable.class);

    // Set the output value class
    job.setOutputValueClass(NullWritable.class);

    // Set the output format
    job.setOutputFormatClass(NullOutputFormat.class);

    // Set the number of reducers
    job.setNumReduceTasks(0);

    return job;
  }

  /**
   * Create the BAI index.
   * @param conf the Hadoop configuration
   * @param bamFile the BAM file
   * @param indexFile the BAI file
   * @throws IOException if an error occurs while creating the index
   */
  static void createIndex(final Configuration conf, final Path bamFile,
      final Path indexFile) throws IOException {

    final InputStream in = FileSystem.get(conf).open(bamFile);

    final SamReader reader = SamReaderFactory.makeDefault()
        .enable(SamReaderFactory.Option.INCLUDE_SOURCE_IN_RECORDS)
        .validationStringency(ValidationStringency.DEFAULT_STRINGENCY)
        .open(SamInputResource.of(in));

    final BAMIndexer indexer =
        new BAMIndexer(indexFile.getFileSystem(conf).create(indexFile),
            reader.getFileHeader());

    for (SAMRecord rec : reader) {
      indexer.processAlignment(rec);

    }

    indexer.finish();
  }

}

//
// Hadoop-BAM classes
//

final class SortReducer extends
    Reducer<LongWritable, SAMRecordWritable, NullWritable, SAMRecordWritable> {
  @Override
  protected void reduce(LongWritable ignored,
      Iterable<SAMRecordWritable> records,
      Reducer<LongWritable, SAMRecordWritable, NullWritable, SAMRecordWritable>.Context ctx)
      throws IOException, InterruptedException {
    for (SAMRecordWritable rec : records)
      ctx.write(NullWritable.get(), rec);
  }
}

// Because we want a total order and we may change the key when merging
// headers, we can't use a mapper here: the InputSampler reads directly from
// the InputFormat.
final class SortInputFormat
    extends FileInputFormat<LongWritable, SAMRecordWritable> {
  private AnySAMInputFormat baseIF = null;

  private void initBaseIF(final Configuration conf) {
    if (baseIF == null)
      baseIF = new AnySAMInputFormat(conf);
  }

  @Override
  public RecordReader<LongWritable, SAMRecordWritable> createRecordReader(
      InputSplit split, TaskAttemptContext ctx)
      throws InterruptedException, IOException {
    initBaseIF(ContextUtil.getConfiguration(ctx));

    final RecordReader<LongWritable, SAMRecordWritable> rr =
        new SortRecordReader(baseIF.createRecordReader(split, ctx));
    rr.initialize(split, ctx);
    return rr;
  }

  @Override
  protected boolean isSplitable(JobContext job, Path path) {
    initBaseIF(ContextUtil.getConfiguration(job));
    return baseIF.isSplitable(job, path);
  }

  @Override
  public List<InputSplit> getSplits(JobContext job) throws IOException {
    initBaseIF(ContextUtil.getConfiguration(job));
    return baseIF.getSplits(job);
  }
}

final class SortRecordReader
    extends RecordReader<LongWritable, SAMRecordWritable> {
  private final RecordReader<LongWritable, SAMRecordWritable> baseRR;

  private Configuration conf;

  public SortRecordReader(RecordReader<LongWritable, SAMRecordWritable> rr) {
    baseRR = rr;
  }

  @Override
  public void initialize(InputSplit spl, TaskAttemptContext ctx)
      throws InterruptedException, IOException {
    conf = ContextUtil.getConfiguration(ctx);
  }

  @Override
  public void close() throws IOException {
    baseRR.close();
  }

  @Override
  public float getProgress() throws InterruptedException, IOException {
    return baseRR.getProgress();
  }

  @Override
  public LongWritable getCurrentKey() throws InterruptedException, IOException {
    return baseRR.getCurrentKey();
  }

  @Override
  public SAMRecordWritable getCurrentValue()
      throws InterruptedException, IOException {
    return baseRR.getCurrentValue();
  }

  @Override
  public boolean nextKeyValue() throws InterruptedException, IOException {
    if (!baseRR.nextKeyValue())
      return false;

    final SAMRecord rec = getCurrentValue().get();

    final int ri = rec.getReferenceIndex();

    Utils.correctSAMRecordForMerging(rec, conf);

    if (rec.getReferenceIndex() != ri)
      getCurrentKey().set(BAMRecordReader.getKey(rec));

    return true;
  }

  //
  // Index creation map reduces classes
  //

  /**
   * This class define the mapper that index a BAM file.
   * @author Laurent Jourdren
   */
  public static final class IndexerMapper
      extends Mapper<LongWritable, Text, NullWritable, NullWritable> {

    @Override
    protected void map(final LongWritable key, final Text value,
        final Context context) throws IOException, InterruptedException {

      final String[] files = value.toString().split("\t");

      if (files.length != 2) {
        throw new IOException("Invalid arguments: " + value);
      }

      final Path bamFile = new Path(files[0]);
      final Path indexFile = new Path(files[1]);

      // Create index
      SAM2BAMHadoopModule.createIndex(context.getConfiguration(), bamFile,
          indexFile);
    }
  }

}
