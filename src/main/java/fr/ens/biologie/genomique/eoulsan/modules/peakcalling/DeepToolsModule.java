package fr.ens.biologie.genomique.eoulsan.modules.peakcalling;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_BAM;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_INDEX_BAI;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.util.BinariesInstaller;
import fr.ens.biologie.genomique.eoulsan.util.ProcessUtils;

/**
 * This class uses tools from the DeepTools suite. Needs matplotlib... (sudo
 * apt-get install python-matplotlib AND sudo pip install pysam)
 * @author Celine Hernandez - CSB lab - ENS - Paris
 */
@LocalOnly
public class DeepToolsModule extends AbstractModule {

  private static final String SOFTWARE_LABEL = "deeptools";
  private static final String SHIPPED_PACKAGE_VERSION = "1.5.11";
  private static final String PACKAGE_ARCHIVE = "deepTools-1.5.11.tar.gz";

  private static DataFormat PEAK =
      DataFormatRegistry.getInstance().getDataFormatFromName("peaks");

  /**
   * Settings
   */

  private String deeptoolsPath = "";
  private String deeptoolsLibPath = "";

  /**
   * Methods
   */

  @Override
  public String getName() {
    return "deeptools";
  }

  @Override
  public String getDescription() {
    return "This step runs QC tools from the DeepTools suite.";
  }

  @Override
  public Version getVersion() {
    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {
    final InputPortsBuilder builder = new InputPortsBuilder();
    builder.addPort("inputpeak", false, PEAK);
    builder.addPort("inputbamlist", true, MAPPER_RESULTS_BAM);
    builder.addPort("inputbailist", true, MAPPER_RESULTS_INDEX_BAI);
    return builder.create();
  }

  /**
   * Set the parameters of the step to configure the step.
   * @param stepParameters parameters of the step
   * @throws EoulsanException if a parameter is invalid
   */
  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      getLogger().info(
          "DeepTools parameter: " + p.getName() + " : " + p.getStringValue());

      throw new EoulsanException(
          "Unknown parameter for " + getName() + " step: " + p.getName());
    }

