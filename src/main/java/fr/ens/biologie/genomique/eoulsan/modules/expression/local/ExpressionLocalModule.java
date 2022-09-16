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

package fr.ens.biologie.genomique.eoulsan.modules.expression.local;

import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GTF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.expression.AbstractExpressionModule;
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterUtils;
import fr.ens.biologie.genomique.kenetre.KenetreException;
import fr.ens.biologie.genomique.kenetre.bio.expressioncounter.ExpressionCounter;
import fr.ens.biologie.genomique.kenetre.bio.io.CountsWriter;
import fr.ens.biologie.genomique.kenetre.bio.io.TSVCountsWriter;
import fr.ens.biologie.genomique.kenetre.util.LocalReporter;
import fr.ens.biologie.genomique.kenetre.util.Reporter;

/**
 * This class is the module to compute expression in local mode
 * @since 1.0
 * @author Laurent Jourdren
 * @author Claire Wallon
 */
@LocalOnly
public class ExpressionLocalModule extends AbstractExpressionModule {

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      final Data featuresAnnotationData = context
          .getInputData(isGTFInputFormat() ? ANNOTATION_GTF : ANNOTATION_GFF);
      final Data alignmentData = context.getInputData(MAPPER_RESULTS_SAM);
      final Data genomeDescriptionData = context.getInputData(GENOME_DESC_TXT);
      final Data expressionData = context.getOutputData(
          isSAMOutputFormat() ? MAPPER_RESULTS_SAM : EXPRESSION_RESULTS_TSV,
          alignmentData);

      final ExpressionCounter counter = getExpressionCounter();

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

      // Initialize the counter
      ExpressionCounterUtils.init(counter, genomeDescFile, annotationFile,
          isGTFInputFormat());

      final String sampleCounterHeader = "Expression computation with "
          + counter.getName() + " (" + alignmentData.getName() + ", "
          + alignmentFile.getName() + ", " + annotationFile.getName() + ", "
          + counter.toString() + ")";

      status.setDescription(sampleCounterHeader);

      final Map<String, Integer> result;

      if (isSAMOutputFormat()) {

        result = counter.count(alignmentFile.open(), expressionFile.create(),
            context.getLocalTempDirectory(), reporter, COUNTER_GROUP);
      } else {
        // Launch counting
        result = ExpressionCounterUtils.count(counter, alignmentFile, reporter,
            COUNTER_GROUP);

        // Add features with zero count
        counter.addZeroCountFeatures(result);

        // Save result
        try (CountsWriter writer =
            new TSVCountsWriter(expressionFile.create())) {
          writer.write(result);
        }
      }

      status.setCounters(reporter, COUNTER_GROUP);

      // Write log file
      return status.createTaskResult();

    } catch (

    FileNotFoundException e) {
      return status.createTaskResult(e, "File not found: " + e.getMessage());
    } catch (IOException e) {
      return status.createTaskResult(e,
          "Error while computing expression: " + e.getMessage());
    } catch (KenetreException e) {
      return status.createTaskResult(e,
          "Error while reading the annotation file: " + e.getMessage());
    }
  }

}
