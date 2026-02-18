package fr.ens.biologie.genomique.eoulsan.util.r;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class define a RServe RExecutor.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class RserveRExecutor extends AbstractRExecutor {

  public static final String REXECUTOR_NAME = "rserve";

  private final String serverName;
  protected RSConnection rConnection;

  @Override
  public String getName() {

    return REXECUTOR_NAME;
  }

  @Override
  public void getOutputFiles() throws IOException {

    checkConnection();

    try {

      // Get all the filenames
      final List<String> filenames = this.rConnection.listFiles();

      for (String filename : filenames) {

        // Retrieve the file
        this.rConnection.getFile(filename,
            getOutputDirectory().resolve(filename));

        // Delete the file
        removeFile(filename);
      }

    } catch (REngineException | REXPMismatchException e) {
      throw new IOException(e);
    }
  }

  //
  // Protected methods
  //

  @Override
  public void openConnection() throws IOException {

    // Check if temporary and output directories exists
    super.openConnection();

    this.rConnection = new RSConnection(serverName);
  }

  @Override
  public void closeConnection() throws IOException {

    this.rConnection.disConnect();
    this.rConnection = null;
  }

  @Override
  protected void removeFile(final String filename) throws IOException {

    checkConnection();

    // Check if Rserve files can be removed
    if (EoulsanRuntime.getSettings().isKeepRServeFiles()) {
      return;
    }

    // Remove file from Rserve server
    try {
      this.rConnection.removeFile(filename);
    } catch (REngineException e) {
      throw new IOException(e);
    }
  }

  protected void putFile(final DataFile inputFile, final String inputFilename)
      throws IOException {

    checkConnection();

    getLogger()
        .info("Put file on RServe: " + inputFile + " to " + inputFilename);

    try {
      this.rConnection.putFile(inputFile.open(), inputFilename);
    } catch (REngineException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void writeFile(final String content, final String outputFilename)
      throws IOException {

    checkConnection();

    if (content == null) {
      throw new NullPointerException("content argument cannot be null");
    }

    if (outputFilename == null) {
      throw new NullPointerException("outputFilename argument cannot be null");
    }

    try (Writer writer = new OutputStreamWriter(
        this.rConnection.getFileOutputStream(outputFilename))) {
      writer.write(content);
    } catch (REngineException e) {
      throw new IOException(e);
    }

  }

  @Override
  protected void executeRScript(final Path rScriptFile, final boolean sweave,
      final String sweaveOuput, final Path workflowOutputDir,
      final String... scriptArguments) throws IOException {

    checkConnection();

    final String rScriptOnRservePath = rScriptFile.getFileName().toString();

    // Put the R script on the Rserve server
    putFile(new DataFile(rScriptFile), rScriptOnRservePath);

    try {

      // Set the command line arguments
      if (scriptArguments != null) {
        this.rConnection.setCommandArgs(Arrays.asList(scriptArguments));
      }

      if (sweave) {

        // Execute Sweave script
        getLogger().info("Execute RNW script: " + rScriptFile);
        this.rConnection.executeRnwCode(rScriptOnRservePath, sweaveOuput);

      } else {

        // Execute a R script
        getLogger().info("Execute R script: " + rScriptFile);
        this.rConnection.executeRCode(rScriptOnRservePath);
      }
    } catch (REngineException e) {
      throw new IOException(e);
    }

    // Remove the R script on the server
    removeFile(rScriptOnRservePath);
  }

  @Override
  public void executeR(String code, File workflowOutputDir)
      throws IOException {

    try {
      this.rConnection.executeR(code);
      ;
    } catch (REngineException e) {
      throw new IOException(e);
    }
  }

  //
  // Other methods
  //

  private void checkConnection() {

    if (this.rConnection == null) {
      throw new IllegalStateException("Connection has not been openned");
    }

  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param outputDirectory the output directory
   * @param temporaryDirectory the temporary directory
   * @param serverName Rserve server name
   * @throws IOException if an error occurs
   */
  public RserveRExecutor(final File outputDirectory,
      final File temporaryDirectory, final String serverName)
      throws IOException {

    super(outputDirectory, temporaryDirectory);

    if (serverName == null) {
      throw new NullPointerException("serverName cannot be null");
    }
    this.serverName = serverName.trim();
  }

}
