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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.expression.local;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.FileNotFoundException;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.BadBioEntryException;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.ExpressionCounter;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter;
import fr.ens.biologie.genomique.eoulsan.core.StepContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.StepStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.steps.expression.AbstractExpressionStep;
import fr.ens.biologie.genomique.eoulsan.util.LocalReporter;
import fr.ens.biologie.genomique.eoulsan.util.Reporter;

/**
 * This class is the step to compute expression in local mode
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@LocalOnly
public class ExpressionLocalStep extends AbstractExpressionStep {

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    try {

      final Data featuresAnnotationData = context.getInputData(ANNOTATION_GFF);
      final Data alignmentData = context.getInputData(MAPPER_RESULTS_SAM);
      final Data genomeDescriptionData = context.getInputData(GENOME_DESC_TXT);
      final Data expressionData =
          context.getOutputData(EXPRESSION_RESULTS_TSV, alignmentData);

      final ExpressionCounter counter = getCounter();

      // Create the reporter
      final Reporter reporter = new LocalReporter();

      // Get annotation file
      final DataFile annotationFile = featuresAnnotationData.getDataFile();

      // Get alignment file
      final DataFile alignmentFile = alignmentData.getDataFile();

      // Get genome desc file
      final DataFile genomeDescFile = genomeDescriptionData.getDataFile();

      // Get final expression file
      final DataFile expressionFile = expressionData.getDataFile();

      // Expression counting
      count(context, counter, annotationFile, alignmentFile, expressionFile,
          genomeDescFile, reporter);

      final String htSeqArgsLog = ", "
          + getAttributeId() + ", stranded: " + getStranded()
          + ", removeAmbiguousCases: " + isRemoveAmbiguousCases();

      final String sampleCounterHeader = "Expression computation with "
          + counter.getCounterName() + " (" + alignmentData.getName() + ", "
          + alignmentFile.getName() + ", " + annotationFile.getName() + ", "
          + getGenomicType()
          // If counter is HTSeq-count add additional parameters to log
          + (HTSeqCounter.COUNTER_NAME.equals(counter.getCounterName())
              ? htSeqArgsLog : "")
          + ")";

      status.setDescription(sampleCounterHeader);
      status.setCounters(reporter, COUNTER_GROUP);

      // Write log file
      return status.createStepResult();

    } catch (FileNotFoundException e) {
      return status.createStepResult(e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      return status.createStepResult(e,
          "Error while computing expression: " + e.getMessage());
    } catch (EoulsanException e) {
      return status.createStepResult(e,
          "Error while reading the annotation file: " + e.getMessage());
    } catch (BadBioEntryException e) {
      return status.createStepResult(e,
          "Error while reading the annotation file: " + e.getMessage());
    }

  }

  private void count(final StepContext context, final ExpressionCounter counter,
      final DataFile annotationFile, final DataFile alignmentFile,
      final DataFile expressionFile, final DataFile genomeDescFile,
      final Reporter reporter)
          throws IOException, EoulsanException, BadBioEntryException {

    // Init expression counter
    counter.init(getGenomicType(), getAttributeId(), reporter, COUNTER_GROUP);

    // Set counter arguments
    initCounterArguments(counter,
        context.getLocalTempDirectory().getAbsolutePath());

    getLogger().info("Expression computation in SAM file: "
        + alignmentFile + ", use " + counter.getCounterName());

    // Process to counting
    counter.count(alignmentFile, annotationFile, expressionFile,
        genomeDescFile);
  }

  private void initCounterArguments(final ExpressionCounter counter,
      final String tempDirectory) {

    if (getStranded() != null) {
      counter.setStranded(getStranded());
    }

    if (getOverlapMode() != null) {
      counter.setOverlapMode(getOverlapMode());
    }

    counter.setRemoveAmbiguousCases(isRemoveAmbiguousCases());
    counter.setSplitAttributeValues(isSplitAttributeValues());

    // Set counter temporary directory
    counter.setTempDirectory(tempDirectory);
  }

}
