package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;

/**
 * This class define a preprocessor for Picard RnaSeqMetrics reports.
 * @since 2.7
 * @author Laurent Jourdren
 */
public class RnaSeqMetricsPreprocessor implements InputPreprocessor {

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
    DataFile symLink =
        new DataFile(multiQCInputDirectory, name + ".RNA_Metrics");

    // Create the symlink
    metricsFile.symlink(symLink);
  }
}