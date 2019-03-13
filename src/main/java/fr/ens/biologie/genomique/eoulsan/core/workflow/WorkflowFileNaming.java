package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.core.FileNaming;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;

/**
 * This class contains methods to create workflow data file names.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowFileNaming extends FileNaming {

  //
  // Setters
  //

  /**
   * Set several field of the object from a workflow output port.
   * @param port the workflow output port
   */
  private void set(final StepOutputPort port) {

    requireNonNull(port, "port argument cannot be null");

    setStepId(port.getStep().getId());
    setPortName(port.getName());
    setFormat(port.getFormat());
    setCompression(port.getCompression());
  }

  //
  // Static methods
  //

  /**
   * Create the prefix of a filename.
   * @param port output port that generate the file
   * @return a String with the prefix of the file
   */
  public static String filePrefix(final StepOutputPort port) {

    final WorkflowFileNaming f = new WorkflowFileNaming();
    f.set(port);

    return f.filePrefix();
  }

  /**
   * Create the glob for the port.
   * @return a glob in a string
   */
  public static String glob(final StepOutputPort port) {

    final WorkflowFileNaming f = new WorkflowFileNaming();
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
  public static String fileMiddle(final StepOutputPort port,
      final DataElement data, final int fileIndex) {

    final WorkflowFileNaming f = new WorkflowFileNaming();
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
  public static String fileMiddle(final StepOutputPort port,
      final String dataName, final int fileIndex, final int part) {

    final WorkflowFileNaming f = new WorkflowFileNaming();
    f.set(port);
    f.setDataName(dataName);
    f.setPart(part);
    f.setFileIndex(fileIndex);

    return f.fileMiddle();
  }

  //
  // Suffix creation
  //

  /**
   * Create the suffix of a filename.
   * @param port a workflow port
   * @return a string with the suffix that correspond to the filename
   */
  public static String fileSuffix(final StepOutputPort port) {

    final WorkflowFileNaming f = new WorkflowFileNaming();
    f.set(port);

    return f.fileSuffix();
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
  public static String filename(final StepOutputPort port,
      final DataElement data, final int fileIndex) {

    final WorkflowFileNaming f = new WorkflowFileNaming();
    f.set(port);
    f.set(data);
    f.setFileIndex(fileIndex);

    return f.filename();
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
  public static DataFile file(final StepOutputPort port, final DataElement data,
      final int fileIndex) {

    return new DataFile(port.getStep().getStepOutputDirectory(),
        filename(port, data, fileIndex));
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private WorkflowFileNaming() {

  }

}
