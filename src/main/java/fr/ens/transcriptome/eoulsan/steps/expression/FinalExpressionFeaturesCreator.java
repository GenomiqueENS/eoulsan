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

package fr.ens.transcriptome.eoulsan.steps.expression;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.GenomicArray;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class generates the final expression file after counting the alignments
 * for each feature with HTSeq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class FinalExpressionFeaturesCreator {

  /* Default Charset. */
  private static final Charset CHARSET = Charset
      .forName(Globals.DEFAULT_FILE_ENCODING);

  private GenomicArray<String> ga = new GenomicArray<String>();
  private final Map<String, ExpressionFeature> expressionResults =
      new HashMap<String, ExpressionFeature>();

  private static final class ExpressionFeature implements
      Comparable<ExpressionFeature> {

    private String id;
    private int alignementCount = 0;

    public void setExpressionResult(final int alignementCount) {

      this.alignementCount = alignementCount;
    }

    @Override
    public int compareTo(final ExpressionFeature o) {

      if (o == null)
        return 1;

      int diff = this.id.compareTo(o.id);

      if (diff != 0)
        return diff;

      return (o.alignementCount - this.alignementCount);

    }

    @Override
    public boolean equals(final Object o) {

      if (o == this)
        return true;

      if (!(o instanceof ExpressionFeature))
        return false;

      final ExpressionFeature et = (ExpressionFeature) o;

      if (Utils.equal(this.id, et.id)
          && this.alignementCount == et.alignementCount)
        return true;

      return false;
    }

    @Override
    public int hashCode() {

      return Objects.hash(this.id, this.alignementCount);
    }

    @Override
    public String toString() {

      return this.id + "\t" + this.alignementCount;
    }

    //
    // Constructor
    //

    /**
     * Constructor for ExpressionTranscript.
     * @param id identifier to set
     */
    public ExpressionFeature(String id) {

      if (id == null)
        throw new NullPointerException("Identifier to add is null");

      this.id = id;
    }

  }

  /**
   * Clear.
   */
  public void initializeExpressionResults() {

    this.expressionResults.clear();
    for (String id : ga.getFeaturesIds())
      this.expressionResults.put(id, new ExpressionFeature(id));
  }

  /**
   * Load pre result file.
   * @param preResultFile pre-result file
   * @throws IOException if an error occurs while reading data
   */
  public void loadPreResults(final File preResultFile) throws IOException {

    loadPreResults(FileUtils.createInputStream(preResultFile));
  }

  /**
   * Load pre-result file.
   * @param is input stream of pre-results
   * @throws IOException if an error occurs while reading data
   */
  public void loadPreResults(final InputStream is) throws IOException {

    final BufferedReader br =
        new BufferedReader(new InputStreamReader(is, CHARSET));

    final String[] tab = new String[2];
    String line = null;

    while ((line = br.readLine()) != null) {

      StringUtils.fastSplit(line, tab);

      final String id = tab[0];
      final int alignementCount = Integer.parseInt(tab[1]);

      if (this.expressionResults.containsKey(id))
        this.expressionResults.get(id).setExpressionResult(alignementCount);
    }

    br.close();
  }

  /**
   * Save the final results.
   * @param resultFile output result file
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

    final List<ExpressionFeature> list =
        new ArrayList<ExpressionFeature>(this.expressionResults.values());

    Collections.sort(list);

    final OutputStreamWriter osw = new OutputStreamWriter(os, CHARSET);

    osw.write("Id\tCount\n");
    for (ExpressionFeature ef : list)
      osw.write(ef.toString() + "\n");

    osw.close();

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param indexFile index file
   */
  public FinalExpressionFeaturesCreator(final File indexFile)
      throws IOException {

    this(FileUtils.createInputStream(indexFile));
  }

  /**
   * Public constructor.
   * @param indexIs index input stream
   */
  public FinalExpressionFeaturesCreator(final InputStream indexIs)
      throws IOException {

    this.ga = new GenomicArray<String>();
    this.ga.load(indexIs);
  }

  /**
   * Public constructor.
   * @param ga GenomicArray object
   */
  public FinalExpressionFeaturesCreator(final GenomicArray<String> ga) {

    if (ga == null)
      throw new NullPointerException("GenomicArray is null.");

    this.ga = ga;
  }

}
