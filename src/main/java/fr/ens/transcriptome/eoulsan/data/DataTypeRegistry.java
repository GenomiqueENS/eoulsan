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
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class register DataType to allow get the DataType of a file from its
 * filename.
 * @since 1.2
 * @author Laurent Jourdren
 */
public class DataTypeRegistry {

  /** Logger. */
  private static final Logger LOGGER = EoulsanLogger.getLogger();
  private static final String RESOURCE_PREFIX =
      "META-INF/services/xmldatatypes/";

  private Set<DataType> types = newHashSet();
  private Set<String> prefixes = newHashSet();
  private Map<String, DataType> mapTypes = newHashMap();

  private static DataTypeRegistry instance;

  /**
   * Register a DataType.
   * @param df the DataType to register
   * @throws EoulsanException if the DataType is not valid
   */
  public void register(final DataType dt) throws EoulsanException {

    if (dt == null || types.contains(dt))
      return;

    check(dt);

    this.types.add(dt);
    this.mapTypes.put(dt.getName(), dt);
    this.prefixes.add(dt.getPrefix());
  }

  /**
   * Register DataTypes.
   * @param array Array with DataTypes to register
   * @throws EoulsanException if the DataType is not valid
   */
  public void register(final DataType[] array) throws EoulsanException {

    if (array == null)
      return;

    for (DataType dt : array)
      register(dt);
  }

  private void check(final DataType dt) throws EoulsanException {

    if (dt.getName() == null)
      throw new EoulsanException("The DataType "
          + dt.getClass().getName() + " as no name.");

    if (!dt.getName().toLowerCase().trim().equals(dt.getName())) {
      throw new EoulsanException(
          "The DataType name can't contains upper case character"
              + dt.getClass().getName() + " as no name.");
    }

    for (DataType type : this.types) {
      if (type.getName().equals(dt.getName()))
        throw new EoulsanException("A DataType named "
            + dt.getName() + " is already registered.");
    }

    final String prefix = dt.getPrefix();

    if (prefix == null || "".equals(prefix))
      throw new EoulsanException(
          "The prefix of a DataType can't be null or empty ("
              + dt.getName() + ")");

    if (prefix.indexOf('\t') != -1)
      throw new EoulsanException(
          "The prefix of a DataType can't contains tab character: " + prefix);

    if (this.prefixes.contains(prefix))
      throw new EoulsanException("The prefix of DataType \""
          + dt.getName() + "\" is already registered.");
  }

  /**
   * Get a DataType from its name.
   * @param dataTypeName the name of the DataType to get
   * @return a DataType if found or null
   */
  public DataType getDataTypeFromName(final String dataTypeName) {

    if (dataTypeName == null) {
      return null;
    }

    return this.mapTypes.get(dataTypeName);
  }

  /**
   * Get all the registered types.
   * @return a set with all the registered types
   */
  public Set<DataType> getAllTypes() {

    return Collections.unmodifiableSet(this.types);
  }

  /**
   * Register all type defines by classes.
   */
  private void registerAllClassServices() {

    final Iterator<DataType> it = ServiceLoader.load(DataType.class).iterator();

    for (final DataType dt : Utils.newIterable(it)) {

      try {

        LOGGER.fine("try to register type: " + dt);
        register(dt);

      } catch (EoulsanException e) {
        LOGGER.warning("Cannot register "
            + dt.getName() + ": " + e.getMessage());
      }
    }

  }

  /**
   * Register all type defines by XML files.
   */
  private void registerAllXMLServices() {

    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try {
      for (String filename : ServiceListLoader
          .load(XMLDataType.class.getName())) {

        final String resource = RESOURCE_PREFIX + filename;
        register(new XMLDataType(loader.getResourceAsStream(resource)));

      }
    } catch (EoulsanException e) {
      LOGGER.severe("Cannot register XML data type: " + e.getMessage());
    } catch (IOException e) {
      LOGGER.severe("Unable to load the list of XML data type files: "
          + e.getMessage());
    }

  }

  /**
   * Reload the list of the available data types.
   */
  public void reload() {

    registerAllClassServices();
    registerAllXMLServices();
  }

  //
  // Static method
  //

  /**
   * Get the singleton instance of DataTypeRegistry
   * @return the DataTypeRegistry singleton
   */
  public static DataTypeRegistry getInstance() {

    if (instance == null)
      instance = new DataTypeRegistry();

    return instance;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private DataTypeRegistry() {

    reload();
  }

}
