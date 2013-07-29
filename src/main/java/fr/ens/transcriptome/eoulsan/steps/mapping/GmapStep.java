/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.steps.mapping;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.READS_FASTQ;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a mapping step using the gmap mapper.
 * @since 1.1
 * @author Laurent Jourdren
 */
@LocalOnly
public class GmapStep extends AbstractStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String COUNTER_GROUP = "reads_mapping";
  private String mapperArguments = "";

  @Override
  public String getName() {
    // This method return the name of the step
    return "gmap";
  }

  @Override
  public String getDescription() {
    // This method return a description of the step. This method is optional
    return "This step map reads using gsnap";
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {

      if ("mapperarguments".equals(p.getName()))
        this.mapperArguments = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.READS_FASTQ,
        DataFormats.GMAP_INDEX_ZIP, DataFormats.GENOME_DESC_TXT};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.MAPPER_RESULTS_SAM};
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    // The design object contain the list of all the sample to process. This
    // object contains all the information of the design file

    // The context object had many useful method for writing a Step
    // (e.g. access to file to process, the workflow description, the logger...)

    try {
      // Save the start time
      final long startTime = System.currentTimeMillis();

      // Log message to write at the end of the step
      final StringBuilder log = new StringBuilder();

      // For each sample of the analysis
      for (final Sample sample : design.getSamples()) {

        // Create the reporter. The reporter collect information about the
        // process of the data (e.g. the number of reads, the number of
        // alignments
        // generated...)
        final Reporter reporter = new Reporter();

        // Get the path to the archive that contains the GMAP genome index
        // In Eoulsan, to get the path of a file, you just have to call the
        // context.getDataFile() with the data type and sample object as
        // argument
        final File archiveIndexFile =
            context.getInputDataFile(DataFormats.GMAP_INDEX_ZIP, sample)
                .toFile();

        // Get input file count for the sample
        // It could have one or two fastq files by sample (single end or
        // paired-end data)
        final int inFileCount =
            context.getInputDataFileCount(DataFormats.READS_FASTQ, sample);

        // Throw error if no reads file found.
        if (inFileCount < 1)
          throw new IOException("No reads file found.");

        // Throw error if more that 2 reads files found.
        if (inFileCount > 2)
          throw new IOException(
              "Cannot handle more than 2 reads files at the same time.");

        // Get the path to the output SAM file
        final File outSamFile =
            context.getOutputDataFile(DataFormats.MAPPER_RESULTS_SAM, sample)
                .toFile();

        // Log message for this sample
        String logMsg = "";

        // Single end mode
        if (inFileCount == 1) {

          // Get the source
          // For data format with more that one file (e.g. fastq file in
          // paired-end),
          // You must must add a third argument to context.getDataFile with the
          // number
          // of the requested file. With single end fastq the value is always 0.
          // In paired-end mode, the number of the second end is 1.
          final File inFile =
              context.getInputDataFile(READS_FASTQ, sample, 0).toFile();

          // Single read mapping
          mapSingleEnd(context, inFile, sample.getMetadata().getFastqFormat(),
              archiveIndexFile, outSamFile, reporter);

          logMsg =
              "Mapping reads in "
                  + sample.getMetadata().getFastqFormat() + " with Gmap ("
                  + sample.getName() + ", " + inFile.getName() + ")";
        }

        // Paired end mode
        if (inFileCount == 2) {

          // Get the path of the first end
          // The third argument of context.getDataFile is 0 like in single end
          // mode.
          final File inFile1 =
              context.getInputDataFile(READS_FASTQ, sample, 0).toFile();

          // Get the path of the second end
          // The third argument of context.getDataFile is 1.
          final File inFile2 =
              context.getInputDataFile(READS_FASTQ, sample, 1).toFile();

          // Single read mapping
          mapPairedEnd(context, inFile1, inFile2, sample.getMetadata()
              .getFastqFormat(), archiveIndexFile, outSamFile, reporter);

          logMsg =
              "Mapping reads in "
                  + sample.getMetadata().getFastqFormat() + " with Gmap ("
                  + sample.getName() + ", " + inFile1.getName() + ","
                  + inFile2.getName() + ")";
        }

        // Add the log message of the process of the sample to the step log
        log.append(reporter.countersValuesToString(COUNTER_GROUP, logMsg));
      }

      // Write log file
      return new StepResult(context, startTime, log.toString());

    } catch (IOException e) {

      return new StepResult(context, e, "Error while mapping: "
          + e.getMessage());
    }
  }

  // This method launch the computation in single end mode.
  private void mapSingleEnd(final Context context, final File inFile,
      final FastqFormat format, final File archiveIndexFile,
      final File outSamFile, final Reporter reporter) throws IOException {

    map(context, new String[] {inFile.getAbsolutePath()}, this.mapperArguments,
        format, archiveIndexFile, outSamFile, reporter);
  }

  // This method launch the computation in paired-end mode
  private void mapPairedEnd(final Context context, final File inFile1,
      final File inFile2, final FastqFormat format,
      final File archiveIndexFile, final File outSamFile,
      final Reporter reporter) throws IOException {

    map(context,
        new String[] {inFile1.getAbsolutePath(), inFile2.getAbsolutePath()},
        this.mapperArguments, format, archiveIndexFile, outSamFile, reporter);
  }

  private void map(final Context context, final String[] filenames,
      final String cmdArg, final FastqFormat format,
      final File archiveIndexFile, final File outSamFile,
      final Reporter reporter) throws IOException {

    int mapperThreads = Runtime.getRuntime().availableProcessors();

    // Extract and install the gsnap binary for eoulsan jar archive
    final String gsnapPath =
        BinariesInstaller.install("gmap", context.getSettings()
            .getTempDirectory());

    // Get the path to the uncommpressed genome index
    final File archiveIndexDir =
        new File(archiveIndexFile.getParent(),
            StringUtils.filenameWithoutExtension(archiveIndexFile.getName()));

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Select the argument for the FASTQ format
    final String formatArg;
    switch (format) {

    case FASTQ_ILLUMINA:
      formatArg = "--quality-protocol=illumina";
      break;
    case FASTQ_ILLUMINA_1_5:
      formatArg = "--quality-protocol=illumina";
      break;
    case FASTQ_SOLEXA:
      throw new IOException("Gmap not handle the Solexa FASTQ format.");

    case FASTQ_SANGER:
    default:
      formatArg = "--quality-protocol=sanger";
      break;
    }

    // Build the command line

    final String cmd =
        "cat "
            + StringUtils.join(filenames, " ")
            + " | awk '{ if (NR%4==1) {print \">\" substr($0,2);} else if (NR%4==2) {print $0;} }' | "
            + gsnapPath + " " + formatArg + " -t " + mapperThreads + " -D "
            + archiveIndexDir.getAbsolutePath() + " -d genome -f samse"
            + cmdArg + (filenames.length == 1 ? "" : " | LANG=C sort") + " > "
            + outSamFile.getAbsolutePath() + " 2> /dev/null";

    // Log the command line to execute
    LOGGER.info(cmd);

    // Execute the command line and save the exit value
    final int exitValue = ProcessUtils.sh(cmd);

    // if the exit value is not success (0) throw an exception
    if (exitValue != 0) {
      throw new IOException("Bad error result for gsnap execution: "
          + exitValue);
    }

    // Count the number of alignment generated for the sample
    parseSAMResults(outSamFile, reporter);
  }

  // Uncompress
  private static final void unzipArchiveIndexFile(final File archiveIndexFile,
      final File archiveIndexDir) throws IOException {

    // Test if genome index file exists
    if (!archiveIndexFile.exists())
      throw new IOException("No index for the mapper found: "
          + archiveIndexFile);

    // Uncompress archive if necessary
    if (!archiveIndexDir.exists()) {

      if (!archiveIndexDir.mkdir())
        throw new IOException("Can't create directory for gmap index: "
            + archiveIndexDir);

      LOGGER.fine("Unzip archiveIndexFile "
          + archiveIndexFile + " in " + archiveIndexDir);
      FileUtils.unzip(archiveIndexFile, archiveIndexDir);
    }

    // Test if extracted directory exists
    FileUtils.checkExistingDirectoryFile(archiveIndexDir,
        "gmaps index directory");
  }

  // Count the number of alignment in a SAM file and save the result in the
  // reporter object
  private static final void parseSAMResults(final File samFile,
      final Reporter reporter) throws IOException {

    String line;

    // Parse SAM result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(samFile);

    int entriesParsed = 0;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine) || trimmedLine.startsWith("@"))
        continue;

      final int tabPos = trimmedLine.indexOf('\t');

      if (tabPos != -1) {

        entriesParsed++;

        reporter.incrCounter(COUNTER_GROUP,
            MappingCounters.OUTPUT_MAPPING_ALIGNMENTS_COUNTER.counterName(), 1);
      }
    }

    readerResults.close();

    LOGGER.info(entriesParsed + " entries parsed in gsnap output file");

  }

}
