package fr.ens.transcriptome.eoulsan.steps.mapping.hadoop;

import static fr.ens.transcriptome.eoulsan.core.CommonHadoop.createConfiguration;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.allPortsRequiredInWorkingDirectory;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_INDEX_BAI;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import hadoop.org.apache.hadoop.mapreduce.lib.partition.TotalOrderPartitioner;
import hbparquet.hadoop.util.ContextUtil;
import htsjdk.samtools.BAMIndexer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.InputSampler;
import org.seqdoop.hadoop_bam.AnySAMInputFormat;
import org.seqdoop.hadoop_bam.AnySAMOutputFormat;
import org.seqdoop.hadoop_bam.BAMRecordReader;
import org.seqdoop.hadoop_bam.KeyIgnoringAnySAMOutputFormat;
import org.seqdoop.hadoop_bam.SAMFormat;
import org.seqdoop.hadoop_bam.SAMRecordWritable;
import org.seqdoop.hadoop_bam.cli.CLIMergingAnySAMOutputFormat;
import org.seqdoop.hadoop_bam.cli.Utils;
import org.seqdoop.hadoop_bam.util.SAMHeaderReader;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.core.CommonHadoop;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAM2BAMStep;
import fr.ens.transcriptome.eoulsan.util.hadoop.MapReduceUtils;

/**
 * This class define a step for converting SAM files into BAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class SAM2BAMHadoopStep extends AbstractSAM2BAMStep {

  //
  // Step methods
  //

  @Override
  public InputPorts getInputPorts() {

    return allPortsRequiredInWorkingDirectory(super.getInputPorts());
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Create configuration object
    final Configuration conf = createConfiguration();

    // Get input and output data
    final Data samData = context.getInputData(MAPPER_RESULTS_SAM);
    final Data bamData = context.getOutputData(MAPPER_RESULTS_BAM, samData);
    final Data indexData =
        context.getOutputData(MAPPER_RESULTS_INDEX_BAI, samData);

    final Path bamPath = new Path(bamData.getDataFile().toUri());

    final Path workPath =
        new Path(bamPath.getParent(), bamPath.getName() + ".tmp");

    final Job job;
    try {

      // Create the job to run
      job =
          createJobConf(conf, context, samData.getName(),
              samData.getDataFile(), bamData.getDataFile(), workPath);

      // Submit main job
      MapReduceUtils.submitAndWaitForJob(job, samData.getName(),
          CommonHadoop.CHECK_COMPLETION_TIME, status, COUNTER_GROUP);
    } catch (IOException | ClassNotFoundException | InterruptedException
        | EoulsanException e) {
      return status.createStepResult(e);
    }

    try {
      HadoopBamUtils.mergeSAMInto(bamPath, workPath, "", "", SAMFormat.BAM,
          job.getConfiguration(), "sort");
    } catch (IOException e) {
      return status.createStepResult(e);
    }

    try {
      createIndex(conf, bamData.getDataFile(), indexData.getDataFile());
    } catch (IOException e) {
      return status.createStepResult(e);
    }

    return status.createStepResult();
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
  private Job createJobConf(final Configuration conf,
      final StepContext context, final String sampleName,
      final DataFile samFile, final DataFile bamFile, final Path workPath)
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

    final Job job =
        Job.getInstance(conf, "Sam2Bam ("
            + sampleName + ", input file: " + input + ", output file: "
            + workPath + ")");

    job.setJarByClass(SAM2BAMHadoopStep.class);
    job.setMapperClass(Mapper.class);
    job.setReducerClass(SortReducer.class);

    job.setMapOutputKeyClass(LongWritable.class);
    job.setOutputKeyClass(NullWritable.class);
    job.setOutputValueClass(SAMRecordWritable.class);

    job.setInputFormatClass(SortInputFormat.class);
    job.setOutputFormatClass(CLIMergingAnySAMOutputFormat.class);

    // Set the reducer task count
    // if (getReducerTaskCount() > 0) {
    // job.setNumReduceTasks(getReducerTaskCount());
    // }
    // FIXME Do not work when reduce != 1
    job.setNumReduceTasks(1);

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
            job.getConfiguration().setStrings(
                Utils.HEADERMERGER_INPUTS_PROPERTY,
                new String[] { p.toString() });
          }

          context.getLogger().info("add path1: " + p);
        }
      }
    } else {
      FileInputFormat.addInputPath(job, input);
      job.getConfiguration().setStrings(Utils.HEADERMERGER_INPUTS_PROPERTY,
          new String[] { input.toString() });
      context.getLogger().info("add path2: " + input);
    }

    FileOutputFormat.setOutputPath(job, workPath);

    job.setPartitionerClass(TotalOrderPartitioner.class);
    context.getLogger().info(
        Utils.HEADERMERGER_INPUTS_PROPERTY
            + ":"
            + job.getConfiguration().get(Utils.HEADERMERGER_INPUTS_PROPERTY));
    InputSampler.<LongWritable, SAMRecordWritable> writePartitionFile(job,
        new InputSampler.RandomSampler<LongWritable, SAMRecordWritable>(0.01,
            10000, Math.max(100, job.getNumReduceTasks())));

    return job;
  }

  /**
   * Create the BAI index.
   * @param conf the Hadoop configuration
   * @param bamFile the BAM file
   * @param indexFile the BAI file
   * @throws IOException if an error occurs while creating the index
   */
  private void createIndex(final Configuration conf, final DataFile bamFile,
      final DataFile indexFile) throws IOException {

    // TODO Must create mapper task to do the job

    Path input = new Path(bamFile.toUri());
    Path output = new Path(indexFile.toUri());

    // final ValidationStringency stringency =
    // ValidationStringency.DEFAULT_STRINGENCY;

    final SamReader reader;

    // reader = new SAMFileReader(WrapSeekable.openPath(conf, input), false);

    reader =
        SamReaderFactory.makeDefault(). open(
            SamInputResource.of(input.getFileSystem(conf).open(input)));

    final SAMFileHeader header;

    header = reader.getFileHeader();

    final BAMIndexer indexer;

    final Path p = output;
    indexer = new BAMIndexer(p.getFileSystem(conf).create(p), header);

    // Necessary lest the BAMIndexer complain
    // reader.enableFileSource(true);

    final SAMRecordIterator it = reader.iterator();

    while (it.hasNext())
      indexer.processAlignment(it.next());

    indexer.finish();
  }
}

//
// Hadoop-BAM classes
//

final class SortReducer extends
    Reducer<LongWritable, SAMRecordWritable, NullWritable, SAMRecordWritable> {
  @Override
  protected void reduce(
      LongWritable ignored,
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
final class SortInputFormat extends
    FileInputFormat<LongWritable, SAMRecordWritable> {
  private AnySAMInputFormat baseIF = null;

  private void initBaseIF(final Configuration conf) {
    if (baseIF == null)
      baseIF = new AnySAMInputFormat(conf);
  }

  @Override
  public RecordReader<LongWritable, SAMRecordWritable> createRecordReader(
      InputSplit split, TaskAttemptContext ctx) throws InterruptedException,
      IOException {
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

final class SortRecordReader extends
    RecordReader<LongWritable, SAMRecordWritable> {
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
  public SAMRecordWritable getCurrentValue() throws InterruptedException,
      IOException {
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
}
