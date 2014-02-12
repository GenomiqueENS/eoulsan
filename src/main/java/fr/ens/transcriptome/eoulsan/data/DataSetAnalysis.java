package fr.ens.transcriptome.eoulsan.data;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSetAnalysis {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final boolean expected;
  private final String dataSetPath;
  private final DataFile dataSet;

  private DataFile designFile;
  private DataFile paramFile;
  private DataFile eoulsanLog;

  private Map<String, DataFile> fileByName;

  // private Set<DataFile> allFiles;
  // private Map<String, Collection<DataFile>> allFilesInAnalysis;

  public void init() throws EoulsanException {

    parseDirectory(new File(dataSetPath));

    Collection<DataFile> files;

    // Check design file
    files = getDataFileStartwith("design");
    if (files.isEmpty()) {
      LOGGER.warning("Design file doesn't exist");
      throw new EoulsanException("Design file doesn't exist");
    }

    this.designFile = files.iterator().next();

    // Check parameter file
    files = getDataFileStartwith("param");
    if (files.isEmpty()) {
      LOGGER.warning("Parameter file doesn't exist");
      throw new EoulsanException("Parameter file doesn't exist");
    }

    this.paramFile = files.iterator().next();

    // Check log eoulsan file
    // files = getDataFileStartwith("eoulsan", ".log");
    // if (files.isEmpty()) {
    // LOGGER.warning("Log file doesn't exist");
    // throw new EoulsanException("Log file doesn't exist");
    // }
    // this.eoulsanLog = files.iterator().next();
  }

  private void parseDirectory(final File dir) {

    // // TODO
    // System.out.println(dir.getAbsolutePath()
    // + " dir " + StringUtils.join(dir.list(), "\n\t"));

    for (final File fileEntry : dir.listFiles()) {
      if (fileEntry.isDirectory()) {
        parseDirectory(fileEntry);
      } else {

        DataFile df = new DataFile(fileEntry);

        // Skip serizalisation file for bloomFilter
        if (!df.getExtension().equals(".ser")) {
          // Add entry in map
          fileByName.put(df.getName(), df);
        }

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
    for (DataFile df : datasetSource.getDataFileWithExtension(".fastq")) {

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

  /**
   * @return
   */
  public String getRootPath() {
    return dataSet.getBasename();
  }

  private Collection<DataFile> getDataFileWithExtension(final String extension) {
    Set<DataFile> files = Sets.newHashSet();

    for (Map.Entry<String, DataFile> entry : fileByName.entrySet()) {
      if (entry.getValue().getExtension().equals(extension))
        files.add(entry.getValue());
    }

    if (files.size() == 0)
      return Collections.emptySet();

    return Collections.unmodifiableSet(files);
  }

  private Collection<DataFile> getDataFileStartwith(final String prefix) {
    Set<DataFile> files = Sets.newHashSet();

    for (Map.Entry<String, DataFile> entry : fileByName.entrySet()) {
      if (entry.getKey().startsWith(prefix))
        files.add(entry.getValue());
    }

    if (files.size() == 0)
      return Collections.emptySet();

    return Collections.unmodifiableSet(files);
  }

  private Collection<DataFile> getDataFileStartwith(final String prefix,
      final String extension) {

    Set<DataFile> files = Sets.newHashSet();

    for (Map.Entry<String, DataFile> entry : fileByName.entrySet()) {
      if (entry.getKey().startsWith(prefix)
          && entry.getValue().getExtension().equals(extension))
        files.add(entry.getValue());
    }

    if (files.size() == 0)
      return Collections.emptySet();

    return Collections.unmodifiableSet(files);
  }

  /**
   * @param df
   * @return
   */
  public DataFile getDataFileByName(final String filename) {

    if (filename == null || filename.length() == 0)
      return null;

    return fileByName.get(filename);
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

  // public Map<String, Collection<DataFile>> getAllFilesInAnalysis() {
  // return allFilesInAnalysis;
  // }
  //
  // public Set<DataFile> getAllFiles() {
  // return allFiles;
  // }

  public Map<String, DataFile> getFilesByName() {
    return fileByName;
  }

  //
  // Constructor
  //

  public DataSetAnalysis(final String dataSetPath, final boolean expected)
      throws EoulsanException {

    this.expected = expected;
    this.dataSetPath = dataSetPath;
    this.dataSet = new DataFile(dataSetPath);

    this.fileByName = Maps.newHashMap();
    // this.allFilesInAnalysis = Maps.newHashMap();
    // this.allFiles = Sets.newHashSet();

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
