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
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

/**
 * This class define a mapper index.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MapperIndex {

  private final MapperInstance mapperInstance;
  private final InputStream in;
  private final File indexDirectory;
  private boolean unzipped;

  //
  // Getters
  //

  /**
   * Get the mapper instance.
   * @return the mapper instance
   */
  MapperInstance getMapperInstance() {
    return this.mapperInstance;
  }

  /**
   * Get the index directory.
   * @return the index output directory
   */
  public File getIndexDirectory() {
    return this.indexDirectory;
  }

  /**
   * Get the mapper index archive input stream.
   * @return the mapper index archive input stream
   */
  private InputStream getInputStream() {
    return this.in;
  }

  /**
   * Get the mapper name.
   * @return the mapper name
   */
  String getMapperName() {
    return this.getMapperInstance().getMapper().getName();
  }

  //
  // Mapping object creation
  //

  /**
   * Create a new mapping that will use entries as input.
   * @param fastqFormat the FASTQ format
   * @param mapperArguments mapper arguments
   * @param threadNumber thread number
   * @param multipleInstanceEnabled true if multiple instance must be enabled
   * @param incrementer the incrementer
   * @param counterGroup the counter group
   * @return a new Mapping object
   * @throws IOException if an error occurs while creating the Mapping object
   */
  public EntryMapping newEntryMapping(final FastqFormat fastqFormat,
      final String mapperArguments, final int threadNumber,
      final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    return newEntryMapping(fastqFormat,
        MapperUtils.argumentsAsList(mapperArguments), threadNumber,
        multipleInstanceEnabled, incrementer, counterGroup);
  }

  /**
   * Create a new mapping that will use entries as input.
   * @param fastqFormat the FASTQ format
   * @param mapperArguments mapper arguments
   * @param threadNumber thread number
   * @param multipleInstanceEnabled true if multiple instance must be enabled
   * @param incrementer the incrementer
   * @param counterGroup the counter group
   * @return a new Mapping object
   * @throws IOException if an error occurs while creating the Mapping object
   */
  public EntryMapping newEntryMapping(final FastqFormat fastqFormat,
      final List<String> mapperArguments, final int threadNumber,
      final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    synchronized (this) {
      if (!unzipped) {
        unzipArchiveIndexFile(getInputStream(), getIndexDirectory());
        this.unzipped = false;
      }
    }

    return new EntryMapping(this, fastqFormat, mapperArguments, threadNumber,
        multipleInstanceEnabled, incrementer, counterGroup);
  }

  /**
   * Create a new mapping that will use entries as files.
   * @param fastqFormat the FASTQ format
   * @param mapperArguments mapper arguments
   * @param threadNumber thread number
   * @param multipleInstanceEnabled true if multiple instance must be enabled
   * @param incrementer the incrementer
   * @param counterGroup the counter group
   * @return a new Mapping object
   * @throws IOException if an error occurs while creating the Mapping object
   */
  public FileMapping newFileMapping(final FastqFormat fastqFormat,
      final String mapperArguments, final int threadNumber,
      final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    return newFileMapping(fastqFormat,
        MapperUtils.argumentsAsList(mapperArguments), threadNumber,
        multipleInstanceEnabled, incrementer, counterGroup);
  }

  /**
   * Create a new mapping that will use entries as files.
   * @param fastqFormat the FASTQ format
   * @param mapperArguments mapper arguments
   * @param threadNumber thread number
   * @param multipleInstanceEnabled true if multiple instance must be enabled
   * @param incrementer the incrementer
   * @param counterGroup the counter group
   * @return a new Mapping object
   * @throws IOException if an error occurs while creating the Mapping object
   */
  public FileMapping newFileMapping(final FastqFormat fastqFormat,
      final List<String> mapperArguments, final int threadNumber,
      final boolean multipleInstanceEnabled,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    synchronized (this) {
      if (!unzipped) {
        unzipArchiveIndexFile(getInputStream(), getIndexDirectory());
        this.unzipped = false;
      }
    }

    return new FileMapping(this, fastqFormat, mapperArguments, threadNumber,
        multipleInstanceEnabled, incrementer, counterGroup);
  }

  //
  // Unzip methods
  //

  private void unzipArchiveIndexFile(final InputStream archiveIndexFile,
      final File archiveIndexDir) throws IOException {

    final File lockFile =
        new File(archiveIndexDir.getAbsoluteFile().getParentFile(),
            archiveIndexDir.getName() + ".lock");

    final RandomAccessFile lockIs = new RandomAccessFile(lockFile, "rw");

    final FileLock lock = lockIs.getChannel().lock();

    try {
      // Uncompress archive if necessary
      if (!archiveIndexDir.exists()) {

        if (!archiveIndexDir.mkdir()) {
          throw new IOException("Can't create directory for "
              + getMapperName() + " index: " + archiveIndexDir);
        }

        getLogger().fine("Unzip archiveIndexFile "
            + archiveIndexFile + " in " + archiveIndexDir);
        FileUtils.unzip(archiveIndexFile, archiveIndexDir);
      }
    } finally {

      lock.release();
      lockIs.close();
      lockFile.delete();
    }

    FileUtils.checkExistingDirectoryFile(archiveIndexDir,
        getMapperName() + " index directory");
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapperInstance mapper instance object
   * @param archiveIndexFileInputStream archive index file input stream
   * @param indexOutputDirectory index output directory
   */
  MapperIndex(final MapperInstance mapperInstance,
      final InputStream archiveIndexFileInputStream,
      final File indexOutputDirectory) {

    requireNonNull(mapperInstance, "mapperInstance cannot be null");
    requireNonNull(archiveIndexFileInputStream,
        "archiveIndexFileInputStream cannot be null");
    requireNonNull(indexOutputDirectory, "archiveIndexDir cannot be null");

    this.mapperInstance = mapperInstance;
    this.in = archiveIndexFileInputStream;
    this.indexDirectory = indexOutputDirectory;
  }
}
