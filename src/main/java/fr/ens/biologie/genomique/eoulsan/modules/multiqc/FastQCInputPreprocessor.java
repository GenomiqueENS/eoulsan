package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;

/**
 * This class define a preprocessor for FastQC reports.
 * @since 2.2
 * @author Laurent Jourdren
 */
public class FastQCInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "fastqc";

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public DataFormat getDataFormat() {
    return DataFormats.FASTQC_REPORT_ZIP;
  }

  @Override
  public void preprocess(final TaskContext context, final Data data,
      final File multiQCInputDirectory) throws IOException {

    // Get data name
    String name = data.getName();

    int fileCount = data.getDataFileCount();

    for (int i = 0; i < fileCount; i++) {

      // Define symbolic link path
      DataFile symlink = new DataFile(multiQCInputDirectory,
          name + (fileCount <= 1 ? "" : "_read" + i) + "_fastqc.zip");

      // Define target log file
      DataFile fastQCReportFile = data.getDataFile(i);

      // Create symbolic link
      if (fastQCReportFile.exists()) {
        fastQCReportFile.symlink(symlink);
      }
    }

  }
}
