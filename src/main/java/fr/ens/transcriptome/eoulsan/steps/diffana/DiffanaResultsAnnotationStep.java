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

package fr.ens.transcriptome.eoulsan.steps.diffana;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.core.ParallelizationMode.OWN_PARALLELIZATION;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ADDITIONAL_ANNOTATION_TSV;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_ODS;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_TSV;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATED_EXPRESSION_RESULTS_XLSX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
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
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step that create annotated expression files in TSV, ODS
 * or XLSX format.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
public class DiffanaResultsAnnotationStep extends AbstractStep {

  public static final String STEP_NAME = "diffanaresultsannotation";

  private static final DataFormat DEFAULT_FORMAT =
      ANNOTATED_EXPRESSION_RESULTS_TSV;

  private DataFile annotationFile;
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

    return "This step add annotation to diffana files.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    // If no annotation file is set in parameter use the annotation provided by
    // the workflow
    if (this.annotationFile == null) {

      builder.addPort("additionalannotation", ADDITIONAL_ANNOTATION_TSV);
    }

    return builder.create();
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

    for (final Parameter p : stepParameters) {

      // Set annotation file
      if ("annotationfile".equals(p.getName())) {
        this.annotationFile = new DataFile(p.getStringValue());
      } else if ("outputformat".equals(p.getName())) {

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
      } else {
        // Unknown option
        throw new EoulsanException("Unknown option: " + p.getName());
      }
    }

    // Check if annotation file exists
    if (this.annotationFile != null && !this.annotationFile.exists()) {
      throw new EoulsanException("The annotation file does not exists: "
          + this.annotationFile);
    }

    // Set the default format
    if (this.outputFormats.isEmpty()) {
      this.outputFormats.put(DEFAULT_FORMAT.getDefaultExtension().substring(1),
          DEFAULT_FORMAT);
    }
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Load translator
    final Translator translator;
    try {

      // TODO annotation may be shared by several threads

      if (this.annotationFile == null) {

        // If no annotation file parameter set
        Data annotationData = context.getInputData(ADDITIONAL_ANNOTATION_TSV);
        translator = loadTranslator(annotationData.getDataFile());
      } else {

        translator = loadTranslator(this.annotationFile);
      }

    } catch (IOException | EoulsanIOException e) {
      return status.createStepResult(e);
    }

    // Description string
    final StringBuilder descriptionString = new StringBuilder();

    try {

      final DataFile outputDir = context.getOutputDirectory();
      final List<DataFile> files = outputDir.list();
      final List<DataFile> filesToConvert = new ArrayList<>();

      // Filter files to convert
      for (DataFile f : files) {
        if (f.getName().startsWith("diffana_") && f.getName().endsWith(".tsv")) {
          filesToConvert.add(f);
        }
      }

      // Annotate all selected files
      for (DataFile inFile : filesToConvert) {

        // For each formats
        for (Map.Entry<String, DataFormat> e : this.outputFormats.entrySet()) {

          // Get format
          final DataFormat format = e.getValue();

          final String prefix =
              "annotated_"
                  + StringUtils.filenameWithoutExtension(inFile.getName());

          final TranslatorOutputFormat of;
          final DataFile outFile;

          if (format == ANNOTATED_EXPRESSION_RESULTS_XLSX) {

            // XLSX output
            outFile =
                new DataFile(outputDir, prefix
                    + ANNOTATED_EXPRESSION_RESULTS_XLSX.getDefaultExtension());
            of = new XLSXTranslatorOutputFormat(outFile.create());

          } else if (format == ANNOTATED_EXPRESSION_RESULTS_ODS) {

            // ODS output
            outFile =
                new DataFile(outputDir, prefix
                    + ANNOTATED_EXPRESSION_RESULTS_ODS.getDefaultExtension());
            of = new ODSTranslatorOutputFormat(outFile.create());

          } else {

            // TSV output
            outFile =
                new DataFile(outputDir, prefix
                    + ANNOTATED_EXPRESSION_RESULTS_TSV.getDefaultExtension());
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
      return status.createStepResult(e);
    }

    // Set the description of the context
    status.setDescription(descriptionString.toString());

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

        return new String[] { "EnsemblGeneID" };
      }
    };

    return new CommonLinksInfoTranslator(new ConcatTranslator(did,
        new MultiColumnTranslatorReader(annotationFile.open()).read()));

  }

}
