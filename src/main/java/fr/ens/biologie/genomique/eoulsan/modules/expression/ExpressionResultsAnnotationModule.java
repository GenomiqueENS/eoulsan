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

import static fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode.OWN_PARALLELIZATION;
import static fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode.STANDARD;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_ODS;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_XLSX;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.util.EoulsanTranslatorUtils.loadTranslator;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.HadoopCompatible;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.EoulsanTranslatorUtils;
import fr.ens.biologie.genomique.kenetre.translator.Translator;
import fr.ens.biologie.genomique.kenetre.translator.TranslatorUtils;
import fr.ens.biologie.genomique.kenetre.translator.io.TSVTranslatorOutputFormat;
import fr.ens.biologie.genomique.kenetre.translator.io.TranslatorOutputFormat;
import fr.ens.biologie.genomique.kenetre.translator.io.XLSXTranslatorOutputFormat;

/**
 * This class define a module that create annotated expression files in TSV, ODS
 * or XLSX format.
 * @since 2.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class ExpressionResultsAnnotationModule extends AbstractModule {

  public static final String MODULE_NAME = "expressionresultsannotation";

  public static final String COUNTER_GROUP = "expressionresultsannotation";

  private static final DataFormat DEFAULT_FORMAT =
      ANNOTATED_EXPRESSION_RESULTS_TSV;

  private final Map<String, DataFormat> outputFormats = new HashMap<>();
  private boolean useAdditionalAnnotationFile = true;

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "This module add annotation to expression files";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    // Add the port for the expression file
    builder.addPort("expressionfile", EXPRESSION_RESULTS_TSV, true);

    // Add the port for the additional annotation
    if (this.useAdditionalAnnotationFile) {
      builder.addPort("additionalannotation", ADDITIONAL_ANNOTATION_TSV, true);
    }

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
        Modules.removedParameter(context, p);
        break;

      case "use.additional.annotation.file":
        this.useAdditionalAnnotationFile = p.getBooleanValue();
        break;

      case "outputformat":
        Modules.renamedParameter(context, p, "output.format");
      case "output.format":

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
        Modules.unknownParameter(context, p);
      }
    }

    // Set the default format
    if (this.outputFormats.isEmpty()) {
      this.outputFormats.put(DEFAULT_FORMAT.getDefaultExtension().substring(1),
          DEFAULT_FORMAT);
    }
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Avoid issue with AWT in ODF Toolkit
    System.setProperty("javax.accessibility.assistive_technologies", "");

    // Get hypertext links file
    final DataFile linksFile =
        EoulsanTranslatorUtils.getLinksFileFromSettings(context.getSettings());

    // Load translator
    final Translator translator;
    try {

      if (this.useAdditionalAnnotationFile) {

        // If no annotation file parameter set
        final Data additionalAnnotationData =
            context.getInputData(ADDITIONAL_ANNOTATION_TSV);

        // Create translator with additional annotation file
        translator =
            loadTranslator(additionalAnnotationData.getDataFile(), linksFile);
      } else {
        // Create translator without additional annotation file
        translator = loadTranslator(linksFile);
      }

    } catch (IOException e) {
      return status.createTaskResult(e);
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
          of = new XLSXTranslatorOutputFormat(outFile.create(),
              context.getLocalTempDirectory());
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
      return status.createTaskResult(e);
    }

    // Set the description of the context
    status.setDescription(resultString.toString());

    // Return the result
    return status.createTaskResult();
  }


}
