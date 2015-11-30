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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.core.ParallelizationMode.OWN_PARALLELIZATION;
import static fr.ens.transcriptome.eoulsan.core.ParallelizationMode.STANDARD;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_ODS;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_TSV;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_XLSX;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.ParallelizationMode;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.EoulsanIOException;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.translators.BasicTranslator;
import fr.ens.transcriptome.eoulsan.translators.CommonLinksInfoTranslator;
import fr.ens.transcriptome.eoulsan.translators.ConcatTranslator;
import fr.ens.transcriptome.eoulsan.translators.Translator;
import fr.ens.transcriptome.eoulsan.translators.TranslatorUtils;
import fr.ens.transcriptome.eoulsan.translators.io.MultiColumnTranslatorReader;
import fr.ens.transcriptome.eoulsan.translators.io.ODSTranslatorOutputFormat;
import fr.ens.transcriptome.eoulsan.translators.io.TSVTranslatorOutputFormat;
import fr.ens.transcriptome.eoulsan.translators.io.TranslatorOutputFormat;
import fr.ens.transcriptome.eoulsan.translators.io.XLSXTranslatorOutputFormat;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step that create annotated expression files in TSV, ODS
 * or XLSX format.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class ExpressionResultsAnnotationStep extends AbstractStep {

  public static final String STEP_NAME = "expressionresultsannotation";

  public static final String COUNTER_GROUP = "expressionresultsannotation";

  private static final DataFormat DEFAULT_FORMAT =
      ANNOTATED_EXPRESSION_RESULTS_TSV;

  private final Map<String, DataFormat> outputFormats = new HashMap<>();

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step add annotation to expression files";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    // Add the port for the expression file
    builder.addPort("expressionfile", EXPRESSION_RESULTS_TSV);

    // Add the port for the additional annotation
    builder.addPort("additionalannotation", ADDITIONAL_ANNOTATION_TSV);

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    final OutputPortsBuilder builder = new OutputPortsBuilder();

    // Set the output ports
    for (Map.Entry<String, DataFormat> e : this.outputFormats.entrySet()) {
      builder.addPort(e.getKey() + "output", e.getValue());
    }

    return builder.create();
  }

  @Override
  public ParallelizationMode getParallelizationMode() {

    final Collection<DataFormat> formats = this.outputFormats.values();

    // XLSX and ODS file creation require lot of memory so multithreading is
    // disable in local mode to avoid out of memory when several files are
    // processing at the same time
    if (EoulsanRuntime.getRuntime().getMode() == EoulsanExecMode.LOCAL
        && (formats.contains(ANNOTATED_EXPRESSION_RESULTS_ODS)
            || formats.contains(ANNOTATED_EXPRESSION_RESULTS_XLSX))) {
      return OWN_PARALLELIZATION;
    }

    // TSV creation can be multithreaded
    return STANDARD;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (final Parameter p : stepParameters) {

      switch (p.getName()) {

      case "annotationfile":
        throw new EoulsanException("The option \""
            + p.getName() + "\" has been removed from "
            + context.getCurrentStep().getStepName() + " step");

      case "outputformat":

        // Set output format
        for (String format : Splitter.on(',').trimResults().omitEmptyStrings()
            .split(p.getValue())) {

          switch (format) {

          case "tsv":
            this.outputFormats.put(format, ANNOTATED_EXPRESSION_RESULTS_TSV);
            break;

          case "ods":
            this.outputFormats.put(format, ANNOTATED_EXPRESSION_RESULTS_ODS);
            break;

          case "xlsx":
            this.outputFormats.put(format, ANNOTATED_EXPRESSION_RESULTS_XLSX);
            break;

          default:
            throw new EoulsanException("Unknown output format: " + format);
          }
        }

        break;

      default:
        // Unknown option
        throw new EoulsanException("Unknown option: " + p.getName());
      }
    }

    // Set the default format
    if (this.outputFormats.isEmpty()) {
      this.outputFormats.put(DEFAULT_FORMAT.getDefaultExtension().substring(1),
          DEFAULT_FORMAT);
    }
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    // Load translator
    final Translator translator;
    try {

      // If no annotation file parameter set
      Data annotationData = context.getInputData(ADDITIONAL_ANNOTATION_TSV);
      translator = loadTranslator(annotationData.getDataFile());

    } catch (IOException | EoulsanIOException e) {
      return status.createStepResult(e);
    }

    // Define Result
    StringBuilder resultString = new StringBuilder();

    // Convert to TSV
    try {

      final Data inData = context.getInputData(EXPRESSION_RESULTS_TSV);

      final DataFile inFile = inData.getDataFile();

      // For each formats
      for (Map.Entry<String, DataFormat> e : this.outputFormats.entrySet()) {

        // Get format
        final DataFormat format = e.getValue();

        final Data outData = context.getOutputData(format, inData);

        final DataFile outFile = outData.getDataFile();

        final TranslatorOutputFormat of;

        if (format == ANNOTATED_EXPRESSION_RESULTS_XLSX) {
          of = new XLSXTranslatorOutputFormat(outFile.create());
        } else if (format == ANNOTATED_EXPRESSION_RESULTS_ODS) {
          of = new ODSTranslatorOutputFormat(outFile.create());
        } else {
          of = new TSVTranslatorOutputFormat(outFile.create());
        }

        TranslatorUtils.addTranslatorFields(inFile.open(), 0, translator, of);
        resultString.append("Convert ");
        resultString.append(inFile);
        resultString.append(" to ");
        resultString.append(outFile);
        resultString.append('\n');
      }

    } catch (IOException e) {
      return status.createStepResult(e);
    }

    // Set the description of the context
    status.setDescription(resultString.toString());

    // Return the result
    return status.createStepResult();
  }

  //
  // Other methods
  //

  /**
   * Load translator annotation.
   * @param annotationFile the annotation file to use
   * @return a Translator object with the additional annotation
   * @throws EoulsanIOException if an error occurs while reading additional
   *           annotation
   * @throws IOException if an error occurs while reading additional annotation
   */
  private Translator loadTranslator(final DataFile annotationFile)
      throws EoulsanIOException, IOException {

    checkNotNull(annotationFile, "annotationFile argument cannot be null");

    final Translator did = new BasicTranslator() {

      @Override
      public String translateField(final String id, final String field) {

        if (id == null || field == null) {
          return null;
        }

        if ("EnsemblGeneID".equals(field)
            && id.length() == 18 && id.startsWith("ENS")) {
          return id;
        }

        return null;
      }

      @Override
      public String[] getFields() {

        return new String[] {"EnsemblGeneID"};
      }
    };

    return new CommonLinksInfoTranslator(new ConcatTranslator(did,
        new MultiColumnTranslatorReader(annotationFile.open()).read()));

  }

}
