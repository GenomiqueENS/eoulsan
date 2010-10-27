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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;
import fr.ens.transcriptome.eoulsan.core.ExecutorInfo;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.expression.ExpressionStep;
import fr.ens.transcriptome.eoulsan.steps.expression.FinalExpressionTranscriptsCreator;

public class ExpressionLocalStep extends ExpressionStep {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  
  @Override
  public ExecutionMode getExecutionMode() {
    
    return Step.ExecutionMode.LOCAL;
  }
  
  @Override
  public String getLogName() {

    return "expression";
  }

  @Override
  public StepResult execute(final Design design, final ExecutorInfo info) {

    try {
      final long startTime = System.currentTimeMillis();
      final StringBuilder log = new StringBuilder();

      ExpressionPseudoMapReduce epmr = null;
      String lastAnnotationKey = null;
      final String genomicType = getGenomicType();

      for (Sample s : design.getSamples()) {

        final String annotationFilename =
            s.getMetadata().getAnnotation().trim();

        final String annotationKey = annotationFilename + " " + genomicType;

        if (!annotationKey.equals(lastAnnotationKey)) {

          epmr =
              new ExpressionPseudoMapReduce(new File(annotationFilename),
                  genomicType);

          lastAnnotationKey = annotationKey;
        }

        final File alignmentFile =
            new File(Common.SAMPLE_SOAP_ALIGNMENT_PREFIX
                + s.getId() + Common.SOAP_RESULT_EXTENSION);

        final File expressionTmpFile =
            new File(Common.SAMPLE_EXPRESSION_FILE_PREFIX
                + s.getId() + Common.SOAP_RESULT_EXTENSION + ".tmp");

        final File expressionFile =
            new File(Common.SAMPLE_EXPRESSION_FILE_PREFIX
                + s.getId() + Common.SAMPLE_EXPRESSION_FILE_SUFFIX);

        if (getTmpDir() != null)
          epmr.setMapReduceTemporaryDirectory(new File(getTmpDir()));
        epmr.doMap(alignmentFile);
        epmr.doReduce(expressionTmpFile);

        final FinalExpressionTranscriptsCreator fetc =
            new FinalExpressionTranscriptsCreator(epmr
                .getTranscriptAndExonFinder());

        fetc.initializeExpressionResults();
        fetc.loadPreResults(expressionTmpFile, epmr.getReporter()
            .getCounterValue(ExpressionPseudoMapReduce.COUNTER_GROUP,
                "reads used"));
        fetc.saveFinalResults(expressionFile);

        // Remove expression Temp file
        if (!expressionTmpFile.delete())
          logger.warning("Can not delete expression temporary file: "
              + expressionTmpFile.getAbsolutePath());

        // Add counters for this sample to log file
        log.append(epmr.getReporter().countersValuesToString(
            ExpressionPseudoMapReduce.COUNTER_GROUP,
            "Expression computation ("
                + s.getName() + ", " + alignmentFile.getName() + ", "
                + s.getMetadata().getAnnotation() + ", " + genomicType + ")"));

      }

      // Write log file
      return new StepResult(this, startTime, log.toString());

    } catch (FileNotFoundException e) {

      return new StepResult(this, e, "File not found: " + e.getMessage());
    } catch (IOException e) {

      return new StepResult(this, e, "Error while filtering: " + e.getMessage());
    } catch (BadBioEntryException e) {

      return new StepResult(this, e, "Invalid annotation entry: "
          + e.getEntry());
    }
  }
}
