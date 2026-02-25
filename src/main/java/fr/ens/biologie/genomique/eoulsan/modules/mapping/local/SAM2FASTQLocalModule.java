package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import com.google.common.base.Splitter;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractSAM2FASTQModule;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqWriter;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * This class define a module for converting SAM files into FASTQ.
 *
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class SAM2FASTQLocalModule extends AbstractSAM2FASTQModule {

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    try {

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      // Get input SAM data
      final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_SAM);

      // Get input SAM TMP data
      File samTmpFile = File.createTempFile("samTmp", ".sam", context.getLocalTempDirectory());

      // Get output FASTQ data
      final Data outData = context.getOutputData(DataFormats.READS_FASTQ, inData);

      final DataFile samFile = inData.getDataFile();

      final int paired =
          sortConvert(samFile, samTmpFile, reporter, context.getLocalTempDirectory());

      final DataFile fastqFile1 = outData.getDataFile(0);
      final DataFile fastqFile2 = paired == 3 ? outData.getDataFile(1) : null;

      writeConvert(samTmpFile, fastqFile1, fastqFile2, reporter);

      // Set the description of the context
      status.setDescription(
          "Convert alignments (" + inData.getName() + "," + outData.getName() + ")");

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      return status.createTaskResult();

    } catch (final IOException e) {

      return status.createTaskResult(e);
    }
  }

  /**
   * Convert SAM file to FASTQ
   *
   * @param samDataFile input SAM file
   * @param fastqDataFile1 output FASTQ file 1
   * @param fastqDataFile2 output FASTQ file 2
   * @param reporter reporter
   * @throws IOException if an error occurs
   */
  private static void writeConvert(
      final File samDataFile,
      final DataFile fastqDataFile1,
      final DataFile fastqDataFile2,
      final Reporter reporter)
      throws IOException {

    // Open sam file
    final SamReader samReader =
        SamReaderFactory.makeDefault()
            .open(SamInputResource.of(Files.newInputStream(samDataFile.toPath())));

    // Open fastq file
    final FastqWriter fastqWriter1 = new FastqWriter(fastqDataFile1.create());
    final FastqWriter fastqWriter2 =
        fastqDataFile2 == null ? null : new FastqWriter(fastqDataFile2.create());
    String seq1 = null;
    String seq2 = null;
    String qual1 = null;
    String qual2 = null;
    String currentRecordId = null;

    for (final SAMRecord samRecord : samReader) {
      if (currentRecordId != null && !currentRecordId.equals(samRecord.getReadName())) {

        reporter.incrCounter(COUNTER_GROUP, "sorted records", 1);

        writeFastq(fastqWriter1, fastqWriter2, currentRecordId, seq1, qual1, seq2, qual2);
        seq1 = seq2 = qual1 = qual2 = null;
      }
      if (samRecord.getReadPairedFlag() && !samRecord.getFirstOfPairFlag()) {
        seq2 = samRecord.getReadString();
        qual2 = samRecord.getBaseQualityString();
      } else {
        seq1 = samRecord.getReadString();
        qual1 = samRecord.getBaseQualityString();
      }

      currentRecordId = samRecord.getReadName();
    }

    if (seq1 != null && seq2 != null) {
      reporter.incrCounter(COUNTER_GROUP, "sorted records", 1);
      writeFastq(fastqWriter1, fastqWriter2, currentRecordId, seq1, qual1, seq2, qual2);
    }
    samReader.close();
    fastqWriter1.close();

    if (fastqWriter2 != null) {
      fastqWriter2.close();
    }
  }

  private static int sortConvert(
      final DataFile samDataFile, final File samFileTmp, final Reporter reporter, final File tmpDir)
      throws IOException {

    // Open sam file
    final SamReader samReader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(samDataFile.open()));

    // Force sort
    samReader.getFileHeader().setSortOrder(SortOrder.queryname);

    // Open sam file
    final SAMFileWriter samWriter =
        new SAMFileWriterFactory()
            .setCreateIndex(false)
            .setTempDirectory(tmpDir)
            .makeSAMWriter(samReader.getFileHeader(), false, samFileTmp);

    boolean firstPair = false;
    boolean secondPair = false;
    for (final SAMRecord samRecord : samReader) {
      if (!firstPair && samRecord.getReadPairedFlag() && samRecord.getFirstOfPairFlag()) {
        firstPair = true;
      }
      if (!secondPair && samRecord.getReadPairedFlag() && samRecord.getSecondOfPairFlag()) {
        secondPair = true;
      }
      samRecord.setReadName(Splitter.on(' ').splitToList(samRecord.getReadName()).get(0));
      samWriter.addAlignment(samRecord);
      reporter.incrCounter(COUNTER_GROUP, "converted records", 1);
    }
    samWriter.close();
    samReader.close();

    int result = 0;
    if (!firstPair && !secondPair) {
      result = 0;
    }
    if (firstPair && !secondPair) {
      result = 1;
    }
    if (!firstPair && secondPair) {
      result = 2;
    }
    if (firstPair && secondPair) {
      result = 3;
    }

    return result;
  }

  private static void writeFastq(
      FastqWriter fastqWriter1,
      FastqWriter fastqWriter2,
      String currentRecordId,
      String seq1,
      String qual1,
      String seq2,
      String qual2)
      throws IOException {

    ReadSequence read1 = seq1 == null ? null : new ReadSequence(currentRecordId, seq1, qual1);
    ReadSequence read2 = seq2 == null ? null : new ReadSequence(currentRecordId, seq2, qual2);

    if (fastqWriter2 != null) {
      if (seq1 != null && seq2 != null) {
        fastqWriter1.write(read1);
        fastqWriter2.write(read2);
      }
    } else {
      if (seq1 != null) {
        fastqWriter1.write(read1);
      } else {
        fastqWriter1.write(read2);
      }
    }
  }
}
