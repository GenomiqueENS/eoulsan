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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingStandardFile;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.bio.io.FastqReader;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define a mapping using files as input.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class FileMapping extends EntryMapping {

  private IOException mappingException;

  //
  // Getters
  //

  /**
   * Throws the exception if occurs
   * @throws IOException if an error has occurred
   */
  public void throwMappingException() throws IOException {

    if (this.mappingException != null) {
      throw this.mappingException;
    }
  }

  //
  // Mapping methods
  //

  /**
   * Map files in paired-end mode.
   * @param readsFile1 first file
   * @param readsFile2 second file
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess object
   * @throws IOException if an error occurs while launching the mapper
   */
  public final MapperProcess mapPE(final DataFile readsFile1,
      final DataFile readsFile2, final File errorFile, final File logFile)
      throws IOException {

    requireNonNull(readsFile1, "readsFile1 is null");
    requireNonNull(readsFile2, "readsFile2 is null");

    if (!readsFile1.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    if (!readsFile2.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    // Use file mapping only if files are local and not compressed
    if (readsFile1.isLocalFile()
        && readsFile1.getCompressionType() == CompressionType.NONE
        && readsFile2.isLocalFile()
        && readsFile2.getCompressionType() == CompressionType.NONE) {
      return mapPE(readsFile1.toFile(), readsFile2.toFile(), errorFile,
          logFile);
    }

    getLogger().fine("First pair FASTQ file to map: " + readsFile1);
    getLogger().fine("Second pair FASTQ file to map: " + readsFile2);

    return mapPE(readsFile1.open(), readsFile2.open(), errorFile, logFile);
  }

  /**
   * Map files in paired-end mode.
   * @param readsFile1 first file
   * @param readsFile2 second file
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess object
   * @throws IOException if an error occurs while launching the mapper
   */
  public final MapperProcess mapPE(final File readsFile1, final File readsFile2,
      final File errorFile, final File logFile) throws IOException {

    requireNonNull(readsFile1, "readsFile1 is null");
    requireNonNull(readsFile2, "readsFile2 is null");

    checkExistingStandardFile(readsFile1,
        "readsFile1 not exits or is not a standard file.");
    checkExistingStandardFile(readsFile2,
        "readsFile2 not exits or is not a standard file.");

    getLogger().fine("First pair FASTQ file to map: " + readsFile1);
    getLogger().fine("Second pair FASTQ file to map: " + readsFile2);
    getLogger().fine("Mapping with "
        + this.mapperIndex.getMapperName() + " in paired-end mode");

    // Process to mapping
    final MapperProcess result =
        getProvider().mapPE(this, readsFile1, readsFile2, errorFile, logFile);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    // Start mapper
    result.startProcess();

    return result;
  }

  /**
   * Map reads of FASTQ file in paired end mode.
   * @param in1 FASTQ input file with reads of the first end
   * @param in2 FASTQ input file with reads of the first end mapper
   * @param errorFile standard error file
   * @param logFile log file
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  private MapperProcess mapPE(final InputStream in1, final InputStream in2,
      final File errorFile, final File logFile) throws IOException {

    requireNonNull(in1, "in1 argument is null");
    requireNonNull(in2, "in2 argument is null");

    getLogger().fine("Mapping with "
        + this.mapperIndex.getMapperName() + " in paired-end mode");

    requireNonNull(in1, "readsFile1 is null");
    requireNonNull(in2, "readsFile2 is null");

    // Process to mapping
    final MapperProcess mapperProcess = super.mapPE(errorFile, logFile);

    // Copy reads files to named pipes
    writeFirstPairEntries(in1, mapperProcess);
    writeSecondPairEntries(in2, mapperProcess);

    return mapperProcess;
  }

  /**
   * Map a file in single-end mode.
   * @param readsFile first file
   * @param errorFile standard error file
   * @param logFile log file
   * @return a MapperProcess object
   * @throws IOException if an error occurs while launching the mapper
   */
  public final MapperProcess mapSE(final DataFile readsFile,
      final File errorFile, final File logFile) throws IOException {

    requireNonNull(readsFile, "readsFile is null");

    if (!readsFile.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    // Use file mapping only if file is local and not compressed
    if (readsFile.isLocalFile()
        && readsFile.getCompressionType() == CompressionType.NONE) {
      return mapSE(readsFile.toFile(), errorFile, logFile);
    }

    getLogger().fine("FASTQ file to map: " + readsFile);

    return mapSE(readsFile.open(), errorFile, logFile);
  }

  /**
   * Map reads of FASTQ file in single end mode.
   * @param in FASTQ input stream
   * @param errorFile standard error file
   * @param logFile log file
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  private MapperProcess mapSE(final InputStream in, final File errorFile,
      final File logFile) throws IOException {

    requireNonNull(in, "in argument is null");

    getLogger().fine("Mapping with "
        + this.mapperIndex.getMapperName() + " in single-end mode");

    // Process to mapping
    final MapperProcess mapperProcess = super.mapSE(errorFile, logFile);

    // Copy reads file to named pipe
    writeFirstPairEntries(in, mapperProcess);

    return mapperProcess;
  }

  /**
   * Map reads of FASTQ file in single end mode.
   * @param readsFile FASTQ input file
   * @param errorFile standard error file
   * @param logFile log file
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  public MapperProcess mapSE(final File readsFile, final File errorFile,
      final File logFile) throws IOException {

    requireNonNull(readsFile, "readsFile is null");

    checkExistingStandardFile(readsFile,
        "reads File not exits or is not a standard file.");

    getLogger().fine("FASTQ file to map: " + readsFile);
    getLogger().fine("Mapping with "
        + this.mapperIndex.getMapperName() + " in single-end mode");

    // Process to mapping
    final MapperProcess result =
        getProvider().mapSE(this, readsFile, errorFile, logFile);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    // Start mapper
    result.startProcess();

    return result;
  }

  /**
   * Write first pairs entries to the mapper process.
   * @param in first pairs FASTQ file
   * @param mp mapper process
   * @throws FileNotFoundException if the input cannot be found
   */
  private void writeFirstPairEntries(final InputStream in,
      final MapperProcess mp) throws FileNotFoundException {

    requireNonNull(in, "in argument cannot be null");
    requireNonNull(mp, "mp argument cannot be null");

    final Thread t = new Thread(new Runnable() {

      @Override
      public void run() {

        try {
          final FastqReader reader = new FastqReader(in);

          for (ReadSequence read : reader) {
            mp.writeEntry1(read);
          }

          reader.close();
          mp.closeWriter1();

        } catch (IOException e) {
          mappingException = e;
        }
      }
    }, "Mapper writeFirstPairEntries thread");

    t.start();
  }

  /**
   * Write first pairs entries to the mapper process.
   * @param in first pairs FASTQ file
   * @param mp mapper process
   * @throws FileNotFoundException if the input cannot be found
   */
  private void writeSecondPairEntries(final InputStream in,
      final MapperProcess mp) throws FileNotFoundException {

    requireNonNull(in, "in argument cannot be null");
    requireNonNull(mp, "mp argument cannot be null");

    final Thread t = new Thread(new Runnable() {

      @Override
      public void run() {

        try {

          final FastqReader reader = new FastqReader(in);

          for (ReadSequence read : reader) {
            mp.writeEntry2(read);
          }

          reader.close();
          mp.closeWriter2();

        } catch (IOException e) {
          mappingException = e;
        }
      }
    }, "Mapper writeSecondPairEntries thread");

    t.start();
  }

  @Override
  public MapperProcess mapPE(final File errorFile, final File logFile)
      throws IOException {

    throw new IllegalStateException();
  }

  @Override
  public MapperProcess mapSE(final File errorFile, final File logFile)
      throws IOException {

    throw new IllegalStateException();
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
  public FileMapping(final MapperIndex mapperIndex,
      final FastqFormat fastqFormat, final List<String> mapperArguments,
      final int threadNumber, final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup) {

    super(mapperIndex, fastqFormat, mapperArguments, threadNumber,
        multipleInstanceEnabled, incrementer, counterGroup);
  }

}
