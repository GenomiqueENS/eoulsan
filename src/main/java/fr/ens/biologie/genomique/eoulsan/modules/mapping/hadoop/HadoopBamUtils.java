package fr.ens.biologie.genomique.eoulsan.modules.mapping.hadoop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.partition.TotalOrderPartitioner;
import org.seqdoop.hadoop_bam.KeyIgnoringAnySAMOutputFormat;
import org.seqdoop.hadoop_bam.SAMFormat;
import org.seqdoop.hadoop_bam.SAMRecordWritable;
import org.seqdoop.hadoop_bam.util.SAMOutputPreparer;

import htsjdk.samtools.ReservedTagConstants;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamFileHeaderMerger;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.BlockCompressedStreamConstants;

public class HadoopBamUtils {

  private static final String HEADERMERGER_SORTORDER_PROP =
      "hadoopbam.headermerger.sortorder";

  static final String HEADERMERGER_INPUTS_PROPERTY =
      "hadoopbam.headermerger.inputs";
  static final String WORK_FILENAME_PROPERTY = "hadoopbam.work.filename";

  private static SamFileHeaderMerger headerMerger = null;

  /**
   * Computes the merger of the SAM headers in the files listed in
   * HEADERMERGER_INPUTS_PROPERTY. The sort order of the result is set according
   * to the last call to setHeaderMergerSortOrder, or otherwise to "unsorted".
   * The result is cached locally to prevent it from being recomputed too often.
   */
  public static SamFileHeaderMerger getSAMHeaderMerger(Configuration conf)
      throws IOException {
    // TODO: it would be preferable to cache this beforehand instead of
    // having every task read the header block of every input file. But that
    // would be trickier, given that SamFileHeaderMerger isn't trivially
    // serializable.

    final List<SAMFileHeader> headers = new ArrayList<>();

    for (final String in : conf.getStrings(HEADERMERGER_INPUTS_PROPERTY)) {
      final Path p = new Path(in);

      final SamReader r = SamReaderFactory.makeDefault()
          .open(SamInputResource.of(p.getFileSystem(conf).open(p)));
      headers.add(r.getFileHeader());
      r.close();
    }

    final String orderStr = conf.get(HEADERMERGER_SORTORDER_PROP);
    final SAMFileHeader.SortOrder order = orderStr == null
        ? SAMFileHeader.SortOrder.unsorted
        : SAMFileHeader.SortOrder.valueOf(orderStr);

    return new SamFileHeaderMerger(order, headers, true);
  }

  /**
   * Merges the files in the given directory that have names given by
   * getMergeableWorkFile() into out in the given SAMFormat, using
   * getSAMHeaderMerger().getMergedHeader() as the header. Outputs progress
   * reports if commandName is non-null.
   */
  public static void mergeSAMInto(Path out, Path directory, String basePrefix,
      String basePostfix, SAMFormat format, Configuration conf,
      String commandName) throws IOException {

    final OutputStream outs = out.getFileSystem(conf).create(out);

    // First, place the SAM or BAM header.
    //
    // Don't use the returned stream, because we're concatenating directly
    // and don't want to apply another layer of compression to BAM.
    new SAMOutputPreparer().prepareForRecords(outs, format,
        getSAMHeaderMerger(conf).getMergedHeader());

    // Then, the actual SAM or BAM contents.
    mergeInto(outs, directory, basePrefix, basePostfix, conf, commandName);

    // And if BAM, the BGZF terminator.
    if (format == SAMFormat.BAM)
      outs.write(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);

    outs.close();
  }

  /**
   * Merges the files in the given directory that have names given by
   * getMergeableWorkFile() into out. Outputs progress reports if commandName is
   * non-null.
   */
  public static void mergeInto(OutputStream out, Path directory,
      String basePrefix, String basePostfix, Configuration conf,
      String commandName) throws IOException {
    final FileSystem fs = directory.getFileSystem(conf);

    final FileStatus[] parts = fs.globStatus(new Path(directory,
        basePrefix
            + conf.get(WORK_FILENAME_PROPERTY) + basePostfix
            + "-[0-9][0-9][0-9][0-9][0-9][0-9]*"));

    for (final FileStatus part : parts) {

      final InputStream in = fs.open(part.getPath());
      IOUtils.copyBytes(in, out, conf, false);
      in.close();
    }

    for (final FileStatus part : parts) {
      fs.delete(part.getPath(), false);
    }
  }

  //
  // Missing methods from org.seqdoop.hadoop_bam.cli
  //

