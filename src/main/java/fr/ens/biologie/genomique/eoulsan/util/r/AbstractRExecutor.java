package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class define an abstract RExecutor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractRExecutor implements RExecutor {

  private static final String R_FILE_EXTENSION = ".R";
  private static final String SWEAVE_FILE_EXTENSION = ".Rnw";

  private final List<String> inputFilenames = new ArrayList<>();
  private final File outputDirectory;
  private final File temporaryDirectory;

  //
  // Protected methods
  //

  /**
   * Put a file for the analysis.
   * @param inputFile the input file
   * @param outputFilename the output filename
   * @throws IOException if an error occurs while putting the file
   */
  protected abstract void putFile(DataFile inputFile, String outputFilename)
      throws IOException;

  /**
   * Remove a file of the analysis.
   * @param filename the filename of the file to remove
   * @throws IOException if the removing of the file fails
   */
  protected abstract void removeFile(String filename) throws IOException;

  /**
   * Execute a R script.
   * @param rScriptFile The R script file
   * @param sweave execute the R using Sweave
   * @param sweaveOuput sweave output file
   * @param scriptArguments script arguments
   * @throws IOException if an error occurs while executing the script
   */
  protected abstract void executeRScript(File rScriptFile, boolean sweave,
      String sweaveOuput, File workflowOutputDir, String... scriptArguments)
      throws IOException;

  /**
   * Get the output directory of the analysis.
   * @return the output directory of the analysis
   */
  protected File getOutputDirectory() {

    return this.outputDirectory;
  }

  /**
   * Get the temporary directory.
   * @return the temporary directory
   */
  protected File getTemporaryDirectory() {

    return this.temporaryDirectory;
  }

  //
  // RExecutor methods
  //

  @Override
  public void openConnection() throws IOException {

    if (!outputDirectory.isDirectory()) {
      throw new IOException(
          "The output directory does not exist or is not a directory: "
              + outputDirectory);
    }

    if (!temporaryDirectory.isDirectory()) {
      throw new IOException(
          "The output directory does not exist or is not a directory: "
              + outputDirectory);
    }

  }

  @Override
  public void closeConnection() throws IOException {

    this.inputFilenames.clear();
  }

  @Override
  public void getOutputFiles() throws IOException {

    // Nothing to do for the default implementation
  }

  @Override
  public void putInputFile(DataFile inputFile) throws IOException {

    if (inputFile == null) {
      throw new NullPointerException("inputFile argument cannot be null");
    }

    putFile(inputFile, inputFile.getName());
  }

  @Override
  public void putInputFile(DataFile inputFile, String outputFilename)
      throws IOException {

    if (inputFile == null) {
      throw new NullPointerException("inputFile argument cannot be null");
    }

    if (outputFilename == null) {
      throw new NullPointerException("inputFilename argument cannot be null");
    }

    // Check if try to overwrite an existing output file
    if (inputFilenames.contains(outputFilename)) {
      throw new IOException(
          "Cannot overwrite input file on Rserve: " + outputFilename);
    }

    this.inputFilenames.add(outputFilename);

    // Put file on Rserve
    putFile(inputFile, outputFilename);
  }

  @Override
  public void removeInputFiles() throws IOException {

    for (String inputFilename : inputFilenames) {
      removeFile(inputFilename);
    }
  }

  @Override
  public void executeRScript(final String rScript, final boolean sweave,
      final String sweaveOutput, final boolean saveRscript,
      final String description, final DataFile workflowOutputDir,
      final String... scriptArguments) throws IOException {

    if (rScript == null) {
      throw new NullPointerException("rScript argument cannot be null");
    }

    if (description == null) {
      throw new NullPointerException("description argument cannot be null");
    }

    final File rScriptFile = new File(this.outputDirectory,
        description + (sweave ? SWEAVE_FILE_EXTENSION : R_FILE_EXTENSION));

    // Write R script in a File
    Writer writer = new FileWriter(rScriptFile, Charset.defaultCharset());
    writer.write(rScript);
    writer.close();

    // Execute R script
    executeRScript(rScriptFile, sweave, sweaveOutput,
        workflowOutputDir.toFile(), scriptArguments);

    // Remove temporary R script
    if (!saveRscript) {

      if (!rScriptFile.delete()) {
        EoulsanLogger
            .logWarning("Cannot removing temporary R script: " + rScriptFile);
      }
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param outputDirectory output directory
   * @throws IOException if the output directory does not exists
   */
  protected AbstractRExecutor(final File outputDirectory,
      final File temporaryDirectory) throws IOException {

    if (outputDirectory == null) {
      throw new NullPointerException("outputDirectory argument cannot be null");
    }

    if (temporaryDirectory == null) {
      throw new NullPointerException("outputDirectory argument cannot be null");
    }

    this.outputDirectory = outputDirectory;
    this.temporaryDirectory = temporaryDirectory;
  }

}
