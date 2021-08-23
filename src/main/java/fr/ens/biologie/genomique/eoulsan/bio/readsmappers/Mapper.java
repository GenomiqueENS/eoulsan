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

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This class define a class allow to define the mapper name, the version, the
 * flavor and how a mapper will be executed.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class Mapper {

  private final MapperProvider provider;
  private MapperLogger logger;
  private File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();
  private File executablesTempDir =
      EoulsanRuntime.getSettings().getExecutablesTempDirectoryFile();

  //
  // Getters
  //

  /**
   * Get the name of the mapper
   * @return the name of the mapper
   */
  public String getName() {
    return this.provider.getName();
  }

  /**
   * Get the provider of the mapper.
   * @return the provider of the mapper
   */
  public MapperProvider getProvider() {
    return provider;
  }

  /**
   * Get the temporary directory.
   * @return the temporary directory
   */
  public File getTemporaryDirectory() {
    return tempDir;
  }

  /**
   * Get the executable temporary directory.
   * @return the executable temporary directory
   */
  public File getExecutablesTemporaryDirectory() {
    return this.executablesTempDir;
  }

  /**
   * Get the software package of the mapper.
   * @return the software package of the mapper
   */
  private String getSoftwarePackage() {
    return getName();
  }

  /**
   * Test if the mapper can only be use for generate the mapper index.
   * @return true if the mapper is a fake mapper
   */
  public boolean isIndexGeneratorOnly() {
    return this.provider.isIndexGeneratorOnly();
  }

  /**
   * Test if the mapping can be split for parallelization.
   * @return true if the mapping can be split for parallelization
   */
  public boolean isSplitsAllowed() {
    return this.provider.isSplitsAllowed();
  }

  /**
   * Get the DataFormat for genome index for the mapper.
   * @return a DataFormat object
   */
  public DataFormat getArchiveFormat() {
    return this.provider.getArchiveFormat();
  }

  /**
   * Test if the mapper index must be compressed in ZIP archive.
   * @return true if the mapper index must be compressed
   */
  public boolean isCompressIndex() {
    return this.provider.isCompressedIndex();
  }

  //
  // Setters
  //

  /**
   * Set the temporary directory to use by the mapper.
   * @param directory the temporary directory to use
   */
  public void setTempDirectory(final File directory) {

    requireNonNull(directory, "directory cannot be null");

    this.tempDir = directory;
  }

  /**
   * Set the temporary directory to use to save the binaries of the mapper.
   * @param directory the temporary directory to use
   */
  public void setExecutablesTempDirectory(final File directory) {

    requireNonNull(directory, "directory cannot be null");

    this.executablesTempDir = directory;
  }

  /**
   * Set the logger to use for the mapping.
   * @param logger the logger to use for the mapping
   */
  public void setLogger(final MapperLogger logger) {

    requireNonNull(logger, "logger cannot be null");

    this.logger = logger;
  }

  //
  // Mapper instance creation methods
  //

  /**
   * Create a new mapper instance using bundled binaries.
   * @param version the version of the mapper
   * @param flavor the flavor of the mapper
   * @return a new MapperInstance object
   * @throws IOException if an error occurs while creating the mapper instance
   */
  public MapperInstance newMapperInstance(final String version,
      final String flavor) throws IOException {

    return newMapperInstance(version, flavor, true);
  }

  /**
   * Create a new mapper instance using bundled binaries.
   * @param version the version of the mapper
   * @param flavor the flavor of the mapper
   * @param useBundledBinaries true if bundled binaries must be used
   * @return a new MapperInstance object
   * @throws IOException if an error occurs while creating the mapper instance
   */
  public MapperInstance newMapperInstance(final String version,
      final String flavor, final boolean useBundledBinaries)
      throws IOException {

    return newMapperInstance(version, flavor, useBundledBinaries, null);
  }

  /**
   * Create a new mapper instance using bundled binaries.
   * @param version the version of the mapper
   * @param flavor the flavor of the mapper
   * @param dockerImage the Docker image to use to executed the mapper
   * @return a new MapperInstance object
   * @throws IOException if an error occurs while creating the mapper instance
   */
  public MapperInstance newMapperInstance(final String version,
      final String flavor, final String dockerImage) throws IOException {

    return newMapperInstance(version, flavor, false, dockerImage);
  }

  /**
   * Create a new mapper instance.
   * @param version the version of the mapper
   * @param flavor the flavor of the mapper
   * @param useBundledBinaries true if bundled binaries must be used
   * @param dockerImage the Docker image to use to executed the mapper
   * @return a new MapperInstance object
   * @throws IOException if an error occurs while creating the mapper instance
   */
  public MapperInstance newMapperInstance(final String version,
      final String flavor, final boolean useBundledBinaries,
      final String dockerImage) throws IOException {

    // Define the version to use
    final String versionToUse =
        chooseVersion(version, this.provider.getDefaultVersion());

    // Define the falvor to use
    final String flavorToUse =
        chooseVersion(flavor, this.provider.getDefaultFlavor());

    // Create the executor
    MapperExecutor executor =
        getExecutor(versionToUse, dockerImage, useBundledBinaries);

    return new MapperInstance(this, executor, versionToUse, flavorToUse,
        getTemporaryDirectory(), this.logger);
  }

  /**
   * Select the best suited executor.
   * @param version the version of the mapper
   * @param dockerImage the docker image of the mapper
   * @param useBundledBinaries true if bundled binaries must be used
   * @return a MapperExecutor instance
   * @throws IOException if an error occurs while creating the executor
   */
  private MapperExecutor getExecutor(final String version,
      final String dockerImage, final boolean useBundledBinaries)
      throws IOException {

    // Set the executor to use
    if (dockerImage != null && !dockerImage.isEmpty()) {
      return new DockerMapperExecutor(dockerImage, getTemporaryDirectory(),
          this.logger);
    }

    if (useBundledBinaries) {
      return new BundledMapperExecutor(getSoftwarePackage(), version,
          getExecutablesTemporaryDirectory(), this.logger);
    }

    return new PathMapperExecutor(this.logger);
  }

  //
  // Static methods
  //

  /**
   * Create a new instance of Mapper for a mapper
   * @param mapperName the mapper name
   * @return a Mapper object
   */
  public static Mapper newMapper(final String mapperName) {
    return newMapper(mapperName, null);
  }

  /**
   * Create a new instance of Mapper for a mapper
   * @param mapperName the mapper name
   * @param logger logger to use
   * @return a Mapper object
   */
  public static Mapper newMapper(final String mapperName,
      final MapperLogger logger) {

    requireNonNull(mapperName, "mapperName cannot be null");

    MapperProvider provider =
        MapperProviderService.getInstance().newService(mapperName);

    if (provider == null) {
      return null;
    }

    return new Mapper(provider,
        logger == null ? new DummyMapperLogger() : logger);
  }

  /**
   * Choose the version to use.
   * @param requiredVersion the version required by user. Can be null or empty
   * @param defaultVersion the default version
   * @return the version to use. This version is the default version if the
   *         required version is null or empty
   */
  private static String chooseVersion(final String requiredVersion,
      final String defaultVersion) {

    if (defaultVersion == null) {
      throw new NullPointerException(
          "the defaultVersion argument cannot be null");
    }

    if (requiredVersion == null) {
      return defaultVersion;
    }

    String result = requiredVersion.trim().toLowerCase();

    return result.isEmpty() ? defaultVersion : result;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   * @param provider the provider to use the the Mapper class.
   * @param logger the logger to use
   */
  private Mapper(final MapperProvider provider, final MapperLogger logger) {

    requireNonNull(provider, "provider cannot be null");
    this.provider = provider;
    this.logger = logger;
  }

}
