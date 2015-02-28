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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

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
   * Test if the mapper can only be use for generate the mapper index.
   * @return true if the mapper is a fake mapper
   */
  boolean isIndexGeneratorOnly();

  /**
   * Test if the mapping can be split for parallelization.
   * @return true if the mapping can be split for parallelization
   */
  boolean isSplitsAllowed();

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
   * Set the temporary directory to use by the mapper.
   * @param tempDirectory the temporary directory to use
   */
  void setTempDirectory(File tempDirectory);

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

  //
  // Mapping methods
  //

  /**
   * Map reads of fastq file in single end mode.
   * @param readsFile fastq input file mapper
   * @param gd genome description
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  InputStream mapSE(File readsFile, GenomeDescription gd) throws IOException;

  /**
   * Map reads of fastq file in single end mode.
   * @param readsFile fastq input file mapper
   * @param gd genome description
   * @param samFile output SAM file
   * @throws IOException if an error occurs while mapping the reads
   */
  void mapSE(File readsFile, GenomeDescription gd, File samFile)
      throws IOException;

  /**
   * Map reads of fastq file in single end mode.
   * @param gd genome description
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapSE(GenomeDescription gd) throws IOException;

  /**
   * Map reads of fastq file in paired end mode.
   * @param readsFile1 fastq input file with reads of the first end
   * @param readsFile2 fastq input file with reads of the first end mapper
   * @param gd genome description
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  InputStream mapPE(File readsFile1, File readsFile2, GenomeDescription gd)
      throws IOException;

  /**
   * Map reads of fastq file in paired end mode.
   * @param readsFile1 fastq input file with reads of the first end
   * @param readsFile2 fastq input file with reads of the first end mapper
   * @param gd genome description
   * @param samFile output SAM file
   * @throws IOException if an error occurs while mapping the reads
   */
  void mapPE(File readsFile1, File readsFile2, GenomeDescription gd,
      File samFile) throws IOException;

  /**
   * Map reads of fastq file in paired end mode.
   * @param gd genome description
   * @throws IOException if an error occurs while mapping the reads
   */
  MapperProcess mapPE(GenomeDescription gd) throws IOException;

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
   * @param incrementer the incrementer to report the processing of the fastq
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
   * @param incrementer the incrementer to report the processing of the fastq
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
