package fr.ens.transcriptome.eoulsan.it;

import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingFile;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createSymbolicLink;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

public class DataSetAnalysis {

  public final static Splitter COMMA_SPLITTER = Splitter.on(',').trimResults()
      .omitEmptyStrings();
  private static final String PATTERNS_INPUT_FILES_KEY = "patterns.input.files";
  private static final String PATTERNS_OUTPUT_FILES_KEY =
      "patterns.output.files";

  private final Properties propsTest;

  private final File inputDataDirectory;
  private final File outputDataDirectory;

  private boolean isExpectedDirectoryExists = false;
  private Map<String, File> outputFilesAnalysis;

  public void parseDirectory() throws IOException, EoulsanException {
    parseDirectory(this.outputDataDirectory);
  }

  private void parseDirectory(final File directory) throws IOException,
      EoulsanException {

    for (final File fileEntry : directory.listFiles()) {
      if (fileEntry.isDirectory()) {
        parseDirectory(fileEntry);

      } else {
        // entry is a file
        // Skip serialization file created by bloomFilter
        if (!fileEntry.getName().endsWith(".ser")) {

          // File match with pattern output file
          if (isOutputFiles(fileEntry)) {

            // Save all output files analysis
            outputFilesAnalysis.put(fileEntry.getName(), fileEntry);

          } else {
            // Remove file
            if (!fileEntry.delete())
              new IOException("Fail to delete file "
                  + fileEntry.getAbsolutePath());
          }
        }
      }
    }

  }

  private void buildDirectoryAnalysis() throws EoulsanException, IOException {

    if (this.isExpectedDirectoryExists) {
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

  }

  private void init() throws EoulsanException, IOException {

    if (!this.outputDataDirectory.exists()) {

      // Build analysis Eoulsan directory
      buildDirectoryAnalysis();
    }

    this.isExpectedDirectoryExists = true;

    // Check all files necessary
    checkExpectedDirectory(false);
  }

  //
  // Useful methods
  //

  private void checkFilesRequired(final String pattern_key, final File source)
      throws EoulsanException {
    // Check input files required correspond to the patterns
    final String patternsFiles = this.propsTest.getProperty(pattern_key);

    // Parse patterns
    for (String regex : COMMA_SPLITTER.split(patternsFiles)) {
      boolean patternFound = false;

      // Parse files in input directory
      for (File file : source.listFiles()) {
        patternFound =
            patternFound
                || Pattern.matches(regex, StringUtils
                    .filenameWithoutCompressionExtension(file.getName()));
      }

      if (!patternFound)
        throw new EoulsanException("Missing files required "
            + regex + " in input directory "
            + this.outputDataDirectory.getAbsolutePath());
    }
  }

  private boolean isOutputFiles(final File file) {

    // Add test configuration file
    final String patternsFiles =
        this.propsTest.getProperty(PATTERNS_OUTPUT_FILES_KEY) + ", test.conf";

    // Parse patterns
    for (String regex : COMMA_SPLITTER.split(patternsFiles)) {

      // Parse files in input directory
      if (Pattern.matches(regex,
          StringUtils.filenameWithoutCompressionExtension(file.getName())))
        return true;
    }

    return false;
  }

  public void checkExpectedDirectory(final boolean asAnalysisExecute)
      throws EoulsanException, IOException {

    // Check test configuration file
    checkExistingFile(getTestConfigurationFile(), "configuration test file");

    // Check input files requiered
    checkFilesRequired(PATTERNS_INPUT_FILES_KEY, this.inputDataDirectory);

    if (asAnalysisExecute) {
      // Check output files requiered
      checkFilesRequired(PATTERNS_OUTPUT_FILES_KEY, this.outputDataDirectory);
    }
  }

  /**
   * @param df
   * @return
   */
  public File searchFileByName(final String filename) {

    if (filename == null || filename.length() == 0)
      return null;

    return outputFilesAnalysis.get(filename);
  }

  //
  // Getter & Setter
  //

  public boolean isResultsAnalysisExists() {
    return isExpectedDirectoryExists;
  }

  public File getTestConfigurationFile() {
    return new File(this.outputDataDirectory, "test.conf");
  }

  public Map<String, File> getOutputFilesAnalysis() {
    return outputFilesAnalysis;
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

    this.outputFilesAnalysis = Maps.newHashMap();

    init();

  }

}
