package fr.ens.transcriptome.eoulsan.data;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.actions.ValidationAction;
import fr.ens.transcriptome.eoulsan.util.FileUtils.PrefixFilenameFilter;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSetAnalysis {

  /** Logger */
  private static final Logger LOGGER_TEST = Logger.getLogger(Globals.APP_NAME);
  private static final Logger LOGGER_GLOBAL = Logger
      .getLogger(ValidationAction.LOGGER_TESTS_GLOBAL);

  private final static Splitter splitter = Splitter.on(',').trimResults()
      .omitEmptyStrings();
  private static final String PATTERNS_INPUT_FILES_KEY = "patterns_input_files";

  private final Properties propsTest;

  private final File inputDataDirectory;
  private final File outputDataDirectory;

  private boolean exists = true;
  private File designFile;
  private File paramFile;
  private File eoulsanLog;

  private Map<String, DataFile> filesByName;

  public void parseDirectory() throws IOException, EoulsanException {
    parseDirectory(this.outputDataDirectory);
  }

  private void parseDirectory(final File directory) throws IOException,
      EoulsanException {

    for (final File fileEntry : directory.listFiles()) {
      if (fileEntry.isDirectory()) {
        parseDirectory(fileEntry);
      } else {

        DataFile df = new DataFile(fileEntry);

        // Check two paths with same filename linked the same file
        if (filesByName.containsKey(df.getName())) {
          File firstFile =
              filesByName.get(df.getName()).toFile().getCanonicalFile();
          File secondFile = df.toFile().getCanonicalFile();

          if (!firstFile.equals(secondFile))
            throw new EoulsanException(
                "Fail parsing analysis directory, they are two differents files with the same filename");
        }
        // Skip serialization file created by bloomFilter
        if (!df.getExtension().equals(".ser")) {

          // Add entry in map
          filesByName.put(df.getName(), df);
        }
      }
    }

  }

  private void buildDirectoryAnalysis() throws EoulsanException, IOException {

    if (this.exists) {
      // Check directory already exists
      // checkInputFilesRequired();
      return;
    }

    if (this.outputDataDirectory.exists())
      throw new IOException("Test output directory already exists "
          + this.outputDataDirectory.getAbsolutePath());

    // Create analysis directory and temporary directory
    if (!new File(this.outputDataDirectory + "/tmp").mkdirs())
      throw new IOException("Cannot create analysis directory "
          + this.outputDataDirectory.getAbsolutePath());

    // Create a symbolic link for each file from input data test directory
    for (File file : this.inputDataDirectory.listFiles()) {
      if (file.isFile())
        createSymbolicLink(file, this.outputDataDirectory);
    }

    // checkInputFilesRequired();
  }

  public void init() throws EoulsanException, IOException {

    if (!this.outputDataDirectory.exists()) {

      // Build analysis Eoulsan directory
      this.exists = false;
      buildDirectoryAnalysis();
    }

    this.designFile =
        filterOneFileWithPrefix(this.outputDataDirectory, "design",
            "design file");

    this.paramFile =
        filterOneFileWithPrefix(this.outputDataDirectory, "param",
            "parameter file");

  }

  //
  // Useful methods
  //

  private void checkInputFilesRequired() throws EoulsanException {
    // Check input files required correspond to the patterns
    final String patternsInputFiles =
        this.propsTest.getProperty(PATTERNS_INPUT_FILES_KEY);

    // Parse patterns
    for (String pattern : splitter.split(patternsInputFiles)) {
      boolean patternFound = false;

      // Parse files in input directory
      for (File file : this.inputDataDirectory.listFiles()) {
        // TODO
        System.out.println(pattern + "\t" + file.getName());
        patternFound =
            patternFound
                || Pattern.matches(pattern, StringUtils
                    .filenameWithoutCompressionExtension(file.getName()));
      }

      if (!patternFound)
        throw new EoulsanException("Missing files required "
            + pattern + " in input directory "
            + this.inputDataDirectory.getAbsolutePath());
    }

  }

  private File[] filterFile(final File dir, final String... suffixes) {

    return dir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        for (String suffix : suffixes)
          return pathname.getName().contains(suffix);
        return false;
      }
    });
  }

  private File filterOneFileWithPrefix(final File dir, final String prefix,
      final String msg) throws EoulsanException {

    File[] files = dir.listFiles(new PrefixFilenameFilter(prefix, false));

    if (files == null)
      throw new EoulsanException("None file: "
          + msg + " filtered in directory " + dir.getAbsolutePath());

    if (files.length != 1)
      throw new EoulsanException(
          "Doesn't have the good number of file searched for " + msg);

    return files[0];

  }

  /**
   * @param df
   * @return
   */
  public DataFile searchFileByName(final String filename) {

    if (filename == null || filename.length() == 0)
      return null;

    return filesByName.get(filename);
  }

  //
  // Getter & Setter
  //

  public boolean isResultsAnalysisExists() {
    return exists;
  }

  public File getDesignFile() {
    return designFile;
  }

  public File getParamFile() {
    return paramFile;
  }

  public File getEoulsanLog() {
    return eoulsanLog;
  }

  public Map<String, DataFile> getAllFilesAnalysis() {
    return filesByName;
  }

  //
  // Constructor
  //

  public DataSetAnalysis(final Properties propsTest,
      final File inputDataDirectory, final File outputDataDirectory)
      throws EoulsanException, IOException {

    checkExistingFile(inputDataDirectory,
        "Input data for analysis doesn't exists.");

    this.propsTest = propsTest;
    this.inputDataDirectory = inputDataDirectory;
    this.outputDataDirectory = outputDataDirectory;

    this.filesByName = Maps.newHashMap();

    // init();

  }

}