  // Like a KeyIgnoringAnySAMOutputFormat<K>, but sets the SAMFileHeader to
  // Utils.getSAMHeaderMerger().getMergedHeader() and allows the output
  // directory
  // (the "work directory") to exist.
  public class CLIMergingAnySAMOutputFormat<K>
      extends FileOutputFormat<K, SAMRecordWritable> {
    private KeyIgnoringAnySAMOutputFormat<K> baseOF;

    private void initBaseOF(Configuration conf) {
      if (baseOF == null)
        baseOF = new KeyIgnoringAnySAMOutputFormat<K>(conf);
    }

    @Override
    public RecordWriter<K, SAMRecordWritable> getRecordWriter(
        TaskAttemptContext context) throws IOException {
      initBaseOF(getConfiguration(context));

      if (baseOF.getSAMHeader() == null)
        baseOF.setSAMHeader(
            getSAMHeaderMerger(getConfiguration(context)).getMergedHeader());

      return baseOF.getRecordWriter(context, getDefaultWorkFile(context, ""));
    }

    @Override
    public Path getDefaultWorkFile(TaskAttemptContext ctx, String ext)
        throws IOException {
      initBaseOF(getConfiguration(ctx));
      return getMergeableWorkFile(
          baseOF.getDefaultWorkFile(ctx, ext).getParent(), "", "", ctx, ext);
    }

    // Allow the output directory to exist.
    @Override
    public void checkOutputSpecs(JobContext job) {
    }
  }

  public static void setHeaderMergerSortOrder(Configuration conf,
      SAMFileHeader.SortOrder order) {
    conf.set(HEADERMERGER_SORTORDER_PROP, order.name());
  }

  @SuppressWarnings("deprecation")
  public static void configureSampling(Path workDir, String outName,
      Configuration conf) throws IOException {
    final Path partition = workDir.getFileSystem(conf)
        .makeQualified(new Path(workDir, "_partitioning" + outName));

    TotalOrderPartitioner.setPartitionFile(conf, partition);
    try {
      final URI partitionURI =
          new URI(partition.toString() + "#" + partition.getName());

      if (partitionURI.getScheme().equals("file"))
        return;

      org.apache.hadoop.mapreduce.filecache.DistributedCache
          .addCacheFile(partitionURI, conf);
      org.apache.hadoop.mapreduce.filecache.DistributedCache
          .createSymlink(conf);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path getMergeableWorkFile(Path directory, String basePrefix,
      String basePostfix, TaskAttemptContext ctx, String extension) {
    return new Path(directory, basePrefix
        + getConfiguration(ctx).get(WORK_FILENAME_PROPERTY) + basePostfix + "-"
        + String.format("%06d", ctx.getTaskAttemptID().getTaskID().getId())
        + (extension.isEmpty() ? extension : "." + extension));
  }

  /**
   * Changes the given SAMRecord as appropriate for being placed in a file whose
   * header is getSAMHeaderMerger(conf).getMergedHeader().
   */
  public static void correctSAMRecordForMerging(SAMRecord r, Configuration conf)
      throws IOException {
    if (headerMerger == null)
      getSAMHeaderMerger(conf);

    final SAMFileHeader h = r.getHeader();

    // Correct the reference indices, and thus the key, if necessary.
    if (headerMerger.hasMergedSequenceDictionary()) {
      final int ri =
          headerMerger.getMergedSequenceIndex(h, r.getReferenceIndex());

      r.setReferenceIndex(ri);
      if (r.getReadPairedFlag())
        r.setMateReferenceIndex(
            headerMerger.getMergedSequenceIndex(h, r.getMateReferenceIndex()));
    }

    // Correct the program group if necessary.
    if (headerMerger.hasProgramGroupCollisions()) {
      final String pg =
          (String) r.getAttribute(ReservedTagConstants.PROGRAM_GROUP_ID);
      if (pg != null)
        r.setAttribute(ReservedTagConstants.PROGRAM_GROUP_ID,
            headerMerger.getProgramGroupId(h, pg));
    }

    // Correct the read group if necessary.
    if (headerMerger.hasReadGroupCollisions()) {
      final String rg =
          (String) r.getAttribute(ReservedTagConstants.READ_GROUP_ID);
      if (rg != null)
        r.setAttribute(ReservedTagConstants.READ_GROUP_ID,
            headerMerger.getProgramGroupId(h, rg));
    }
  }

  public static Configuration getConfiguration(JobContext context) {

    return context.getConfiguration();
  }

}
