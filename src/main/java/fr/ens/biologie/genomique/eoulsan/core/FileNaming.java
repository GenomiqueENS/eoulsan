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

package fr.ens.biologie.genomique.eoulsan.core;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.ens.biologie.genomique.eoulsan.core.Naming.ASCII_LETTER_OR_DIGIT;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toLetter;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Objects;

import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;

/**
 * This class contains methods to create workflow data file names.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileNaming {

  private static final char SEPARATOR = '_';
  private static final String FILE_INDEX_PREFIX = "file";
  private static final String PART_INDEX_PREFIX = "part";

  private String stepId;
  private String portName;
  private String dataName;
  private int sampleNumber = -1;
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
    return this.stepId;
  }

  /**
   * Get the port name.
   * @return the port name
   */
  public String getPortName() {
    return this.portName;
  }

  /**
   * Get the data name.
   * @return the data name
   */
  public String getDataName() {
    return this.dataName;
  }

  /**
   * Get the sample number related to the data. This value is only use when
   * generate compatible filenames.
   * @return the number of the sample related to the file or -1 if not known
   */
  public int getSampleNumber() {
    return this.sampleNumber;
  }

  /**
   * Get the format.
   * @return the format
   */
  public DataFormat getFormat() {
    return this.format;
  }

  /**
   * Get the file index.
   * @return the file index
   */
  public int getFileIndex() {
    return this.fileIndex;
  }

  /**
   * Get the file part.
   * @return the file part
   */
  public int getPart() {
    return this.part;
  }

  /**
   * Get the compression.
   * @return the compression
   */
  public CompressionType getCompression() {
    return this.compression;
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
   * Set the sample number related to the data. This value is only use when
   * generate compatible filenames.
   * @param sampleNumber the number of the sample related to the file or -1 if
   *          not known
   */
  public void setSampleNumber(final int sampleNumber) {

    this.sampleNumber = sampleNumber;
  }

  /**
   * Set the format.
   * @param format the format
   */
  public void setFormat(final DataFormat format) {

    requireNonNull(format, "format argument cannot be null");
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

    requireNonNull(compression, "compression argument cannot be null");
    this.compression = compression;
  }

  /**
   * Set several field of the object from a Data object.
   * @param data the data object
   */
  protected void set(final Data data) {

    requireNonNull(data, "port argument cannot be null");

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

    requireNonNull(this.stepId, "stepId has not been set");
    requireNonNull(this.portName, "portName has not been set");
    requireNonNull(this.format, "format has not been set");

    return filePrefix(this.stepId, this.portName, this.format.getPrefix());
  }

  /**
   * Return the file suffix.
   * @return q string with the file suffix
   */
  public String fileSuffix() {

    requireNonNull(this.format, "format has not been set");
    requireNonNull(this.compression, "compression has not been set");

    return fileSuffix(this.format.getDefaultExtension(),
        this.compression.getExtension());
  }

  /**
   * Return the middle string of the filename.
   * @return a string with the middle string of the filename
   */
  public String fileMiddle() {

    requireNonNull(this.dataName, "datName has not been set");

    checkFormatAndFileIndex();

    return fileMiddle(this.dataName, this.fileIndex, this.part);
  }

  /**
   * Return the filename.
   * @return a string with the filename
   */
  public String filename() {

    requireNonNull(this.stepId, "stepId has not been set");
    requireNonNull(this.portName, "portName has not been set");
    requireNonNull(this.format, "format has not been set");
    requireNonNull(this.dataName, "datName has not been set");
    requireNonNull(this.compression, "compression has not been set");

    checkFormatAndFileIndex();

    return filename(this.stepId, this.portName, this.format, this.dataName,
        this.fileIndex, this.part, this.compression);
  }

  /**
   * Get a glob for the filename.
   * @return a glob in a string
   */
  public String glob() {

    return filePrefix() + '*' + fileSuffix();
  }

  /**
   * Return the filename using Eoulsan 1.x naming.
   * @return a string with the filename using Eoulsan 1.x naming
   */
  public String compatibilityFilename() {

    requireNonNull(this.stepId, "stepId has not been set");
    requireNonNull(this.portName, "portName has not been set");
    requireNonNull(this.format, "format has not been set");
    requireNonNull(this.dataName, "datName has not been set");
    requireNonNull(this.compression, "compression has not been set");

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
      sb.append(this.sampleNumber);
    }

    // Set the file index if needed
    if (this.fileIndex >= 0) {

      sb.append(toLetter(this.fileIndex));
    }

    // Set the extension
    sb.append(this.format.getDefaultExtension());

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
      checkArgument(this.fileIndex < this.format.getMaxFilesCount(),
          "Invalid fileIndex argument for format "
              + this.format.getName() + ": " + this.fileIndex);
    }

  }

  //
  // Static methods
  //

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

    return stepId + SEPARATOR + portName + SEPARATOR + formatPrefix + SEPARATOR;
  }

  //
  // Middle creation
  //

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

  //
  // File name parsing
  //

  /**
   * Create a FileNaming object from a File object.
   * @param file the file
   * @return a new FileNaming object
   */
  public static FileNaming parse(final File file) {

    requireNonNull(file, "file argument cannot be null");

    return parse(file.getName());
  }

  /**
   * Create a FileNaming object from a DataFile object.
   * @param file the file
   * @return a new FileNaming object
   */
  public static FileNaming parse(final DataFile file) {

    requireNonNull(file, "file argument cannot be null");

    return parse(file.getName());
  }

  /**
   * Create a FileNaming object from a filename.
   * @param filename the filename
   * @return a new FileNaming object
   */
  public static FileNaming parse(final String filename) {

    requireNonNull(filename, "filename argument cannot be null");

    final FileNaming result = new FileNaming();

    final String[] extensions = filename.split("\\.");

    if (extensions.length < 2 || extensions.length > 3) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }

    // Get format extension
    final String formatExtension = '.' + extensions[1];

    // Get compression
    if (extensions.length == 3) {
      result.setCompression(
          CompressionType.getCompressionTypeByExtension('.' + extensions[2]));
    }

    final String[] fields = extensions[0].split("_");

    if (fields.length < 4) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }

    if (fields[0].isEmpty() || !isStepIdValid(fields[0])) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }
    result.setStepId(fields[0]);

    if (fields[1].isEmpty() || !isPortNameValid(fields[1])) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }
    result.setPortName(fields[1]);

    final DataFormat format = DataFormatRegistry.getInstance()
        .getDataFormatFromFilename(fields[2], formatExtension);

    if (format == null) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }
    result.setFormat(format);

    if (fields[3].isEmpty() || !isDataNameValid(fields[3])) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }
    result.setDataName(fields[3]);

    for (int i = 4; i < fields.length; i++) {

      if (fields[i].startsWith(FILE_INDEX_PREFIX)) {

        if (result.getFileIndex() != -1) {
          throw new FileNamingParsingRuntimeException(
              "Invalid filename: " + filename);
        }
        try {
          result.setFileIndex(Integer
              .parseInt(fields[i].substring(FILE_INDEX_PREFIX.length())));
        } catch (NumberFormatException e) {
          throw new FileNamingParsingRuntimeException(
              "Invalid filename: " + filename);
        }
      } else if (fields[i].startsWith(PART_INDEX_PREFIX)) {

        if (result.getPart() != -1) {
          throw new FileNamingParsingRuntimeException(
              "Invalid filename: " + filename);
        }
        try {
          result.setPart(Integer
              .parseInt(fields[i].substring(PART_INDEX_PREFIX.length())));
        } catch (NumberFormatException e) {
          throw new FileNamingParsingRuntimeException(
              "Invalid filename: " + filename);
        }
      }

    }

    if (result.getFormat().getMaxFilesCount() > 1
        && result.getFileIndex() == -1) {
      throw new FileNamingParsingRuntimeException(
          "Invalid filename: " + filename);
    }

    return result;
  }

  //
  // Validation names methods
  //

  /**
   * Test if a step id is valid.
   * @param stepId the step id to check
   * @return true if the step id is valid
   */
  public static boolean isStepIdValid(final String stepId) {

    return isNameValid(stepId);
  }

  /**
   * Test if a format prefix id is valid.
   * @param formatPrefix the format prefix to check
   * @return true if the format prefix is valid
   */
  public static boolean isFormatPrefixValid(final String formatPrefix) {

    return isNameValid(formatPrefix);
  }

  /**
   * Test if a port name is valid.
   * @param portName port name to check
   * @return true if the port name is valid
   */
  public static boolean isPortNameValid(final String portName) {

    return isNameValid(portName);
  }

  /**
   * Test if a data name is valid.
   * @param dataName data name to check
   * @return true if the data name is valid
   */
  public static boolean isDataNameValid(final String dataName) {

    return isNameValid(dataName);
  }

  /**
   * Test if name is valid.
   * @param name the name to test
   * @return true if the name is valid
   */
  private static boolean isNameValid(final String name) {

    return !(name == null
        || name.isEmpty() || !ASCII_LETTER_OR_DIGIT.matchesAllOf(name));
  }

  /**
   * Test if a filename is valid.
   * @param file the file to test.
   * @return true if the filename is valid
   */
  public static boolean isFilenameValid(final DataFile file) {

    requireNonNull(file, "file argument cannot be null");

    return isFilenameValid(file.getName());
  }

  /**
   * Test if a filename is valid.
   * @param file the file to test.
   * @return true if the filename is valid
   */
  public static boolean isFilenameValid(final File file) {

    requireNonNull(file, "file argument cannot be null");

    return isFilenameValid(file.getName());
  }

  /**
   * Test if a filename is valid.
   * @param filename the file to test.
   * @return true if the filename is valid
   */
  public static boolean isFilenameValid(final String filename) {

    try {
      FileNaming.parse(filename);
    } catch (FileNamingParsingRuntimeException e) {
      return false;
    }

    return true;
  }

  /**
   * Test if two files are related to the same data.
   * @param file1 the first file
   * @param file2 the second file
   * @return true if the two files are related to the same data
   */
  public static boolean dataEquals(final File file1, final File file2) {

    requireNonNull(file1, "file1 argument cannot be null");
    requireNonNull(file2, "file2 argument cannot be null");

    return dataEquals(file1.getName(), file2.getName());
  }

  /**
   * Test if two files are related to the same data.
   * @param file1 the first file
   * @param file2 the second file
   * @return true if the two files are related to the same data
   */
  public static boolean dataEquals(final DataFile file1, final DataFile file2) {

    requireNonNull(file1, "file1 argument cannot be null");
    requireNonNull(file2, "file2 argument cannot be null");

    return dataEquals(file1.getName(), file2.getName());
  }

  /**
   * Test if two filenames are related to the same data.
   * @param filename1 the first filename
   * @param filename2 the second filename
   * @return true if the two files are related to the same data
   */
  public static boolean dataEquals(final String filename1,
      final String filename2) {

    requireNonNull(filename1, "filename1 argument cannot be null");
    requireNonNull(filename2, "filename2 argument cannot be null");

    final FileNaming fn1;
    final FileNaming fn2;

    try {
      fn1 = parse(filename1);
      fn2 = parse(filename2);
    } catch (FileNamingParsingRuntimeException e) {
      return false;
    }

    return Objects.equals(fn1.stepId, fn2.stepId)
        && Objects.equals(fn1.portName, fn2.portName)
        && Objects.equals(fn1.format, fn2.format)
        && Objects.equals(fn1.dataName, fn2.dataName)
        && Objects.equals(fn1.part, fn2.part);
  }

  /**
   * Check a step Id.
   * @param stepId the step id to check
   */
  private static void checkStepId(final String stepId) {

    requireNonNull(stepId, "stepId argument cannot be null");
    checkArgument(!stepId.isEmpty(), "stepId is empty");
    checkArgument(isStepIdValid(stepId),
        "The step id of the file name can only contains letters or digit: "
            + stepId);
  }

  /**
   * Check a port name.
   * @param portName the port to check
   */
  private static void checkPortName(final String portName) {

    requireNonNull(portName, "portName argument cannot be null");
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

    requireNonNull(formatPrefix, "formatPrefix argument cannot be null");
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

    requireNonNull(dataName, "dataName argument cannot be null");
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

    requireNonNull(extension, "extension argument cannot be null");
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

    requireNonNull(compression, "compression argument cannot be null");

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
  // Constructor
  //

  /**
   * Private constructor.
   */
  protected FileNaming() {
  }

}
