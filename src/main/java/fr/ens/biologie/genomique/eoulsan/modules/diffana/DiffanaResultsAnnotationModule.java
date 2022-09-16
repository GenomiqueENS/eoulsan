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

package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode.OWN_PARALLELIZATION;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_ODS;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_XLSX;
import static fr.ens.biologie.genomique.eoulsan.util.EoulsanTranslatorUtils.getLinksFileFromSettings;
import static fr.ens.biologie.genomique.eoulsan.util.EoulsanTranslatorUtils.loadTranslator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.RequiresAllPreviousSteps;
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
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.kenetre.translator.Translator;
import fr.ens.biologie.genomique.kenetre.translator.TranslatorUtils;
import fr.ens.biologie.genomique.kenetre.translator.io.TSVTranslatorOutputFormat;
import fr.ens.biologie.genomique.kenetre.translator.io.TranslatorOutputFormat;
import fr.ens.biologie.genomique.kenetre.translator.io.XLSXTranslatorOutputFormat;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.Version;

/**
 * This class define a module that create annotated expression files in TSV, ODS
 * or XLSX format.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@RequiresAllPreviousSteps
public class DiffanaResultsAnnotationModule extends AbstractModule {

  public static final String MODULE_NAME = "diffanaresultsannotation";

  private static final DataFormat DEFAULT_FORMAT =
      ANNOTATED_EXPRESSION_RESULTS_TSV;

  private static final String DEFAULT_FILE_INPUT_GLOB_PATTERN =
      "{diffana_*.tsv,deseq2_*.tsv}";

  private final Map<String, DataFormat> outputFormats = new HashMap<>();

  private PathMatcher pathMatcher;
  private String outputPrefix;
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

    return "This module add annotation to diffana files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    // Add the port for the additional annotation
    if (this.useAdditionalAnnotationFile) {
      return InputPortsBuilder.singleInputPort(ADDITIONAL_ANNOTATION_TSV);
    }

    return InputPortsBuilder.noInputPort();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return OutputPortsBuilder.noOutputPort();
  }

  @Override
  public ParallelizationMode getParallelizationMode() {

    final Collection<DataFormat> formats = this.outputFormats.values();

    // XLSX and ODS file creation require lot of memory so multithreading is
    // disable to avoid out of memory
    if (formats.contains(ANNOTATED_EXPRESSION_RESULTS_ODS)
        || formats.contains(ANNOTATED_EXPRESSION_RESULTS_XLSX)) {
      return OWN_PARALLELIZATION;
    }

    // TSV creation can be multithreaded
    return ParallelizationMode.STANDARD;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String pattern = DEFAULT_FILE_INPUT_GLOB_PATTERN;
    this.outputPrefix = context.getCurrentStep().getId() + '_';

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

      case "files":
        pattern = p.getStringValue();
        break;

      case "output.prefix":
        this.outputPrefix = p.getStringValue();
        break;

      default:
        // Unknown option
        Modules.unknownParameter(context, p);
        break;
      }
    }

    // Set the default format
    if (this.outputFormats.isEmpty()) {
      this.outputFormats.put(DEFAULT_FORMAT.getDefaultExtension().substring(1),
          DEFAULT_FORMAT);
    }

    // Set the PathMatcher
    this.pathMatcher =
        FileSystems.getDefault().getPathMatcher("glob:" + pattern);
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Avoid issue with AWT in ODF Toolkit
    System.setProperty("javax.accessibility.assistive_technologies", "");

    // Get hypertext links file
    final DataFile linksFile = getLinksFileFromSettings(context.getSettings());

    // Load translator
    final Translator translator;

    try {

      if (this.useAdditionalAnnotationFile) {

        // If no annotation file parameter set
        Data additionalAnnotationData =
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

    // Description string
    final StringBuilder descriptionString = new StringBuilder();

    try {

      final DataFile outputDir = context.getStepOutputDirectory();
      final List<DataFile> files = new ArrayList<>();
      final List<DataFile> filesToConvert = new ArrayList<>();

      context.getLogger()
          .info("Search files in directory: " + context.getOutputDirectory());
      context.getLogger().info("Output directory: " + outputDir);

      // Handle step output directory
      for (DataFile f : context.getOutputDirectory().list()) {

        // Only handle existing files (not broken links)
        if (!f.exists()) {
          continue;
        }

        if (!f.getMetaData().isDir()) {
          files.add(f);
        } else if (f.getName().endsWith(Globals.STEP_OUTPUT_DIRECTORY_SUFFIX)) {
          files.addAll(f.list());
        }
      }

      // Filter files to convert
      for (DataFile f : files) {
        if (this.pathMatcher.matches(new File(f.getName()).toPath())) {
          filesToConvert.add(f);
        }
      }

      Set<String> processedFilenames = new HashSet<>();

      // Annotate all selected files
      for (DataFile inFile : filesToConvert) {

        // Do not process 2 times the same file
        if (processedFilenames.contains(inFile.getName())) {
          continue;
        } else {
          processedFilenames.add(inFile.getName());
        }

        // For each formats
        for (Map.Entry<String, DataFormat> e : this.outputFormats.entrySet()) {

          // Get format
          final DataFormat format = e.getValue();

          final String prefix = this.outputPrefix
              + StringUtils.filenameWithoutExtension(inFile.getName());

          final TranslatorOutputFormat of;
          final DataFile outFile;

          if (format == ANNOTATED_EXPRESSION_RESULTS_XLSX) {

            // XLSX output
            outFile = new DataFile(outputDir, prefix
                + ANNOTATED_EXPRESSION_RESULTS_XLSX.getDefaultExtension());
            checkIfFileExists(outFile, context);
            of = new XLSXTranslatorOutputFormat(outFile.create(),
                context.getLocalTempDirectory());

          } else {

            // TSV output
            outFile = new DataFile(outputDir, prefix
                + ANNOTATED_EXPRESSION_RESULTS_TSV.getDefaultExtension());
            checkIfFileExists(outFile, context);
            of = new TSVTranslatorOutputFormat(outFile.create());
          }

          TranslatorUtils.addTranslatorFields(inFile.open(), 0, translator, of);
          descriptionString.append("Convert ");
          descriptionString.append(inFile);
          descriptionString.append(" to ");
          descriptionString.append(outFile);
          descriptionString.append("\n");
        }
      }

    } catch (IOException e) {
      return status.createTaskResult(e);
    }

    // Set the description of the context
    status.setDescription(descriptionString.toString());

    // Return the result
    return status.createTaskResult();
  }

  /**
   * Check if the output file already exists.
   * @param file the output file
   * @param context the step context
   * @throws IOException if the the output file already exists
   */
  private static void checkIfFileExists(final DataFile file,
      final TaskContext context) throws IOException {

    if (file.exists()) {
      throw new IOException("Output file of the \""
          + context.getCurrentStep().getId() + "\" already exists: " + file);
    }

  }

}
