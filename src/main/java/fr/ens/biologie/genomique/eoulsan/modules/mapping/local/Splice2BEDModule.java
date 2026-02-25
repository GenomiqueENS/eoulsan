package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import static fr.ens.biologie.genomique.kenetre.bio.SAMUtils.parseIntervalsToBEDEntry;

import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.mapping.AbstractSplice2BEDModule;
import fr.ens.biologie.genomique.kenetre.bio.BEDEntry;
import fr.ens.biologie.genomique.kenetre.bio.EntryMetadata;
import fr.ens.biologie.genomique.kenetre.bio.io.SortedBEDWriter;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.File;
import java.io.IOException;

/**
 * This class define a module for converting SAM files into BED.
 *
 * @since 2.3
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class Splice2BEDModule extends AbstractSplice2BEDModule {

  private static final String PLUS_COLOR = "255,0,0";
  private static final String MINUS_COLOR = "0,0,255";

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    try {

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      // Get input SAM data
      final Data inData = context.getInputData(DataFormats.MAPPER_RESULTS_SAM);

      // Get output BED data
      final Data outBEDData = context.getOutputData(DataFormats.MAPPER_RESULT_BED, inData);

      final DataFile samFile = inData.getDataFile();
      final DataFile bedFile = outBEDData.getDataFile();

      String trackName = inData.getName();
      String trackDescription = inData.getMetadata().get("Description");
      String trackColor = inData.getMetadata().get("TrackColor");

      convert(
          samFile,
          bedFile,
          trackName,
          trackDescription,
          trackColor,
          context.getLocalTempDirectory(),
          reporter);

      // Set the description of the context
      status.setDescription(
          "Convert alignments to BED format ("
              + inData.getName()
              + ", "
              + samFile.getName()
              + ", "
              + bedFile.getName()
              + ")");

      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      return status.createTaskResult();

    } catch (final IOException e) {

      return status.createTaskResult(e);
    }
  }

  private static void convert(
      final DataFile samDataFile,
      final DataFile bedDataFile,
      final String trackName,
      final String trackDescription,
      final String trackColor,
      File temporaryDirectory,
      final Reporter reporter)
      throws IOException {

    try (final SamReader samReader =
            SamReaderFactory.makeDefault().open(SamInputResource.of(samDataFile.open()));
        final SortedBEDWriter bedWriter = new SortedBEDWriter(bedDataFile.create())) {

      // Set the temporary directory for sorting data
      bedWriter.setTemporaryDirectory(temporaryDirectory);

      // Define the metadata
      final EntryMetadata metadata = new EntryMetadata();
      metadata.add(
          "track",
          "name=\""
              + trackName
              + "\" "
              + (trackDescription != null ? "description=\"" + trackDescription + "\"" : "")
              + " itemRgb=\"On\"");

      for (final SAMRecord samRecord : samReader) {

        // Discard unmapped alignments
        if (samRecord.getReadUnmappedFlag()) {
          continue;
        }

        // Parse splice
        BEDEntry entry = parseIntervalsToBEDEntry(samRecord, metadata);

        // Set score
        entry.setScore(1000);

        // Set track color
        if (trackColor != null && !"".equals(trackColor.trim())) {
          entry.setRgbItem(trackColor.trim());
        } else {
          if (entry.getStrand() == '+') {
            entry.setRgbItem(PLUS_COLOR);
          } else if (entry.getStrand() == '-') {
            entry.setRgbItem(MINUS_COLOR);
          }
        }

        // Write BED entry
        bedWriter.write(entry);

        reporter.incrCounter(COUNTER_GROUP, "alignments processed", 1);
      }
    }
  }
}
