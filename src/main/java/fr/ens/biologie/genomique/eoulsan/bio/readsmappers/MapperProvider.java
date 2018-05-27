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
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This class define an interface for a wrapper on reads mapper.
 * @since 2.0
 * @author Laurent Jourdren
 */
public interface MapperProvider {

  /**
   * Get the mapper name.
   * @return the mapper name
   */
  String getName();

  /**
   * Get the default version of the mapper.
   * @return the default version of the mapper
   */
  String getDefaultVersion();

  /**
   * Get the default flavor of the mapper.
   * @return the default flavor of the mapper
   */
  String getDefaultFlavor();

  /**
   * Get the DataFormat for genome index for the mapper.
   * @return a DataFormat object
   */
  DataFormat getArchiveFormat();

  /**
   * Test if the mapper can only be use for generate the mapper index.
   * @return true if the mapper is a fake mapper
   */
  boolean isIndexGeneratorOnly();

  /**
   * Test if the mapper index must be compressed in ZIP archive.
   * @return true if the mapper index must be compressed
   */
  boolean isCompressedIndex();

  /**
   * Test if multiples instances of the read mapper can be used at the same
   * time.
   * @return true if multiples instances of the read mapper can be used at the
   *         same time
   */
  boolean isMultipleInstancesAllowed();

  /**
   * Test if the mapping can be split for parallelization.
   * @return true if the mapping can be split for parallelization
   */
  boolean isSplitsAllowed();

  /**
   * Get binary mapper version.
   * @return a string with the version of the mapper
   */
  String readBinaryVersion(MapperInstance mapperInstance);

  /**
   * Get the default mapper arguments.
   * @return the default mapper arguments
   */
  String getDefaultMapperArguments();

  /**
   * Get the indexer executables.
   * @return the indexer executables
   */
  List<String> getIndexerExecutables(MapperInstance mapperInstance);

  /**
   * Get the executable name.
   * @return the executable name
   */
  String getMapperExecutableName(MapperInstance mapperInstance);

  /**
   * Get the indexer command.
   * @param indexerExecutable the indexer executable file
   * @param genomeFile the genome file
   * @param indexerArguments the indexer arguments
   * @param threads threads to use
   * @return a list that is the command to execute
   */
  List<String> getIndexerCommand(File indexerExecutable, File genomeFile,
      List<String> indexerArguments, int threads);

  /**
   * Check if the mapper flavor exists.
   */
  boolean checkIfFlavorExists(MapperInstance mapperInstance);

  /**
   * Map in single-end.
   * @param mapping the mapping object
   * @param inputFile input file or null if the mapper is launched in entry mode
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess object
   * @throws IOException if an error occurs while launching the mapping
   */
  MapperProcess mapSE(EntryMapping mapping, File inputFile, File errorFile,
      final File logFile) throws IOException;

  /**
   * Map in paired-end.
   * @param mapping the mapping object
   * @param inputFile1 input file #1 or null if the mapper is launched in entry
   *          mode
   * @param inputFile2 input file 32 or null if the mapper is launched in entry
   *          mode
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess object
   * @throws IOException if an error occurs while launching the mapping
   */
  MapperProcess mapPE(EntryMapping mapping, File inputFile1, File inputFile2,
      File errorFile, final File logFile) throws IOException;

}
