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
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules.expression;

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

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.kenetre.bio.expressioncounter.ExpressionCounter;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.Utils;

/**
 * This class generates the final expression file after counting the alignments
 * for each feature with HTSeq-count.
 * @since 1.2
 * @author Claire Wallon
 */
public class FinalExpressionFeaturesCreator {

  /* Default Charset. */
  private static final Charset CHARSET =
      Charset.forName(Globals.DEFAULT_FILE_ENCODING);

  private ExpressionCounter counter;
  private final Map<String, ExpressionFeature> expressionResults =
      new HashMap<>();

  private static final class ExpressionFeature
      implements Comparable<ExpressionFeature> {

    private final String id;
    private int alignmentCount = 0;

    public void setExpressionResult(final int alignmentCount) {

      this.alignmentCount = alignmentCount;
    }

    @Override
    public int compareTo(final ExpressionFeature o) {

      if (o == null) {
        return 1;
      }

      int diff = this.id.compareTo(o.id);

      if (diff != 0) {
        return diff;
      }

      return (o.alignmentCount - this.alignmentCount);

    }

    @Override
    public boolean equals(final Object o) {

      if (o == this) {
        return true;
      }

      if (!(o instanceof ExpressionFeature)) {
        return false;
      }

      final ExpressionFeature et = (ExpressionFeature) o;

      if (Utils.equal(this.id, et.id)
          && this.alignmentCount == et.alignmentCount) {
        return true;
      }

      return false;
    }

    @Override
    public int hashCode() {

      return Objects.hash(this.id, this.alignmentCount);
    }

    @Override
    public String toString() {

      return this.id + "\t" + this.alignmentCount;
    }

    //
    // Constructor
    //

    /**
     * Constructor for ExpressionTranscript.
     * @param id identifier to set
     */
    public ExpressionFeature(final String id) {

      if (id == null) {
        throw new NullPointerException("Identifier to add is null");
      }

      this.id = id;
    }

  }

  /**
   * Clear.
   */
  public void initializeExpressionResults() {

    this.expressionResults.clear();
    Map<String, Integer> emptyMap = new HashMap<>();
    this.counter.addZeroCountFeatures(emptyMap);
    for (String id : emptyMap.keySet()) {
      this.expressionResults.put(id, new ExpressionFeature(id));
    }
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
      final int alignmentCount = Integer.parseInt(tab[1]);

      if (this.expressionResults.containsKey(id)) {
        this.expressionResults.get(id).setExpressionResult(alignmentCount);
      }
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
        new ArrayList<>(this.expressionResults.values());

    Collections.sort(list);

    final OutputStreamWriter osw = new OutputStreamWriter(os, CHARSET);

    osw.write("Id\tCount\n");
    for (ExpressionFeature ef : list) {
      osw.write(ef.toString() + "\n");
    }

    osw.close();

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param counter the counter
   */
  public FinalExpressionFeaturesCreator(final ExpressionCounter counter) {

    if (counter == null) {
      throw new NullPointerException("counter argument is null.");
    }

    this.counter = counter;
  }

}
