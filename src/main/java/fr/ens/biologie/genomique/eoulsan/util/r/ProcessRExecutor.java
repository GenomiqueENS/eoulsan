package fr.ens.biologie.genomique.eoulsan.util.r;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;

import fr.ens.biologie.genomique.eoulsan.EoulsanLogger;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.process.SimpleProcess;
import fr.ens.biologie.genomique.kenetre.util.process.SystemSimpleProcess;

/**
 * This class define a standard RExecutor using a system process.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ProcessRExecutor extends AbstractRExecutor {

  public static final String REXECUTOR_NAME = "process";

  public static final String RSCRIPT_EXECUTABLE = "Rscript";

  protected static final String LANG_ENVIRONMENT_VARIABLE = "LANG";
  protected static final String DEFAULT_R_LANG = "C";

  private final Set<String> filenamesToKeep = new HashSet<>();

  @Override
  public String getName() {

    return REXECUTOR_NAME;
  }

  @Override
  protected void putFile(final DataFile inputFile, final String outputFilename)
      throws IOException {

    final File outputDir = getOutputDirectory().getAbsoluteFile();
    final DataFile outputFile = new DataFile(outputDir, outputFilename);

    // Check if the input and output file are the same
    if (isSameLocalPath(inputFile, outputFile)) {
      return;
    }

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
          || !inputFile.getName().equals(outputFilename)) {

        // If the output file is not in the same directory that the original
        // file or its filename is different, create a symbolic link
        inputFile.symlink(outputFile, true);
      } else {
        this.filenamesToKeep.add(outputFilename);
      }
    }

  }

  @Override
  public void writeFile(final String content, final String outputFilename)
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

    try (Writer writer =
        new OutputStreamWriter(outputFile.create(), Charset.defaultCharset())) {
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
      Collections.addAll(result, scriptArguments);
    }

    return result;
  }

  /**
   * Create the process that will execute the R Script.
   * @return a SimpleProcess object
   * @throws IOException if an error occurs when creation the process object
   */
  protected SimpleProcess createSimpleProcess() throws IOException {

    return new SystemSimpleProcess();
  }

  @Override
  protected void executeRScript(final File rScriptFile, final boolean sweave,
      final String sweaveOuput, final File workflowOutputDir,
      final String... scriptArguments) throws IOException {

    final SimpleProcess process = createSimpleProcess();

    final List<String> commandLine =
        createCommand(rScriptFile, sweave, sweaveOuput, scriptArguments);

    final File stdoutFile = changeFileExtension(rScriptFile, ".out");

    final int exitValue = process.execute(commandLine, getOutputDirectory(),
        Collections.singletonMap(LANG_ENVIRONMENT_VARIABLE, DEFAULT_R_LANG),
        getTemporaryDirectory(), stdoutFile, stdoutFile, true,
        workflowOutputDir);

    ProcessUtils.throwExitCodeException(exitValue,
        Joiner.on(' ').join(commandLine));
  }

  @Override
  public void executeR(String code, File workflowOutputDir)
      throws IOException {

    final SimpleProcess process = createSimpleProcess();

    final List<String> commandLine =
        Arrays.asList(RSCRIPT_EXECUTABLE, "-e", code);

    final File stdoutFile = workflowOutputDir.toPath()
        .resolve("R-" + System.currentTimeMillis() + ".out").toFile();

    final int exitValue = process.execute(commandLine, getOutputDirectory(),
        Collections.singletonMap(LANG_ENVIRONMENT_VARIABLE, DEFAULT_R_LANG),
        getTemporaryDirectory(), stdoutFile, stdoutFile, true,
        workflowOutputDir);

    ProcessUtils.throwExitCodeException(exitValue,
        Joiner.on(' ').join(commandLine));

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
  // Other methods
  //

  /**
   * Check if two file have the same local path
   * @param a first file
   * @param b second file
   * @return true if the file have the same local file
   */
  protected static boolean isSameLocalPath(final DataFile a, final DataFile b) {

    if (a.equals(b)) {
      return true;
    }

    if (a.isLocalFile()) {

      final File fa = a.toFile().getAbsoluteFile();
      final File fb = b.toFile().getAbsoluteFile();

      return fa.equals(fb);
    }

    return false;
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
