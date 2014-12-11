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

import static com.google.common.base.Preconditions.checkState;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;
import static fr.ens.transcriptome.eoulsan.util.Utils.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class abstract implements a generic Mapper.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public abstract class AbstractSequenceReadsMapper implements
    SequenceReadsMapper {

  private static final String SYNC = AbstractSequenceReadsMapper.class
      .getName();

  private File archiveIndexFile;
  private File archiveIndexDir;

  private FastqFormat fastqFormat = FastqFormat.FASTQ_SANGER;

  private String mapperVersionToUse = getDefaultPackageVersion();
  private String flavor;
  private int threadsNumber;
  private String mapperArguments = null;
  private File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();

  private ReporterIncrementer incrementer;
  private String counterGroup;

  private boolean binariesReady;
  private boolean initialized;

  //
  // Binaries management
  //

  @Override
  public boolean isIndexGeneratorOnly() {
    return false;
  }

  /**
   * Get the software package of the mapper.
   * @return the software package of the mapper
   */
  protected String getSoftwarePackage() {

    return getMapperName();
  }

  /**
   * Get the default version of the mapper.
   * @return the default version of the mapper
   */
  protected abstract String getDefaultPackageVersion();

  /**
   * Get the indexer executable.
   * @return the indexer executable
   */
  protected abstract String getIndexerExecutable();

  /**
   * Get the indexer executables.
   * @return the indexer executables
   */
  protected String[] getIndexerExecutables() {
    return new String[] {getIndexerExecutable()};
  }

  /**
   * Get the indexer command.
   * @param indexerPathname the path to the indexer
   * @param genomePathname the path to the genome
   * @return a list that is the command to execute
   */
  protected abstract List<String> getIndexerCommand(
      final String indexerPathname, final String genomePathname);

  @Override
  public String getMapperFlavorToUse() {
    return this.flavor;
  }

  @Override
  public String getMapperVersionToUse() {

    return this.mapperVersionToUse;
  }

  //
  // Getters
  //

  @Override
  public int getThreadsNumber() {

    return this.threadsNumber;
  }

  @Override
  public String getMapperArguments() {

    return this.mapperArguments;
  }

  @Override
  public List<String> getListMapperArguments() {

    if (getMapperArguments() == null) {
      return Collections.emptyList();
    }

    // Split the mapper arguments
    final String[] tabMapperArguments = getMapperArguments().trim().split(" ");

    final List<String> result = new ArrayList<>();

    // Keep only non empty arguments
    for (String arg : tabMapperArguments) {
      if (!arg.isEmpty()) {
        result.add(arg);
      }
    }

    return result;
  }

  /**
   * Get Fastq format.
   * @return the fastq format
   */
  @Override
  public FastqFormat getFastqFormat() {

    return this.fastqFormat;
  }

  @Override
  public File getTempDirectory() {

    return this.tempDir;
  }

  /**
   * Convenient method to directly get the absolute path for the temporary
   * directory.
   * @return the absolute path to the tempory directory as a string
   */
  protected String getTempDirectoryPath() {

    return getTempDirectory().getAbsolutePath();
  }

  //
  // Setters
  //

  @Override
  public void setMapperFlavorToUse(final String flavor) {

    checkState(!this.binariesReady, "Mapper has been initialized");

    this.flavor = flavor;
  }

  @Override
  public void setMapperVersionToUse(final String version) {

    checkState(!this.binariesReady, "Mapper has been initialized");

    this.mapperVersionToUse =
        version == null ? getDefaultPackageVersion() : version;
  }

  @Override
  public void setThreadsNumber(final int threadsNumber) {

    checkState(!this.initialized, "Mapper has been initialized");

    this.threadsNumber = threadsNumber;
  }

  @Override
  public void setMapperArguments(final String arguments) {

    checkState(!this.initialized, "Mapper has been initialized");

    if (arguments == null) {
      this.mapperArguments = "";
    } else {

      this.mapperArguments = arguments;
    }
  }

  @Override
  public void setTempDirectory(final File tempDirectory) {

    checkState(!this.initialized, "Mapper has been initialized");

    this.tempDir = tempDirectory;
  }

  @Override
  public void setFastqFormat(final FastqFormat format) {

    checkState(!this.initialized, "Mapper has been initialized");

    if (format == null) {
      throw new NullPointerException("The FASTQ format is null");
    }

    this.fastqFormat = format;
  }

  //
  // Index creation
  //

  private static File uncompressGenomeIfNecessary(final File genomeFile,
      final File outputDir) throws FileNotFoundException, IOException {

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(genomeFile.getName());

    if (ct == CompressionType.NONE) {
      return genomeFile;
    }

    // Define the output filename
    final File uncompressFile =
        new File(outputDir,
            StringUtils.filenameWithoutCompressionExtension(genomeFile
                .getName()));

    getLogger().fine(
        "Uncompress genome " + genomeFile + " to " + uncompressFile);

    // Create input stream
    final InputStream in =
        ct.createInputStream(FileUtils.createInputStream(genomeFile));

    // Create output stream
    final OutputStream out = FileUtils.createOutputStream(uncompressFile);

    // Uncompress
    FileUtils.copy(in, out);

    // Return the uncompress file
    return uncompressFile;
  }

  private void makeIndex(final File genomeFile, final File outputDir)
      throws IOException {

    checkNotNull(genomeFile, "genome file is null");
    checkNotNull(outputDir, "output directory is null");

    final File unCompressGenomeFile =
        uncompressGenomeIfNecessary(genomeFile, outputDir);

    getLogger().fine(
        "Start computing "
            + getMapperName() + " index for " + unCompressGenomeFile);
    final long startTime = System.currentTimeMillis();

    final String indexerPath;

    synchronized (SYNC) {
      indexerPath = install(getIndexerExecutables());
    }

    if (!outputDir.exists() && !outputDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    final File tmpGenomeFile =
        new File(outputDir, unCompressGenomeFile.getName());

    // Create temporary symbolic link for genome
    if (!unCompressGenomeFile.equals(tmpGenomeFile)) {
      if (!FileUtils.createSymbolicLink(unCompressGenomeFile, tmpGenomeFile)) {
        throw new IOException("Unable to create the symbolic link in "
            + tmpGenomeFile + " directory for " + unCompressGenomeFile);
      }
    }

    // Build the command line and compute the index
    final List<String> cmd = new ArrayList<>();
    cmd.addAll(getIndexerCommand(indexerPath, tmpGenomeFile.getAbsolutePath()));

    getLogger().fine(cmd.toString());

    final int exitValue = ProcessUtils.sh(cmd, tmpGenomeFile.getParentFile());

    if (exitValue != 0) {
      throw new IOException("Bad error result for index creation execution: "
          + exitValue);
    }

    // Remove symbolic link
    if (!tmpGenomeFile.delete()) {
      getLogger().warning(
          "Cannot remove symbolic link while after creating "
              + getMapperName() + " index");
    }

    final long endTime = System.currentTimeMillis();

    getLogger().fine(
        "Create the "
            + getMapperName() + " index in "
            + StringUtils.toTimeHumanReadable(endTime - startTime));

  }

  @Override
  public void makeArchiveIndex(final File genomeFile,
      final File archiveOutputFile) throws IOException {

    getLogger().fine("Start index computation");

    final String indexTmpDirPrefix =
        Globals.APP_NAME_LOWER_CASE
            + "-" + getMapperName().toLowerCase() + "-genomeindexdir-";

    getLogger().fine(
        "Want to create a temporary directory with prefix: "
            + indexTmpDirPrefix + " in " + getTempDirectory());

    final File indexTmpDir =
        File.createTempFile(indexTmpDirPrefix, "", getTempDirectory());

    if (!(indexTmpDir.delete())) {
      throw new IOException("Could not delete temp file ("
          + indexTmpDir.getAbsolutePath() + ")");
    }

    if (!indexTmpDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    makeIndex(genomeFile, indexTmpDir);

    // Zip index files
    FileUtils.createZip(indexTmpDir, archiveOutputFile);

    // Remove temporary directory
    FileUtils.removeDirectory(indexTmpDir);

    getLogger().fine("End index computation");
  }

  /**
   * Create the soap index in a zip archive.
   * @param is InputStream to use for the genome file
   * @throws IOException if an error occurs while creating the index
   */
  @Override
  public void makeArchiveIndex(final InputStream is,
      final File archiveOutputFile) throws IOException {

    checkNotNull(is, "Input steam is null");
    checkNotNull(archiveOutputFile, "Archive output file is null");

    getLogger().fine("Copy genome to local disk before computing index");

    final File genomeTmpFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-genome", ".fasta",
            getTempDirectory());
    FileUtils.copy(is, FileUtils.createOutputStream(genomeTmpFile));

    makeArchiveIndex(genomeTmpFile, archiveOutputFile);

    if (!genomeTmpFile.delete()) {
      getLogger().warning("Cannot delete temporary index zip file");
    }

  }

  protected String getIndexPath(final File archiveIndexDir,
      final String extension, final int extensionLength) throws IOException {

    final File[] indexFiles =
        FileUtils.listFilesByExtension(archiveIndexDir, extension);

    if (indexFiles == null || indexFiles.length != 1) {
      throw new IOException("Unable to get index file for " + getMapperName());
    }

    // Get the path to the index
    final String bwtFile = indexFiles[0].getAbsolutePath();

    return bwtFile.substring(0, bwtFile.length() - extensionLength);
  }

  private void unzipArchiveIndexFile(final File archiveIndexFile,
      final File archiveIndexDir) throws IOException {

    if (!archiveIndexFile.exists()) {
      throw new IOException("No index for the mapper found: "
          + archiveIndexFile);
    }

    // Uncompress archive if necessary
    if (!archiveIndexDir.exists()) {

      if (!archiveIndexDir.mkdir()) {
        throw new IOException("Can't create directory for "
            + getMapperName() + " index: " + archiveIndexDir);
      }

      getLogger().fine(
          "Unzip archiveIndexFile "
              + archiveIndexFile + " in " + archiveIndexDir);
      FileUtils.unzip(archiveIndexFile, archiveIndexDir);
    }

    FileUtils.checkExistingDirectoryFile(archiveIndexDir, getMapperName()
        + " index directory");

  }

  //
  // Mapping with File
  //

  @Override
  public final void mapPE(final File readsFile1, final File readsFile2,
      final GenomeDescription gd, final File samFile) throws IOException {

    FileUtils.copy(mapPE(readsFile1, readsFile2, gd), new FileOutputStream(
        samFile));
  }

  @Override
  public final InputStream mapPE(final File readsFile1, final File readsFile2,
      final GenomeDescription gd) throws IOException {

    getLogger().fine("Mapping with " + getMapperName() + " in pair-end mode");

    checkNotNull(readsFile1, "readsFile1 is null");
    checkNotNull(readsFile2, "readsFile2 is null");

    checkExistingStandardFile(readsFile1,
        "readsFile1 not exits or is not a standard file.");
    checkExistingStandardFile(readsFile2,
        "readsFile2 not exits or is not a standard file.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFile, this.archiveIndexDir);

    // Process to mapping
    return internalMapPE(readsFile1, readsFile2, this.archiveIndexDir, gd);
  }

  @Override
  public final void mapSE(final File readsFile, final GenomeDescription gd,
      final File samFile) throws IOException {

    FileUtils.copy(mapSE(readsFile, gd), new FileOutputStream(samFile));
  }

  @Override
  public final InputStream mapSE(final File readsFile,
      final GenomeDescription gd) throws IOException {

    getLogger().fine("Mapping with " + getMapperName() + " in single-end mode");

    checkNotNull(readsFile, "readsFile1 is null");
    checkExistingStandardFile(readsFile,
        "readsFile1 not exits or is not a standard file.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFile, this.archiveIndexDir);

    // Process to mapping
    return internalMapSE(readsFile, this.archiveIndexDir, gd);
  }

  protected abstract InputStream internalMapPE(final File readsFile1,
      final File readsFile2, final File archiveIndex, final GenomeDescription gd)
      throws IOException;

  protected abstract InputStream internalMapSE(final File readsFile,
      final File archiveIndex, final GenomeDescription gd) throws IOException;

  //
  // Mapping with streams
  //

  @Override
  public final MapperProcess mapPE(final GenomeDescription gd)
      throws IOException {

    getLogger().fine("Mapping with " + getMapperName() + " in single-end mode");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFile, this.archiveIndexDir);

    // Process to mapping
    final MapperProcess result = internalMapPE(this.archiveIndexDir, gd);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    return result;
  }

  @Override
  public final MapperProcess mapSE(final GenomeDescription gd)
      throws IOException {

    getLogger().fine("Mapping with " + getMapperName() + " in single-end mode");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFile, this.archiveIndexDir);

    // Process to mapping
    final MapperProcess result = internalMapSE(this.archiveIndexDir, gd);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    return result;
  }

  protected abstract MapperProcess internalMapPE(final File archiveIndex,
      final GenomeDescription gd) throws IOException;

  protected abstract MapperProcess internalMapSE(final File archiveIndex,
      final GenomeDescription gd) throws IOException;

  //
  // Init
  //

  /**
   * Check if the mapper flavor exists.
   */
  protected boolean checkIfFlavorExists() {
    return true;
  }

  @Override
  public void prepareBinaries() throws IOException {

    // Do nothing if binaries has already been prepared
    if (this.binariesReady) {
      return;
    }

    if (!checkIfBinaryExists(getIndexerExecutables())) {
      throw new IOException("Unable to find mapper "
          + getMapperName() + " version " + this.mapperVersionToUse
          + " (flavor: " + this.flavor == null ? "" : this.flavor + ")");
    }

    if (!checkIfFlavorExists()) {
      throw new IOException("Unable to find mapper "
          + getMapperName() + " flavor " + this.flavor + " for version "
          + this.mapperVersionToUse);
    }

    this.binariesReady = true;
  }

  @Override
  public void init(final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    checkState(!this.initialized, "Mapper has already been initialized");

    checkNotNull(incrementer, "incrementer is null");
    checkNotNull(counterGroup, "counterGroup is null");

    checkNotNull(archiveIndexFile, "archiveIndex is null");
    checkNotNull(archiveIndexDir, "archiveIndexDir is null");
    checkExistingStandardFile(archiveIndexFile,
        "The archive index file not exits or is not a standard file.");

    this.archiveIndexFile = archiveIndexFile;
    this.archiveIndexDir = archiveIndexDir;
    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    this.initialized = true;

    // Prepare binaries
    prepareBinaries();
  }

  //
  // Utilities methods
  //

  /**
   * Install a list of binaries bundled in the jar in a temporary directory.
   * This method automatically use the temporary directory defined in the object
   * for the path where to install the binary.
   * @param binaryFilenames programs to install
   * @return a string with the path of the last installed binary
   * @throws IOException if an error occurs while installing binary
   */
  protected String install(final String... binaryFilenames) throws IOException {

    String result = null;

    if (binaryFilenames != null) {
      for (String binaryFilename : binaryFilenames) {
        result = install(binaryFilename);
      }
    }

    return result;
  }

  /**
   * Install a binary bundled in the jar in a temporary directory. This method
   * automatically use the temporary directory defined in the object for the
   * path where to install the binary.
   * @param binaryFilename program to install
   * @return a string with the path of the installed binary
   * @throws IOException if an error occurs while installing binary
   */
  protected String install(final String binaryFilename) throws IOException {

    return BinariesInstaller.install(getSoftwarePackage(),
        this.mapperVersionToUse, binaryFilename, getTempDirectoryPath());
  }

  /**
   * Check if binaries bundled in the jar exists.
   * @param binaryFilenames program to check
   * @return true if the binary exists
   * @throws IOException if an error occurs while installing binary
   */
  protected boolean checkIfBinaryExists(final String... binaryFilenames) {

    if (binaryFilenames == null || binaryFilenames.length == 0) {
      return false;
    }

    for (String binaryFilename : binaryFilenames) {
      if (!checkIfBinaryExists(binaryFilename)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if a binary bundled in the jar exists.
   * @param binaryFilename program to check
   * @return true if the binary exists
   * @throws IOException if an error occurs while installing binary
   */
  protected boolean checkIfBinaryExists(final String binaryFilename) {

    return BinariesInstaller.check(getSoftwarePackage(),
        this.mapperVersionToUse, binaryFilename);
  }

}
