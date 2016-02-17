package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;

/**
 * This class define a standard RExecutor using a system process.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ProcessRExecutor extends AbstractRExecutor {

  public static final String REXECUTOR_NAME = "process";

  public static final String RSCRIPT_EXECUTABLE = "Rscript";

  private Set<String> filenamesToKeep = new HashSet<>();

  @Override
  public String getName() {

    return REXECUTOR_NAME;
  }

  @Override
  protected void putFile(final DataFile inputFile, final String outputFilename)
      throws IOException {

    final File outputDir = getOutputDirectory().getAbsoluteFile();
    final DataFile outputFile = new DataFile(outputDir, outputFilename);

    if (outputFile.exists()) {
      throw new IOException("The output file already exists: " + outputFile);
    }

    if (!inputFile.isLocalFile()
        || inputFile.getCompressionType().isCompressed()) {

      // Copy the file if the file is not on local file system or compressed
      DataFiles.copy(inputFile, outputFile);

    } else {

      final File parentDir =
          inputFile.toFile().getParentFile().getAbsoluteFile();

      if (!parentDir.equals(outputDir)
          || !inputFile.getName().equals(inputFile)) {

        // If the output file is not in the same directory that the original
        // file or its filename is different, create a symbolic link
        inputFile.symlink(outputFile, true);
      } else {
        this.filenamesToKeep.add(outputFilename);
      }
    }

  }

  @Override
  public void writerFile(final String content, final String outputFilename)
      throws IOException {

    if (content == null) {
      throw new NullPointerException("content argument cannot be null");
    }

    if (outputFilename == null) {
      throw new NullPointerException("outputFilename argument cannot be null");
    }

    final DataFile outputFile =
        new DataFile(getOutputDirectory(), outputFilename);

    if (outputFile.exists()) {
      throw new IOException("The output file already exists: " + outputFile);
    }

    try (Writer writer = new OutputStreamWriter(outputFile.create())) {
      writer.write(content);
    }
  }

  @Override
  protected void removeFile(final String filename) throws IOException {

    // Avoid removing original input files
    if (this.filenamesToKeep.contains(filename)) {
      return;
    }

    final File file = new File(getOutputDirectory(), filename);

    if (!file.delete()) {
      EoulsanLogger.logWarning("Cannot remove file used by R: " + file);
    }
  }

  /**
   * Create R command.
   * @param rScriptFile the R script file to execute
   * @param sweave true if the script is a Sweave file
   * @param scriptArguments script arguments
   * @return the R command as a list
   */
  protected List<String> createCommand(final File rScriptFile,
      final boolean sweave, final String sweaveOuput,
      final String... scriptArguments) {

    final List<String> result = new ArrayList<>();
    result.add(RSCRIPT_EXECUTABLE);

    if (sweave) {

      result.add("-e");

      final StringBuilder sb = new StringBuilder();
      sb.append("Sweave(\"");
      sb.append(rScriptFile.getAbsolutePath());
      sb.append('\"');

      if (sweaveOuput != null) {
        sb.append(", output=\"");
        sb.append(sweaveOuput);
        sb.append('\"');
      }
      sb.append(')');

      result.add(sb.toString());
    } else {
      result.add(rScriptFile.getAbsolutePath());
    }

    if (scriptArguments != null) {
      for (String argument : scriptArguments) {
        result.add(argument);
      }
    }

    return result;
  }

  @Override
  protected void executeRScript(final File rScriptFile, final boolean sweave,
      final String sweaveOuput, final String... scriptArguments)
          throws IOException {

    final List<String> command =
        createCommand(rScriptFile, sweave, sweaveOuput, scriptArguments);

    // Search the command in The PATH
    final File executablePath =
        SystemUtils.searchExecutableInPATH(command.get(0));

    if (executablePath == null) {
      throw new IOException(
          "Unable to find executable in the PATH: " + command.get(0));
    }

    // Update the command with the path of the command
    command.set(0, executablePath.getAbsolutePath());

    final ProcessBuilder pb = new ProcessBuilder();

    // Set the temporary directory for R
    pb.environment().put("TMPDIR", getTemporaryDirectory().getAbsolutePath());

    // Redirect stdout and stderr
    pb.redirectOutput(changeFileExtension(rScriptFile, ".out"));
    pb.redirectError(changeFileExtension(rScriptFile, ".err"));

    ProcessUtils.logEndTime(pb.start(), Joiner.on(' ').join(pb.command()),
        System.currentTimeMillis());
  }

  /**
   * Change the extendsion of a file
   * @param file the file
   * @param newExtension the new extension of the file
   * @return a file object
   */
  protected static File changeFileExtension(final File file,
      final String newExtension) {

    if (file == null) {
      return null;
    }

    if (newExtension == null) {
      return file;
    }

    final String newFilename =
        StringUtils.filenameWithoutExtension(file.getName()) + newExtension;

    return new File(file.getParent(), newFilename);
  }

  @Override
  public void closeConnection() throws IOException {

    this.filenamesToKeep.clear();

    super.closeConnection();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param outputDirectory the output directory
   * @param temporaryDirectory the temporary directory
   * @throws IOException if an error occurs while creating the object
   */
  protected ProcessRExecutor(final File outputDirectory,
      final File temporaryDirectory) throws IOException {
    super(outputDirectory, temporaryDirectory);
  }

}
