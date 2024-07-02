package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;

/**
 * This class define a preprocessor for Picard RnaSeqMetrics reports.
 * @since 2.7
 * @author Laurent Jourdren
 */
public class RnaSeqMetricsInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "rnaseqmetrics";

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public DataFormat getDataFormat() {
    return DataFormatRegistry.getInstance()
        .getDataFormatFromNameOrAlias("rna_metrics");
  }

  @Override
  public void preprocess(final TaskContext context, final Data data,
      final File multiQCInputDirectory) throws IOException {

    /// Get data name
    String name = data.getName();

    // Define metrics file
    DataFile metricsFile = data.getDataFile();

    // Define the name of the symbolic link
    DataFile outputFile =
        new DataFile(multiQCInputDirectory, name + ".RNA_Metrics");

    // Create output file
    if (!outputFile.exists()) {
      DataFiles.copy(metricsFile, outputFile);
    }

    // Update metrics file with the data name
    updateMetricsFile(outputFile.toFile().toPath(), name);
  }

  /**
   * Update RnaSeqMetrics report file with the name of sample.
   * @param reportPath the report file
   * @param name the name of the sample
   * @throws IOException if an error occurs while updating the file
   */
  private static void updateMetricsFile(Path reportPath, String name)
      throws IOException {

    List<String> lines = Files.readAllLines(reportPath);
    List<String> result = new ArrayList<>();

    for (String line : lines) {

      if (line.contains("INPUT")
          && line.toLowerCase().contains("rnaseqmetrics")) {

        Pattern pattern = Pattern.compile("INPUT(?:=|\\s+)([^\\s]+)",
            Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
          String oldPath = matcher.group(1);
          String extension = StringUtils.extension(oldPath);
          String newPath =
              new File(new File(oldPath).getParent(), name + extension)
                  .getPath();
          line = line.replace(oldPath, newPath);
        }
      }
      result.add(line);

    }

    Files.write(reportPath, result);
  }

}