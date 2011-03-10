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

package fr.ens.transcriptome.eoulsan.steps.expression.local;

import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.INVALID_CHROMOSOME_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.INVALID_SAM_ENTRIES_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.PARENTS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.PARENT_ID_NOT_FOUND_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.TOTAL_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.UNUSED_READS_COUNTER;
import static fr.ens.transcriptome.eoulsan.steps.expression.ExpressionCounters.USED_READS_COUNTER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;

import net.sf.samtools.SAMException;
import net.sf.samtools.SAMParser;
import net.sf.samtools.SAMRecord;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.steps.expression.ExonsCoverage;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder.Exon;
import fr.ens.transcriptome.eoulsan.steps.expression.TranscriptAndExonFinder.Transcript;
import fr.ens.transcriptome.eoulsan.util.PseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class implements the local version of map reduce alhorithm for
 * expression computation.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public final class ExpressionPseudoMapReduce extends PseudoMapReduce {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private TranscriptAndExonFinder tef;
  private final String counterGroup;
  private final SAMParser parser;
  private final ExonsCoverage geneExpr = new ExonsCoverage();
  private final String[] fields = new String[9];

  private final Map<String, Exon> oneExonByParentId =
      new HashMap<String, Exon>();

  //
  // Mapper and reducer
  //

  /**
   * Mapper.
   * @param value input of the mapper
   * @param output List of output of the mapper
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the mapper
   */
  public void map(final String value, final List<String> output,
      final Reporter reporter) throws IOException {

    final SAMRecord samRecord;

    try {
      samRecord = parser.parseLine(value);
    } catch (SAMException e) {

      reporter.incrCounter(this.counterGroup, INVALID_SAM_ENTRIES_COUNTER
          .counterName(), 1);
      LOGGER.info("Invalid soap output entry: "
          + e.getMessage() + " line='" + value + "'");
      return;
    }

    final String chr = samRecord.getReferenceName();
    final int start = samRecord.getAlignmentStart();
    final int end = samRecord.getAlignmentEnd();

    final Set<Exon> exons = tef.findExons(chr, start, end);

    reporter.incrCounter(this.counterGroup, TOTAL_READS_COUNTER.counterName(),
        1);
    if (exons == null) {
      reporter.incrCounter(this.counterGroup, UNUSED_READS_COUNTER
          .counterName(), 1);
      return;
    }

    reporter
        .incrCounter(this.counterGroup, USED_READS_COUNTER.counterName(), 1);
    int count = 1;
    final int nbExons = exons.size();

    this.oneExonByParentId.clear();

    // Sort the exon
    final List<Exon> exonSorted = Lists.newArrayList(exons);
    Collections.sort(exonSorted);

    for (Exon e : exonSorted)
      oneExonByParentId.put(e.getParentId(), e);

    List<String> keysSorted = new ArrayList<String>(oneExonByParentId.keySet());
    Collections.sort(keysSorted);

    for (String key : keysSorted) {

      final Exon e = oneExonByParentId.get(key);

      output.add(e.getParentId()
          + "\t" + e.getChromosome() + "\t" + e.getStart() + "\t" + e.getEnd()
          + "\t" + e.getStrand() + "\t" + (count++) + "\t" + nbExons + "\t"
          + chr + "\t" + start + "\t" + end);
    }
  }

  /**
   * Reducer
   * @param key input key of the reducer
   * @param values values for the key
   * @param output list of output values of the reducer
   * @param reporter reporter
   * @throws IOException if an error occurs while executing the reducer
   */
  public void reduce(final String key, Iterator<String> values,
      final List<String> output, final Reporter reporter) throws IOException {

    geneExpr.clear();

    reporter.incrCounter(this.counterGroup, PARENTS_COUNTER.counterName(), 1);

    final String parentId = key;

    boolean first = true;
    String chr = null;

    int count = 0;

    while (values.hasNext()) {

      count++;
      StringUtils.fastSplit(values.next().toString(), this.fields);

      final String exonChr = this.fields[0];
      final int exonStart = Integer.parseInt(this.fields[1]);
      final int exonEnd = Integer.parseInt(this.fields[2]);
      // codingStrand = Boolean.parseBoolean(this.fields[3]);

      // final int exonNumber = Integer.parseInt(this.fields[4]);
      // final int exonTotal = Integer.parseInt(this.fields[5]);

      final String alignementChr = this.fields[6];
      final int alignmentStart = Integer.parseInt(this.fields[7]);
      final int alignementEnd = Integer.parseInt(this.fields[8]);

      if (first) {
        chr = exonChr;
        first = false;
      }

      if (!exonChr.equals(alignementChr) || !chr.equals(alignementChr)) {
        reporter.incrCounter(this.counterGroup, INVALID_CHROMOSOME_COUNTER
            .counterName(), 1);
        continue;
      }

      geneExpr.addAlignement(exonStart, exonEnd, alignmentStart, alignementEnd,
          true);
    }

    if (count == 0)
      return;

    final Transcript transcript = tef.getTranscript(parentId);

    if (transcript == null) {
      reporter.incrCounter(this.counterGroup, PARENT_ID_NOT_FOUND_COUNTER
          .counterName(), 1);
      return;
    }

    final int geneLength = transcript.getLength();
    final int notCovered = geneExpr.getNotCovered(geneLength);

    final String result =
        key + "\t" + notCovered + "\t" + geneExpr.getAlignementCount();

    output.add(result);
  }

  /**
   * Get the annotation object.
   * @return the annotation object
   */
  public TranscriptAndExonFinder getTranscriptAndExonFinder() {

    return this.tef;
  }

  //
  // 
  //

  /**
   * Load annotation information
   * @param annotationFile annotation file to load
   * @param expressionType expression type to use
   * @throws IOException if an error occurs while reading annotation file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  private void loadAnnotationFile(final File annotationFile,
      final String expressionType) throws IOException, BadBioEntryException {

    this.tef = new TranscriptAndExonFinder(annotationFile, expressionType);
  }

  //
  // Constructor
  //

  /**
   * Load annotation information
   * @param annotationFile annotation file to load
   * @param expressionType expression type to use
   * @param genomeDescFile genome description file\
   * @param counterGroup counter group
   * @throws IOException if an error occurs while reading annotation file
   * @throws BadBioEntryException if an entry of the annotation file is invalid
   */
  public ExpressionPseudoMapReduce(final File annotationFile,
      final String expressionType, final File genomeDescFile,
      final String counterGroup) throws IOException, BadBioEntryException {

    this.counterGroup = counterGroup;

    // Create parser object
    this.parser = new SAMParser();

    // Load genome description object
    final GenomeDescription genomeDescription =
        GenomeDescription.load(genomeDescFile);

    // Set the chromosomes sizes in the parser
    this.parser.setGenomeDescription(genomeDescription);

    loadAnnotationFile(annotationFile, expressionType);
  }

}
