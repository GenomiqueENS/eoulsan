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

package fr.ens.transcriptome.eoulsan.steps.mapping.local;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_READS_FASTQ;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.SOAP_INDEX_ZIP;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.SOAP_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.UNMAP_READS_FASTA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.mapping.MapReadsStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * Main class for mapping reads program.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
@LocalOnly
public class SoapMapReadsLocalStep extends MapReadsStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static String PROGRAM_NAME = "soapmapreads";
  public static final String COUNTER_GROUP = "Map reads with SOAP";

  //
  // Step methods
  // 

  @Override
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.LOCAL;
  }

  @Override
  public String getLogName() {

    return "soapmapreads";
  }

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      int genomeCount = 0;
      final Map<String, Integer> genomes = new HashMap<String, Integer>();

      for (Sample s : design.getSamples()) {

        final String genomeFilename = s.getMetadata().getGenome().trim();
        if (!genomes.containsKey(genomeFilename))
          genomes.put(genomeFilename, ++genomeCount);

        final Reporter reporter = new Reporter();

        final File soapIndexDir =
            new File(SOAP_INDEX_ZIP.getType().getPrefix()
                + genomes.get(genomeFilename));

        final File inputFile =
            new File(context.getDataFilename(FILTERED_READS_FASTQ, s));

        final File alignmentFile =
            new File(context.getDataFilename(SOAP_RESULTS_TXT, s)
                + ".tmp");

        final File unmapFile =
            new File(context.getDataFilename(UNMAP_READS_FASTA, s));

        final File resultFile =
            new File(context.getDataFilename(SOAP_RESULTS_TXT, s));

        if (!soapIndexDir.exists()) {

          DataFormat df = SOAP_INDEX_ZIP;

          final File soapIndexArchive =
              new File(df.getType().getPrefix()
                  + genomes.get(genomeFilename) + df.getDefaultExtention());

          if (!soapIndexArchive.exists())
            throw new IOException("No index for the mapper found: "
                + soapIndexArchive);

          if (!soapIndexDir.mkdir())
            throw new IOException("Can't create directory for SOAP index: "
                + soapIndexDir);

          FileUtils.unzip(soapIndexArchive, soapIndexDir);
        }

        SOAPWrapper.map(inputFile, soapIndexDir, alignmentFile, unmapFile,
            Common.SOAP_ARGS_DEFAULT, getThreads() == -1 ? Runtime.getRuntime()
                .availableProcessors() : getThreads());

        filterSoapResult(alignmentFile, resultFile, reporter);
        countUnmap(unmapFile, reporter);
        if (!alignmentFile.delete())
          logger.warning("Can not delete alignment file: "
              + alignmentFile.getAbsolutePath());

        // Add counters for this sample to log file
        log.append(reporter.countersValuesToString(COUNTER_GROUP,
            "Map reads with SOAP ("
                + s.getName() + ", " + inputFile.getName() + ")"));

      }

      // Write log file
      return new StepResult(this, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(this, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(this, e, "error while filtering: " + e.getMessage());
    }
  }

  //
  // Other methods
  //

  /**
   * Filter soap results to remove results with more than one hit
   * @param soapAlignFile SOAP alignment result file
   * @param soapAlignFilteredFile result file
   * @param reporter reporter that record events
   * @throws IOException if an error occurs while filtering data
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

      try {
        aln.parseResultLine(trimmedLine);
      } catch (BadBioEntryException e) {
        reporter.incrCounter(COUNTER_GROUP, "invalid soap output", 1);
        logger.info("Invalid soap output entry: "
            + e.getMessage() + " line='" + e.getEntry() + "'");
        continue;
      }

      reporter.incrCounter(COUNTER_GROUP, "soap alignments", 1);

      final String currentSequenceId = aln.getSequenceId();

      if (aln.getNumberOfHits() == 1) {
        bw.write(line + "\n");
        reporter.incrCounter(COUNTER_GROUP,
            Common.SOAP_ALIGNEMENT_WITH_ONLY_ONE_HIT_COUNTER, 1);
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

}
