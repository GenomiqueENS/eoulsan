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
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.readsfilters.SAMParserLine;
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
  // Input file creating methods
  //

  /**
   * Write a read in the file that will be used as an input by the the mapper.
   * @param read SequenceRead object to write
   * @throws IOException if an error occurs will adding the read to the file
   */
  void writeInputEntry(ReadSequence read) throws IOException;

  /**
   * Write the paired reads in files that will be used as inputs by the the
   * mapper.
   * @param read1 SequenceRead object of the first end to write
   * @param read2 SequenceRead object of the second end to write
   * @throws IOException if an error occurs will adding on the two read to the
   *           files
   */
  void writeInputEntry(ReadSequence read1, ReadSequence read2)
      throws IOException;

  /**
   * Write a read in the file that will be used as an input by the the mapper.
   * @param sequenceName name of the read
   * @param sequence sequence of the read
   * @param quality quality string of the read
   * @throws IOException if an error occurs will adding the read to the file
   */
  void writeInputEntry(String sequenceName, String sequence,
      final String quality) throws IOException;

  /**
   * Write the paired reads in files that will be used as inputs by the the
   * mapper.
   * @param sequenceName1 name of the read of the first end
   * @param sequence1 sequence of the read of the first end
   * @param quality1 quality string of the read of the first end
   * @param sequenceName2 name of the read of the second end
   * @param sequence2 sequence of the read of the second end
   * @param quality2 quality string of the read of the second end
   * @throws IOException if an error occurs will adding on the two read to the
   *           files
   */
  void writeInputEntry(String sequenceName1, String sequence1, String quality1,
      String sequenceName2, String sequence2, String quality2)
      throws IOException;

  /**
   * Close the input(s) file(s)
   * @throws IOException
   */
  void closeInput() throws IOException;

  //
  // Configuration methods
  //

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

  String getMapperArguments();

  List<String> getListMapperArguments();
  
  /**
   * Set the mapper additional arguments.
   * @param arguments the additional mapper arguments
   */
  void setMapperArguments(String arguments);

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

  //
  // Mapping methods
  //

  /**
   * Map reads that has been written using writeInputEntry() methods.
   * @throws IOException if an error occurs while mapping the reads
   */
  // Old version, two files move in method init()
  // void map(File archiveIndexFile, File archiveIndexDir) throws IOException;
  void map() throws IOException;

  
  
  /**
   * Map reads of fastq file in single end mode.
   * @param readsFile fastq input file mapper
   * @throws IOException if an error occurs while mapping the reads
   */
  // Old version, two last files move in method init()
  // void map(File readsFile, File archiveIndexFile, File archiveIndexDir)
  // throws IOException;
  void map(File readsFile) throws IOException;

  /**
   * Map reads of fastq file in paired end mode.
   * @param readsFile1 fastq input file with reads of the first end
   * @param readsFile2 fastq input file with reads of the first end mapper
   * @throws IOException if an error occurs while mapping the reads
   */
  // Old version, two last files move in method init()
  // void map(File readsFile1, File readsFile2, File archiveIndexFile,
  // File archiveIndexDir) throws IOException;
  // @param archiveIndexFile genome index for the mapper as a ZIP file
  // @param archiveIndexDir uncompressed directory for the genome index for the
  void map(File readsFile1, File readsFile2) throws IOException;

  
  /**
   * mode single-end : method used only by bowtie mapper
   * @param readsFile fastq input file with reads
   * @param parser SAMParserLine which parses the outputstream without create a file
   * @throws IOException
   */
  void map(File readsFile, SAMParserLine parserLine) throws IOException;
  
  /**
   * mode pair-end : method used only by bowtie mapper
   * @param readsFile1 fastq input file with reads
   * @param readsFile2 fastq input file with reads
   * @param parser SAMParserLine which parses the outputstream without create a file
   * @throws IOException
   */
  void map(File readsFile1, File readsFile2, SAMParserLine parserLine) throws IOException;

  /**
   * Get the output of the mapper as an SAM file.
   * @param gd genome description for the header of the SAM file
   * @return a sam file
   * @throws IOException if an error occurs while creating the SAM file (if
   *           necessary)
   */
  File getSAMFile(GenomeDescription gd) throws IOException;

  //
  // Other methods
  //

  /**
   * Clean temporary files.
   */
  void clean();

  /**
   * Initialize the mapper.
   * @param pairEnd true if the paired end mode must be enable
   * @param fastqFormat the format of the fastq files
   * @param archiveIndexFile genome index for the mapper as a ZIP file
   * @param archiveIndexDir uncompressed directory for the genome index for the
   * @param incrementer the incrementer to report the processing of the fastq
   *          files
   * @param counterGroup the group for the reporter
   */
  // New version : add archiveIndexFile, archiveIndexDir, before present in method map()
  void init(boolean pairEnd, FastqFormat fastqFormat, File archiveIndexFile,
      File archiveIndexDir, ReporterIncrementer incrementer, String counterGroup) throws IOException ;

  /**
   * Get the DataFormat for genome index for the mapper.
   * @return a DataFormat object
   */
  DataFormat getArchiveFormat();

}
