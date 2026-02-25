package fr.ens.biologie.genomique.eoulsan.modules.chipseq.qc;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.INPUT_ALIGNMENTS_COUNTER;
import static fr.ens.biologie.genomique.eoulsan.modules.mapping.MappingCounters.OUTPUT_FILTERED_ALIGNMENTS_COUNTER;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Version;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import java.io.IOException;
import java.util.Set;

/**
 * This step computes how many spliced alignments there are in a SAM file. This QC step computes the
 * number of spliced alignments in a SAM file, as well as the total number of mapped reads.
 *
 * @author Celine Hernandez - CSB lab - ENS - Paris
 */
@LocalOnly
public class CountSplicedReadsModule extends AbstractModule {

  private static final String STEP_NAME = "countspliced";

  protected static final String COUNTER_GROUP = "sam_stats";

  //
  // Overriden methods.
  //

  /** Name of the Step. */
  @Override
  public String getName() {
    return STEP_NAME;
  }

  /** A short description of the tool and what is done in the step. */
  @Override
  public String getDescription() {
    return "This step performs a quality control by counting spliced mapped reads.";
  }

  /** Version. */
  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  /** Define input ports. */
  @Override
  public InputPorts getInputPorts() {
    return singleInputPort(MAPPER_RESULTS_SAM);
  }

  //
  // Step
  //

  /**
   * Set the parameters of the step to configure the step. No parameter accepted.
   *
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is provided
   */
  @Override
  public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
      throws EoulsanException {

    // No parameters
    if (!stepParameters.isEmpty()) {
      throw new EoulsanException("Unknown parameter(s) for " + getName() + " step.");
    }
  }

  /** Install all the files necessary in the tmp folder, then run idr. */
  @Override
  // public StepResult execute(final Design design, final Context context) {
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    // Get input data (SAM format)
    final Data inData = context.getInputData(MAPPER_RESULTS_SAM);

    // Get the source
    final DataFile samFile = inData.getDataFile();

    getLogger().info("Counting entries in SAM file: " + samFile);

    // Create the reporter to collect information about the file content
    final LocalReporter reporter = new LocalReporter();

    try {
      // Open SAM file
      final SamReader reader =
          SamReaderFactory.makeDefault().open(SamInputResource.of(samFile.open()));

      // To count total number of records
      int recordCount = 0;
      // To count how many are spliced
      int splicedRecords = 0;
      // Flag whether it's paired end data
      boolean pairedEnd = false;

      for (final SAMRecord record : reader) {

        // single-end or paired-end mode ?
        if (recordCount == 0) {
          pairedEnd = record.getReadPairedFlag();
        }

        // Increment total number of records
        recordCount++;

        // Loop through all characters of the cigar (supposedly faster using
        // charAt() for short String)
        String cigar = record.getCigarString();
        for (int i = 0; i < cigar.length(); i++) {

          // Increment counter if it's an 'N' (spliced read) and stop looping
          if (cigar.charAt(i) == 'N') {
            splicedRecords++;
            break;
          }
        }
      }

      reader.close();

      getLogger().info("Spliced SAM entries : " + splicedRecords);
      getLogger().info("All SAM entries : " + recordCount);

      // paired-end mode
      if (pairedEnd) {
        reporter.setCounter(COUNTER_GROUP, INPUT_ALIGNMENTS_COUNTER.counterName(), recordCount / 2);
        reporter.setCounter(COUNTER_GROUP, OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);
        reporter.setCounter(
            COUNTER_GROUP, OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), splicedRecords / 2);
      }

      // single-end mode
      else {
        reporter.setCounter(COUNTER_GROUP, INPUT_ALIGNMENTS_COUNTER.counterName(), recordCount);
        reporter.setCounter(COUNTER_GROUP, OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), 1);
        reporter.setCounter(
            COUNTER_GROUP, OUTPUT_FILTERED_ALIGNMENTS_COUNTER.counterName(), splicedRecords);
      }

      // Set the context description
      status.setDescription(
          "Count entries in SAM file (" + inData.getName() + ", " + samFile.getName() + ")");
      // Add counters for this sample to log file
      status.setCounters(reporter, COUNTER_GROUP);

      // Create the reporter. The reporter collect information about the
      // process of the data (e.g. the number of reads, the number of
      // alignments generated...)
      getLogger()
          .info(
              reporter.countersValuesToString(
                  COUNTER_GROUP, " (" + inData.getName() + ", " + samFile.getName() + ") "));

    } catch (IOException ioe) {
      getLogger()
          .severe("Could not open SAM file (" + inData.getName() + ", " + samFile.getName() + ")");
      return status.createTaskResult();
    }

    return status.createTaskResult();
  }
} // End of class CountSplicedReadsStep
