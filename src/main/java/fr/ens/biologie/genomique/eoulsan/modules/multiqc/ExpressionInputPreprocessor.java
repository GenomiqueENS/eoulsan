package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.modules.expression.AbstractExpressionModule;
import fr.ens.biologie.genomique.eoulsan.modules.expression.ExpressionCounterCounter;

/**
 * This class define a preprocessor for expression reports.
 * @since 2.2
 * @author Laurent Jourdren
 */
public class ExpressionInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "expression";
  private Map<String, Map<String, Integer>> sampleStats;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public DataFormat getDataFormat() {
    return DataFormats.EXPRESSION_RESULTS_TSV;
  }

  @Override
  public void preprocess(final TaskContext context, final Data data,
      final File multiQCInputDirectory) throws IOException {

    if (this.sampleStats == null) {
      loadExpressionResultStats(context);
    }

    // Get data name
    String name = data.getName();

    // Define expression result file
    DataFile expressionFile = data.getDataFile();

    // Define the new File
    DataFile newFile = new DataFile(multiQCInputDirectory, name + ".txt");

    // If expression file exists, create a copy of the expression file enhanced
    // with HTSeq-count statistics entries
    if (expressionFile.exists()) {
      enhanceExpressionFile(expressionFile.toFile(), newFile.toFile(),
          this.sampleStats.get(name));
    }
  }

  /**
   * Load expression step result counters.
   * @param context the step counters
   * @throws IOException if an error occurs while reading the expression step
   *           result file
   */
  private void loadExpressionResultStats(final TaskContext context)
      throws IOException {

    for (Step step : context.getWorkflow().getSteps()) {

      if (AbstractExpressionModule.MODULE_NAME.equals(step.getModuleName())) {

        final String stepId = step.getId();

        final DataFile expressionStepResultFile = new DataFile(
            context.getJobDirectory(), stepId + Globals.STEP_RESULT_EXTENSION);

        // Parse result file if exists
        if (expressionStepResultFile.exists()) {
          this.sampleStats = parseStepResultFile(expressionStepResultFile);
        }
      }
    }
  }

  private static Map<String, Map<String, Integer>> parseStepResultFile(
      DataFile file) throws IOException {

    // Define result
    final Map<String, Map<String, Integer>> result = new HashMap<>();

    // Load JSON file in memory
    JsonReader jsonReader = Json.createReader(file.open());
    JsonObject object = jsonReader.readObject();
    jsonReader.close();

    // Get the "Counters" section of the JSON file
    JsonObject counters = object.getJsonObject("Counters");
    if (counters == null) {
      return result;
    }

    // Parse the content of the "Counters" section and put it in the result
    for (Map.Entry<String, JsonValue> e : counters.entrySet()) {

      String sampleId = e.getKey();
      Map<String, Integer> stats = new HashMap<>();
      result.put(sampleId, stats);

      JsonObject dict = counters.getJsonObject(sampleId);

      for (ExpressionCounterCounter ec : ExpressionCounterCounter.values()) {

        if (ec.htSeqCountCounterName() != null) {

          if (!dict.containsKey(ec.counterName())) {
            stats.put(ec.htSeqCountCounterName(), 0);
          } else {
            stats.put(ec.htSeqCountCounterName(),
                dict.getInt(ec.counterName()));
          }
        }
      }
    }

    return result;
  }

  /**
   * Create a copy of the expression result files and add at the end of the
   * files the HTSeq-count statistics entries
   * @param inFile input file
   * @param outFile output file
   * @param stats sample expression statistics
   * @throws IOException if an error occurs while reading or writing the files
   */
  private void enhanceExpressionFile(final File inFile, final File outFile,
      final Map<String, Integer> stats) throws IOException {

    try (BufferedReader reader = new BufferedReader(new FileReader(inFile));
        Writer writer = new FileWriter(outFile)) {

      String line;

      while ((line = reader.readLine()) != null) {
        writer.write(line + '\n');
      }

      if (stats != null) {
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
          writer.write("__" + e.getKey() + "\t" + e.getValue() + "\n");
        }
      }
    }

  }

}
