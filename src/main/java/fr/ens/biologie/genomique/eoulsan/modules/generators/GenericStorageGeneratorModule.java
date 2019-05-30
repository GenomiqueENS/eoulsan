package fr.ens.biologie.genomique.eoulsan.modules.generators;

import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_FASTA;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.bio.GenomeDescription;
import fr.ens.biologie.genomique.eoulsan.core.FileNaming;
import fr.ens.biologie.genomique.eoulsan.core.InputPort;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.core.workflow.ModuleRegistry;
import fr.ens.biologie.genomique.eoulsan.core.workflow.TaskContextImpl;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This module allow to easily store output of generator to avoid computation at
 * each Eoulsan startup.
 * @author Laurent Jourdren
 * @since 2.4
 */
@LocalOnly
public class GenericStorageGeneratorModule extends AbstractModule {

  public static final String MODULE_NAME = "genericstoragegenerator";

  private Module module;
  private List<Parameter> moduleParameter = new ArrayList<>();
  private boolean storeResult = true;
  private boolean useGenomeDescriptionForGenome;

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

    boolean genomeFound = false;
    boolean genomeDescFound = false;

    InputPortsBuilder builder = new InputPortsBuilder();

    for (InputPort p : this.module.getInputPorts()) {

      DataFormat format = p.getFormat();

      if (GENOME_FASTA.equals(format)) {
        genomeFound = true;
      }

      if (GENOME_DESC_TXT.equals(format)) {
        genomeDescFound = true;
      }

      builder.addPort(p.getName(), p.isList(), format,
          p.getCompressionsAccepted(), p.isRequiredInWorkingDirectory());
    }

    // Add Genome description if genome format is in inputs
    if (!genomeDescFound && genomeFound) {
      builder.addPort("genomedescription", GENOME_DESC_TXT);
      this.useGenomeDescriptionForGenome = true;
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return this.module.getOutputPorts();
  }

