package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractSAM2BAMModule;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

/**
 * This class define a module for converting SAM files into BAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class SAM2BAMLocalModule extends AbstractSAM2BAMModule {

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      // Get input SAM data
      final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_SAM);

      // Get output BAM data
      final Data outBAMData =
          context.getOutputData(DataFormats.MAPPER_RESULTS_BAM, inData);

      // Get output BAM data
      final Data outBAIData =
          context.getOutputData(DataFormats.MAPPER_RESULTS_INDEX_BAI, inData);

      final DataFile samFile = inData.getDataFile();
      final DataFile bamFile = outBAMData.getDataFile();
      final DataFile bamIndexFile = outBAIData.getDataFile();

      convert(samFile, bamFile, bamIndexFile, getCompressionLevel(),
          getMaxRecordsInRam(), reporter, context.getLocalTempDirectory());

      // Set the description of the context
      status.setDescription("Convert alignments ("
          + inData.getName() + ", " + samFile.getName() + ", "
          + bamFile.getName() + "," + bamIndexFile.getName() + ")");

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      return status.createTaskResult();

    } catch (final IOException e) {

      return status.createTaskResult(e);
    }
  }

  /**
   * Convert SAM file to sorted BAM with Picard
   * @param samDataFile input SAM file
   * @param bamDataFile output SAM file
   * @param bamIndexDataFile output index file
   * @param compressionLevel compression level
   * @param maxRecordsInRam the maximum records in RAM
   * @param reporter reporter
   * @param tmpDir temporary directory
   * @throws IOException if an error occurs
   */
  private static void convert(final DataFile samDataFile,
      final DataFile bamDataFile, final DataFile bamIndexDataFile,
      final int compressionLevel, final int maxRecordsInRam,
      final Reporter reporter, final File tmpDir) throws IOException {

    checkArgument(compressionLevel >= 0 && compressionLevel <= 9,
        "Invalid compression level [0-9]: " + compressionLevel);

    // Open sam file
    final SamReader samReader = SamReaderFactory.makeDefault()
        .open(SamInputResource.of(samDataFile.open()));

    // Force sort
    samReader.getFileHeader().setSortOrder(SortOrder.coordinate);

    // Get Bam file
    final File bamFile = bamDataFile.toFile();

    // Open Bam file
    final SAMFileWriter samWriter =
        new SAMFileWriterFactory().setCreateIndex(true).setTempDirectory(tmpDir)
            .setMaxRecordsInRam(maxRecordsInRam).makeBAMWriter(
                samReader.getFileHeader(), false, bamFile, compressionLevel);

    for (final SAMRecord samRecord : samReader) {
      samWriter.addAlignment(samRecord);
      reporter.incrCounter(COUNTER_GROUP, "sorted records", 1);
    }

    samReader.close();
    samWriter.close();

    // Rename index bai file
    final String createdBamIndexFilename =
        bamDataFile.getName().substring(0, bamDataFile.getName().length() - 1)
            + "i";
    final File createdBamIndexFile =
        new File(bamDataFile.toFile().getParentFile(), createdBamIndexFilename);

    if (!createdBamIndexFile.renameTo(bamIndexDataFile.toFile())) {
      EoulsanLogger.getLogger().warning("Unable to rename the BAI file "
          + createdBamIndexFile + " to " + bamIndexDataFile.toFile());
    }

    // Create a symbolic links
    bamIndexDataFile.symlink(new DataFile(createdBamIndexFile), true);
    bamIndexDataFile.symlink(
        new DataFile(bamDataFile.getParent(), bamDataFile.getName() + ".bai"),
        true);
  }

}
