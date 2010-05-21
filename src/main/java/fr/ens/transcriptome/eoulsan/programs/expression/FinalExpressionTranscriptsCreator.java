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

package fr.ens.transcriptome.eoulsan.programs.expression;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.programs.expression.TranscriptAndExonFinder.Transcript;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class generate the final expression file after counting the alignment
 * for each transcript.
 * @author Laurent Jourdren
 */
public class FinalExpressionTranscriptsCreator {

  private TranscriptAndExonFinder tef = new TranscriptAndExonFinder();
  private final Map<String, ExpressionTranscript> expressionResults =
      new HashMap<String, ExpressionTranscript>();

  private static final class ExpressionTranscript implements
      Comparable<ExpressionTranscript> {

    private Transcript transcript;
    private int baseNotCovered;
    private int alignementCount = 0;
    private double ratio;

    public void setExpressionResult(final int baseNotCovered,
        final int alignementCount, final long readsUsed) {

      this.baseNotCovered = baseNotCovered;
      this.alignementCount = alignementCount;
      this.ratio = (double) alignementCount / (double) readsUsed;
    }

    @Override
    public int compareTo(final ExpressionTranscript o) {

      if (o == null)
        return 1;

      final int diff = o.alignementCount - this.alignementCount;

      if (diff != 0)
        return diff;

      return transcript.getName().compareTo(o.transcript.getName());
    }

    @Override
    public String toString() {

      final Transcript t = this.transcript;

      return t.getName()
          + "\t" + t.getType() + "\t" + t.getChromosome() + "\t" + t.getStart()
          + "\t" + t.getEnd() + "\t" + t.getStrand() + "\t" + t.getLength()
          + "\t" + (this.baseNotCovered == 0) + "\t" + this.baseNotCovered
          + "\t" + this.ratio + "\t" + alignementCount;
    }

    //
    // Constructor
    //

    /**
     * Constructor for ExpressionTranscript
     * @param transcript Transcript to set
     */
    public ExpressionTranscript(final Transcript transcript) {

      if (transcript == null)
        throw new NullPointerException("Transcript to add is null");

      this.transcript = transcript;
      this.baseNotCovered = this.transcript.getLength();
    }

  }

  /**
   * Clear.
   */
  public void initializeExpressionResults() {

    this.expressionResults.clear();
    for (String id : tef.getTranscriptsIds())
      this.expressionResults.put(id, new ExpressionTranscript(tef
          .getTranscript(id)));
  }

  /**
   * Load pre result file
   * @param is input stream of pre-results
   * @param readsUsed the number of read useds
   * @throws IOException if an error occurs while reading data
   */
  public void loadPreResults(final File preResultFile, final long readsUsed)
      throws IOException {

    loadPreResults(FileUtils.createInputStream(preResultFile), readsUsed);
  }

  /**
   * Load pre result file
   * @param is input stream of pre-results
   * @param readsUsed the number of read useds
   * @throws IOException if an error occurs while reading data
   */
  public void loadPreResults(final InputStream is, final long readsUsed)
      throws IOException {

    final BufferedReader br = new BufferedReader(new InputStreamReader(is));

    final String[] tab = new String[3];
    String line = null;

    while ((line = br.readLine()) != null) {

      StringUtils.fastSplit(line, tab);

      final String id = tab[0];
      final int baseNotCovered = Integer.parseInt(tab[1]);
      final int alignementCount = Integer.parseInt(tab[2]);

      if (this.expressionResults.containsKey(id))
        this.expressionResults.get(id).setExpressionResult(baseNotCovered,
            alignementCount, readsUsed);
    }

  }

  /**
   * Save the final results.
   * @param os output stream
   * @throws IOException if an error occurs while writing data
   */
  public void saveFinalResults(final File resultFile) throws IOException {

    saveFinalResults(FileUtils.createOutputStream(resultFile));
  }

  /**
   * Save the final results.
   * @param os output stream
   * @throws IOException if an error occurs while writing data
   */
  public void saveFinalResults(final OutputStream os) throws IOException {

    final List<ExpressionTranscript> list =
        new ArrayList<ExpressionTranscript>(this.expressionResults.values());

    Collections.sort(list);

    final OutputStreamWriter osw = new OutputStreamWriter(os);

    osw
        .write("#ID\tType\tChromosome\tStart\tEnd\tStrand\tlength\tFullCovered\tBasesNotCovered\tRatio\tCount\n");
    for (ExpressionTranscript et : list)
      osw.write(et.toString() + "\n");

    osw.close();
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param indexFile index file
   */
  public FinalExpressionTranscriptsCreator(final File indexFile)
      throws IOException {

    this(FileUtils.createInputStream(indexFile));
  }

  /**
   * Public constructor.
   * @param indexIs index input stream
   */
  public FinalExpressionTranscriptsCreator(final InputStream indexIs)
      throws IOException {

    this.tef = new TranscriptAndExonFinder();
    tef.load(indexIs);
  }

  /**
   * Public constructor.
   * @param tef TranscriptAndExonFinder object
   */
  public FinalExpressionTranscriptsCreator(final TranscriptAndExonFinder tef) {

    if (tef == null)
      throw new NullPointerException("TranscriptAndExonFinder is null.");

    this.tef = tef;
  }

}
