package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

  File getSAMFile() throws IOException;

  //
  // Other methods
  //
  
  void clean();

  void init(boolean pairEnd, ReporterIncrementer incrementer,
      String counterGroup);
  
  DataFormat getArchiveFormat();
  

}
