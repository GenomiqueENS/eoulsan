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

package fr.ens.transcriptome.eoulsan.programs.expression.local;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.bio.AlignResult;
import fr.ens.transcriptome.eoulsan.programs.expression.ExonsCoverage;
import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder;
import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder.Exon;
import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder.Transcript;
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

  public static final String COUNTER_GROUP = "Expression";

  private TranscriptAndExonFinder tef;
  private final AlignResult ar = new AlignResult();
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

    ar.parseResultLine(value);
    final String chr = ar.getChromosome();
    final int start = ar.getLocation();
    final int stop = start + ar.getReadLength();

    final Set<Exon> exons = tef.findExons(chr, start, stop);

    reporter.incrCounter(COUNTER_GROUP, "read total", 1);
    if (exons == null) {
      reporter.incrCounter(COUNTER_GROUP, "reads unused", 1);
      return;
    }

    reporter.incrCounter(COUNTER_GROUP, "reads used", 1);
    int count = 1;
    final int nbExons = exons.size();

    this.oneExonByParentId.clear();

    for (Exon e : exons)
      oneExonByParentId.put(e.getParentId(), e);

    for (Map.Entry<String, Exon> entry : oneExonByParentId.entrySet()) {

      final Exon e = entry.getValue();

      output.add(e.getParentId()
          + "\t" + e.getChromosome() + "\t" + e.getStart() + "\t" + e.getEnd()
          + "\t" + e.getStrand() + "\t" + (count++) + "\t" + nbExons + "\t"
          + ar.getChromosome() + "\t" + ar.getLocation() + "\t"
          + (ar.getLocation() + ar.getReadLength()));

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

    reporter.incrCounter(COUNTER_GROUP, "parent", 1);

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
        reporter.incrCounter(COUNTER_GROUP, "invalid chromosome", 1);
        continue;
      }

      geneExpr.addAlignement(exonStart, exonEnd, alignmentStart, alignementEnd,
          true);
    }

    if (count == 0)
      return;

    final Transcript transcript = tef.getTranscript(parentId);

    if (transcript == null) {
      reporter.incrCounter(COUNTER_GROUP, "Parent Id not found in exon range",
          1);
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
   */
  private void loadAnnotationFile(final File annotationFile,
      final String expressionType) throws IOException {

    this.tef = new TranscriptAndExonFinder(annotationFile, expressionType);
  }

  //
  // Constructor
  //

  public ExpressionPseudoMapReduce(final File annotationFile,
      final String expressionType) throws IOException {

    loadAnnotationFile(annotationFile, expressionType);
  }

}
