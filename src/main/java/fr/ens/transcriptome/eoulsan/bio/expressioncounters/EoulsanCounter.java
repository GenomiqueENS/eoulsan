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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;

import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionTranscriptsCreator;
import fr.ens.transcriptome.eoulsan.steps.expression.local.ExpressionPseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class defines a wrapper for the homemade algorithme for the expression
 * step.
 * @since 1.2
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
public class EoulsanCounter extends AbstractExpressionCounter {

  /** Counter name. */
  public static final String COUNTER_NAME = "eoulsanCounter";

  @Override
  public String getCounterName() {

    return COUNTER_NAME;
  }

  @Override
  protected void internalCount(final DataFile alignmentFile,
      final DataFile annotationFile, final DataFile expressionFile,
      final DataFile genomeDescFile, final Reporter reporter,
      final String counterGroup) throws IOException, BadBioEntryException {

    ExpressionPseudoMapReduce epmr = null;

    // Get expression temporary file
    final File expressionTmpFile =
        new File(alignmentFile.toFile().getAbsolutePath() + ".tmp");

    // try {

    epmr =
        new ExpressionPseudoMapReduce(annotationFile.open(), getGenomicType(),
            getAttributeId(), genomeDescFile.open(), counterGroup);

    if (getTempDirectory() != null) {
      epmr.setMapReduceTemporaryDirectory(new File(getTempDirectory()));
    }
    epmr.doMap(alignmentFile.open());
    epmr.doReduce(expressionTmpFile);

    final FinalExpressionTranscriptsCreator fetc =
        new FinalExpressionTranscriptsCreator(epmr.getTranscriptAndExonFinder());

    fetc.initializeExpressionResults();
    fetc.loadPreResults(expressionTmpFile,
        epmr.getReporter().getCounterValue(counterGroup, "reads used"));
    fetc.saveFinalResults(expressionFile.toFile());

    // Remove expression Temp file
    if (!expressionTmpFile.delete()) {
      getLogger().warning(
          "Can not delete expression temporary file: "
              + expressionTmpFile.getAbsolutePath());
    }

    // } catch (BadBioEntryException e) {
    // exit("Invalid annotation entry: " + e.getEntry());
    // }
  }

}
