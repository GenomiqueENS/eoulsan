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

import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public interface SequenceReadsMapper {

  /**
   * Get the mapper name.
   * @return the mapper name
   */
  String getMapperName();

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
  void makeArchiveIndex(final InputStream is, final File archiveOutputFile)
      throws IOException;

  void makeArchiveIndex(final File genomeFile, final File archiveOutputFile)
      throws IOException;

  //
  // Input file creating methods
  //

  void writeInputEntry(final ReadSequence read) throws IOException;

  void writeInputEntry(final ReadSequence read1, final ReadSequence read2)
      throws IOException;

  void writeInputEntry(final String sequenceName, final String sequence,
      final String quality) throws IOException;

  void writeInputEntry(final String sequenceName1, final String sequence1,
      final String quality1, final String sequenceName2,
      final String sequence2, final String quality2) throws IOException;

  void closeInput() throws IOException;

  //
  // Configuration methods
  //

  int getThreadsNumber();

  void setThreadsNumber(int threadsNumber);

  String getMapperArguments();

  void setMapperArguments(String arguments);

  File getTempDirectory();

  void setTempDirectory(File tempDirectory);

  //
  // Mapping methods
  //

  void map(File archiveIndexFile, File archiveIndexDir) throws IOException;

  void map(File readsFile, File archiveIndexFile, File archiveIndexDir)
      throws IOException;

  void map(File readsFile1, File readsFile2, File archiveIndexFile,
      File archiveIndexDir) throws IOException;

  File getSAMFile(GenomeDescription gd) throws IOException;

  //
  // Other methods
  //

  void clean();

  void init(boolean pairEnd, int phredOffset, ReporterIncrementer incrementer,
      String counterGroup);

  DataFormat getArchiveFormat();

}
