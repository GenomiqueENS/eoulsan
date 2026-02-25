package fr.ens.biologie.genomique.eoulsan.modules.multiqc;

import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MULTIQC_REPORT_HTML;
import static fr.ens.biologie.genomique.eoulsan.requirements.DockerRequirement.newDockerRequirement;
import static fr.ens.biologie.genomique.eoulsan.requirements.PathRequirement.newPathRequirement;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Splitter;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.EoulsanDockerManager;
import fr.ens.biologie.genomique.kenetre.io.FileUtils;
import fr.ens.biologie.genomique.kenetre.util.GuavaCompatibility;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.kenetre.util.process.SimpleProcess;
import fr.ens.biologie.genomique.kenetre.util.process.SystemSimpleProcess;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class define a module for MultiQC.
 *
 * @since 2.2
 * @author Laurent Jourdren
 */
@LocalOnly
public class MultiQCModule extends AbstractModule {

  /** Module name */
  private static final String MODULE_NAME = "multiqc";

  private static final String MULTIQC_DOCKER_IMAGE = "multiqc/multiqc:v1.33";
  private static final String MULTIQC_EXECUTABLE = "multiqc";

  private boolean dockerMode;
  private String dockerImage = MULTIQC_DOCKER_IMAGE;
  private final Set<Requirement> requirements = new HashSet<>();
  private final Map<DataFormat, InputPreprocessor> formats = new HashMap<>();
  private boolean keepTemporaryFiles;

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    int count = 0;
    for (DataFormat format : this.formats.keySet()) {

      builder.addPort("inputport" + count++, true, format);
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(MULTIQC_REPORT_HTML);
  }

  @Override
  public Set<Requirement> getRequirements() {

    return unmodifiableSet(this.requirements);
  }

