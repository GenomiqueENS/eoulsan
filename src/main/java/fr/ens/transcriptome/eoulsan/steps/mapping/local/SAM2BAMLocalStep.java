package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;

import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.mapping.AbstractSAM2BAMStep;
import fr.ens.transcriptome.eoulsan.util.LocalReporter;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class define a step for converting SAM files into BAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class SAM2BAMLocalStep extends AbstractSAM2BAMStep {

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

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

      convert(samFile, bamFile, bamIndexFile, getCompressionLevel(), reporter,
          context.getLocalTempDirectory());

      // Set the description of the context
      status.setDescription("Convert alignments ("
          + inData.getName() + ", " + samFile.getName() + ", "
          + bamFile.getName() + "," + bamIndexFile.getName() + ")");

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      return status.createStepResult();

    } catch (final IOException e) {

      return status.createStepResult(e);
    }
  }

  /**
   * Convert SAM file to sorted BAM with picard
   * @param samDataFile input SAM file
   * @param bamDataFile output SAM file
   * @param bamIndexDataFile output index file
   * @param compressionLevel compression level
   * @param reporter reporter
   * @param tmpDir temporary directory
   * @throws IOException if an error occurs
   */
  private static final void convert(final DataFile samDataFile,
      final DataFile bamDataFile, final DataFile bamIndexDataFile,
      final int compressionLevel, final Reporter reporter, final File tmpDir)
      throws IOException {

    checkArgument(compressionLevel >= 0 && compressionLevel <= 9,
        "Invalid compression level [0-9]: " + compressionLevel);

    // Open sam file
    final SAMFileReader samReader = new SAMFileReader(samDataFile.open());

    // Forse sort
    samReader.getFileHeader().setSortOrder(SortOrder.coordinate);

    // Get Bam file
    final File bamFile = bamDataFile.toFile();

    // Open Bam file
    final SAMFileWriter samWriter =
        new SAMFileWriterFactory()
            .setCreateIndex(true)
            .setTempDirectory(tmpDir)
            .makeBAMWriter(samReader.getFileHeader(), false, bamFile,
                compressionLevel);

    for (final SAMRecord samRecord : samReader) {
      samWriter.addAlignment(samRecord);
      reporter.incrCounter(COUNTER_GROUP, "sorted records", 1);
    }

    // Change index bai file
    final String bamIndexFilename =
        bamDataFile.getName().substring(0, bamDataFile.getName().length() - 1)
            + "i";
    final File bamIndexFile =
        new File(bamDataFile.toFile().getParentFile(), bamIndexFilename);
    bamIndexFile.renameTo(bamIndexDataFile.toFile());

    // Create a symbolic link
    bamIndexDataFile.symlink(new DataFile(bamIndexFile), true);

    samReader.close();
    samWriter.close();
  }

}
