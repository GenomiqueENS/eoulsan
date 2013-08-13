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

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toLetter;
import static fr.ens.transcriptome.eoulsan.util.Utils.equal;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFileMetadata;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define an output file of workflow set.
 * @author Laurent Jourdren
 * @since 1.3
 */
public final class WorkflowStepOutputDataFile implements
    Comparable<WorkflowStepOutputDataFile> {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private final AbstractWorkflowStep step;
  private final DataFormat format;
  private final Sample sample;
  private final DataFile file;
  private final int fileIndex;
  private final boolean mayNotExist;

  /**
   * Get the workflow step that produced the file.
   * @return the workflow step
   */
  public AbstractWorkflowStep getStep() {

    return this.step;
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
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multifile data
   * @return a new DataFile object
   */
  private static final DataFile newDataFile(final AbstractWorkflowStep step,
      final DataFormat format, final Sample sample, final int fileIndex) {

    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    switch (step.getType()) {

    case STANDARD_STEP:
    case GENERATOR_STEP:

      if (!Arrays.asList(step.getStep().getOutputFormats()).contains(format))
        throw new EoulsanRuntimeException("The "
            + format.getFormatName()
            + " format is not an output format of the step "
            + step.getStep().getName());

      // Return a file created by a step
      return newStandardDataFile(step.getContext(), step, format, sample,
          fileIndex);

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
  private static final List<String> getDesignValues(final Design design,
      final DataFormat format, final Sample sample) {

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();
    final String designFieldName =
        registry.getDesignFieldnameForDataFormat(design, format);

    if (designFieldName == null)
      throw new EoulsanRuntimeException("The "
          + format.getFormatName()
          + " format was not found in the design file for sample "
          + sample.getId() + " (" + sample.getName() + ")");

    return sample.getMetadata().getFieldAsList(designFieldName);
  }

  /**
   * Create a new DataFile object defined in the design.
   * @param fieldValues field values in the design
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multifile data
   * @return a new DataFile object
   */
  private static final DataFile newDesignDataFile(
      final List<String> fieldValues, final DataFormat format,
      final Sample sample, final int fileIndex) {

    if (fileIndex >= 0 && fileIndex > fieldValues.size())
      return null;

    final DataFile file =
        new DataFile(fieldValues.get(fileIndex == -1 ? 0 : fileIndex));

    if (!isDesignDataFileValidFormat(file, format))
      throw new EoulsanRuntimeException("The file "
          + file + " in design file is not a " + format.getFormatName()
          + format.getFormatName() + " format for " + sample.getId() + " ("
          + sample.getName() + ")");

    return file;
  }

  /**
   * Create a DataFile object that correspond to a standard Eoulsan output file.
   * @param context context object
   * @param step step that generated the file
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multifile data
   * @return a new DataFile object
   */
  private static final DataFile newStandardDataFile(final StepContext context,
      final WorkflowStep step, final DataFormat format, final Sample sample,
      final int fileIndex) {

    final StringBuilder sb = new StringBuilder();

    // Set base path if exists
    final String basePath = context.getStepWorkingPathname();
    if (basePath != null) {
      sb.append(basePath);
      sb.append('/');
    }

    sb.append(newStandardFilename(step, format, sample, fileIndex));

    return new DataFile(sb.toString());
  }

  /**
   * Check if a DataFile from the design has a the good format.
   * @param file the DataFile to test
   * @param df the DataFormat
   * @return true if a DataFile from the design has a the good format
   */
  private static final boolean isDesignDataFileValidFormat(final DataFile file,
      final DataFormat df) {

    if (file == null || df == null)
      return false;

    DataFileMetadata md = null;

    try {
      md = file.getMetaData();
    } catch (IOException e) {
      LOGGER.warning("Error while getting metadata for file "
          + file + ": " + e.getMessage());
      md = null;
    }

    if (md != null && df.equals(md.getDataFormat()))
      return true;

    final DataFormatRegistry dfr = DataFormatRegistry.getInstance();
    final DataFormat sourceDf =
        dfr.getDataFormatFromExtension(file.getExtension());

    if (sourceDf != null && sourceDf.equals(df))
      return true;

    return false;
  }

  //
  // Object methods overrides
  //

  @Override
  public int compareTo(final WorkflowStepOutputDataFile o) {

    return this.file.compareTo(o.file);
  }

  @Override
  public boolean equals(final Object o) {

    if (o == this)
      return true;

    if (o == null || !(o instanceof WorkflowStepOutputDataFile))
      return false;

    final WorkflowStepOutputDataFile that = (WorkflowStepOutputDataFile) o;

    return equal(this.file, that.file);
  }

  @Override
  public int hashCode() {

    return this.file.hashCode();
  }

  //
  // Static method
  //

  public static final String newStandardFilename(final WorkflowStep step,
      final DataFormat format, final Sample sample, final int fileIndex) {

    Preconditions.checkNotNull(step, "Step argument cannot be null");
    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    final StringBuilder sb = new StringBuilder();

    // Set the name of the step that generated the file
    sb.append(step.getId());
    sb.append('_');

    // Set the name of the format
    sb.append(format.getFormatName());
    sb.append('_');

    // Set the id of the sample
    if (format.isOneFilePerAnalysis())
      sb.append('1');
    else
      sb.append(sample.getId());

    // Set the file index if needed
    if (fileIndex >= 0)
      sb.append(toLetter(fileIndex));

    // Set the extension
    sb.append(format.getDefaultExtention());

    return sb.toString();
  }

  /**
   * Get the count of files that exists for a step, a format and sample (case of
   * multifiles data).
   * @param step step that generated the file
   * @param format format of the file
   * @param sample sample of the file
   * @param existingFiles true if existence of file must be tested. If false the
   *          return value will be the maximum number files
   * @return the number of files
   */
  public static final int dataFileCount(final WorkflowStep step,
      final DataFormat format, final Sample sample, final boolean existingFiles) {

    Preconditions.checkNotNull(format, "Format argument cannot be null");
    Preconditions.checkNotNull(sample, "Sample argument cannot be null");

    if (format.getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    switch (step.getType()) {

    case STANDARD_STEP:

      if (!existingFiles)
        return format.getMaxFilesCount();

      int count = 0;
      boolean found = false;

      do {

        final DataFile file =
            newStandardDataFile(step.getContext(), step, format, sample, count);

        found = file.exists();
        if (found)
          count++;
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
   * @param step step that create the file
   * @param format format of the file
   * @param sample sample of the file
   */
  public WorkflowStepOutputDataFile(final AbstractWorkflowStep step,
      final DataFormat format, final Sample sample) {

    this(step, format, sample, -1);
  }

  /**
   * Constructor.
   * @param step step that create the file
   * @param format format of the file
   * @param sample sample of the file
   * @param fileIndex file index of the file for multifile data
   */
  public WorkflowStepOutputDataFile(final AbstractWorkflowStep step,
      final DataFormat format, final Sample sample, final int fileIndex) {

    Preconditions.checkNotNull(step, "step cannot be null");
    Preconditions.checkNotNull(format, "format cannot be null");
    Preconditions.checkNotNull(sample, "sample cannot be null");
    Preconditions.checkArgument(fileIndex >= -1,
        "fileIndex cannot be lower than -1");

    this.step = step;
    this.format = format;
    this.sample = format.isOneFilePerAnalysis() ? null : sample;
    this.file = newDataFile(step, format, sample, fileIndex);
    this.fileIndex = fileIndex;
    this.mayNotExist = fileIndex > 0;
  }

}