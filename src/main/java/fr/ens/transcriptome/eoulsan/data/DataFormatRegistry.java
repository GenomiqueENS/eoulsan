/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * this class register DataFormat to allow get the DataFormat of a file from its
 * filename.
 * @author Laurent Jourdren
 */
public class DataFormatRegistry {

  private Set<DataFormat> formats = new HashSet<DataFormat>();
  private Map<String, DataFormat> map = new HashMap<String, DataFormat>();

  private static DataFormatRegistry instance;

  /**
   * Register a DataFormat.
   * @param df the DataFormat to register
   * @throws EoulsanException if the DataFormat is not valid
   */
  public void register(final DataFormat df) throws EoulsanException {

    if (df == null || formats.contains(df))
      return;

    if (df.getFormatName() == null)
      throw new EoulsanException("The DataFormat "
          + df.getClass().getName() + " as no name.");

    final DataType dt = df.getType();

    if (dt == null)
      throw new EoulsanException("The DataFormat \""
          + df.getFormatName() + "\" as no type");

    final String prefix = df.getType().getPrefix();

    if (prefix == null || "".equals(prefix))
      throw new EoulsanException(
          "The prefix of a DataType can't be null or empty ("
              + df.getFormatName() + ")");

    if (prefix.indexOf('\t') != -1)
      throw new EoulsanException(
          "The prefix of a DataType can't contains tab character: " + prefix);

    final String[] extensions = df.getExtensions();

    if (extensions == null || extensions.length == 0)
      throw new EoulsanException(
          "The extensions of a DataFormat can't be null or empty.");

    if (df.getDefaultExtention() == null)
      throw new EoulsanException(
          "The no default extension is provided for DataFormat: "
              + df.getFormatName());

    formats.add(df);

    boolean defaultExtensionFound = false;

    for (String suffix : extensions) {

      if (suffix == null)
        throw new EoulsanException(
            "The extension of a DataFormat can't be null");
      if (suffix.indexOf('\t') != -1)
        throw new EoulsanException(
            "The extension of a DataType can't contains tab character: "
                + suffix);

      if (suffix.equals(df.getDefaultExtention()))
        defaultExtensionFound = true;

      final String key = prefix + "\t" + suffix;

      if (this.map.containsKey(key))
        throw new EoulsanException(
            "The DataFormat registry already contains entry for prefix \""
                + prefix + "\" and extension \"" + suffix + "\"");
      this.map.put(key, df);

    }

    if (!defaultExtensionFound)
      throw new EoulsanException("The default extension of DataFormat \""
          + df.getFormatName() + "\" is not in the list of extensions.");
  }

  /**
   * Register DataFormats.
   * @param array Array with DataFormats to register
   * @throws EoulsanException if the DataFormat is not valid
   */
  public void register(final DataFormat[] array) throws EoulsanException {

    if (array == null)
      return;

    for (DataFormat df : array)
      register(df);
  }

  /**
   * Get a DataFormat From a file prefix and extension
   * @param prefix the prefix of the file
   * @param extension the extension of the file without compression extension
   * @return a DataFormat or null if the DataFormat was not found
   */
  public DataFormat getDataFormat(final String prefix, final String extension) {

    if (prefix == null)
      throw new NullPointerException("The prefix is null");

    if (extension == null)
      throw new NullPointerException("The extension is null");

    final String key = prefix + "\t" + extension;

    return this.map.get(key);
  }

  /**
   * Get the DataFormat of a file from its filename
   * @param filename the filename of the file
   * @return a DataFormat or null if the DataFormat was not found
   */
  public DataFormat getDataFormat(final String filename) {

    if (filename == null)
      throw new NullPointerException("The filename is null");

    final String f =
        StringUtils.filenameWithoutCompressionExtension(filename.trim());

    final int dotPos = f.indexOf('.');

    if (dotPos == -1)
      return null;

    final String ext = f.substring(dotPos);

    final int underscorePos = f.lastIndexOf('_', dotPos);

    if (underscorePos != -1) {

      final String prefix = f.substring(0, underscorePos + 1);
      DataFormat df = getDataFormat(prefix, ext);

      if (df != null)
        return df;
    }

    for (DataFormat df : this.formats)
      if (df.getType().isDataTypeFromDesignFile())
        for (String dfExt : df.getExtensions())
          if (dfExt.equals(ext))
            return df;

    return null;
  }

  //
  // Static method
  //

  /**
   * Get the singleton instance of DataFormatRegistry
   * @return the DataFormatRegistry singleton
   */
  public static DataFormatRegistry getInstance() {

    if (instance == null)
      instance = new DataFormatRegistry();

    return instance;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DataFormatRegistry() {
  }

}
