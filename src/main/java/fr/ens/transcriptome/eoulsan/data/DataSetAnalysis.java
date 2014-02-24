package fr.ens.transcriptome.eoulsan.data;

import java.io.File;
import java.io.IOException;
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

public class DataSetAnalysis {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private final boolean exists;
  private final File dataSet;

  private DataFile designFile;
  private DataFile paramFile;
  private DataFile eoulsanLog;
  private Collection<File> fastqFiles;

  private Map<String, DataFile> fileByName;

  // private Set<DataFile> allFiles;
  // private Map<String, Collection<DataFile>> allFilesInAnalysis;

  public void init() throws EoulsanException {

    parseDirectory(this.dataSet);

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

  public void parseDirectory(final File directory) {

    // // TODO
    // System.out.println(dir.getAbsolutePath()
    // + " dir " + StringUtils.join(dir.list(), "\n\t"));

    for (final File fileEntry : directory.listFiles()) {
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

  public void buildDirectoryAnalysis() throws EoulsanException, IOException {
    if (exists)
      // Directory already exists
      return;

    if (this.dataSet.exists())
      throw new IOException("Test output directory already exists "
          + this.dataSet.getAbsolutePath());

    // Create test directory
    if (!this.dataSet.mkdirs())
      throw new IOException("Cannot create test output directory "
          + this.dataSet.getAbsolutePath());

    if (!new File(this.dataSet + "/tmp").mkdir())
      throw new IOException(
          "Cannot create tmp directory in test output directory "
              + this.dataSet.getAbsolutePath());

    // Create symbolic link to fastq files
    for (File fastq : fastqFiles) {

      // Only for fastq at the root directory analysis
      FileUtils.createSymbolicLink(fastq, this.dataSet);
    }

    // Create symbolic link to design file
    FileUtils.createSymbolicLink(this.designFile.toFile(), this.dataSet);
    this.designFile =
        new DataFile(new File(this.dataSet, this.designFile.getName()));

    // Create symbolic link to parameters file
    FileUtils.createSymbolicLink(this.paramFile.toFile(), this.dataSet);
    this.paramFile =
        new DataFile(new File(this.dataSet, this.paramFile.getName()));

    // Initialization
    // init();
  }

  // public void buildDirectoryAnalysis(final DataSetAnalysis datasetSource)
  // throws EoulsanException {
  // if (expected)
  // // Directory exists
  // return;
  //
  // if (dataSet.exists())
  // throw new EoulsanException("Test directory already exists here "
  // + dataSet);
  //
  // // Create directory and tmp
  // dataSet.mkdir();
  //
  // // TODO fail Eoulsan for test
  // File tmp = new File(dataSet + "/tmp");
  // tmp.mkdir();
  //
  // // Create symbolic link to fastq files
  // for (DataFile df : datasetSource.getDataFileWithExtension(".fastq")) {
  //
  // // Only for fastq at the root directory analysis
  // if (df.toFile().getParent().equals(datasetSource))
  // FileUtils.createSymbolicLink(df.toFile(), dataSet);
  // }
  //
  // // Create symbolic link to design file
  // FileUtils.createSymbolicLink(datasetSource.getDesignFile().toFile(),
  // dataSet);
  //
  // // Create symbolic link to parameters file
  // FileUtils
  // .createSymbolicLink(datasetSource.getParamFile().toFile(), dataSet);
  //
  // // Initialization
  // init();
  // }

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

  public boolean exists() {
    return exists;
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

  public DataSetAnalysis(final File dataSet, final boolean exists)
      throws EoulsanException {

    this.exists = exists;
    this.dataSet = dataSet;

    this.fileByName = Maps.newHashMap();
    // this.allFilesInAnalysis = Maps.newHashMap();
    // this.allFiles = Sets.newHashSet();

    if (this.exists) {
      // Check dataset directory exists
      if (!this.dataSet.exists()) {
        throw new EoulsanException("Data set doesn't exist at this path "
            + dataSet.getAbsolutePath());
      }
      init();
    }

  }

  public DataSetAnalysis(final File dataSet, final boolean expected,
      final File param, final DataSetTest dst) throws EoulsanException {

    this(dataSet, expected);

    this.designFile = new DataFile(dst.getDesignFile());
    this.paramFile = new DataFile(param);
    this.fastqFiles = dst.getFastqFiles();

  }

}
