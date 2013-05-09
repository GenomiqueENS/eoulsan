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

package fr.ens.transcriptome.eoulsan.data;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.util.ServiceListLoader;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * this class register DataFormat to allow get the DataFormat of a file from its
 * filename.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFormatRegistry {

  /** Logger. */
  private static final Logger LOGGER = EoulsanLogger.getLogger();
  private static final String RESOURCE_PREFIX =
      "META-INF/services/xmldataformats/";

  private Set<DataFormat> formats = newHashSet();
  private Map<String, DataFormat> mapFormats = newHashMap();
  private Map<String, DataType> mapDesignDataType = newHashMap();

  private static DataFormatRegistry instance;

  /**
   * Register a DataFormat.
   * @param df the DataFormat to register
   * @throws EoulsanException if the DataFormat is not valid
   */
  public void register(final DataFormat df) throws EoulsanException {

    register(df, false);
  }

  /**
   * Register a DataFormat.
   * @param df the DataFormat to register
   * @param callFromConstructor true if method is call from constructor
   * @throws EoulsanException if the DataFormat is not valid
   */
  private void register(final DataFormat df, final boolean callFromConstructor)
      throws EoulsanException {

    if (df == null || formats.contains(df))
      return;

    if (df.getFormatName() == null)
      throw new EoulsanException("The DataFormat "
          + df.getClass().getName() + " as no name.");

    if (!df.getFormatName().toLowerCase().trim().equals(df.getFormatName())) {
      throw new EoulsanException(
          "The DataFormat name can't contains upper case character"
              + df.getClass().getName() + " as no name.");
    }

    for (DataFormat format : this.formats) {
      if (format.getFormatName().equals(df.getFormatName()))
        throw new EoulsanException("A DataFormat named "
            + df.getFormatName() + " is already registered.");
    }

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

      if (this.mapFormats.containsKey(key)) {
        throw new EoulsanException(
            "The DataFormat registry already contains entry for prefix \""
                + prefix + "\" and extension \"" + suffix + "\"");
      }

      if (!callFromConstructor)
        throw new EoulsanException("This DataFormat "
            + df.getFormatName()
            + " is not registered as a spi service. Cannot register it.");

      // Register DataType is necessary
      final DataType dataType = df.getType();
      if (dataType.getDesignFieldName() != null)
        this.mapDesignDataType.put(dataType.getDesignFieldName(), dataType);

      formats.add(df);
      this.mapFormats.put(key, df);
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
  public DataFormat getDataFormatFromFilename(final String prefix,
      final String extension) {

    if (prefix == null)
      throw new NullPointerException("The prefix is null");

    if (extension == null)
      throw new NullPointerException("The extension is null");

    final String key = prefix + "\t" + extension;

    return this.mapFormats.get(key);
  }

  /**
   * Get the DataFormat of a file from its filename
   * @param filename the filename of the file
   * @return a DataFormat or null if the DataFormat was not found
   */
  public DataFormat getDataFormatFromFilename(final String filename) {

    if (filename == null)
      throw new NullPointerException("The filename is null");

    final String f =
        StringUtils.filenameWithoutCompressionExtension(filename.trim());

    final int dotPos = f.lastIndexOf('.');

    if (dotPos == -1)
      return null;

    final String ext = f.substring(dotPos);

    final int underscorePos = f.lastIndexOf('_', dotPos);

    if (underscorePos != -1) {

      final String prefix = f.substring(0, underscorePos + 1);
      final DataFormat df = getDataFormatFromFilename(prefix, ext);

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

  /**
   * Get a DataFormat from its name.
   * @param dataFormatName the name of the DataFormat to get
   * @return a DataFormat if found or null
   */
  public DataFormat getDataFormatFromName(final String dataFormatName) {

    if (dataFormatName == null) {
      return null;
    }

    for (DataFormat df : this.formats) {
      if (df.getFormatName().equals(dataFormatName)) {
        return df;
      }
    }

    return null;
  }

  /**
   * Get a DataFormat from its DataType and an extension.
   * @param dataType the name type the DataFormat to get
   * @param extension the extension of the file without compression extension
   * @return a DataFormat if found or null
   */
  public DataFormat getDataFormatFromExtension(final DataType dataType,
      final String extension) {

    if (dataType == null || extension == null) {
      return null;
    }

    // Standard search
    final DataFormat result =
        getDataFormatFromFilename(dataType.getPrefix(), extension);

    if (result != null)
      return result;

    // Search with DataType
    for (DataFormat df : this.formats) {
      if (df.getType().equals(dataType)) {
        for (String ext : df.getExtensions()) {
          if (extension.equals(ext)) {
            return df;
          }
        }
      }
    }

    return null;
  }

  /**
   * Get all the registered formats.
   * @return a set with all the registered formats
   */
  public Set<DataFormat> getAllFormats() {

    return Collections.unmodifiableSet(this.formats);
  }

  /**
   * Get the DataType that define a field in the design file.
   * @param fieldName the name of the field
   * @return a DataType
   */
  public DataType getDataTypeForDesignField(final String fieldName) {

    if (fieldName == null)
      return null;

    return this.mapDesignDataType.get(fieldName);
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

    final Iterator<DataFormat> it =
        ServiceLoader.load(DataFormat.class).iterator();

    for (final DataFormat df : Utils.newIterable(it)) {

      try {

        LOGGER.fine("try to register format: " + df);
        register(df, true);

      } catch (EoulsanException e) {
        LOGGER.warning("Connot register "
            + df.getFormatName() + ": " + e.getMessage());
      }
    }

    // Get the classloader
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      for (String filename : ServiceListLoader.load(XMLDataFormat.class
          .getName())) {

        final String resource = RESOURCE_PREFIX + filename;
        LOGGER.fine("Try to register an XML dataformat from "
            + filename + " resource");

        register(new XMLDataFormat(loader.getResourceAsStream(resource)), true);

      }
    } catch (EoulsanException e) {
      LOGGER.severe("Cannot register XML data format: " + e.getMessage());
    } catch (IOException e) {
      LOGGER.severe("Unable to load the list of XML data format files: "
          + e.getMessage());
    }

  }

}
