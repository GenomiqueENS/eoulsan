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
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Eoulsan project and its aims,
 * or to join the Eoulsan Google group, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/eoulsan
 *
 */

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toLetter;

import java.io.File;

import com.google.common.base.CharMatcher;

import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class contains methods to create workflow data file names.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileNaming {

  private static final char SEPARATOR = '_';
  private static final String FILE_INDEX_PREFIX = "file";
  private static final String PART_INDEX_PREFIX = "part";

  private static final CharMatcher ASCII_LETTER_OR_DIGIT = inRange('a', 'z')
      .or(inRange('A', 'Z')).or(inRange('0', '9'));

  private String stepId;
  private String portName;
  private String dataName;
  private int sampleId = -1;
  private DataFormat format;
  private int fileIndex = -1;
  private int part = -1;
  private CompressionType compression = CompressionType.NONE;

  //
  // Getters
  //

  /**
   * Get Step Id.
   * @return the step Id
   */
  public String getStepId() {
    return stepId;
  }

  /**
   * Get the port name.
   * @return the port name
   */
  public String getPortName() {
    return portName;
  }

  /**
   * Get the data name.
   * @return the data name
   */
  public String getDataName() {
    return dataName;
  }

  /**
   * Get the sample id related to the data. This value is only use when generate
   * compatible filenames.
   * @return the id of the sample related to the file or -1 if not known
   */
  public int getSampleId() {
    return sampleId;
  }

  /**
   * Get the format.
   * @return the format
   */
  public DataFormat getFormat() {
    return format;
  }

  /**
   * Get the file index.
   * @return the file index
   */
  public int getFileIndex() {
    return fileIndex;
  }

  /**
   * Get the file part.
   * @return the file part
   */
  public int getPart() {
    return part;
  }

  /**
   * Get the compression.
   * @return the compression
   */
  public CompressionType getCompression() {
    return compression;
  }

  //
  // Setters
  //

  /**
   * Set the step id.
   * @param stepId the step id
   */
  public void setStepId(final String stepId) {

    checkStepId(stepId);
    this.stepId = stepId;
  }

  /**
   * Set the port name.
   * @param portName the port name
   */
  public void setPortName(final String portName) {

    checkPortName(portName);
    this.portName = portName;
  }

  /**
   * Set the data name.
   * @param dataName the data name
   */
  public void setDataName(final String dataName) {

    checkDataName(dataName);
    this.dataName = dataName;
  }

  /**
   * Set the sample id related to the data. This value is only use when generate
   * compatible filenames.
   * @param sampleId the id of the sample related to the file or -1 if not known
   */
  public void setSampleId(final int sampleId) {

    this.sampleId = sampleId;
  }

  /**
   * Set the format.
   * @param format the format
   */
  public void setFormat(final DataFormat format) {

    checkNotNull(format, "format argument cannot be null");
    this.format = format;
  }

  /**
   * Set the file index.
   * @param fileIndex the file index
   */
  public void setFileIndex(final int fileIndex) {

    this.fileIndex = fileIndex < 0 ? -1 : fileIndex;
  }

  /**
   * Set the part number.
   * @param part the part number
   */
  public void setPart(final int part) {

    this.part = part < 0 ? -1 : part;
  }

  /**
   * Set the compression
   * @param compression the compression type
   */
  public void setCompression(final CompressionType compression) {

    checkNotNull(compression, "compression argument cannot be null");
    this.compression = compression;
  }

  /**
   * Set several field of the object from a workflow output port.
   * @param port the workflow output port
   */
  private void set(final WorkflowOutputPort port) {

    checkNotNull(port, "port argument cannot be null");

    setStepId(port.getStep().getId());
    setPortName(port.getName());
    setFormat(port.getFormat());
    setCompression(port.getCompression());
  }

  /**
   * Set several field of the object from a Data object.
   * @param port the data object
   */
  private void set(final Data data) {

    checkNotNull(data, "port argument cannot be null");

    setDataName(data.getName());
    setPart(data.getPart());
  }

  //
  // Other methods
  //

  /**
   * Return the file prefix.
   * @return a string with the file prefix
   */
  public String filePrefix() {

    checkNotNull(this.stepId, "stepId has not been set");
    checkNotNull(this.portName, "portName has not been set");
    checkNotNull(this.format, "format has not been set");

    return filePrefix(this.stepId, this.portName, this.format.getPrefix());
  }

  /**
   * Return the file suffix.
   * @return q string with the file suffix
   */
  public String fileSuffix() {

    checkNotNull(this.format, "format has not been set");
    checkNotNull(this.compression, "compression has not been set");

    return fileSuffix(this.format.getDefaultExtention(),
        this.compression.getExtension());
  }

  /**
   * Return the middle string of the filename.
   * @return a string with the middle string of the filename
   */
  public String fileMiddle() {

    checkNotNull(this.dataName, "datName has not been set");

    checkFormatAndFileIndex();

    return fileMiddle(this.dataName, this.fileIndex, this.part);
  }

  /**
   * Return the filename.
   * @return a string with the filename
   */
  public String filename() {

    checkNotNull(this.stepId, "stepId has not been set");
    checkNotNull(this.portName, "portName has not been set");
    checkNotNull(this.format, "format has not been set");
    checkNotNull(this.dataName, "datName has not been set");
    checkNotNull(this.compression, "compression has not been set");

    checkFormatAndFileIndex();

    return filename(this.stepId, this.portName, this.format, this.dataName,
        this.fileIndex, this.part, this.compression);
  }

  /**
   * Get a glob for the filename.
   * @return a glob in a string
   */
  public String glob() {

    final StringBuilder sb = new StringBuilder();

    sb.append(filePrefix());
    sb.append('*');
    sb.append(fileSuffix());

    return sb.toString();
  }

  /**
   * Return the filename using Eoulsan 1.x naming.
   * @return a string with the filename using Eoulsan 1.x naming
   */
  public String compatibilityFilename() {

    checkNotNull(this.stepId, "stepId has not been set");
    checkNotNull(this.portName, "portName has not been set");
    checkNotNull(this.format, "format has not been set");
    checkNotNull(this.dataName, "datName has not been set");
    checkNotNull(this.compression, "compression has not been set");

    checkFormatAndFileIndex();

    final StringBuilder sb = new StringBuilder();

    final String prefix;

    // Set the prefix against step name
    switch (this.stepId) {

    case "filterreads":
      prefix = "filtered_reads";
      break;

    case "mapreads":
      prefix = "mapper_results";
      break;

    case "filtersam":
    case "filterandmap":
      prefix = "filtered_mapper_results";
      break;

    default:
      prefix = this.stepId;
      break;
    }

    sb.append(prefix);
    sb.append('_');

    // Set the id of the sample
    if (this.format.isOneFilePerAnalysis()) {
      sb.append('1');
    } else {
      sb.append(this.sampleId);
    }

    // Set the file index if needed
    if (fileIndex >= 0) {

      sb.append(toLetter(fileIndex));
    }

    // Set the extension
    sb.append(this.format.getDefaultExtention());

    return sb.toString();
  }

  /**
   * Check if the file index is valid for the current format
   */
  private void checkFormatAndFileIndex() {

    if (this.format.getMaxFilesCount() == 1) {
      checkArgument(this.fileIndex == -1,
          "Invalid fileIndex argument for format "
              + this.format.getName() + ": " + this.fileIndex);
    } else {
      checkArgument(fileIndex < format.getMaxFilesCount(),
          "Invalid fileIndex argument for format "
              + this.format.getName() + ": " + this.fileIndex);
    }

  }

  //
  // Static methods
  //

  /**
   * Create the prefix of a filename.
   * @param port output port that generate the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final WorkflowOutputPort port) {

    final FileNaming f = new FileNaming();
    f.set(port);

    return f.filePrefix();
  }

  /**
   * Create the prefix of a filename.
   * @param stepId step id
   * @param portName port name
   * @param format format of the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final String stepId, final String portName,
      final DataFormat format) {

    final FileNaming f = new FileNaming();
    f.setStepId(stepId);
    f.setPortName(portName);
    f.setFormat(format);

    return f.filePrefix();
  }

  /**
   * Create the prefix of a filename.
   * @param stepId step id
   * @param portName port name
   * @param formatPrefix format prefix of the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final String stepId, final String portName,
      final String formatPrefix) {

    checkStepId(stepId);
    checkPortName(portName);
    checkFormatPrefix(formatPrefix);

    final StringBuilder sb = new StringBuilder();

    // Set the name of the step that generated the file
    sb.append(stepId);
    sb.append(SEPARATOR);

    // Set the port of the step that generated the file
    sb.append(portName);
    sb.append(SEPARATOR);

    // Set the name of the format
    sb.append(formatPrefix);
    sb.append(SEPARATOR);

    return sb.toString();
  }

  /**
   * Create the glob for the port.
   * @return a glob in a string
   */
  public static String glob(final WorkflowOutputPort port) {

    final FileNaming f = new FileNaming();
    f.set(port);

    return f.glob();
  }

  //
  // Middle creation
  //

  /**
   * Create the middle of a filename.
   * @param port output port that generate the file
   * @param data the data
   * @param fileIndex file index
   * @return a String with the suffix of a file
   */
  public static String fileMiddle(final WorkflowOutputPort port,
      final DataElement data, final int fileIndex) {

    final FileNaming f = new FileNaming();
    f.set(port);
    f.set(data);
    f.setFileIndex(fileIndex);

    return f.fileMiddle();
  }

  /**
   * Create the middle of a filename.
   * @param port output port that generate the file
   * @param fileIndex file index
   * @param part file part
   * @return a String with the suffix of a file
   */
  public static String fileMiddle(final WorkflowOutputPort port,
      final String dataName, final int fileIndex, final int part) {

    final FileNaming f = new FileNaming();
    f.set(port);
    f.setDataName(dataName);
    f.setPart(part);
    f.setFileIndex(fileIndex);

    return f.fileMiddle();
  }

  /**
   * Create the middle of a filename.
   * @param dataName data name
   * @param fileIndex file index
   * @param part file part
   * @return a String with the suffix of a file
   */
  public static String fileMiddle(final String dataName, final int fileIndex,
      final int part) {

    checkDataName(dataName);

    StringBuilder sb = new StringBuilder();

    // Set the name of the date
    sb.append(dataName);

    // Set the file index if needed
    if (fileIndex >= 0) {
      sb.append(SEPARATOR);
      sb.append(FILE_INDEX_PREFIX);
      sb.append(fileIndex);
    }

    if (part > -1) {
      sb.append(SEPARATOR);
      sb.append(PART_INDEX_PREFIX);
      sb.append(part);
    }

    return sb.toString();
  }

  //
  // Suffix creation
  //

  /**
   * Create the suffix of a filename.
   * @param port a workflow port
   * @return a string with the suffix that correspond to the filename
   */
  public static String fileSuffix(final WorkflowOutputPort port) {

    final FileNaming f = new FileNaming();
    f.set(port);

    return f.fileSuffix();
  }

  /**
   * Create the suffix of a file.
   * @param format format of the file
   * @param compression file compression
   * @return a String with the suffix of a file
   */
  public static String fileSuffix(final DataFormat format,
      final CompressionType compression) {

    final FileNaming f = new FileNaming();
    f.setFormat(format);
    f.setCompression(compression);

    return f.fileSuffix();
  }

  /**
   * Create the suffix of a file.
   * @param extension file extension
   * @param compression file compression
   * @return a String with the suffix of a file
   */
  public static String fileSuffix(final String extension,
      final String compression) {

    checkExtension(extension);
    checkCompression(compression);

    StringBuilder sb = new StringBuilder();

    // Set the extension
    sb.append(extension);

    // Set the compression extension
    if (compression != null) {
      sb.append(compression);
    }

    return sb.toString();
  }

  //
  // File name
  //

  /**
   * Create the filename from several parameters.
   * @param stepId the step id
   * @param portName the port name
   * @param format the format
   * @param dataName the data name
   * @param fileIndex the file index
   * @param part the part
   * @param compression the compression type
   * @return a string with the filename
   */
  public static String filename(final String stepId, final String portName,
      final DataFormat format, final String dataName, final int fileIndex,
      final int part, final CompressionType compression) {

    return filePrefix(stepId, portName, format)
        + fileMiddle(dataName, fileIndex, part)
        + fileSuffix(format, compression);
  }

  /**
   * Create the name of a data file of the workflow.
   * @param port port that generate the data
   * @param data data
   * @param fileIndex file index
   * @return a String with the name of the file
   */
  public static String filename(final WorkflowOutputPort port,
      final DataElement data, final int fileIndex) {

    FileNaming f = new FileNaming();
    f.set(port);
    f.set(data);
    f.setFileIndex(fileIndex);

    return f.filename();
  }

  //
  // File name parsing
  //

  /**
   * Create a FileNaming object from a File object.
   * @param file the file
   * @return a new FileNaming object
   */
  public static FileNaming parse(final File file) {

    checkNotNull(file, "file argument cannot be null");

    return parse(file.getName());
  }

  /**
   * Create a FileNaming object from a DataFile object.
   * @param file the file
   * @return a new FileNaming object
   */
  public static FileNaming parse(final DataFile file) {

    checkNotNull(file, "file argument cannot be null");

    return parse(file.getName());
  }

  /**
   * Create a FileNaming object from a filename.
   * @param filename the filename
   * @return a new FileNaming object
   */
  public static FileNaming parse(final String filename) {

    checkNotNull(filename, "filename argument cannot be null");

    final FileNaming result = new FileNaming();

    final String[] extensions = filename.split("\\.");

    if (extensions.length < 2 || extensions.length > 3) {
      throw new FileNamingParsingRuntimeException("Invalid filename: "
          + filename);
    }

    // Get format extension
    final String formatExtension = '.' + extensions[1];

    // Get compression
    if (extensions.length == 3) {
      result.setCompression(CompressionType
          .getCompressionTypeByExtension('.' + extensions[2]));
    }

    final String[] fields = extensions[0].split("_");

    if (fields.length < 4) {
      throw new FileNamingParsingRuntimeException("Invalid filename: "
          + filename);
    }

    result.setStepId(fields[0]);
    result.setPortName(fields[1]);

    final DataFormat format =
        DataFormatRegistry.getInstance().getDataFormatFromFilename(fields[2],
            formatExtension);

    if (format == null) {
      throw new FileNamingParsingRuntimeException("Invalid filename: "
          + filename);
    }

    result.setFormat(format);

    result.setDataName(fields[3]);

    for (int i = 4; i < fields.length; i++) {

      if (fields[i].startsWith(FILE_INDEX_PREFIX)) {

        if (result.getFileIndex() != -1) {
          throw new FileNamingParsingRuntimeException("Invalid filename: "
              + filename);
        }
        try {
          result.setFileIndex(Integer.parseInt(fields[i]
              .substring(FILE_INDEX_PREFIX.length())));
        } catch (NumberFormatException e) {
          throw new FileNamingParsingRuntimeException("Invalid filename: "
              + filename);
        }
      } else if (fields[i].startsWith(PART_INDEX_PREFIX)) {

        if (result.getPart() != -1) {
          throw new FileNamingParsingRuntimeException("Invalid filename: "
              + filename);
        }
        try {
          result.setPart(Integer.parseInt(fields[i].substring(PART_INDEX_PREFIX
              .length())));
        } catch (NumberFormatException e) {
          throw new FileNamingParsingRuntimeException("Invalid filename: "
              + filename);
        }
      }

    }

    if (result.getFormat().getMaxFilesCount() > 1
        && result.getFileIndex() == -1) {
      throw new FileNamingParsingRuntimeException("Invalid filename: "
          + filename);
    }

    return result;
  }

  //
  // File creation
  //

  /**
   * Create a DataFile object for a file of the workflow.
   * @param port port that generate the data
   * @param data data
   * @param fileIndex file index
   * @return a DataFile object
   */
  public static DataFile file(final WorkflowOutputPort port,
      final DataElement data, final int fileIndex) {

    return new DataFile(port.getStep().getStepWorkingDir(), filename(port,
        data, fileIndex));
  }

  //
  // Validation names methods
  //

  /**
   * Test if a step id is valid.
   * @param stepId the step id to check
   * @return true if the step id is valid
   */
  public static final boolean isStepIdValid(String stepId) {

    return isNameValid(stepId);
  }

  /**
   * Test if a format prefix id is valid.
   * @param formatPrefix the format prefix to check
   * @return true if the format prefix is valid
   */
  public static final boolean isFormatPrefixValid(final String formatPrefix) {

    return isNameValid(formatPrefix);
  }

  /**
   * Test if a port name is valid.
   * @param portName port name to check
   * @return true if the port name is valid
   */
  public static final boolean isPortNameValid(final String portName) {

    return isNameValid(portName);
  }

  /**
   * Test if a data name is valid.
   * @param dataName data name to check
   * @return true if the data name is valid
   */
  public static final boolean isDataNameValid(final String dataName) {

    return isNameValid(dataName);
  }

  /**
   * Test if name is valid.
   * @param name the name to test
   * @return true if the name is valid
   */
  private static final boolean isNameValid(final String name) {

    return !(name == null || name.isEmpty() || !ASCII_LETTER_OR_DIGIT
        .matchesAllOf(name));
  }

  /**
   * Test if a filename is valid.
   * @param file the file to test.
   * @return true if the filename is valid
   */
  public static final boolean isFilenameValid(final DataFile file) {

    checkNotNull(file, "file argument cannot be null");

    return isFilenameValid(file.getName());
  }

  /**
   * Test if a filename is valid.
   * @param file the file to test.
   * @return true if the filename is valid
   */
  public static final boolean isFilenameValid(final File file) {

    checkNotNull(file, "file argument cannot be null");

    return isFilenameValid(file.getName());
  }

  /**
   * Test if a filename is valid.
   * @param filename the file to test.
   * @return true if the filename is valid
   */
  public static final boolean isFilenameValid(final String filename) {

    try {
      FileNaming.parse(filename);
    } catch (FileNamingParsingRuntimeException e) {
      return false;
    }

    return true;
  }

  /**
   * Check a step Id.
   * @param stepId the step id to check
   */
  private static void checkStepId(final String stepId) {

    checkNotNull(stepId, "stepId argument cannot be null");
    checkArgument(!stepId.isEmpty(), "stepId is empty");
    checkArgument(isNameValid(stepId),
        "The step id of the file name can only contains letters or digit: "
            + stepId);
  }

  /**
   * Check a port name.
   * @param portName the port to check
   */
  private static void checkPortName(final String portName) {

    checkNotNull(portName, "portName argument cannot be null");
    checkArgument(!portName.isEmpty(), "portName argument is empty");
    checkArgument(isPortNameValid(portName),
        "The port name of the file name can only contains letters or digit: "
            + portName);
  }

  /**
   * Check a format prefix.
   * @param formatPrefix the format prefix
   */
  private static void checkFormatPrefix(final String formatPrefix) {

    checkNotNull(formatPrefix, "formatPrefix argument cannot be null");
    checkArgument(!formatPrefix.isEmpty(), "formatPrefix is empty");
    checkArgument(isFormatPrefixValid(formatPrefix),
        "The format prefix of the file name can only contains letters or digit: "
            + formatPrefix);
  }

  /**
   * Check a data name.
   * @param dataName the data name to check
   */
  private static void checkDataName(final String dataName) {

    checkNotNull(dataName, "dataName argument cannot be null");
    checkArgument(!dataName.isEmpty(), "dataName is empty");
    checkArgument(isFormatPrefixValid(dataName),
        "The data name of the file name can only contains letters or digit: "
            + dataName);
  }

  /**
   * Check an extension.
   * @param extension the extension to check
   */
  private static void checkExtension(final String extension) {

    checkNotNull(extension, "extension argument cannot be null");
    checkArgument(!extension.isEmpty(), "A part of the file name is empty");
    checkArgument(extension.charAt(0) == '.',
        "The extension do not starts with a dot: " + extension);
    checkArgument(ASCII_LETTER_OR_DIGIT.matchesAllOf(extension.substring(1)),
        "The extension of the file name can only contains letters or digit: "
            + extension);
  }

  /**
   * Check a compression name.
   * @param compression the compression name to check
   */
  private static void checkCompression(final String compression) {

    checkNotNull(compression, "compression argument cannot be null");

    // Empty compression string is allowed
    if (compression.isEmpty()) {
      return;
    }

    checkArgument(compression.charAt(0) == '.',
        "The compression do not starts with a dot: " + compression);
    checkArgument(ASCII_LETTER_OR_DIGIT.matchesAllOf(compression.substring(1)),
        "The compression of the file name can only contains letters or digit: "
            + compression);
  }

  //
  // Other static methods
  //

  /**
   * Convert a string to a valid name string that can be used for step id or
   * data name.
   * @param name the name to convert
   * @return a string with only the name characters argument that are allowed by
   *         the file naming convention
   */
  public static final String toValidName(final String name) {

    checkNotNull(name, "name argument cannot be null");

    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {

      final char c = name.charAt(i);
      if (ASCII_LETTER_OR_DIGIT.matches(c)) {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private FileNaming() {
  }

}
