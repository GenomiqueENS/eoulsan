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

package fr.ens.transcriptome.eoulsan.bio.expressioncounters;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This abstract class implements a generic Expression Counter.
 * @since 1.2
 * @author Claire Wallon
 */
public abstract class AbstractExpressionCounter implements ExpressionCounter {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private String genomicType;
  private String stranded;
  private String overlapMode;
  private Reporter reporter;
  private String counterGroup;
  private String tempDir = EoulsanRuntime.getSettings().getTempDirectory();
  
  //
  // Getters
  //

  public String getStranded() {
    return this.stranded;
  }

  public String getOverlapMode() {
    return this.overlapMode;
  }

  public String getTempDirectory() {
    return this.tempDir;
  }
  
  public String getGenomicType() {
    return this.genomicType;
  }

  //
  // Setters
  //

  public void setStranded(String stranded) {

    if (stranded == null)
      this.stranded = "false";
    else
      this.stranded = stranded;
  }

  public void setOverlapMode(String mode) {

    if (mode == null)
      this.overlapMode = "union";
    else
      this.overlapMode = mode;
  }

  public void setTempDirectory(String tempDirectory) {

    this.tempDir = tempDirectory;
  }

  //
  // Counting
  //

  public final void count(final File alignmentFile,
      final DataFile annotationFile, final File expressionFile,
      final DataFile genomeDescFile) throws IOException {

    LOGGER.fine("Counting with " + getCounterName());

    checkNotNull(alignmentFile, "alignmentFile is null");
    checkNotNull(annotationFile, "annotationFile is null");
    checkNotNull(expressionFile, "expressionFile is null");
    checkExistingStandardFile(alignmentFile,
        "alignmentFile not exits or is not a standard file.");

    // Process to counting
    internalCount(alignmentFile, annotationFile, expressionFile,
        genomeDescFile, this.reporter, this.counterGroup);
  }

  protected abstract void internalCount(final File alignmentFile,
      final DataFile annotationFile, final File expressionFile,
      final DataFile genomeDescFile, Reporter reporter, String counterGroup)
      throws IOException;

  //
  // Init
  //

  /**
   * Initialize counter.
   * @param incrementer Objet to use to increment counters
   * @param counterGroup counter name group
   */
  @Override
  public void init(final String genomicType, final Reporter reporter,
      final String counterGroup) {

    checkNotNull(reporter, "reporter is null");
    checkNotNull(counterGroup, "counterGroup is null");

    this.genomicType = genomicType;
    this.reporter = reporter;
    this.counterGroup = counterGroup;
  }

}
