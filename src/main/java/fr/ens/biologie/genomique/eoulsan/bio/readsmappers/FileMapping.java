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
import static fr.ens.biologie.genomique.eoulsan.util.Utils.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.bio.io.FastqReader;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

public class FileMapping extends EntryMapping {

  private IOException mappingException;

  //
  // Getters
  //

  public void throwMappingException() throws IOException {

    if (this.mappingException != null) {
      throw this.mappingException;
    }
  }

  //
  // Mapping methods
  //

  public final MapperProcess mapPE(final DataFile readsFile1,
      final DataFile readsFile2) throws IOException {

    checkNotNull(readsFile1, "readsFile1 is null");
    checkNotNull(readsFile2, "readsFile2 is null");

    if (!readsFile1.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    if (!readsFile2.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    getLogger().fine("First pair FASTQ file to map: " + readsFile1);
    getLogger().fine("Second pair FASTQ file to map: " + readsFile2);

    return mapPE(readsFile1.open(), readsFile2.open());
  }

  public final MapperProcess mapPE(final File readsFile1, final File readsFile2)
      throws IOException {

    checkNotNull(readsFile1, "readsFile1 is null");
    checkNotNull(readsFile2, "readsFile2 is null");

    checkExistingStandardFile(readsFile1,
        "readsFile1 not exits or is not a standard file.");
    checkExistingStandardFile(readsFile2,
        "readsFile2 not exits or is not a standard file.");

    getLogger().fine("First pair FASTQ file: " + readsFile1);
    getLogger().fine("Second pair FASTQ file: " + readsFile2);

    return mapPE(new FileInputStream(readsFile1),
        new FileInputStream(readsFile2));
  }

  /**
   * Map reads of FASTQ file in paired end mode.
   * @param in1 FASTQ input file with reads of the first end
   * @param in2 FASTQ input file with reads of the first end mapper
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  private MapperProcess mapPE(final InputStream in1, final InputStream in2)
      throws IOException {

    checkNotNull(in1, "in1 argument is null");
    checkNotNull(in2, "in2 argument is null");

    getLogger().fine("Mapping with "
        + this.mapperIndex.getMapperName() + " in paired-end mode");

    checkNotNull(in1, "readsFile1 is null");
    checkNotNull(in2, "readsFile2 is null");

    // Process to mapping
    final MapperProcess mapperProcess = super.mapPE();

    // Copy reads files to named pipes
    writeFirstPairEntries(in1, mapperProcess);
    writeSecondPairEntries(in2, mapperProcess);

    return mapperProcess;
  }

  public final MapperProcess mapSE(final DataFile readsFile)
      throws IOException {

    checkNotNull(readsFile, "readsFile is null");

    if (!readsFile.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    getLogger().fine("FASTQ file to map: " + readsFile);

    return mapSE(readsFile.open());
  }

  public final MapperProcess mapSE(final File readsFile) throws IOException {

    checkNotNull(readsFile, "readsFile is null");
    checkExistingStandardFile(readsFile,
        "readsFile1 not exits or is not a standard file.");

    getLogger().fine("FASTQ file to map: " + readsFile);

    return mapSE(new FileInputStream(readsFile));
  }

  /**
   * Map reads of FASTQ file in single end mode.
   * @param in FASTQ input stream
   * @return an InputStream with SAM data
   * @throws IOException if an error occurs while mapping the reads
   */
  private MapperProcess mapSE(final InputStream in) throws IOException {

    checkNotNull(in, "in argument is null");

    getLogger().fine("Mapping with "
        + this.mapperIndex.getMapperName() + " in single-end mode");

    // Process to mapping
    final MapperProcess mapperProcess = super.mapSE();

    // Copy reads file to named pipe
    writeFirstPairEntries(in, mapperProcess);

    return mapperProcess;
  }

  /**
   * Write first pairs entries to the mapper process
   * @param in first pairs FASTQ file
   * @param mp mapper process
   * @throws FileNotFoundException if the input cannot be found
   */
  private void writeFirstPairEntries(final InputStream in,
      final MapperProcess mp) throws FileNotFoundException {

    checkNotNull(in, "in argument cannot be null");
    checkNotNull(mp, "mp argument cannot be null");

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
   * Write first pairs entries to the mapper process
   * @param in first pairs FASTQ file
   * @param mp mapper process
   * @throws FileNotFoundException if the input cannot be found
   */
  private void writeSecondPairEntries(final InputStream in,
      final MapperProcess mp) throws FileNotFoundException {

    checkNotNull(in, "in argument cannot be null");
    checkNotNull(mp, "mp argument cannot be null");

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

  public MapperProcess mapPE() throws IOException {

    throw new IllegalStateException();
  }

  public MapperProcess mapSE() throws IOException {

    throw new IllegalStateException();
  }

  //
  // Constructor
  //

  public FileMapping(final MapperIndex mapperIndex,
      final FastqFormat fastqFormat, final List<String> mapperArguments,
      final int threadNumber, final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup) {

    super(mapperIndex, fastqFormat, mapperArguments, threadNumber,
        multipleInstanceEnabled, incrementer, counterGroup);
  }

}
