package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import static fr.ens.biologie.genomique.kenetre.util.StringUtils.filenameWithoutExtension;

import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import java.io.File;
import java.io.IOException;

/**
 * This class define a preprocessor for mapper reports.
 *
 * @since 2.2
 * @author Laurent Jourdren
 */
public class MapperInputPreprocessor implements InputPreprocessor {

  public static final String REPORT_NAME = "mapreads";

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  private static final String STAR_LOG_SUFFIX = "Log.final.out";

  @Override
  public DataFormat getDataFormat() {
    return DataFormats.MAPPER_RESULTS_LOG;
  }

  @Override
  public void preprocess(
      final TaskContext context, final Data data, final File multiQCInputDirectory)
      throws IOException {

    // Get data name
    String name = data.getName();

    // Define symbolic link path
    DataFile symlink = new DataFile(multiQCInputDirectory, name + ".samlog");

    // Define target log file
    DataFile logFile = data.getDataFile();

    // Exits if the log file does not exists
    if (!logFile.exists()) {
      return;
    }

    // Resolve symbolic links
    logFile = logFile.toRealDataFile();

    // Create symbolic link
    logFile.symlink(symlink);

    // STAR log
    final DataFile starLog =
        new DataFile(
            logFile.getParent(),
            filenameWithoutExtension(logFile.getName()) + '.' + STAR_LOG_SUFFIX);

    // If STAR log exists create symbolic link to this log file
    if (starLog.exists()) {
      DataFile starLogSymlink = new DataFile(multiQCInputDirectory, name + '.' + STAR_LOG_SUFFIX);
      starLog.symlink(starLogSymlink);
    }
  }
}
