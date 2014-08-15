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

import com.google.common.base.CharMatcher;

import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class contains methods to create workflow data file names.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileNaming {

  private static final char SEPARATOR = '_';
  private static final CharMatcher ASCII_LETTER_OR_DIGIT = inRange('a', 'z')
      .or(inRange('A', 'Z')).or(inRange('0', '9'));

  /**
   * Create the prefix of a file.
   * @param port output port that generate the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final WorkflowOutputPort port) {

    checkNotNull(port, "port argument cannot be null");

    return filePrefix(port.getStep().getId(), port.getName(), port.getFormat()
        .getPrefix());
  }

  /**
   * Create the prefix of a file.
   * @param stepId step id
   * @param portName port name
   * @param format format of the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final String stepId, final String portName,
      final DataFormat format) {

    checkNotNull(format, "format argument cannot be null");

    return filePrefix(stepId, portName, format.getPrefix());
  }

  /**
   * Create the prefix of a file.
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

  //
  // Suffix creation
  //

  /**
   * Create the suffix of a file.
   * @param port output port that generate the file
   * @param data the data
   * @param fileIndex file index
   * @return a String with the suffix of a file
   */
  public static String fileSuffix(final WorkflowOutputPort port,
      final DataElement data, final int fileIndex) {

    checkNotNull(data, "data argument cannot be null");

    return fileSuffix(port, data.getName(), fileIndex, data.getPart());
  }

  /**
   * Create the suffix of a file.
   * @param port output port that generate the file
   * @param fileIndex file index
   * @param part file part
   * @return a String with the suffix of a file
   */
  public static String fileSuffix(final WorkflowOutputPort port,
      final String dataName, final int fileIndex, final int part) {

    checkNotNull(port, "port argument cannot be null");

    return fileSuffix(dataName, fileIndex, part, port.getFormat(),
        port.getCompression());

  }

  /**
   * Create the suffix of a file.
   * @param dataName data name
   * @param fileIndex file index
   * @param part file part
   * @param format format of the file
   * @param compression file compression
   * @return a String with the suffix of a file
   */
  public static String fileSuffix(final String dataName, final int fileIndex,
      final int part, final DataFormat format, final CompressionType compression) {

    checkNotNull(format, "format argument cannot be null");
    checkNotNull(compression, "compression argument cannot be null");

    if (format.getMaxFilesCount() == 1) {
      checkArgument(fileIndex == -1, "Invalid fileIndex argument for format "
          + format.getName() + ": " + fileIndex);
    } else {
      checkArgument(fileIndex < format.getMaxFilesCount(),
          "Invalid fileIndex argument for format "
              + format.getName() + ": " + fileIndex);
    }

    return fileSuffix(dataName, fileIndex, part, format.getDefaultExtention(),
        compression.getExtension());
  }

  /**
   * Create the suffix of a file.
   * @param dataName data name
   * @param fileIndex file index
   * @param part file part
   * @param extension file extension
   * @param compression file compression
   * @return a String with the suffix of a file
   */
  public static String fileSuffix(final String dataName, final int fileIndex,
      final int part, final String extension, final String compression) {

    checkDataName(dataName);
    checkExtension(extension);
    checkCompression(compression);

    StringBuilder sb = new StringBuilder();

    // Set the name of the date
    sb.append(dataName);

    // Set the file index if needed
    if (fileIndex >= 0) {
      sb.append(SEPARATOR);
      sb.append("file");
      sb.append(fileIndex);
    }

    if (part > -1) {
      sb.append(SEPARATOR);
      sb.append("part");
      sb.append(part);
    }

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
   * Create the name of a data file of the workflow.
   * @param port port that generate the data
   * @param data data
   * @param fileIndex file index
   * @return a String with the name of the file
   */
  public static String name(final WorkflowOutputPort port,
      final DataElement data, final int fileIndex) {

    return filePrefix(port) + fileSuffix(port, data, fileIndex);
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

    return new DataFile(port.getStep().getStepWorkingDir(), name(port, data,
        fileIndex));
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
  // Constructor
  //

  /**
   * Private constructor.
   */
  private FileNaming() {
  }

}