  @Override
  public Set<Requirement> getRequirements() {

    return this.module.getRequirements();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "modulename":

        this.module =
            ModuleRegistry.getInstance().loadModule(p.getStringValue(), null);

        if (this.module == null) {
          throw new EoulsanException(
              "The \"" + p.getStringValue() + "\" module cannot be loaded");
        }

        break;

      case "storeresult":
        this.storeResult = p.getBooleanValue();
        break;

      default:
        this.moduleParameter.add(p);
        break;
      }
    }

    if (this.module == null) {
      throw new EoulsanException(
          "The \"moduleName\" attribute is missing in the \"generator\" XML tag");
    }

    // Module wrapped module
    this.module.configure(context, stepParameters);

    // Check that there is only one output port
    if (getOutputPorts().size() != 1) {
      throw new EoulsanException(
          "A generator can only have one output port. Found "
              + getOutputPorts().size() + " output ports");
    }

    if (getOutputPorts().getFirstPort().getFormat().getMaxFilesCount() > 1) {
      throw new EoulsanException(
          "The generator output format cannot have multiple output files. Found "
              + getOutputPorts().getFirstPort().getName() + " format");
    }

    // Sort parameters
    this.moduleParameter.sort(Comparator.naturalOrder());
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Execute wrapped module if storage is disabled
    if (!this.storeResult) {
      return this.module.execute(context, status);
    }

    try {

      // Get storage name
      String storageName =
          getOutputPorts().getFirstPort().getFormat().getPrefix();

      // Get storage path
      DataFile storageDirectory = getRepositoryDirectory(storageName, context);

      // Compute MD5
      String md5 = computeMD5(context);

      context.getLogger().info("Computed MD5 sum is: " + md5);

      // Define archive file
      DataFile archiveFile = new DataFile(storageDirectory, md5 + ".zip");

      if (archiveFile.exists()) {

        context.getLogger()
            .info("Use already computed data from archive: " + archiveFile);

        // Get the name of the first file
        String unzippedFilename = firstZipEntryName(archiveFile);

        // Use the same data name as the original data name
        DataFile expectedOutputFile =
            context
                .getOutputData(getOutputPorts().getFirstPort().getName(),
                    FileNaming.parse(unzippedFilename).getDataName())
                .getDataFile();

        DataFile unzippedDataFile =
            new DataFile(expectedOutputFile.getParent(), unzippedFilename);

        // Unzip the archive
        FileUtils.unzip(archiveFile.toFile(),
            expectedOutputFile.getParent().toFile());

        // Rename the output if required
        if (!expectedOutputFile.equals(unzippedDataFile)) {
          unzippedDataFile.renameTo(expectedOutputFile);
        }

        return status.createTaskResult();
      }

      context.getLogger()
          .info("Data has not been already computed. Execute generator.");

      // Execute wrapped module
      TaskResult result = this.module.execute(context, status);

      context.getLogger()
          .info("Zip output of the generator in archive file: " + archiveFile);

      // Get output file prefix
      String outputFileprefix =
          FileNaming.filePrefix(context.getCurrentStep().getId(),
              getOutputPorts().getFirstPort().getName(),
              getOutputPorts().getFirstPort().getFormat());

      // Get the list of file to zip
      final File[] filesToAdd =
          context.getStepOutputDirectory().toFile().listFiles(file -> file.getName().startsWith(outputFileprefix));

      // Zip index files
      FileUtils.createZip(context.getStepOutputDirectory().toFile(),
          Arrays.asList(filesToAdd), archiveFile.toFile(), false);

      return result;
    } catch (EoulsanException | IOException e) {
      return status.createTaskResult(e);
    }
  }

  /**
   * Create new MessageDigest object for MD5.
   * @return a new MessageDigest object
   * @throws EoulsanException if an error occurs while creating the object
   */
  private static MessageDigest newMD5MessageDigest() throws EoulsanException {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new EoulsanException("Unable to create MD5 message digest", e);
    }
  }

  /**
   * Compute input file MD5 sum.
   * @param inputFiles input files
   * @return a list of MD5 sums
   * @throws IOException if an error occurs while computing MD5 sums
   */
  private static List<String> computeAndSortInputFileMD5(
      final Set<DataFile> inputFiles, final TaskContext context)
      throws IOException {

    List<String> result = new ArrayList<>();

    for (DataFile f : inputFiles) {
      String md5 = FileUtils.computeMD5Sum(f.rawOpen());
      result.add(md5);
      context.getLogger().info("MD5 sum of " + f + " file: " + md5);
    }

    result.sort(Comparator.naturalOrder());

    return result;
  }

  /**
   * Get the repository directory.
   * @param storageName the storage name
   * @param context the context
   * @return the repository directory
   * @throws IOException if an error occurs while creating the repository
   *           directory
   */
  private static DataFile getRepositoryDirectory(final String storageName,
      final TaskContext context) throws IOException {

    DataFile result;

    String storagePath = EoulsanRuntime.getSettings()
        .getSetting("main." + storageName + ".data.storage.path");

    if (storagePath == null || storagePath.trim().isEmpty()) {
      result =
          new DataFile(((TaskContextImpl) context).getDataRepositoryDirectory(),
              storageName);
    } else {
      result = new DataFile(storagePath);
    }

    if (!result.exists()) {
      result.mkdirs();
    }

    return result;
  }

  /**
   * Compute MD5 sum.
   * @param context step context
   * @return the MD5 sum
   * @throws EoulsanException if an error occurs while computing the sum
   * @throws IOException if an error occurs while read input files
   */
  private String computeMD5(final TaskContext context)
      throws EoulsanException, IOException {

    // Get the list of input datafiles
    Set<DataFile> inputFiles = new HashSet<>();
    for (InputPort port : this.module.getInputPorts()) {

      Data data = context.getInputData(port.getName());
      DataFormat format = data.getFormat();

      if (this.useGenomeDescriptionForGenome
          && (GENOME_FASTA.equals(format) || GENOME_DESC_TXT.equals(format))) {
        continue;
      }

      for (Data d : data.getListElements()) {
        if (format.getMaxFilesCount() > 1) {

          for (int i = 0; i < d.getDataFileCount(); i++) {
            inputFiles.add(d.getDataFile(i));
          }
        } else {
          inputFiles.add(d.getDataFile());
        }
      }
    }

    MessageDigest md5Digest = newMD5MessageDigest();
    md5Digest.update(this.module.getName().getBytes(Globals.DEFAULT_CHARSET));
    md5Digest.update(
        this.module.getVersion().toString().getBytes(Globals.DEFAULT_CHARSET));

    // Step parameters
    for (Parameter p : this.moduleParameter) {
      md5Digest.update(p.getName().getBytes(Globals.DEFAULT_CHARSET));
      md5Digest.update(p.getStringValue().getBytes(Globals.DEFAULT_CHARSET));
    }

    // Genome MD5 sum
    if (this.useGenomeDescriptionForGenome) {

      context.getLogger().info("Load Genome description to get genome MD5 sum");
      // Load Genome description
      final GenomeDescription desc = GenomeDescription
          .load(context.getInputData(GENOME_DESC_TXT).getDataFile().open());
      context.getLogger().info("Genome MD5 sum: " + desc.getMD5Sum());
      md5Digest.update(desc.getMD5Sum().getBytes(Globals.DEFAULT_CHARSET));
    }

    // Input file MD5 sums
    for (String md5Sum : computeAndSortInputFileMD5(inputFiles, context)) {
      md5Digest.update(md5Sum.getBytes(Globals.DEFAULT_CHARSET));
    }

    return StringUtils.md5DigestToString(md5Digest);
  }

  /**
   * List the entries of a Zip file.
   * @param zipDataFile the zip file
   * @return a list with entries in the zip file
   * @throws ZipException if an error occurs while reading the zip file
   * @throws IOException if an error occurs while reading the zip file
   */
  private static List<String> listZipEntries(DataFile zipDataFile)
      throws ZipException, IOException {

    List<String> result = new ArrayList<>();

    try (ZipFile zipFile = new ZipFile(zipDataFile.toFile())) {
      Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();

      while (zipEntries.hasMoreElements()) {
        String fileName = zipEntries.nextElement().getName();
        result.add(fileName);
      }
    }

    return result;
  }

  /**
   * Get the first entry of a zip file
   * @param zipDataFile the zip file
   * @return a string with the first entry of the zip file
   * @throws EoulsanException if the zip file is empty
   * @throws ZipException if an error occurs while reading the zip file
   * @throws IOException if an error occurs while reading the zip file
   */
  private static String firstZipEntryName(DataFile zipDataFile)
      throws EoulsanException, ZipException, IOException {

    // Get the list of zipped files
    List<String> zipEntries = listZipEntries(zipDataFile);
    if (zipEntries.isEmpty()) {
      throw new EoulsanException("No data found in archive: " + zipDataFile);
    }

    // Get the name of the first file
    String result = zipEntries.get(0);

    if (result.endsWith("/")) {
      return result.substring(0, result.length() - 1);
    }

    return result;
  }

}
