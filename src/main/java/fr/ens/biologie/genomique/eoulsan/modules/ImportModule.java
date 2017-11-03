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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;

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

  private static final Splitter SPACE_SPLITTER =
      Splitter.on(' ').trimResults().omitEmptyStrings();

  private Set<DataFile> files;
  private OutputPorts outputPorts;
  private boolean copy;

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

    DataFile baseDir = new DataFile(new File("."));
    String pattern = "";

    // Parse parameters
    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "files":
        pattern = p.getStringValue();
        break;

      case "directory":

        if (p.getStringValue().length() > 0) {
          baseDir = new DataFile(p.getStringValue());
        }
        break;

      case "copy":
        this.copy = p.getBooleanValue();
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
      this.files = listFilesFromPatterns(baseDir, pattern);

      // Check if some files has been found
      if (this.files.isEmpty()) {
        Modules.invalidConfiguration(context,
            "No input file found in the " + getName() + " step");
      }

      // Get the format and the compression of the files
      final Map<DataFormat, CompressionType> formats =
          listDataFormatFromFileList(this.files);

      // Create the output ports
      final OutputPortsBuilder builder = new OutputPortsBuilder();
      int count = 0;

      for (Map.Entry<DataFormat, CompressionType> e : formats.entrySet()) {
        builder.addPort("output" + (++count), true, e.getKey(), e.getValue());
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

          final DataFormat format = fileFormat(registry, inputFile);
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
   * Build collection of PathMatcher for selection files to tread according to a
   * pattern file define in test configuration. Patterns set in string with
   * space to separator. Get input and output patterns files.
   * @param patterns sequences of patterns filesList.
   * @return a set of PathMatcher, one per pattern.
   */
  private static Set<PathMatcher> createPathMatchers(final String patterns) {

    // No pattern defined
    if (patterns == null || patterns.trim().isEmpty()) {

      return Collections.emptySet();
    }

    // Initialize collection
    final Set<PathMatcher> result = new HashSet<>();

    // Parse patterns
    for (final String globSyntax : SPACE_SPLITTER.split(patterns)) {

      // Convert in syntax reading by Java
      final PathMatcher matcher =
          FileSystems.getDefault().getPathMatcher("glob:" + globSyntax);

      // Add in list patterns files to treat
      result.add(matcher);
    }

    // Return unmodifiable collection
    return Collections.unmodifiableSet(result);
  }

  /**
   * Listing recursively all files in the source directory which match with
   * patterns files
   * @param patternKey the pattern key
   * @return the list with all files which match with pattern
   * @throws IOException if an error occurs while parsing input directory
   * @throws EoulsanException if no file to compare found
   */
  private Set<DataFile> listFilesFromPatterns(final DataFile directory,
      final String patternKey) throws IOException, EoulsanException {

    final Set<PathMatcher> fileMatchers = createPathMatchers(patternKey);

    final Set<DataFile> files = listFilesFromPatterns(directory, fileMatchers);

    // Return unmodifiable list
    return Collections.unmodifiableSet(files);
  }

  /**
   * Create list files matching to the patterns
   * @param patterns set of pattern to filter file in result directory
   * @return unmodifiable list of files or empty list
   * @throws IOException
   */
  private Set<DataFile> listFilesFromPatterns(final DataFile directory,
      final Set<PathMatcher> patterns) throws IOException {

    final Set<DataFile> filesFound = new HashSet<>();
    final List<DataFile> files = directory.list();

    for (final PathMatcher matcher : patterns) {

      for (DataFile f : files) {
        if (matcher.matches(new File(f.getName()).toPath())) {
          filesFound.add(f);
        }
      }
    }

    return Collections.unmodifiableSet(filesFound);
  }

  /**
   * Get the format and compression of a list of files.
   * @param files the list of file
   * @return a map with for each format the common compression of the files
   * @throws EoulsanException if format of a file cannot be determined
   */
  private static Map<DataFormat, CompressionType> listDataFormatFromFileList(
      final Set<DataFile> files) throws EoulsanException {

    if (files == null) {
      return Collections.emptyMap();
    }

    final Map<DataFormat, CompressionType> result = new HashMap<>();
    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (DataFile file : files) {

      final DataFormat format = fileFormat(registry, file);
      final CompressionType compression = file.getCompressionType();

      final CompressionType previous = result.get(format);

      if (previous == null || previous == CompressionType.NONE) {
        result.put(format, compression);
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
