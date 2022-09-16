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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.kenetre.util.StringUtils.toLetter;
import static fr.ens.biologie.genomique.kenetre.util.Utils.equal;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFileMetadata;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;

/**
 * This class define an output file of workflow set.
 * @author Laurent Jourdren
 * @since 2.0
 */
public final class StepOutputDataFile
    implements Comparable<StepOutputDataFile> {

  private final AbstractStep step;
  private final String portName;
  private final DataFormat format;
  private final Sample sample;
  private final DataFile file;
  private final int fileIndex;
  private final boolean mayNotExist;

  /**
   * Get the workflow step that produced the file.
   * @return the workflow step
   */
  public AbstractStep getStep() {

    return this.step;
  }

  /**
   * Get the port name that produced the file.
   * @return the port name
   */
  public String getPortName() {

    return this.portName;
  }

  /**
   * Get the format of the output
   * @return the DataFormat of the output
   */
  public DataFormat getFormat() {

    return this.format;
  }

  /**
   * Get the sample that produced the file.
   * @return the Sample
   */
  public Sample getSample() {

    return this.sample;
  }

  /**
   * Get file index.
   * @return the file index
   */
  public int getFileIndex() {

    return this.fileIndex;
  }

  public boolean isMayNotExist() {

    return this.mayNotExist;
  }

  /**
   * Get the file as a DataFile.
   * @return a DataFile
   */
  public DataFile getDataFile() {

    return this.file;
  }

  //
  // DataFile creation
  //

  /**
   * Create a new DataFile object from the step, format, sample and file index.
   * @param step step of the file
   * @param portName the port name
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multi-file data
   * @return a new DataFile object
   */
  private static DataFile newDataFile(final AbstractStep step,
      final String portName, final DataFormat format, final Sample sample,
      final int fileIndex) {

    requireNonNull(format, "Format argument cannot be null");
    requireNonNull(sample, "Sample argument cannot be null");

    switch (step.getType()) {

    case STANDARD_STEP:
    case GENERATOR_STEP:

      // if (!step.getStep().getOutputFormats().contains(portName))
      // throw new EoulsanRuntimeException("The "
      // + format.getName() + " format is not an output format of the step "
      // + step.getStep().getName());
      //
      // // Return a file created by a step
      // return newStandardDataFile(step.getContext(), step, portName, format,
      // sample, fileIndex, step.getOutputPorts().getPort(portName)
      // .getCompression());

    case DESIGN_STEP:

      // Get the values for the format and the sample in the design
      final List<String> designValues =
          getDesignValues(step.getWorkflow().getDesign(), format, sample);

      return newDesignDataFile(designValues, format, sample, fileIndex);

    default:
      return null;
    }

  }

  /**
   * Get the field values in the design for a format and a sample.
   * @param design design object
   * @param format format of the file
   * @param sample sample of the file
   * @return a list with the values in the design
   */
  private static List<String> getDesignValues(final Design design,
      final DataFormat format, final Sample sample) {

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    final String designMetadataKey =
        registry.getDesignMetadataKeyForDataFormat(design, format);

    if (designMetadataKey != null) {
      return design.getMetadata().getAsList(designMetadataKey);
    }

    final String sampleMetadataKey =
        registry.getSampleMetadataKeyForDataFormat(sample, format);

    if (sampleMetadataKey == null) {
      throw new EoulsanRuntimeException("The "
          + format.getName()
          + " format was not found in the design file for sample "
          + sample.getId() + " (" + sample.getName() + ")");
    }

    return sample.getMetadata().getAsList(sampleMetadataKey);
  }

  /**
   * Create a new DataFile object defined in the design.
   * @param fieldValues field values in the design
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multi-file data
   * @return a new DataFile object
   */
  private static DataFile newDesignDataFile(final List<String> fieldValues,
      final DataFormat format, final Sample sample, final int fileIndex) {

    if (fileIndex >= 0 && fileIndex > fieldValues.size()) {
      return null;
    }

    final DataFile file =
        new DataFile(fieldValues.get(fileIndex == -1 ? 0 : fileIndex));

    if (!isDesignDataFileValidFormat(file, format)) {
      throw new EoulsanRuntimeException("The file "
          + file + " in design file is not a " + format.getName()
          + format.getName() + " format for " + sample.getId() + " ("
          + sample.getName() + ")");
    }

    return file;
  }

  /**
   * Create a DataFile object that correspond to a standard Eoulsan output file.
   * @param step step that generated the file
   * @param portName the port that generated the file
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multi-file data
   * @return a new DataFile object
   */
  private static DataFile newStandardDataFile(final AbstractStep step,
      final String portName, final DataFormat format, final Sample sample,
      final int fileIndex, final CompressionType compression) {

    final StringBuilder sb = new StringBuilder();

    // Set base path if exists
    final String basePath = step.getStepOutputDirectory().toString();
    if (basePath != null) {
      sb.append(basePath);
      sb.append('/');
    }

    sb.append(newStandardFilename(step, portName, format, sample, fileIndex,
        compression));

    return new DataFile(sb.toString());
  }

  /**
   * Check if a DataFile from the design has a the good format.
   * @param file the DataFile to test
   * @param df the DataFormat
   * @return true if a DataFile from the design has a the good format
   */
  private static boolean isDesignDataFileValidFormat(final DataFile file,
      final DataFormat df) {

    if (file == null || df == null) {
      return false;
    }

    DataFileMetadata md;

    try {
      md = file.getMetaData();
    } catch (IOException e) {
      getLogger().warning("Error while getting metadata for file "
          + file + ": " + e.getMessage());
      md = null;
    }

    if (md != null && df.equals(md.getDataFormat())) {
      return true;
    }

    final DataFormatRegistry dfr = DataFormatRegistry.getInstance();

    for (DataFormat sourceDf : dfr
        .getDataFormatsFromExtension(file.getExtension())) {

      if (sourceDf.equals(df)) {
        return true;
      }
    }

    return false;
  }

  //
  // Object methods overrides
  //

  @Override
  public int compareTo(final StepOutputDataFile o) {

    requireNonNull(o, "o is null");

    return this.file.compareTo(o.file);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this) {
      return true;
    }

    if (o == null || !(o instanceof StepOutputDataFile)) {
      return false;
    }

    final StepOutputDataFile that = (StepOutputDataFile) o;

    return equal(this.file, that.file);
  }

  @Override
  public int hashCode() {

    return this.file.hashCode();
  }

  //
  // Static method
  //

  /**
   * Create a standard filename.
   * @param step the step that generated the file
   * @param portName the port name that generated the file
   * @param format the format of the file
   * @param sample the sample of the file
   * @param fileIndex file index of the file for multi-file data
   * @param compression the compression of the file
   * @return a file name as a String
   */
  public static String newStandardFilename(final Step step,
      final String portName, final DataFormat format, final Sample sample,
      final int fileIndex, final CompressionType compression) {

    requireNonNull(step, "step argument cannot be null");
    requireNonNull(portName, "portName argument cannot be null");
    requireNonNull(format, "format argument cannot be null");
    requireNonNull(sample, "sample argument cannot be null");
    requireNonNull(compression, "compression argument cannot be null");

    final StringBuilder sb = new StringBuilder();

    // Set the name of the step that generated the file
    sb.append(step.getId());
    sb.append('_');

    // Set the port of the step that generated the file
    sb.append(format.getName());
    sb.append('_');

    // Set the name of the format
    sb.append(format.getName());
    sb.append('_');

    // Set the id of the sample
    if (format.isOneFilePerAnalysis()) {
      sb.append('0');
    } else {
      sb.append(sample.getId());
    }

    // Set the file index if needed
    if (fileIndex >= 0) {
      sb.append(toLetter(fileIndex));
    }

    // Set the extension
    sb.append(format.getDefaultExtension());

    // Set the compression extension
    sb.append(compression.getExtension());

    return sb.toString();
  }

  /**
   * Get the count of files that exists for a step, a format and sample (case of
   * multi-files data).
   * @param outputPort output port that create the file
   * @param sample sample of the file
   * @param existingFiles true if existence of file must be tested. If false the
   *          return value will be the maximum number files
   * @return the number of files
   */
  public static int dataFileCount(final StepOutputPort outputPort,
      final Sample sample, final boolean existingFiles) {

    requireNonNull(outputPort, "outputPort cannot be null");
    requireNonNull(sample, "Sample argument cannot be null");

    final AbstractStep step = outputPort.getStep();
    final DataFormat format = outputPort.getFormat();
    final CompressionType compression = outputPort.getCompression();

    if (format.getMaxFilesCount() < 2) {
      throw new EoulsanRuntimeException(
          "Only multi-files DataFormat are handled by this method.");
    }

    switch (step.getType()) {

    case STANDARD_STEP:

      if (!existingFiles) {
        return format.getMaxFilesCount();
      }

      int count = 0;
      boolean found;

      do {

        final DataFile file = newStandardDataFile(step, outputPort.getName(),
            format, sample, count, compression);

        found = file.exists();
        if (found) {
          count++;
        }
      } while (found);

      return count;

    case DESIGN_STEP:

      return getDesignValues(step.getWorkflow().getDesign(), format, sample)
          .size();

    default:
      return 0;
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param outputPort output port that create the file
   * @param sample sample of the file
   */
  public StepOutputDataFile(final StepOutputPort outputPort,
      final Sample sample) {

    this(outputPort, sample, -1);
  }

  /**
   * Constructor.
   * @param outputPort output port that create the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multi-file data
   */
  public StepOutputDataFile(final StepOutputPort outputPort,
      final Sample sample, final int fileIndex) {

    requireNonNull(outputPort, "outputPort cannot be null");
    requireNonNull(sample, "sample cannot be null");

    final DataFormat format = outputPort.getFormat();

    if (format.getMaxFilesCount() == 1 && fileIndex != -1) {
      throw new IllegalArgumentException(
          "file index must be used for multi files formats");
    }

    if (format.getMaxFilesCount() > 1 && fileIndex < 0) {
      throw new IllegalArgumentException("file index ("
          + fileIndex
          + ") must be greater or equals to 0 for multi files formats ("
          + format.getName() + ")");
    }

    this.step = outputPort.getStep();
    this.portName = outputPort.getName();
    this.format = format;
    this.sample = format.isOneFilePerAnalysis() ? null : sample;
    this.file =
        newDataFile(this.step, this.portName, format, sample, fileIndex);
    this.fileIndex = fileIndex;
    this.mayNotExist = fileIndex > 0;
  }

}
