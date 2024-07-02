package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;

/**
 * This class define a preprocessor for Cutadapt reports.
 * @since 2.7
 * @author Laurent Jourdren
 */
public class CutAdaptInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "cutadapt";

  private static final String INPUT_FILENAME_STRING = "Input filename: ";

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public DataFormat getDataFormat() {
    return DataFormatRegistry.getInstance()
        .getDataFormatFromNameOrAlias("cutadapt_report");
  }

  @Override
  public void preprocess(final TaskContext context, final Data data,
      final File multiQCInputDirectory) throws IOException {

    /// Get data name
    String name = data.getName();

    // Define metrics file
    DataFile reportFile = data.getDataFile();

    // Define the name of the output file
    DataFile outputFile =
        new DataFile(multiQCInputDirectory, name + "_trimming_report.txt");

    // Create output file
    if (!outputFile.exists()) {
      DataFiles.copy(reportFile, outputFile);
    }

    // Update report with the data name
    updateReportFile(outputFile.toFile().toPath(), name);
  }

  /**
   * Update Cutadapt report file with the name of sample.
   * @param reportPath the report file
   * @param name the name of the sample
   * @throws IOException if an error occurs while updating the file
   */
  private static void updateReportFile(Path reportPath, String name)
      throws IOException {

    List<String> lines = Files.readAllLines(reportPath);
    List<String> result = new ArrayList<>();

    String oldFilename = null;
    String newFilename = name + ".fq";

    for (String line : lines) {

      if (line.startsWith(INPUT_FILENAME_STRING)) {
        oldFilename = line.substring(INPUT_FILENAME_STRING.length());
      }

      if (oldFilename == null) {
        result.add(line);
      } else {
        result.add(line.replace(oldFilename, newFilename));
      }
    }

    Files.write(reportPath, result);
  }

}
