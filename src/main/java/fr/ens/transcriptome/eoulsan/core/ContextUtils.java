package fr.ens.transcriptome.eoulsan.core;

import static fr.ens.transcriptome.eoulsan.util.StringUtils.toLetter;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class contains utility methods for Context class.
 * @author Laurent Jourdren
 */
public final class ContextUtils {

  private static final boolean NEW_STYLE = false;

  /**
   * Get the pathname for a new file from its DataFormat and a sample object.
   * This method works only for a non multifile DataFormat.
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throw EoulsanRuntimeException if the DataFormat is multifile
   */
  public static String getNewDataFilename(final DataFormat df,
      final Sample sample) {

    if (df == null || sample == null)
      return null;

    if (df.getMaxFilesCount() != 1)
      throw new EoulsanRuntimeException(
          "Multifiles DataFormat are not handled by this method.");

    return getNewDataFilenameInternal(df, sample, -1);
  }

  /**
   * Get the pathname for a new file from its DataFormat and a sample object.
   * This method works only for a multifile DataFormat.
   * @param df the DataFormat of the source
   * @param sample the sample for the source
   * @param fileIndex file index for multifile data
   * @return a String with the pathname
   * @throw EoulsanRuntimeException if the DataFormat is not multifile
   */
  public static String getNewDataFilename(final DataFormat df,
      final Sample sample, final int fileIndex) {

    if (df == null || sample == null || fileIndex < 0)
      return null;

    if (df.getMaxFilesCount() < 2)
      throw new EoulsanRuntimeException(
          "Only multifiles DataFormat are handled by this method.");

    if (fileIndex > df.getMaxFilesCount())
      throw new EoulsanRuntimeException(
          "The file index is greater than the maximal number of file for this format.");

    return getNewDataFilenameInternal(df, sample, fileIndex);
  }

  private static String getNewDataFilenameInternal(final DataFormat df,
      final Sample sample, final int fileIndex) {

    final StringBuilder sb = new StringBuilder();

    // Set the prefix of the file
    sb.append(df.getType().getPrefix());

    // Set the id of the sample
    if (df.getType().isOneFilePerAnalysis()) {
      sb.append('1');
      if (NEW_STYLE)
        sb.append("-common");
    } else {
      sb.append(sample.getId());
      if (NEW_STYLE) {
        sb.append('-');
        sb.append(getCompactSampleName(sample));
      }
    }

    // Set the file index if needed
    if (fileIndex >= 0) {
      if (NEW_STYLE)
        sb.append('-');
      sb.append(toLetter(fileIndex));
    }

    // Set the extension
    sb.append(df.getDefaultExtention());

    return sb.toString();
  }

  private static String getCompactSampleName(final Sample sample) {

    if (sample == null)
      return null;

    final String sampleName = sample.getName();
    final StringBuilder sb = new StringBuilder();

    final int len = sampleName.length();

    for (int i = 0; i < len; i++) {

      final int codePoint = sampleName.codePointAt(i);
      if (Character.isLetterOrDigit(codePoint))
        sb.append(codePoint);
    }

    return sb.toString();
  }

  /**
   * Private constructor.
   */
  private ContextUtils() {
  }

}
