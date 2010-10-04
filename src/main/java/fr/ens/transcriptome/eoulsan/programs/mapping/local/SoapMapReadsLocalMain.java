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

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.core.SOAPWrapper;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.programs.mapping.MapReadsStep;
import fr.ens.transcriptome.eoulsan.programs.mgmt.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.programs.mgmt.StepResult;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * Main class for mapping reads program.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public class SoapMapReadsLocalMain extends MapReadsStep {

  public static String PROGRAM_NAME = "soapmapreads";
  public static final String COUNTER_GROUP = "Map reads with SOAP";

  //
  // Step methods
  // 

  @Override
  public String getLogName() {

    return "soapmapreads";
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

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

        // Create SOAP index if necessary
        if (!soapIndexDir.exists())
          SOAPWrapper.makeIndex(new File(genomeFilename), soapIndexDir);

        SOAPWrapper.map(inputFile, soapIndexDir, alignmentFile, unmapFile,
            Common.SOAP_ARGS_DEFAULT, getThreads() == -1 ? Runtime.getRuntime()
                .availableProcessors() : getThreads());

        filterSoapResult(alignmentFile, resultFile, reporter);
        countUnmap(unmapFile, reporter);
        alignmentFile.delete();

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

      aln.parseResultLine(trimmedLine);
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
