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
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define a mapping.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class EntryMapping {

  protected final MapperIndex mapperIndex;
  protected final FastqFormat fastqFormat;
  protected final ReporterIncrementer incrementer;
  protected final String counterGroup;
  protected final List<String> mapperArguments;
  protected final int threadNumber;
  protected final boolean multipleInstanceEnabled;
  protected final MapperLogger logger;

  //
  // Getters
  //

  /**
   * Get the FASTQ format.
   * @return the mapper arguments
   */
  public FastqFormat getFastqFormat() {
    return this.fastqFormat;
  }

  /**
   * Get the mapper arguments.
   * @return the mapper arguments
   */
  public List<String> getMapperArguments() {
    return this.mapperArguments;
  }

  /**
   * Get the number of threads to use.
   * @return the number of threads to use
   */
  public int getThreadNumber() {
    return this.threadNumber;
  }

  /**
   * Test if multiple instances is enabled.
   * @return true if multiple instances is enabled
   */
  public boolean isMultipleInstancesEnabled() {
    return this.multipleInstanceEnabled;
  }

  /**
   * Get the name of the mapper
   * @return the name of the mapper
   */
  public String getName() {
    return this.mapperIndex.getMapperInstance().getMapper().getName();
  }

  /**
   * Get the mapper version.
   * @return a string with the version of the mapper
   */
  public String getVersion() {
    return this.mapperIndex.getMapperInstance().getVersion();
  }

  /**
   * Get the mapper flavor.
   * @return a string with the flavor of the mapper
   */
  public String getFlavor() {
    return this.mapperIndex.getMapperInstance().getFlavor();
  }

  /**
   * Get the temporary directory to use by the mapper.
   * @return the temporary directory to use by the mapper
   */
  public File getTemporaryDirectory() {
    return this.mapperIndex.getMapperInstance().getTemporaryDirectory();
  }

  /**
   * Get the executor.
   * @return the executor
   */
  public MapperExecutor getExecutor() {
    return this.mapperIndex.getMapperInstance().getExecutor();
  }

  /**
   * Get the index directory.
   * @return the index output directory
   */
  public File getIndexDirectory() {
    return this.mapperIndex.getIndexDirectory();
  }

  /**
   * Get the provider of the mapper.
   * @return the provider of the mapper
   */
  protected MapperProvider getProvider() {
    return this.mapperIndex.getMapperInstance().getMapper().getProvider();
  }

  /**
   * Get the mapper instance.
   * @return the mapper instance
   */
  public MapperInstance getMapperInstance() {
    return this.mapperIndex.getMapperInstance();
  }

  //
  // Other methods
  //

  /**
   * Map in single-end mode.
   * @return a MapperProcess process
   * @throws IOException if an error occurs while starting the mapping
   */
  public MapperProcess mapSE() throws IOException {

    return mapSE(null, null);
  }

  /**
   * Map in single-end mode.
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess process
   * @throws IOException if an error occurs while starting the mapping
   */
  public MapperProcess mapSE(final File errorFile, final File logFile)
      throws IOException {

    this.logger.debug("Mapping with "
        + this.mapperIndex.getMapperName() + " in single-end mode");

    // Process to mapping
    final MapperProcess result =
        getProvider().mapSE(this, null, errorFile, logFile);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    // Start mapper
    result.startProcess();

    return result;
  }

  /**
   * Map in paired-end mode.
   * @return a MapperProcess process
   * @throws IOException if an error occurs while starting the mapping
   */
  public MapperProcess mapPE() throws IOException {

    return mapPE(null, null);
  }

  /**
   * Map in paired-end mode.
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess process
   * @throws IOException if an error occurs while starting the mapping
   */
  public MapperProcess mapPE(final File errorFile, final File logFile)
      throws IOException {

    this.logger.debug("Mapping with "
        + this.mapperIndex.getMapperName() + " in paired-end mode");

    // Process to mapping
    final MapperProcess result =
        getProvider().mapPE(this, null, null, errorFile, logFile);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    // Start mapper
    result.startProcess();

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapperIndex mapper index object
   * @param fastqFormat FASTQ format
   * @param mapperArguments the mapper arguments
   * @param threadNumber the thread number
   * @param multipleInstanceEnabled true if multiple instance must be enabled
   * @param incrementer the incrementer
   * @param counterGroup the counter group
   */
  EntryMapping(final MapperIndex mapperIndex, final FastqFormat fastqFormat,
      final List<String> mapperArguments, final int threadNumber,
      final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup,
      final MapperLogger logger) {

    requireNonNull(mapperIndex, "mapperIndex cannot be null");
    requireNonNull(fastqFormat, "fastqFormat cannot be null");
    requireNonNull(mapperArguments, "mapperArguments cannot be null");

    this.mapperIndex = mapperIndex;
    this.fastqFormat = fastqFormat;
    this.mapperArguments = mapperArguments;
    this.multipleInstanceEnabled = mapperIndex.getMapperInstance().getMapper()
        .getProvider().isMultipleInstancesAllowed()
        && multipleInstanceEnabled;
    this.threadNumber =
        threadNumber > 1 && !this.multipleInstanceEnabled ? threadNumber : 1;

    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
    this.logger = logger;
  }

}
