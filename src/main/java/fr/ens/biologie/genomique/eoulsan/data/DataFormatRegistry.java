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

package fr.ens.biologie.genomique.eoulsan.data;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.EoulsanRuntime.getSettings;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.eoulsan.util.ClassPathResourceLoader;
import fr.ens.biologie.genomique.eoulsan.util.FileResourceLoader;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.Utils;

/**
 * this class register DataFormat to allow get the DataFormat of a file from its
 * filename.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class DataFormatRegistry {

  private static final String RESOURCE_PREFIX =
      "META-INF/services/xmldataformats/";
  private static final String FORMAT_SUBDIR = "formats";

  private final Set<DataFormat> formats = new HashSet<>();
  private final Map<String, DataFormat> mapFormats = new HashMap<>();

  private boolean xmlServicesCurrentlyLoading;
  private final Map<String, DataFormat> mapDesignMetadataKeyDataFormat =
      new HashMap<>();
  private final Map<String, DataFormat> mapSampleMetadataKeyDataFormat =
      new HashMap<>();

  private static DataFormatRegistry instance;

  //
  // Inner classes
  //

  /**
   * This class define a resource loader for resource defined in the file
   * system.
   */
  private static final class DataFormatFileResourceLoader
      extends FileResourceLoader<XMLDataFormat> {

    @Override
    protected String getExtension() {

      return ".xml";
    }

    @Override
    protected String getResourceName(final XMLDataFormat resource) {

      requireNonNull(resource, "resource argument cannot be null");

      return resource.getName();
    }

    @Override
    protected XMLDataFormat load(final InputStream in, final String source)
        throws IOException, EoulsanException {

      return new XMLDataFormat(in, source);
    }

    /**
     * Get the default format directory.
     * @return the default format directory
     */
    private static DataFile getDefaultFormatDirectory() {

      final Main main = Main.getInstance();

      if (main == null) {
        return new DataFile(FORMAT_SUBDIR);
      }

      return new DataFile(new File(main.getEoulsanDirectory(), FORMAT_SUBDIR));
    }

    //
    // Constructors
    //

    /**
     * Constructor.
     * @param resourcePaths paths where searching for the resources.
     */
    public DataFormatFileResourceLoader(final List<String> resourcePaths) {

      super(XMLDataFormat.class, getDefaultFormatDirectory());

      if (resourcePaths != null) {

        addResourcePaths(resourcePaths);
      }
    }
  }

  /**
   * This class define a resource loader for resource defined in the class path.
   */
  private static final class DataFormatClassPathLoader
      extends ClassPathResourceLoader<XMLDataFormat> {

    @Override
    protected String getResourceName(final XMLDataFormat resource) {

      requireNonNull(resource, "resource argument cannot be null");

      return resource.getName();
    }

    @Override
    protected XMLDataFormat load(final InputStream in, final String source)
        throws IOException, EoulsanException {

      if (in == null) {
        throw new NullPointerException(
            "The input stream of the XML DataFormat source is null: " + source);
      }

      return new XMLDataFormat(in, source);
    }

    public DataFormatClassPathLoader() {

      super(XMLDataFormat.class, RESOURCE_PREFIX);
    }
  }

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

    if (df == null || this.formats.contains(df)) {
      return;
    }

    check(df, callFromConstructor);

    for (String suffix : df.getExtensions()) {

      final String prefix = df.getPrefix();
      final String key = prefix + "\t" + suffix;

      this.formats.add(df);
      this.mapFormats.put(key, df);
    }
  }

  /**
   * Register DataFormats.
   * @param array Array with DataFormats to register
   * @throws EoulsanException if the DataFormat is not valid
   */
  public void register(final DataFormat[] array) throws EoulsanException {

    if (array == null) {
      return;
    }

    for (DataFormat df : array) {
      register(df);
    }
  }

  private void check(final DataFormat df, final boolean callFromConstructor)
      throws EoulsanException {

    if (df.getName() == null) {
      throw new EoulsanException(
          "The DataFormat " + df.getClass().getName() + " as no name.");
    }

    if (!df.getName().toLowerCase().trim().equals(df.getName())) {
      throw new EoulsanException(
          "The DataFormat name can't contains upper case character"
              + df.getClass().getName() + " as no name.");
    }

    for (DataFormat format : this.formats) {
      if (format.getName().equals(df.getName())) {
        throw new EoulsanException(
            "A DataFormat named " + df.getName() + " is already registered.");
      }
    }

    final String prefix = df.getPrefix();

    if (prefix == null || "".equals(prefix)) {
      throw new EoulsanException(
          "The prefix of a DataFormat can't be null or empty ("
              + df.getName() + ")");
    }

    if (prefix.indexOf('\t') != -1) {
      throw new EoulsanException(
          "The prefix of a DataFormat can't contains tab character: " + prefix);
    }

    final List<String> extensions = df.getExtensions();

    if (extensions == null || extensions.size() == 0) {
      throw new EoulsanException(
          "The extensions of a DataFormat can't be null or empty.");
    }

    if (df.getDefaultExtension() == null) {
      throw new EoulsanException(
          "The no default extension is provided for DataFormat: "
              + df.getName());
    }

    boolean defaultExtensionFound = false;

    for (String suffix : df.getExtensions()) {

      if (suffix == null) {
        throw new EoulsanException(
            "The extension of a DataFormat can't be null");
      }
      if (suffix.indexOf('\t') != -1) {
        throw new EoulsanException(
            "The extension of a DataFormat can't contains tab character: "
                + suffix);
      }

      if (suffix.equals(df.getDefaultExtension())) {
        defaultExtensionFound = true;
      }

      final String key = prefix + "\t" + suffix;

      if (this.mapFormats.containsKey(key)) {
        throw new EoulsanException(
            "The DataFormat registry already contains entry for prefix \""
                + prefix + "\" and extension \"" + suffix + "\"");
      }

      if (!callFromConstructor) {
        throw new EoulsanException("This DataFormat "
            + df.getName()
            + " is not registered as a spi service. Cannot register it.");
      }

      // Register DataFormat for design fields is necessary
      if (df.getSampleMetadataKeyName() != null) {
        this.mapSampleMetadataKeyDataFormat.put(df.getSampleMetadataKeyName(),
            df);
      }

      // Register DataFormat for design fields is necessary
      if (df.getDesignMetadataKeyName() != null) {
        this.mapDesignMetadataKeyDataFormat.put(df.getDesignMetadataKeyName(),
            df);
      }

      this.formats.add(df);
      this.mapFormats.put(key, df);

    }

    if (!defaultExtensionFound) {
      throw new EoulsanException("The default extension of DataFormat \""
          + df.getName() + "\" is not in the list of extensions.");
    }

  }

  /**
   * Get a DataFormat From a file prefix and extension
   * @param prefix the prefix of the file
   * @param extension the extension of the file without compression extension
   * @return a DataFormat or null if the DataFormat was not found
   */
  public DataFormat getDataFormatFromFilename(final String prefix,
      final String extension) {

    if (prefix == null) {
      throw new NullPointerException("The prefix is null");
    }

    if (extension == null) {
      throw new NullPointerException("The extension is null");
    }

    final String key = prefix + "\t" + extension;

    return this.mapFormats.get(key);
  }

  /**
   * Get the DataFormat of a file from its filename
   * @param filename the filename of the file
   * @return a DataFormat or null if the DataFormat was not found
   */
  public DataFormat getDataFormatFromFilename(final String filename) {

    if (filename == null) {
      throw new NullPointerException("The filename is null");
    }

    final String f =
        StringUtils.filenameWithoutCompressionExtension(filename.trim());

    final int dotPos = f.lastIndexOf('.');

    if (dotPos == -1) {
      return null;
    }

    final String ext = f.substring(dotPos);

    final int underscorePos = f.lastIndexOf('_', dotPos);

    if (underscorePos != -1) {

      final String prefix = f.substring(0, underscorePos + 1);
      final DataFormat df = getDataFormatFromFilename(prefix, ext);

      if (df != null) {
        return df;
      }
    }

    for (DataFormat df : this.formats) {
      if (df.isDataFormatFromDesignFile()) {
        for (String dfExt : df.getExtensions()) {
          if (dfExt.equals(ext)) {
            return df;
          }
        }
      }
    }

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
      if (df.getName().equals(dataFormatName)) {
        return df;
      }
    }

    return null;
  }

  /**
   * Get a DataFormat from its alias.
   * @param dataFormatAlias the name of the DataFormat to get
   * @return a DataFormat if found or null
   */
  public DataFormat getDataFormatFromAlias(final String dataFormatAlias) {

    if (dataFormatAlias == null) {
      return null;
    }

    for (DataFormat df : this.formats) {

      final String alias = df.getAlias();

      if (dataFormatAlias.toLowerCase().equals(alias)) {
        return df;

      }
    }

    return null;
  }

  /**
   * Get DataFormat from an Galaxy format name.
   * @param formatName Galaxy name extension.
   * @return DataFormat
   */
  public DataFormat getDataFormatFromGalaxyFormatName(final String formatName) {

    if (formatName == null || formatName.isEmpty()) {
      return null;
    }

    // Search with DataType
    for (DataFormat df : this.formats) {

      // Parse Galaxy tool extension
      for (String galaxyFormatName : df.getGalaxyFormatNames()) {

        if (formatName.toLowerCase(Globals.DEFAULT_LOCALE)
            .equals(galaxyFormatName)) {
          return df;
        }
      }
    }

    return null;
  }

  /**
   * Get a DataFormat from its alias.
   * @param name the name of the DataFormat to get
   * @return a DataFormat if found or null
   */
  public DataFormat getDataFormatFromNameOrAlias(final String name) {

    DataFormat result = getDataFormatFromName(name);

    return result != null ? result : getDataFormatFromAlias(name);
  }

  /**
   * Get a DataFormat from its Galaxy format name or its name or alias.
   * @param name the name of the DataFormat to get
   * @return a DataFormat if found or null
   */
  public DataFormat getDataFormatFromGalaxyFormatNameOrNameOrAlias(
      final String name) {

    DataFormat result = getDataFormatFromGalaxyFormatName(name);

    return result != null ? result : getDataFormatFromNameOrAlias(name);
  }

  /**
   * Get DataFormats from an extension.
   * @param extension the extension of the file without compression extension
   * @return a set of DataFormat
   */
  public Set<DataFormat> getDataFormatsFromExtension(final String extension) {

    if (extension == null) {
      return Collections.emptySet();
    }

    final Set<DataFormat> result = new HashSet<>();

    // Search with DataType
    for (DataFormat df : this.formats) {
      for (String ext : df.getExtensions()) {
        if (extension.equals(ext)) {
          result.add(df);
        }
      }
    }

    return Collections.unmodifiableSet(result);
  }

  /**
   * Get all the registered formats.
   * @return a set with all the registered formats
   */
  public Set<DataFormat> getAllFormats() {

    return Collections.unmodifiableSet(this.formats);
  }

  /**
   * Get the DataFormat that define a metadata entry in the design file.
   * @param key the name of the metadata key
   * @return a DataFormat
   */
  public DataFormat getDataFormatForDesignMetadata(final String key) {

    if (key == null) {
      return null;
    }

    return this.mapDesignMetadataKeyDataFormat.get(key);
  }

  /**
   * Get the DataFormat that define a metadata entry of a sample in the design
   * file.
   * @param key the name of the metadata key
   * @return a DataFormat
   */
  public DataFormat getDataFormatForSampleMetadata(final String key) {

    if (key == null) {
      return null;
    }

    return this.mapSampleMetadataKeyDataFormat.get(key);
  }

  /**
   * Get the field name in a Design object that correspond to a dataformat.
   * @param design design object
   * @param dataformat dataformat to search
   * @return the field name if found or null
   */
  public String getDesignMetadataKeyForDataFormat(final Design design,
      final DataFormat dataformat) {

    if (design == null || dataformat == null) {
      return null;
    }

    for (String fieldname : design.getMetadata().keySet()) {

      final DataFormat df = getDataFormatForDesignMetadata(fieldname);
      if (dataformat.equals(df)) {
        return fieldname;
      }
    }

    return null;
  }

  /**
   * Get the field name in a Sample object that correspond to a dataformat.
   * @param sample sample object
   * @param dataformat dataformat to search
   * @return the field name if found or null
   */
  public String getSampleMetadataKeyForDataFormat(final Sample sample,
      final DataFormat dataformat) {

    if (sample == null || dataformat == null) {
      return null;
    }

    for (String fieldname : sample.getMetadata().keySet()) {

      final DataFormat df = getDataFormatForSampleMetadata(fieldname);
      if (dataformat.equals(df)) {
        return fieldname;
      }
    }

    return null;
  }

  /**
   * Register all type defines by classes.
   */
  private void registerAllClassServices() {

    final Iterator<DataFormat> it =
        ServiceLoader.load(DataFormat.class).iterator();

    for (final DataFormat df : Utils.newIterable(it)) {

      try {

        getLogger().fine("try to register format: " + df);
        register(df, true);

      } catch (EoulsanException e) {
        getLogger()
            .warning("Cannot register " + df.getName() + ": " + e.getMessage());
      }
    }
  }

  /**
   * Register all type defines by XML files.
   */
  private void registerAllXMLServices() {

    try {

      // Load XML formats from the Jar
      DataFormatClassPathLoader formatClassLoader =
          new DataFormatClassPathLoader();
      formatClassLoader.reload();
      final List<DataFormat> formats =
          new ArrayList<>(formatClassLoader.loadAllResources());

      // Load XML formats from external resources (files...)
      DataFormatFileResourceLoader formatFileLoader =
          new DataFormatFileResourceLoader(getSettings().getDataFormatPaths());
      formatFileLoader.reload();
      formats.addAll(formatFileLoader.loadAllResources());

      // Register formats
      for (DataFormat format : formats) {

        register(format, true);
      }

    } catch (EoulsanException e) {
      getLogger().severe("Cannot register XML data format: " + e.getMessage());
    }
  }

  /**
   * Reload the list of the available data types.
   */
  public void reload() {

    registerAllClassServices();

    // Avoid to load XML formats if XML formats are currently loading
    if (!this.xmlServicesCurrentlyLoading) {
      this.xmlServicesCurrentlyLoading = true;
      registerAllXMLServices();
      this.xmlServicesCurrentlyLoading = false;
    }
  }

  //
  // Static method
  //

  /**
   * Get the singleton instance of DataFormatRegistry
   * @return the DataFormatRegistry singleton
   */
  public static synchronized DataFormatRegistry getInstance() {

    if (instance == null) {
      instance = new DataFormatRegistry();

      // Initial loading of the formats
      instance.reload();
    }

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
