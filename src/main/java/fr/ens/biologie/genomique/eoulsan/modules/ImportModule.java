/*
 *                  Eoulsan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public License version 2.1 or
 * later and CeCILL-C. This should be distributed with the code.
 * If you do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/lgpl-2.1.txt
 *      http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.txt
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École normale supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.NoLog;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.DataUtils;
import fr.ens.biologie.genomique.eoulsan.core.FileNaming;
import fr.ens.biologie.genomique.eoulsan.core.FileNamingParsingRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.core.workflow.DataMetadataStorage;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;

/**
 * This class define a import step.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@ReuseModuleInstance
@NoLog
public class ImportModule extends AbstractModule {

  public static final String MODULE_NAME = "import";

  private Set<DataFile> files;
  private OutputPorts outputPorts;
  private boolean copy;
  private DataFormat format;

  /**
   * This class allow to find files that matche to a pattern.
   */
  private static class Finder extends SimpleFileVisitor<Path> {

    private final PathMatcher matcher;
    private final Set<File> files = new HashSet<>();

    /**
     * Test if a file matches to the pattern.
     * @param file the file to test
     */
    private void find(Path file) {

      if (matcher.matches(file.toAbsolutePath())) {
        this.files.add(file.toFile());
      }
    }

    /**
     * Get the file found.
     * @return a set with the files found
     */
    Set<DataFile> getFiles() {

      final Set<DataFile> result = new HashSet<>();
      for (File f : this.files) {
        result.add(new DataFile(f));
      }

      return result;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      find(file);
      return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
        final BasicFileAttributes attrs) {
      find(dir);
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      return CONTINUE;
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param workingDirectory the working directory
     * @param pattern the file matching pattern
     */
    Finder(final File workingDirectory, final String pattern) {

      String finalPattern = !pattern.startsWith("/")
          ? workingDirectory.getAbsolutePath() + '/' + pattern : pattern;

      this.matcher =
          FileSystems.getDefault().getPathMatcher("glob:" + finalPattern);
    }

  }

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public OutputPorts getOutputPorts() {

    return this.outputPorts;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    DataFile baseDir = context.getOutputDirectory();
    String pattern = "";

    // Parse parameters
    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "files":
        pattern = p.getStringValue();
        break;

      case "directory":
        Modules.deprecatedParameter(context, p, true);
        break;

      case "copy":
        this.copy = p.getBooleanValue();
        break;

      case "format":
        this.format = DataFormatRegistry.getInstance()
            .getDataFormatFromGalaxyFormatNameOrNameOrAlias(p.getValue());
        if (this.format == null) {
          Modules.badParameterValue(context, p, "Unknown format");
        }
        break;

      default:
        Modules.unknownParameter(context, p);
      }
    }

    // Set the output ports
    try {

      // Check if base directory exists
      if (!(baseDir.exists() && baseDir.getMetaData().isDir())) {
        Modules.invalidConfiguration(context,
            "The directory does not exists: " + baseDir);
      }

      // Get the list of the files to import
      this.files = findFiles(baseDir, pattern);
      // this.files = listFilesFromPatterns(baseDir, pattern);

      // Check if some files has been found
      if (this.files.isEmpty()) {
        Modules.invalidConfiguration(context,
            "No input file found in the " + getName() + " step");
      }

      // Get the format and the compression of the files
      final Map<DataFormat, CompressionType> formats =
          listDataFormatFromFileList(this.files, this.format);

      if (formats.isEmpty()) {
        Modules.invalidConfiguration(context,
            "No format found for the files matching to the pattern");
      }

      if (formats.size() > 1) {
        Modules.invalidConfiguration(context,
            "More than one file format found for the files matching "
                + "to the pattern");
      }

      // Create the output ports
      final OutputPortsBuilder builder = new OutputPortsBuilder();

      for (Map.Entry<DataFormat, CompressionType> e : formats.entrySet()) {
        builder.addPort("output", true, e.getKey(), e.getValue());
      }

      this.outputPorts = builder.create();

    } catch (IOException e) {
      throw new EoulsanException(e);
    }
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    // Create a map with the samples
    final Map<String, Sample> samples = new HashMap<>();
    for (Sample sample : context.getWorkflow().getDesign().getSamples()) {
      samples.put(Naming.toValidName(sample.getId()), sample);
    }

    try {

      // Sort the list of files to process
      final List<DataFile> sortedFiles = new ArrayList<>(this.files);
      Collections.sort(sortedFiles);

      // Group files related to the same data
      final Set<List<DataFile>> groupedFiles = groupFiles(registry, files);

      // For each data
      for (List<DataFile> inputFiles : groupedFiles) {

        Data data = null;

        // For each files of the data
        for (DataFile inputFile : inputFiles) {

          final DataFormat format = this.format == null
              ? fileFormat(registry, inputFile) : this.format;
          final FileNaming fileNaming = fileNaming(inputFile);

          // Define the data object
          if (data == null) {

            // If file use the Eoulsan naming
            if (fileNaming != null) {

              data = context.getOutputData(format, format.getPrefix())
                  .addDataToList(fileNaming.getDataName(),
                      fileNaming.getPart());

              // Set metadata of imported files
              final boolean isMetadataSet =
                  DataMetadataStorage.getInstance(context.getOutputDirectory())
                      .loadMetadata(data, Collections.singletonList(inputFile));

              // Set the metadata from sample metadata
              if (!isMetadataSet && samples.containsKey(data.getName())) {
                DataUtils.setDataMetaData(data, samples.get(data.getName()));
              }

            }
            // If file does not use Eoulsan naming
            else {

              // Define the data name
              final String dataName =
                  Naming.toValidName(inputFile.getBasename());

              data = context.getOutputData(format, format.getPrefix())
                  .addDataToList(dataName);
            }
          }

          DataFile outputFile;

          if (format.getMaxFilesCount() > 1) {

            if (fileNaming != null) {
              outputFile = data.getDataFile(fileNaming.getFileIndex());
            } else {
              outputFile = data.getDataFile(0);
            }
          } else {
            outputFile = data.getDataFile();
          }

          // Copy or create symbolic link
          if (this.copy) {
            DataFiles.copy(inputFile, outputFile);

          } else {
            DataFiles.symlinkOrCopy(inputFile, outputFile, true);
          }
        }

        // Set the metadata for the data
        DataMetadataStorage.getInstance(context.getOutputDirectory())
            .loadMetadata(data, inputFiles);
      }

    } catch (EoulsanException | IOException e) {
      return status.createTaskResult(e);
    }

    return status.createTaskResult();
  }

  //
  // Other methods
  //

  /**
   * Get the part of a path that exists.
   * @param path the path to check
   * @return a new Path with the part of the path that exists
   */
  private static Path getMinExistingPath(final String path) {

    File result = new File("/");

    for (Path p : new File(path).toPath()) {

      File f = new File(result, p.toString());
      if (f.exists()) {
        result = f;
      } else {
        break;
      }
    }

    return result.toPath();
  }

  /**
   * Find files that match with the pattern.
   * @param workingDirectory the working directory
   * @param pattern the pattern
   * @return a set with the matching files
   * @throws IOException if a error occurs while finding files
   */
  private static Set<DataFile> findFiles(final DataFile workingDirectory,
      final String pattern) throws IOException {

    Objects.requireNonNull(workingDirectory,
        "workingDirectory argument cannot be null");
    Objects.requireNonNull(pattern, "pattern argument cannot be null");

    Finder finder = new Finder(workingDirectory.toFile(), pattern);

    Path baseDir = pattern.startsWith("/")
        ? getMinExistingPath(pattern)
        : workingDirectory.toFile().toPath();

    Files.walkFileTree(baseDir, finder);
    return finder.getFiles();
  }

  /**
   * Get the format and compression of a list of files.
   * @param files the list of file
   * @param format format of the file. Can be null
   * @return a map with for each format the common compression of the files
   * @throws EoulsanException if format of a file cannot be determined
   */
  private static Map<DataFormat, CompressionType> listDataFormatFromFileList(
      final Set<DataFile> files, final DataFormat format)
      throws EoulsanException {

    if (files == null) {
      return Collections.emptyMap();
    }

    final Map<DataFormat, CompressionType> result = new HashMap<>();
    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (DataFile file : files) {

      final DataFormat fileFormat =
          format == null ? fileFormat(registry, file) : format;
      final CompressionType compression = file.getCompressionType();

      final CompressionType previous = result.get(fileFormat);

      if (previous == null || previous == CompressionType.NONE) {
        result.put(fileFormat, compression);
      }

    }

    return Collections.unmodifiableMap(result);
  }

  /**
   * Get the format of a file.
   * @param registry the format registry
   * @param file the file which name must be parsed
   * @return the DataFormat of the file
   * @throws EoulsanException if no format or several format for the file has
   *           been found
   */
  private static DataFormat fileFormat(final DataFormatRegistry registry,
      final DataFile file) throws EoulsanException {

    try {

      // First try to get format of file if file name use Eoulsan file naming

      FileNaming name = FileNaming.parse(file);

      return name.getFormat();

    } catch (FileNamingParsingRuntimeException e) {

      // If not work, try to get the format of the file from its file extension

      final String extension = file.getExtension();

      final Set<DataFormat> formats =
          registry.getDataFormatsFromExtension(extension);

      if (formats.isEmpty()) {
        throw new EoulsanException("No format found for file: " + file);
      }

      if (formats.size() > 1) {
        throw new EoulsanException(
            "More than one format found for file: " + file);
      }

      return formats.iterator().next();
    }
  }

  /**
   * Get the FileNaming related to a file if can be created.
   * @param file file which name must be parsed
   * @return a FileNaming object or null, if the file name cannot be parsed
   */
  private static FileNaming fileNaming(final DataFile file) {

    try {

      return FileNaming.parse(file);
    } catch (FileNamingParsingRuntimeException e) {

      return null;
    }
  }

  /**
   * Group file by data.
   * @param registry format registry
   * @param files files to process
   */
  private static Set<List<DataFile>> groupFiles(
      final DataFormatRegistry registry, final Set<DataFile> files) {

    final Set<List<DataFile>> result = new HashSet<>();

    List<DataFile> group = new ArrayList<>();
    DataFile previous = null;

    // Sort files
    List<DataFile> sortedFiles = new ArrayList<>();
    sortedFiles.addAll(files);
    Collections.sort(sortedFiles);

    // For each files
    for (DataFile file : sortedFiles) {

      // Test if current file is related to the same data than the previous data
      if (group.isEmpty() || FileNaming.dataEquals(previous, file)) {
        group.add(file);
      } else {
        result.add(group);
        group = new ArrayList<>();
        group.add(file);
      }

      previous = file;
    }

    if (!group.isEmpty()) {
      result.add(group);
    }

    return Collections.unmodifiableSet(result);
  }

}
