package fr.ens.transcriptome.eoulsan.data;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class DataSetAnalysis {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final boolean expected;
  private final String dataSetPath;
  private final DataFile dataSet;

  private DataFile designFile;
  private DataFile paramFile;
  private DataFile eoulsanLog;

  private Map<String, Collection<DataFile>> allFilesInAnalysis;

  public void init() throws EoulsanException {

    Multimap<String, DataFile> allFiles = ArrayListMultimap.create();
    parseDirectory(new File(dataSetPath), allFiles);

    allFilesInAnalysis = allFiles.asMap();

    for (DataFile df : allFilesInAnalysis.get(".txt")) {
      if (df.getName().startsWith("design")) {
        this.designFile = df;
      }
    }

    for (DataFile df : allFilesInAnalysis.get(".xml")) {
      if (df.getName().startsWith("param"))
        this.paramFile = df;
    }

    if (allFilesInAnalysis.containsKey(".log")) {
      for (DataFile df : allFilesInAnalysis.get(".log")) {
        if (df.getName().startsWith("eoulsan"))
          this.eoulsanLog = df;
      }
    }

    if (this.designFile == null) {
      LOGGER.warning("Design file doesn't exist");
      throw new EoulsanException("Design file doesn't exist");
    }

    if (this.paramFile == null) {
      LOGGER.warning("Parameter file doesn't exist");
      throw new EoulsanException("Parameter file doesn't exist");
    }
  }

  private void parseDirectory(final File dir,
      final Multimap<String, DataFile> allFiles) {

    for (final File fileEntry : dir.listFiles()) {
      if (fileEntry.isDirectory()) {
        parseDirectory(fileEntry, allFiles);
      } else {

        DataFile df = new DataFile(fileEntry);
        // Add entry in map
        allFiles.put(df.getExtension(), df);

      }
    }
  }

  public void buildDirectoryAnalysis(final DataSetAnalysis datasetSource)
      throws EoulsanException {
    if (expected)
      // Directory exists
      return;

    if (dataSet.toFile().exists())
      throw new EoulsanException("Test directory already exists here "
          + dataSetPath);

    // Create directory and tmp
    dataSet.toFile().mkdir();

    // TODO fail Eoulsan for test
    File tmp = new File(dataSetPath + "/tmp");
    tmp.mkdir();

    // Create symbolic link to fastq files
    for (DataFile df : datasetSource.getAllFilesInAnalysis().get(".fastq")) {

      // Only for fastq at the root directory analysis
      if (df.toFile().getParent().equals(datasetSource.getDataSetPath()))
        FileUtils.createSymbolicLink(df.toFile(), dataSet.toFile());
    }

    // Create symbolic link to design file
    FileUtils.createSymbolicLink(datasetSource.getDesignFile().toFile(),
        dataSet.toFile());

    // Create symbolic link to parameters file
    FileUtils.createSymbolicLink(datasetSource.getParamFile().toFile(),
        dataSet.toFile());

    // Initialization
    init();
  }

  public String getRootPath() {
    return dataSet.getBasename();
  }

  public DataFile getDataFileSameName(final DataFile df) {
    return getDataFileSameName(df.getExtension(), df.getName());
  }

  public DataFile getDataFileSameName(final String extension, final String name) {

    if (extension == null || extension.length() < 3)
      return null;

    if (name == null || name.length() == 0)
      return null;

    if (!allFilesInAnalysis.containsKey(extension))
      return null;

    for (DataFile df : allFilesInAnalysis.get(extension)) {
      if (df.getName().equals(name))
        return df;
    }

    return null;
  }

  //
  // Getter & Setter
  //

  public boolean isExpected() {
    return expected;
  }

  public String getDataSetPath() {
    return dataSetPath;
  }

  public DataFile getDesignFile() {
    return designFile;
  }

  public DataFile getParamFile() {
    return paramFile;
  }

  public DataFile getEoulsanLog() {
    return eoulsanLog;
  }

  public Map<String, Collection<DataFile>> getAllFilesInAnalysis() {
    return allFilesInAnalysis;
  }

  //
  // Constructor
  //

  public DataSetAnalysis(final String dataSetPath, final boolean expected)
      throws EoulsanException {
    this.expected = expected;
    this.dataSetPath = dataSetPath;
    this.dataSet = new DataFile(dataSetPath);

    if (this.expected) {
      // Check dataset directory exists
      if (!new File(this.dataSetPath).exists()) {
        throw new EoulsanException("Data set doesn't exist at this path "
            + dataSetPath);
      }
      init();
    }

  }

}
