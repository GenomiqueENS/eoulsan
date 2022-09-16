package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractBAM2SAMModule;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

/**
 * This class define a module for converting BAM files into SAM.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class BAM2SAMLocalModule extends AbstractBAM2SAMModule {

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      // Get input SAM data
      final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_BAM);

      // Get output BAM data
      final Data outSAMData =
          context.getOutputData(DataFormats.MAPPER_RESULTS_SAM, inData);

      final DataFile samFile = outSAMData.getDataFile();
      final DataFile bamFile = inData.getDataFile();

      convert(bamFile, samFile, reporter, context.getLocalTempDirectory());

      // Set the description of the context

      status.setDescription("Convert alignments ("
          + inData.getName() + ", " + outSAMData.getName() + ")");

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      return status.createTaskResult();

    } catch (final IOException e) {

      return status.createTaskResult(e);
    }
  }

  /**
   * Convert BAM file to sorted SAM with Picard
   * @param bamDataFile input SAM file
   * @param samDataFile output SAM file
   * @param reporter reporter
   * @param tmpDir temporary directory
   * @throws IOException if an error occurs
   */

  // private static final void convert(final File in, final File out)
  private static void convert(final DataFile bamDataFile,
      final DataFile samDataFile, final Reporter reporter, final File tmpDir)
      throws IOException {

    InputStream in = bamDataFile.open();
    OutputStream out = samDataFile.create();

    // Open bam file
    final SamReader bamReader =
        SamReaderFactory.makeDefault().open(SamInputResource.of(in));

    // Force sort
    bamReader.getFileHeader().setSortOrder(SortOrder.unsorted);

    // Open sam file
    final SAMFileWriter samWriter = new SAMFileWriterFactory()
        .setCreateIndex(false).setTempDirectory(tmpDir)
        .makeSAMWriter(bamReader.getFileHeader(), false, out);

    for (final SAMRecord samRecord : bamReader) {
      samWriter.addAlignment(samRecord);
      reporter.incrCounter(COUNTER_GROUP, "converted records", 1);
    }
    samWriter.close();
    bamReader.close();

  }

}
