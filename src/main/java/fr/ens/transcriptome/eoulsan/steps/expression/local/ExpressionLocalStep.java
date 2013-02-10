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

package fr.ens.transcriptome.eoulsan.steps.expression.local;

import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.FILTERED_MAPPER_RESULTS_SAM;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.ExpressionCounter;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.HTSeqCounter;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.transcriptome.eoulsan.util.Reporter;

/**
 * This class is the step to compute expression in local mode
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@LocalOnly
public class ExpressionLocalStep extends AbstractExpressionStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  @Override
  public StepResult execute(final Design design, final Context context) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      final ExpressionCounter counter = getCounter();

      final String genomicType = getGenomicType();

      for (Sample s : design.getSamples()) {

        // Create the reporter
        final Reporter reporter = new Reporter();

        // Get annotation file
        final DataFile annotationFile =
            context.getInputDataFile(ANNOTATION_GFF, s);

        // Get alignment file
        final DataFile alignmentFile =
            context.getInputDataFile(FILTERED_MAPPER_RESULTS_SAM, s);

        // Get genome desc file
        final DataFile genomeDescFile =
            context.getInputDataFile(DataFormats.GENOME_DESC_TXT, s);

        // Get final expression file
        final DataFile expressionFile =
            context.getOutputDataFile(EXPRESSION_RESULTS_TXT, s);

        // Expression counting
        count(context, counter, annotationFile, alignmentFile, expressionFile,
            genomeDescFile, reporter);

        final String htSeqArgsLog =
            ", "
                + getAttributeId() + ", stranded: " + getStranded()
                + ", removeAmbiguousCases: " + isRemoveAmbiguousCases();

        log.append(reporter.countersValuesToString(COUNTER_GROUP,
            "Expression computation with "
                + counter.getCounterName() + " ("
                + s.getName()
                + ", "
                + alignmentFile.getName()
                + ", "
                + annotationFile.getName()
                + ", "
                + genomicType
                // If counter is HTSeq-count add additional parameters to log
                + (HTSeqCounter.COUNTER_NAME.equals(counter.getCounterName())
                    ? htSeqArgsLog : "") + ")"));

      }

      // Write log file
      return new StepResult(context, startTime, log.toString());

    } catch (FileNotFoundException e) {
      return new StepResult(context, e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      return new StepResult(context, e, "Error while filtering: "
          + e.getMessage());
    } catch (EoulsanException e) {
      return new StepResult(context, e,
          "Error while reading the annotation file: " + e.getMessage());
    } catch (BadBioEntryException e) {
      return new StepResult(context, e,
          "Error while reading the annotation file: " + e.getMessage());
    }

  }

  private void count(final Context context, final ExpressionCounter counter,
      final DataFile annotationFile, final DataFile alignmentFile,
      final DataFile expressionFile, final DataFile genomeDescFile,
      final Reporter reporter) throws IOException, EoulsanException,
      BadBioEntryException {

    // Init expression counter
    counter.init(getGenomicType(), getAttributeId(), reporter, COUNTER_GROUP);

    // Set counter arguments
    initCounterArguments(counter, context.getSettings().getTempDirectory());

    LOGGER.info("Expression computation in SAM file: "
        + alignmentFile + ", use " + counter.getCounterName());

    // Process to counting
    counter
        .count(alignmentFile, annotationFile, expressionFile, genomeDescFile);
  }

  private void initCounterArguments(ExpressionCounter counter,
      final String tempDirectory) {

    if (getStranded() != null)
      counter.setStranded(getStranded());

    if (getOverlapMode() != null)
      counter.setOverlapMode(getOverlapMode());

    counter.setRemoveAmbiguousCases(isRemoveAmbiguousCases());

    // Set counter temporary directory
    counter.setTempDirectory(tempDirectory);

  }

}
