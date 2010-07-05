/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.programs.mapping.local;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.MainCLI;
import fr.ens.transcriptome.eoulsan.core.AlignResult;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.DesignReader;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * Main class for mapping reads program.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public class SoapMapReadsLocalMain {

  public static String PROGRAM_NAME = "soapmapreads";
  public static final String COUNTER_GROUP = "Map reads with SOAP";

  private static int threads = -1;

  /**
   * Map with soap the filtered reads
   * @param designFilename design filename
   * @param threads number of threads to use with SOAP
   */
  private static void map(final String designFilename, final int threads) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final DesignReader dr = new SimpleDesignReader(designFilename);
      final Design design = dr.read();

      int genomeCount = 0;
      final Map<String, Integer> genomes = new HashMap<String, Integer>();

      for (Sample s : design.getSamples()) {

        final String genomeFilename = s.getMetadata().getGenome().trim();
        if (!genomes.containsKey(genomeFilename))
          genomes.put(genomeFilename, ++genomeCount);

        final Reporter reporter = new Reporter();

        final File soapIndexDir =
            new File(Common.GENOME_SOAP_INDEX_DIR_PREFIX
                + genomes.get(genomeFilename));

        final File inputFile =
            new File(Common.SAMPLE_FILTERED_PREFIX
                + s.getId() + Common.FASTQ_EXTENSION);

        final File alignmentFile =
            new File(Common.SAMPLE_SOAP_ALIGNMENT_PREFIX
                + s.getId() + Common.SOAP_RESULT_EXTENSION + ".tmp");

        final File unmapFile =
            new File(Common.SAMPLE_SOAP_ALIGNMENT_PREFIX
                + s.getId() + Common.UNMAP_EXTENSION);

        final File resultFile =
            new File(Common.SAMPLE_SOAP_ALIGNMENT_PREFIX
                + s.getId() + Common.SOAP_RESULT_EXTENSION);

        SOAPWrapper.map(inputFile, soapIndexDir, alignmentFile, unmapFile,
            Common.SOAP_ARGS_DEFAULT, threads == -1 ? Runtime.getRuntime()
                .availableProcessors() : threads);

        filterSoapResult(alignmentFile, resultFile, reporter);
        countUnmap(unmapFile, reporter);
        alignmentFile.delete();

        // Add counters for this sample to log file
        log.append(reporter.countersValuesToString(COUNTER_GROUP,
            "Map reads with SOAP ("
                + s.getName() + ", " + inputFile.getName() + ")"));

      }

      // Write log file
      Common.writeLog(new File("soapmapreads.log"), startTime, log.toString());

    } catch (EoulsanIOException e) {
      System.err.println("Error while reading design file: " + e.getMessage());
      System.exit(1);
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("error while filtering: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Filter soap results to remove results with more than one hit
   * @param soapAlignFile SOAP alignment result file
   * @param soapAlignFilteredFile result file
   * @param reporter reporter that record events
   * @throws IOException if an error occurs while filtring data
   */
  private static void filterSoapResult(final File soapAlignFile,
      final File soapAlignFilteredFile, final Reporter reporter)
      throws IOException {

    // Parse SOAP main result file
    final BufferedReader readerResults =
        FileUtils.createBufferedReader(soapAlignFile);

    final BufferedWriter bw =
        new BufferedWriter(new FileWriter(soapAlignFilteredFile));

    final AlignResult aln = new AlignResult();

    String line = null;
    String lastSequenceId = null;

    while ((line = readerResults.readLine()) != null) {

      final String trimmedLine = line.trim();
      if ("".equals(trimmedLine))
        continue;

      aln.parseResultLine(trimmedLine);
      reporter.incrCounter(COUNTER_GROUP, "soap alignments", 1);

      final String currentSequenceId = aln.getSequenceId();

      if (aln.getNumberOfHits() == 1) {
        bw.write(line + "\n");
        reporter.incrCounter(COUNTER_GROUP, "soap alignment with only one hit",
            1);
      } else if (currentSequenceId != null
          && (!currentSequenceId.equals(lastSequenceId)))
        reporter.incrCounter(COUNTER_GROUP, "soap alignment with more one hit",
            1);

      lastSequenceId = currentSequenceId;
    }

    readerResults.close();
    bw.close();
  }

  /**
   * Count the number of unmap reads.
   * @param unmapFile unmap file to read
   * @param reporter the reporter for the report
   * @throws IOException if an error occurs while reading file
   */
  private static void countUnmap(final File unmapFile, final Reporter reporter)
      throws IOException {

    final BufferedReader br = FileUtils.createBufferedReader(unmapFile);

    String line = null;

    long count = 0;

    while ((line = br.readLine()) != null)
      if (line.startsWith(">"))
        count++;

    br.close();

    reporter.incrCounter(COUNTER_GROUP, "soap unmap reads", count);
  }

  /**
   * Show command line help.
   * @param options Options of the software
   */
  private static void help(final Options options) {

    // Show help message
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(Globals.APP_NAME_LOWER_CASE
        + " " + PROGRAM_NAME + " [options] design", options);

    System.exit(0);
  }

  /**
   * Create options for command line
   * @return an Options object
   */
  private static Options makeOptions() {

    // create Options object
    final Options options = new Options();

    options.addOption("version", false, "show version of the software");
    options
        .addOption("about", false, "display information about this software");
    options.addOption("h", "help", false, "display this help");
    options.addOption("license", false,
        "display information about the license of this software");

    options.addOption(OptionBuilder.withArgName("value").hasArg()
        .withDescription("number of threads to use").create("threads"));

    return options;
  }

  /**
   * Parse the options of the command line
   * @param args command line arguments
   * @return the number of optional arguments
   */
  private static int parseCommandLine(final String args[]) {

    final Options options = makeOptions();
    final CommandLineParser parser = new GnuParser();

    int argsOptions = 0;

    try {

      // parse the command line arguments
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("help"))
        help(options);

      if (line.hasOption("about"))
        MainCLI.about();

      if (line.hasOption("version"))
        MainCLI.version();

      if (line.hasOption("license"))
        MainCLI.license();

      if (line.hasOption("threads")) {
        threads = Integer.parseInt(line.getOptionValue("threads"));
        argsOptions += 2;
      }

    } catch (ParseException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return argsOptions;
  }

  /**
   * Main method
   * @param args command line arguments
   */
  public static void main(final String[] args) {

    // Parse the command line
    final int argsOptions = parseCommandLine(args);

    if (args == null || args.length != argsOptions + 1) {

      System.err
          .println("Invalid number of arguments. Use the -h option to get more information.");
      System.err.println("usage:"
          + Globals.APP_NAME_LOWER_CASE + " " + PROGRAM_NAME
          + " [options] design");
      System.exit(1);
    }

    // Parse the command line
    final String designFilename = args[argsOptions];

    map(designFilename, threads);
  }

}
