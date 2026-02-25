package fr.ens.biologie.genomique.eoulsan.modules.mapping.local;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.READS_FASTQ;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.INPUT_RAW_READS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_FILTERED_READS_COUNTER;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.bio.BadBioEntryException;
import fr.ens.biologie.genomique.kenetre.bio.ReadSequence;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqReader;
import fr.ens.biologie.genomique.kenetre.bio.io.FastqWriter;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

/**
 * This class define a module for reads splitting.
 *
 * @since 2.7
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class SplitReadsModule extends AbstractModule {

  protected static final String MODULE_NAME = "splitreads";

  protected static final String COUNTER_GROUP = "reads_splitting";

  private static final int DEFAULT_SPLIT_LENGTH = 100;

  private int splitLength = DEFAULT_SPLIT_LENGTH;

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "This step filters reads.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(READS_FASTQ);
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(READS_FASTQ);
  }

  @Override
  public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {
        case "split.length":
          this.splitLength = p.getIntValueGreaterOrEqualsTo(1);

          break;

        default:
          Modules.unknownParameter(context, p);
          break;
      }
    }
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    // Create the reporter
    final Reporter reporter = new LocalReporter();

    try {

      // Get input and output data
      final Data inData = context.getInputData(READS_FASTQ);
      final Data outData = context.getOutputData(READS_FASTQ, inData);

      // get input file count for the sample
      final int inFileCount = inData.getDataFileCount();

      if (inFileCount < 1) {
        throw new IOException("No reads file found.");
      }

      if (inFileCount > 2) {
        throw new IOException("Cannot handle more than 2 reads files at the same time.");
      }

      // Run the splitter in single or pair-end mode
      if (inFileCount == 1) {
        singleEnd(inData, outData, reporter, status, this.splitLength);
      } else {
        pairedEnd(inData, outData, reporter, status, this.splitLength);
      }

    } catch (FileNotFoundException e) {
      return status.createTaskResult(e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      return status.createTaskResult(e, "Error while filtering reads: " + e.getMessage());
    }

    return status.createTaskResult();
  }

  /**
   * Split a sample data in single end mode.
   *
   * @param inData input Data
   * @param outData output Data
   * @param reporter reporter to use
   * @param status step status
   * @param splitLength read length
   * @throws IOException if an error occurs while filtering reads
   */
  private static void singleEnd(
      final Data inData,
      final Data outData,
      final Reporter reporter,
      final TaskStatus status,
      final int splitLength)
      throws IOException {

    // Get the source
    final DataFile inFile = inData.getDataFile(0);

    // Get the dest
    final DataFile outFile = outData.getDataFile(0);

    // Filter reads
    splitFile(inFile, outFile, reporter, splitLength);

    // Set the description of the context
    status.setDescription("Split reads (" + inData.getName() + ", " + inFile.getName() + ")");

    // Add counters for this sample to log file
    status.setCounters(reporter, COUNTER_GROUP);
  }

  /**
   * Split a sample data in paired-end mode.
   *
   * @param inData input Data
   * @param outData output Data
   * @param reporter reporter to use
   * @param splitLength read length
   * @throws IOException if an error occurs while filtering reads
   */
  private static void pairedEnd(
      final Data inData,
      final Data outData,
      final Reporter reporter,
      final TaskStatus status,
      final int splitLength)
      throws IOException {

    // Get the source
    DataFile inFile = inData.getDataFile(0);

    // Get the dest
    DataFile outFile = outData.getDataFile(0);

    // Filter reads
    splitFile(inFile, outFile, reporter, splitLength);

    // Get the source
    inFile = inData.getDataFile(1);

    // Get the dest
    outFile = outData.getDataFile(1);

    // Filter reads
    splitFile(inFile, outFile, reporter, splitLength);

    // Set the description of the context
    status.setDescription("Split reads (" + inData.getName() + ", " + inFile.getName() + ")");

    // Add counters for this sample to log file
    status.setCounters(reporter, COUNTER_GROUP);
  }

  /**
   * Split a file.
   *
   * @param inFile input file
   * @param outFile output file
   * @param reporter reporter to use
   * @param splitLength split length
   * @throws IOException if an error occurs while filtering data
   */
  private static void splitFile(
      final DataFile inFile, final DataFile outFile, final Reporter reporter, final int splitLength)
      throws IOException {

    getLogger().info("Filter file: " + inFile);

    try (FastqReader reader = new FastqReader(inFile.open());
        FastqWriter writer = new FastqWriter(outFile.create())) {
      for (final ReadSequence read : reader) {

        reporter.incrCounter(COUNTER_GROUP, INPUT_RAW_READS_COUNTER.counterName(), 1);

        for (ReadSequence rout : read.split(splitLength)) {

          writer.write(rout);
          reporter.incrCounter(COUNTER_GROUP, OUTPUT_FILTERED_READS_COUNTER.counterName(), 1);
        }
      }
      reader.throwException();

    } catch (BadBioEntryException e) {

      throw new IOException(
          "Invalid Fastq format: "
              + e.getMessage()
              + " File: "
              + inFile
              + " Entry: "
              + e.getEntry());
    }
  }
}
