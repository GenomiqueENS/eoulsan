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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.spotify.docker.client.DockerClient;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define an interface for a wrapper on reads mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public interface SequenceReadsMapper {

  /**
   * Get the mapper name.
   * @return the mapper name
   */
  String getMapperName();

  /**
   * Get mapper version.
   * @return a string with the version of the mapper
   */
  String getMapperVersion();

  /**
   * Get mapper flavor.
   * @return a string with the flavor of the mapper
   */
  String getMapperFlavor();

  /**
   * Get the mapper version to use.
   * @return a string with the mapper version to use
   */
  String getMapperVersionToUse();

  /**
   * Get the flavor of the mapper to use.
   * @return a string the flavor of the mapper to use
   */
  String getMapperFlavorToUse();

  /**
   * Test if the bundled binaries must be used to perform the mapping.
   * @return true if the bundled binaries must be used to perform the mapping
   */
  boolean isUseBundledBinaries();

  /**
   * Get the mapper Docker image to use.
   * @return the mapper Docker image to use or an empty string if no Docker
   *         image is set
   */
  String getMapperDockerImage();

  /**
   * Test if the mapper can only be use for generate the mapper index.
   * @return true if the mapper is a fake mapper
   */
  boolean isIndexGeneratorOnly();

  /**
   * Test if the mapping can be split for parallelization.
   * @return true if the mapping can be split for parallelization
   */
  boolean isSplitsAllowed();

  /**
   * Test if multiples instances of the read mapper can be used at the same
   * time.
   * @return true if multiples instances of the read mapper can be used at the
   *         same time
   */
  boolean isMultipleInstancesAllowed();

  /**
   * Get the Docker connection URI.
   * @return the Docker connection URI
   */
  DockerClient getDockerClient();

  //
  // Index creation methods
  //

  /**
   * Create the index in a ZIP archive
   * @param is input stream of the genome file
   * @param archiveOutputFile the result file that contains the index in a zip
   *          file
   * @throws IOException if an error occurs while creating the index
   */
  void makeArchiveIndex(InputStream is, File archiveOutputFile)
      throws IOException;

  /**
   * Create the index in a ZIP archive
   * @param genomeFile file for the genome file
   * @param archiveOutputFile the result file that contains the index in a zip
   *          file
   * @throws IOException if an error occurs while creating the index
   */
  void makeArchiveIndex(File genomeFile, File archiveOutputFile)
      throws IOException;

  //
  // Configuration methods
  //

  /**
   * Set the flavor of the mapper to use.
   * @param flavor the flavor to use. If null, the default flavor will be used
   */
  void setMapperFlavorToUse(String flavor);

  /**
   * Set the version of the mapper to use.
   * @param version the version to use. If null, the default version will be
   *          used
   */
  void setMapperVersionToUse(String version);

  /**
   * Set if the bundled binaries must be used to perform the mapping.
   * @param use true if the bundled binaries must be used to perform the mapping
   */
  void setUseBundledBinaries(boolean use);

  /**
   * Set the mapper Docker image to use.
   * @param dockerImage the mapper Docker image to use
   */
  void setMapperDockerImage(String dockerImage);

  /**
   * Get the number of thread to use by the mapper.
   * @return the number of thread to use by the mapper
   */
  int getThreadsNumber();

  /**
   * Set the number of thread to use by the mapper.
   * @param threadsNumber the number of threads
   */
  void setThreadsNumber(int threadsNumber);

  /**
   * Get the user options for the mapper.
   * @return the user options as a String
   */
  String getMapperArguments();

  /**
   * Get the user options for the indexer.
   * @return the user options as a String
   */
  String getIndexerArguments();

  /**
   * Get the user options for the mapper.
   * @return the user options as a list
   */
  List<String> getListMapperArguments();

  /**
   * Get the user options for the mapper.
   * @return the user options as a list
   */
  List<String> getListIndexerArguments();

  /**
   * Set the mapper additional arguments.
   * @param arguments the additional mapper arguments
   */
  void setMapperArguments(String arguments);

  /**
   * Set the indexer additional arguments.
   * @param arguments the additional indexer arguments
   */
  void setIndexerArguments(String arguments);

  /**
   * Get the temporary directory to use by the mapper.
   * @return the temporary directory to use by the mapper
   */
  File getTempDirectory();

  /**
   * Get the temporary directory for executables.
   * @return the temporary directory for executables
   */
  File getExecutablesTempDirectory();

  /**
   * Set the temporary directory to use by the mapper.
   * @param tempDirectory the temporary directory to use
   */
  void setTempDirectory(File tempDirectory);

  /**
   * Set the temporary directory to store executables.
   * @param executableTempDirectory the temporary directory for executables to
   *          use
   */
  void setExecutablesTempDirectory(File executableTempDirectory);

  /**
   * Set the FASTQ format.
   * @param format the FASTQ format to use
   */
  void setFastqFormat(FastqFormat format);

  /**
   * Get the FASTQ format currently used.
   * @return the FASTQ format
   */
  FastqFormat getFastqFormat();

  /**
   * Test if multiples instances of the read mapper must be used at the same
   * time.
   * @return true if multiples instances of the read mapper must be used at the
   *         same time
   */
  boolean isMultipleInstancesEnabled();

  /**
   * set if multiples instances of the read mapper must be used at the same
   * time.
   * @param enable true if multiples instances of the read mapper must be used
   *          at the same time
   */
  void setMultipleInstancesEnabled(boolean enable);

  /**
   * Set the Docker connection URI.
   * @param uri the URI to set
   */
  void setDockerClient(DockerClient uri);

  //
  // Mapping methods
  //

  /**
   * Map reads of fastq file in single end mode.
   * @param readsFile fastq input file mapper
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapSE(DataFile readsFile) throws IOException;

  /**
   * Map reads of fastq file in single end mode.
   * @param readsFile fastq input file mapper
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapSE(File readsFile) throws IOException;

  /**
   * Map reads of fastq file in single end mode.
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapSE() throws IOException;

  /**
   * Map reads of FASTQ file in paired end mode.
   * @param readsFile1 FASTQ input file with reads of the first end
   * @param readsFile2 FASTQ input file with reads of the first end mapper
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapPE(DataFile readsFile1, DataFile readsFile2)
      throws IOException;

  /**
   * Map reads of FASTQ file in paired end mode.
   * @param readsFile1 FASTQ input file with reads of the first end
   * @param readsFile2 FASTQ input file with reads of the first end mapper
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapPE(File readsFile1, File readsFile2) throws IOException;

  /**
   * Map reads of FASTQ file in paired end mode.
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapPE() throws IOException;

  /**
   * Throws an exception if an exception has occurred while mapping.
   * @throws IOException if an exception has occurred while mapping
   */
  void throwMappingException() throws IOException;

  /**
   * Get mapper executable name.
   * @return the name of the mapper executable
   */
  String getMapperExecutableName();

  //
  // Other methods
  //

  /**
   * Prepare binaries.
   * @throws IOException if binaries cannot be prepared
   */
  void prepareBinaries() throws IOException;

  /**
   * Initialize the mapper before the mapping.
   * @param archiveIndexFile genome index for the mapper as a ZIP file
   * @param archiveIndexDir uncompressed directory for the genome index
   * @param incrementer the incrementer to report the processing of the FASTQ
   *          files
   * @param counterGroup the group for the reporter
   */
  void init(DataFile archiveIndexFile, File archiveIndexDir,
      ReporterIncrementer incrementer, String counterGroup) throws IOException;

  /**
   * Initialize the mapper before the mapping.
   * @param archiveIndexFile genome index for the mapper as a ZIP file
   * @param archiveIndexDir uncompressed directory for the genome index
   * @param incrementer the incrementer to report the processing of the FASTQ
   *          files
   * @param counterGroup the group for the reporter
   */
  void init(File archiveIndexFile, File archiveIndexDir,
      ReporterIncrementer incrementer, String counterGroup) throws IOException;

  /**
   * Initialize the mapper before the mapping.
   * @param archiveIndexInputStream genome index for the mapper as a ZIP input
   *          stream
   * @param archiveIndexDir uncompressed directory for the genome index
   * @param incrementer the incrementer to report the processing of the FASTQ
   *          files
   * @param counterGroup the group for the reporter
   */
  void init(InputStream archiveIndexInputStream, File archiveIndexDir,
      ReporterIncrementer incrementer, String counterGroup) throws IOException;

  /**
   * Get the DataFormat for genome index for the mapper.
   * @return a DataFormat object
   */
  DataFormat getArchiveFormat();

}
