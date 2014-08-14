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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class contains methods to create workflow data file names.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileNaming {

  /**
   * Create the prefix of a file.
   * @param port output port that generate the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final WorkflowOutputPort port) {

    checkNotNull(port, "port argument cannot be null");

    return filePrefix(port.getStep().getId(), port.getName(), port.getFormat()
        .getName());
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

    return filePrefix(stepId, portName, format.getName());
  }

  /**
   * Create the prefix of a file.
   * @param stepId step id
   * @param portName port name
   * @param formatName format name of the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final String stepId, final String portName,
      final String formatName) {

    checkNotNull(stepId, "stepId argument cannot be null");
    checkNotNull(portName, "portName argument cannot be null");
    checkNotNull(formatName, "formatName argument cannot be null");

    final StringBuilder sb = new StringBuilder();

    // Set the name of the step that generated the file
    sb.append(stepId);
    sb.append('_');

    // Set the port of the step that generated the file
    sb.append(portName);
    sb.append('_');

    // Set the name of the format
    sb.append(formatName);
    sb.append('_');

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

    checkNotNull(dataName, "dataName argument cannot be null");
    checkNotNull(extension, "extension argument cannot be null");

    StringBuilder sb = new StringBuilder();

    // Set the name of the date
    sb.append(dataName);

    // Set the file index if needed
    if (fileIndex >= 0) {
      sb.append("_file");
      sb.append(fileIndex);
    }

    if (part > -1) {
      sb.append("_part");
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
  // Constructor
  //

  /**
   * Private constructor.
   */
  private FileNaming() {
  }

}