    // Install softwares
    this.install();

  }

  /**
   * Check whether DeepTools are already installed. If not decompress Eoulsan's
   * included archives and install them.
   */
  private void install() {

    // // Install
    // if(!BinariesInstaller.check(this.SOFTWARE_LABEL,
    // this.SHIPPED_PACKAGE_VERSION, "deeptools")) {

    // If not installed, install it
    getLogger().info("DeepTools not installed. Running installation....");

    try {
      // Get the shipped archive
      String binaryFile = BinariesInstaller.install(SOFTWARE_LABEL,
          SHIPPED_PACKAGE_VERSION, PACKAGE_ARCHIVE, EoulsanRuntime.getSettings()
              .getTempDirectoryFile().getAbsolutePath());
      getLogger().info("Archive location for DeepTools : " + binaryFile);
      DataFile archive = new DataFile(binaryFile);

      // Extract full archive
      String cmd = String.format("tar -xzf %s -C %s", archive.getSource(),
          archive.getParent().getSource());
      getLogger().info("Extract archive : " + cmd);
      ProcessUtils.system(cmd);

      // Build DeepTools
      cmd =
          String.format("python2 setup.py build --prefix %s/deepTools-1.5.11/",
              archive.getParent());
      getLogger().info("Building : "
          + cmd + " in folder " + archive.getParent() + "/deepTools-1.5.11/");
      ProcessUtils.exec(cmd, false);

      // Install DeepTools
      cmd = String.format(
          "python2 setup.py install --prefix %s/deepTools-1.5.11/",
          archive.getParent());
      getLogger().info("Installing : "
          + cmd + " in folder " + archive.getParent() + "/deepTools-1.5.11/");
      ProcessUtils.exec(cmd, false);

      // Memorize path
      // Deeptoold is using external scripts located in a folder called
      // "deeptools" which must be added to PYTHONPATH before starting the tool
      this.deeptoolsLibPath = archive.getParent() + "/deepTools-1.5.11/";
      this.deeptoolsPath = archive.getParent() + "/deepTools-1.5.11/bin/";

    } catch (java.io.IOException e) {
      getLogger()
          .warning("Error during DeepTools installation : " + e.toString());
      return;
    }
    // }

  }

  /**
   * Run deeptools. Installation (if needed) was made during configuration.
   */
  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    getLogger().info("Running DeepTools " + this.deeptoolsPath);

    // Get peaks data (PEAK format, as generated by a peak caller)
    final Data peaksData = context.getInputData(PEAK);

    // Get experiment name of peaks data
    String currentPeaksfileExpName = peaksData.getMetadata().get("Experiment");
    getLogger().finest("Peaks file Experiment " + currentPeaksfileExpName);

    // Now list BAM files corresponding to this peak file's experiment

    // String for the concatenated BAM files paths/labels
    String bamFileNames = "";
    String bamFileLabels = "";
    // Get bam *list* (BAM format, as generated by a mapper)
    final Data mappedData = context.getInputData(MAPPER_RESULTS_BAM);
    // Loop through all samples
    for (Data oneMappedData : mappedData.getListElements()) {

      // Get metadata of current sample
      DataMetadata metadata = oneMappedData.getMetadata();

      // Get experiment name of current sample
      String currentExpName = metadata.get("Experiment");

      if (currentExpName.equals(currentPeaksfileExpName)) {

        getLogger().finest(
            String.format("BAM Experiment %s - Condition %s - RepTechGroup %s",
                currentExpName, metadata.get("Condition"),
                metadata.get("RepTechGroup")));
        final String bamFileName = oneMappedData.getDataFile().getSource();

        bamFileNames += String.format(" %s", bamFileName);
        bamFileLabels += String.format(" %s%s", metadata.get("Condition"),
            metadata.get("RepTechGroup"));

        // Find the corresponding BAI file
        // This should be replaced when a more convenient way to link BAM/BAI
        // files will be available in Eoulsan

        // Get bai *list* (BAI format, as generated by sam2bam)
        final Data indexData = context.getInputData(MAPPER_RESULTS_INDEX_BAI);
        // Loop through all indexes to find the one corresppnding to current
        // sample
        for (Data oneIndexData : indexData.getListElements()) {

          // Get metadata
          DataMetadata metadataIdx = oneIndexData.getMetadata();
          // Compare to BAM information
          if (metadataIdx.get("Experiment").equals(currentExpName)
              && metadataIdx.get("Condition").equals(metadata.get("Condition"))
              && metadataIdx.get("RepTechGroup")
                  .equals(metadata.get("RepTechGroup"))) {

            try {
              final DataFile newName =
                  new DataFile(String.format("%s.bai", bamFileName));
              if (!newName.exists()) {
                oneIndexData.getDataFile().symlink(newName);
              }
            } catch (java.io.IOException e) {
              e.printStackTrace();
              getLogger().severe(e.getMessage());
              return status.createTaskResult();
            }

            break;
          }
        }

      }
    }

    // Build command line for bamCorrelate: with bed file

    // Executable
    String cmd2 = deeptoolsPath;
    cmd2 += String.format("bamCorrelate BED-file --BED %s",
        peaksData.getDataFile().getSource());
    // BAM files
    cmd2 += String.format(" --bamfiles %s --labels %s", bamFileNames,
        bamFileLabels);
    // Parameters
    cmd2 += " --corMethod spearman -f 200 --colorMap Reds --zMin 0 --zMax 1";
    // Output file
    cmd2 += String.format(" -o bamcorrelatebed_output_report_%s.pdf",
        currentPeaksfileExpName);
    // Run command
    try {
      getLogger()
          .info(String.format("With : PYTHONPATH=%s \n running command: %s ",
              deeptoolsLibPath, cmd2));

      final ProcessBuilder pb2 = new ProcessBuilder("/bin/bash", "-c", cmd2);
      Map<String, String> env2 = pb2.environment();
      env2.put("PYTHONPATH", deeptoolsLibPath);

      final Process p2 = pb2.start();
      String output = loadStream(p2.getInputStream());
      String error = loadStream(p2.getErrorStream());
      try {
        p2.waitFor();
      } catch (InterruptedException e) {
        e.printStackTrace();
        throw new java.io.IOException(e.getMessage());
      }
      getLogger().info("STDOUT:" + output);
      getLogger().info("STDERR:" + error);

    } catch (java.io.IOException e) {
      e.printStackTrace();
      getLogger().severe(e.getMessage());
    }

    // Build command line for bamCorrelate: whole genome

    // Executable
    String cmd1 = deeptoolsPath;
    cmd1 += "bamCorrelate bins";
    // BAM files
    cmd1 += String.format(" --bamfiles %s --labels %s", bamFileNames,
        bamFileLabels);
    // Parameters
    cmd1 += " --corMethod spearman -f 200 --colorMap Blues --zMin 0 --zMax 1";
    // Output file
    cmd1 += String.format(" -o ./bamcorrelatebins_output_report_%s.pdf",
        currentPeaksfileExpName);
    // Run command
    try {
      getLogger()
          .info(String.format("With : PYTHONPATH=%s \n running command: %s ",
              deeptoolsLibPath, cmd1)); // PYTHONPATH=\"" + archive.getParent()
                                        // +
                                        // "/deepTools-1.5.11/lib/python2.7/site-packages/\"

      final ProcessBuilder pb1 = new ProcessBuilder("/bin/bash", "-c", cmd1);
      Map<String, String> env1 = pb1.environment();
      env1.put("PYTHONPATH", deeptoolsLibPath);

      final Process p1 = pb1.start();
      String output = loadStream(p1.getInputStream());
      String error = loadStream(p1.getErrorStream());
      try {
        p1.waitFor();
      } catch (InterruptedException e) {
        e.printStackTrace();
        throw new java.io.IOException(e.getMessage());
      }
      getLogger().info("STDOUT:" + output);
      getLogger().info("STDERR:" + error);

    } catch (java.io.IOException e) {
      e.printStackTrace();
      getLogger().severe(e.getMessage());
    }

    // Build command line for bamFingerprint: whole genome

    // Executable
    String cmd3 = deeptoolsPath;
    cmd3 += "bamFingerprint";
    // BAM files
    cmd3 += String.format(" --bamfiles %s --labels %s", bamFileNames,
        bamFileLabels);
    // Output file
    cmd3 += String.format(
        " --plotFile bamfingerprint_output_report_%s.pdf --plotFileFormat pdf",
        currentPeaksfileExpName);
    // Run command
    try {
      getLogger()
          .info(String.format("With : PYTHONPATH=%s \n running command: %s ",
              deeptoolsLibPath, cmd3));

      final ProcessBuilder pb3 = new ProcessBuilder("/bin/bash", "-c", cmd3);
      Map<String, String> env3 = pb3.environment();
      env3.put("PYTHONPATH", deeptoolsLibPath);

      final Process p3 = pb3.start();
      String output = loadStream(p3.getInputStream());
      String error = loadStream(p3.getErrorStream());
      try {
        p3.waitFor();
      } catch (InterruptedException e) {
        e.printStackTrace();
        throw new java.io.IOException(e.getMessage());
      }
      getLogger().info("STDOUT:" + output);
      getLogger().info("STDERR:" + error);

    } catch (java.io.IOException e) {
      e.printStackTrace();
      getLogger().severe(e.getMessage());
    }

    return status.createTaskResult();

  }

  private static String loadStream(InputStream s) throws java.io.IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(s));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null)
      sb.append(line).append("\n");
    return sb.toString();
  }

}