  @Override
  public void configure(StepConfigurationContext context, Set<Parameter> stepParameters)
      throws EoulsanException {

    // By default only process FastQC reports
    String reports = FastQCInputPreprocessor.REPORT_NAME;

    for (Parameter p : stepParameters) {

      switch (p.getName()) {
        case "use.docker":
          this.dockerMode = p.getBooleanValue();
          break;

        case "docker.image":
          this.dockerImage = p.getStringValue().trim();
          if (this.dockerImage.isEmpty()) {
            Modules.badParameterValue(context, p, "The docker image name is empty");
          }
          break;

        case "reports":
          reports = p.getStringValue();
          break;

        case "debug":
          this.keepTemporaryFiles = p.getBooleanValue();
          break;

        default:
          Modules.unknownParameter(context, p);
          break;
      }
    }

    // Parse report parameter and set the formats to handle
    for (InputPreprocessor ip : parseReportParameter(reports, context.getCurrentStep().getId())) {
      DataFormat df = ip.getDataFormat();
      if (df != null) {
        this.formats.put(df, ip);
      }
    }

    // Define requirements
    if (this.dockerMode) {
      this.requirements.add(newDockerRequirement(this.dockerImage));
    } else {
      this.requirements.add(newPathRequirement(MULTIQC_EXECUTABLE));
    }
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    // Define the output file
    Path multiQCReportFile =
        context.getOutputData(MULTIQC_REPORT_HTML, "all").getDataFile().toPath();

    try {

      // Create a temporary directory where all the preprocessed files for
      // MultiQC while be saved
      Path multiQCInputDir = FileUtils.createTempDir(context.getLocalTempDirectory()).toPath();

      // Preprocess input data for MultiQC
      for (Map.Entry<DataFormat, InputPreprocessor> e : this.formats.entrySet()) {
        for (Data d : context.getInputData(e.getKey()).getListElements()) {
          e.getValue().preprocess(context, d, multiQCInputDir.toFile());
        }
      }

      // Create sample names file
      Path sampleNamesFile = multiQCInputDir.resolve("sample-names.tsv");
      createSampleNamesFile(context.getWorkflow().getDesign(), sampleNamesFile);

      List<String> commandLine;

      // Launch MultiQC
      if (this.dockerMode) {
        context.getLogger().info("Docker image: " + this.dockerImage);
        status.setDockerImage(this.dockerImage);
        commandLine =
            createMultiQCReportWithDocker(
                this.dockerImage,
                multiQCInputDir,
                sampleNamesFile,
                multiQCReportFile,
                context.getCommandName(),
                context.getLocalTempDirectory().toPath());
      } else {
        commandLine =
            createMultiQCReport(
                multiQCInputDir,
                sampleNamesFile,
                multiQCReportFile,
                context.getCommandName(),
                context.getLocalTempDirectory().toPath());
      }

      // Set command line in status
      status.setCommandLine(String.join(" ", commandLine));
      context.getLogger().info("Command line: " + commandLine);

      // Cleanup temporary directory
      if (!this.keepTemporaryFiles) {
        new DataFile(multiQCInputDir).delete(true);
      }

    } catch (IOException | EoulsanException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }

  /**
   * Parse the "reports" step parameter.
   *
   * @param reports the parameter value
   * @param stepId stepId
   * @return a collection of InputPreprocessor
   * @throws EoulsanException if the parameter value is invalid
   */
  private static Collection<InputPreprocessor> parseReportParameter(
      final String reports, final String stepId) throws EoulsanException {

    // Get service instance
    final InputPreprocessorService service = InputPreprocessorService.getInstance();

    final Map<String, InputPreprocessor> result = new HashMap<>();

    for (String report :
        GuavaCompatibility.splitToList(
            Splitter.on(',').trimResults().omitEmptyStrings(),
            reports.toLowerCase(Globals.DEFAULT_LOCALE))) {

      // Only process each report type once
      if (result.containsKey(report)) {
        continue;
      }

      if (!service.isService(report)) {
        throw new EoulsanException(
            "In step \""
                + stepId
                + "\", invalid MultiQC configuration: unknown report type: "
                + report);
      }

      result.put(report, service.newService(report));
    }

    if (result.isEmpty()) {
      throw new EoulsanException(
          "In step \"" + stepId + "\", invalid MultiQC configuration: no report selected");
    }

    return result.values();
  }

  /**
   * Create the MultiQC report using docker.
   *
   * @param dockerImage docker image to use
   * @param inputDirectory input directory
   * @param sampleNamesFile sample names file
   * @param multiQCReportFile output report
   * @param projectName project name
   * @return the command line
   * @throws IOException if an error occurs while creating the report
   * @throws EoulsanException if MultiQC execution fails
   */
  private List<String> createMultiQCReportWithDocker(
      final String dockerImage,
      final Path inputDirectory,
      final Path sampleNamesFile,
      final Path multiQCReportFile,
      final String projectName,
      final Path temporaryDirectory)
      throws IOException, EoulsanException {

    SimpleProcess process = EoulsanDockerManager.getInstance().createImageInstance(dockerImage);

    Path executionDirectory = multiQCReportFile.getParent();
    Path stdoutFile = executionDirectory.resolve("multiqc.stdout");
    Path stderrFile = executionDirectory.resolve("multiqc.stderr");

    // Define the list of the files/directory to mount in the Docker instance
    List<File> filesUsed = new ArrayList<>();
    filesUsed.add(executionDirectory.toFile());
    filesUsed.add(temporaryDirectory.toFile());
    filesUsed.add(multiQCReportFile.toFile());

    for (File f : inputDirectory.toFile().listFiles()) {

      // Do not handle files and directory that starts with '.'
      if (!f.getName().startsWith(".")) {
        filesUsed.add(f);
      }
    }

    // Create command line
    List<String> commandLine =
        createCommandLine(inputDirectory, sampleNamesFile, multiQCReportFile, projectName);

    // Launch Docker container
    final int exitValue =
        process.execute(
            commandLine,
            executionDirectory.toFile(),
            temporaryDirectory.toFile(),
            stdoutFile.toFile(),
            stderrFile.toFile(),
            filesUsed.toArray(new File[0]));

    if (exitValue > 0) {
      throw new EoulsanException("Invalid exit code of MultiQC: " + exitValue);
    }

    return commandLine;
  }

  /**
   * Create the MultiQC report using docker.
   *
   * @param inputDirectory input directory
   * @param sampleNamesFile sample names file
   * @param multiQCReportFile output report
   * @param projectName project name
   * @return command line
   * @throws IOException if an error occurs while creating the report
   * @throws EoulsanException if MultiQC execution fails
   */
  private List<String> createMultiQCReport(
      final Path inputDirectory,
      final Path sampleNamesFile,
      final Path multiQCReportFile,
      final String projectName,
      final Path temporaryDirectory)
      throws IOException, EoulsanException {

    SimpleProcess process = new SystemSimpleProcess();

    Path executionDirectory = multiQCReportFile.getParent();
    Path stdoutFile = executionDirectory.resolve("multiqc.stdout");
    Path stderrFile = executionDirectory.resolve("multiqc.stderr");

    // Create command line
    List<String> commandLine =
        createCommandLine(inputDirectory, sampleNamesFile, multiQCReportFile, projectName);

    // Launch Docker container
    int exitValue =
        process.execute(
            commandLine,
            executionDirectory.toFile(),
            temporaryDirectory.toFile(),
            stdoutFile.toFile(),
            stderrFile.toFile());

    if (exitValue > 0) {
      throw new EoulsanException("Invalid exit code of MultiQC: " + exitValue);
    }

    return commandLine;
  }

  /**
   * Creating MultiQC command line.
   *
   * @param inputDirectory input directory
   * @param sampleNamesFile sample names file
   * @param multiQCReportFile output report
   * @param sampleNamesFile sample names file
   * @param projectName project name
   * @return a list with the MultiQC arguments
   */
  private static List<String> createCommandLine(
      final Path inputDirectory,
      final Path sampleNamesFile,
      final Path multiQCReportFile,
      final String projectName) {

    List<String> result = new ArrayList<>();

    // The MultiQC executable name
    result.add(MULTIQC_EXECUTABLE);

    // MultiQC options
    result.add("--title");
    result.add("Project " + projectName + " report");
    result.add("--sample-names");
    result.add(sampleNamesFile.toAbsolutePath().toString());
    result.add("--filename");
    result.add(multiQCReportFile.toAbsolutePath().toString());

    // MultiQC input directory
    result.add(inputDirectory.toAbsolutePath().toString());

    return result;
  }

  /**
   * Create a sample name file for MultiQC.
   *
   * @param design the Eoulsan design
   * @param sampleNamesFile the file to create
   * @throws IOException if an error occurs while creating the file
   */
  private static void createSampleNamesFile(final Design design, final Path sampleNamesFile)
      throws IOException {

    requireNonNull(design);
    requireNonNull(sampleNamesFile);

    boolean isDescription = false;
    for (Sample s : design.getSamples()) {
      if (s.getMetadata().containsDescription()) {
        isDescription = true;
        break;
      }
    }

    List<String> result = new ArrayList<>();
    result.add("MultiQC Names\tNames" + (isDescription ? "\tDescriptions" : ""));
    for (Sample s : design.getSamples()) {
      String description = s.getMetadata().getDescription();
      if (description == null) {
        description = "";
      }
      result.add(s.getId() + '\t' + s.getName() + (isDescription ? '\t' + description : ""));
    }

    Files.write(sampleNamesFile, result);
  }
}
