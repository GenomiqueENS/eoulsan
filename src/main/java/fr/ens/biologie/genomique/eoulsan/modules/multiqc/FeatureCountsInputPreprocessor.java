package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.FEATURECOUNTS_SUMMARY_TXT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This class define a preprocessor for FeatureCounts reports.
 * @since 2.2
 * @author Laurent Jourdren
 */
public class FeatureCountsInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "featurecounts";

  @Override
  public String getReportName() {

    return REPORT_NAME;
  }

  @Override
  public DataFormat getDataFormat() {

    return FEATURECOUNTS_SUMMARY_TXT;
  }

  @Override
  public void preprocess(final TaskContext context, final Data data,
      final File multiQCInputDirectory) throws IOException {

    // Get data name
    String name = data.getName();

    // Define target log file
    DataFile summaryFile = data.getDataFile();

    // Define symbolic link path
    DataFile finalSummaryFile = new DataFile(multiQCInputDirectory,
        name + FEATURECOUNTS_SUMMARY_TXT.getDefaultExtension());

    // Create a summary file for MultiQC with a more explicit sample name
    if (summaryFile.exists()) {
      rewriteSummaryFile(summaryFile.toFile(), finalSummaryFile.toFile(), name);
    }

  }

  /**
   * Rewrite a summary file with a more explicit sample name
   * @param inputFile input summary file
   * @param outputFile output summary file
   * @param dataName name of the sample
   * @throws IOException if an error occurs while rewriting the summary file
   */
  private void rewriteSummaryFile(final File inputFile, final File outputFile,
      final String dataName) throws IOException {

    try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        Writer writer = new FileWriter(outputFile)) {

      String line = null;

      while ((line = reader.readLine()) != null) {

        if (line.startsWith("Status\t")) {
          writer.write("Status\t" + dataName + ".sam\n");
        } else {
          writer.write(line + '\n');
        }
      }
    }
  }

}
