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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperExecutor.Result;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define a mapper instance.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class MapperInstance {

  private static final String SYNC = MapperInstance.class.getName();

  private final Mapper mapper;
  private final MapperExecutor executor;

  private final String version;
  private final String flavor;
  private final File temporaryDirectory;

  private boolean mapperInstalled;

  //
  // Getters
  //

  /**
   * Get the mapper version.
   * @return a string with the version of the mapper
   */
  public String getName() {
    return this.mapper.getName();
  }

  /**
   * Get the mapper version.
   * @return a string with the version of the mapper
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Get the mapper flavor.
   * @return a string with the flavor of the mapper
   */
  public String getFlavor() {
    return this.flavor;
  }

  /**
   * Get the temporary directory to use by the mapper.
   * @return the temporary directory to use by the mapper
   */
  public File getTemporaryDirectory() {
    return this.temporaryDirectory;
  }

  /**
   * Get the mapper object.
   * @return the mapper object
   */
  public Mapper getMapper() {
    return this.mapper;
  }

  /**
   * Get the provider.
   * @return the provider
   */
  private MapperProvider getProvider() {
    return this.mapper.getProvider();
  }

  /**
   * Get the executor.
   * @return the executor
   */
  public MapperExecutor getExecutor() {
    return this.executor;
  }

  //
  // Index creation methods
  //

  /**
   * Create the mapper index for a genome in a zip archive.
   * @param genomeIs InputStream to use for the genome file
   * @param indexerArguments indexer arguments
   * @param threads thread number to use
   * @throws IOException if an error occurs while creating the index
   */
  public void makeArchiveIndex(final InputStream genomeIs,
      final File archiveOutputFile, final String indexerArguments,
      final int threads) throws IOException {

    makeArchiveIndex(genomeIs, archiveOutputFile,
        MapperUtils.argumentsAsList(indexerArguments), threads);
  }

  /**
   * Create the mapper index for a genome in a zip archive.
   * @param genomeIs InputStream to use for the genome file
   * @param indexerArguments indexer arguments
   * @param threads thread number to use
   * @throws IOException if an error occurs while creating the index
   */
  public void makeArchiveIndex(final InputStream genomeIs,
      final File archiveOutputFile, final List<String> indexerArguments,
      final int threads) throws IOException {

    requireNonNull(genomeIs, "Input steam is null");
    requireNonNull(archiveOutputFile, "Archive output file is null");

    getLogger().fine("Copy genome to local disk before computing index");

    final File genomeTmpFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-genome", ".fasta",
            getTemporaryDirectory());
    FileUtils.copy(genomeIs, FileUtils.createOutputStream(genomeTmpFile));

    makeArchiveIndex(genomeTmpFile, archiveOutputFile, indexerArguments,
        threads);

    if (!genomeTmpFile.delete()) {
      getLogger().warning("Cannot delete temporary index zip file");
    }

  }

  /**
   * Create the mapper index for a genome in a zip archive.
   * @param genomeFile the genome file
   * @param indexerArguments indexer arguments
   * @param threads thread number to use
   * @throws IOException if an error occurs while creating the index
   */
  public void makeArchiveIndex(final File genomeFile,
      final File archiveOutputFile, final String indexerArguments,
      final int threads) throws IOException {

    makeArchiveIndex(genomeFile, archiveOutputFile,
        MapperUtils.argumentsAsList(indexerArguments), threads);
  }

  /**
   * Create the mapper index for a genome in a zip archive.
   * @param genomeFile the genome file
   * @param indexerArguments indexer arguments
   * @param threads thread number to use
   * @throws IOException if an error occurs while creating the index
   */
  public void makeArchiveIndex(final File genomeFile,
      final File archiveOutputFile, final List<String> indexerArguments,
      final int threads) throws IOException {

    getLogger().fine("Start index computation");

    final String indexTmpDirPrefix = Globals.APP_NAME_LOWER_CASE
        + "-" + this.mapper.getName().toLowerCase() + "-genomeindexdir-";

    getLogger().fine("Want to create a temporary directory with prefix: "
        + indexTmpDirPrefix + " in " + getTemporaryDirectory());

    final File indexCreationDir =
        Files.createTempDirectory(getTemporaryDirectory().toPath(),
            indexTmpDirPrefix).toFile();

    // Uncompress genome file if required
    final File unCompressedGenomeFile =
        uncompressGenomeIfNecessary(genomeFile, indexCreationDir);

    // Define log output files
    File outputDir = archiveOutputFile.getParentFile();
    String basename = StringUtils.basename(archiveOutputFile.getName());
    File stdoutFile = new File(outputDir, basename + ".out");
    File stderrFile = new File(outputDir, basename + ".err");

    // Compute index
    computeIndex(unCompressedGenomeFile, indexCreationDir, indexerArguments,
        threads, stdoutFile, stderrFile);

    // Zip index files
    FileUtils.createZip(indexCreationDir, archiveOutputFile,
        !this.mapper.isCompressIndex());

    // Remove temporary directory
    FileUtils.removeDirectory(indexCreationDir);

    getLogger().fine("End index computation");
  }

  /**
   * Compute the index.
   * @param genomeFile the genome file
   * @param outputDir the output directory
   * @param indexerArguments the index arguments
   * @param threads the number of threads to use
   * @param stdErrorFile standard output file
   * @param stdErrorFile standard error file
   * @throws IOException if an error occurs while creating the index
   */
  private void computeIndex(final File genomeFile, final File outputDir,
      final List<String> indexerArguments, final int threads,
      final File stdOutFile, final File stdErrorFile) throws IOException {

    requireNonNull(genomeFile, "genome file is null");
    requireNonNull(outputDir, "output directory is null");

    getLogger().fine("Start computing "
        + this.mapper.getName() + " index for " + genomeFile);

    final long startTime = System.currentTimeMillis();

    final File indexer = new File(installIndexer());

    if (!outputDir.exists() && !outputDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    final File tmpGenomeFile = new File(outputDir, genomeFile.getName());

    // Create temporary symbolic link for genome
    if (!genomeFile.equals(tmpGenomeFile)) {

      try {

        Files.createSymbolicLink(tmpGenomeFile.toPath(),
            genomeFile.getAbsoluteFile().toPath());
      } catch (IOException e) {
        throw new IOException("Unable to create the symbolic link in "
            + tmpGenomeFile + " directory for " + genomeFile);
      }
    }

    // Build the command line and compute the index
    final List<String> cmd = new ArrayList<>(this.mapper.getProvider()
      .getIndexerCommand(indexer, tmpGenomeFile, indexerArguments, threads));

    getLogger().fine(cmd.toString());

    final Result result =
        this.executor.execute(cmd, tmpGenomeFile.getParentFile(), false,
            stdErrorFile, false, genomeFile, tmpGenomeFile);

    // Create stdout file
    if (stdOutFile != null) {
      FileUtils.copy(result.getInputStream(), new FileOutputStream(stdOutFile));
    }

    final int exitValue = result.waitFor();

    if (exitValue != 0) {
      throw new IOException(
          "Bad error result for index creation execution: " + exitValue);
    }

    // Remove symbolic link
    if (!tmpGenomeFile.delete()) {
      getLogger().warning("Cannot remove symbolic link while after creating "
          + this.mapper.getName() + " index");
    }

    final long endTime = System.currentTimeMillis();

    getLogger().fine("Create the "
        + this.mapper.getName() + " index in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  /**
   * Uncompress genome file if necessary.
   * @param genomeFile the genome file
   * @param outputDir the output directory
   * @return the path to the uncompressed file
   * @throws IOException if an error occurs while uncompressing the genome file
   */
  private static File uncompressGenomeIfNecessary(final File genomeFile,
      final File outputDir) throws IOException {

    final CompressionType ct =
        CompressionType.getCompressionTypeByFilename(genomeFile.getName());

    if (ct == CompressionType.NONE) {
      return genomeFile;
    }

    // Define the output filename
    final File uncompressFile = new File(outputDir,
        StringUtils.filenameWithoutCompressionExtension(genomeFile.getName()));

    getLogger()
        .fine("Uncompress genome " + genomeFile + " to " + uncompressFile);

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

  //
  // Execution methods
  //

  /**
   * Install the indexer.
   * @throws IOException if an error occurs while installing the indexer
   */
  private String installIndexer() throws IOException {

    String result = null;

    synchronized (SYNC) {

      for (String indexer : getProvider().getIndexerExecutables(this)) {

        result = this.executor.install(indexer);
      }
    }

    if (result == null) {
      throw new IOException(
          "No indexer executable found for mapper: " + getMapper().getName());
    }

    return result;
  }

  /**
   * Install the mapper.
   * @throws IOException if an error occurs while installing the mapper
   */
  private void installMapper() throws IOException {

    if (this.mapperInstalled) {
      return;
    }

    synchronized (SYNC) {

      this.executor.install(getProvider().getMapperExecutableName(this));
      this.mapperInstalled = true;
    }
  }

  //
  // Check methods
  //

  /**
   * Check if binaries bundled in the jar exists.
   * @param executor mapper executor
   * @param binaryFilenames program to check
   * @return true if the binary exists
   */
  private boolean checkIfBinaryExists(final MapperExecutor executor,
      final List<String> binaryFilenames) throws IOException {

    if (binaryFilenames == null || binaryFilenames.isEmpty()) {
      return false;
    }

    for (String binaryFilename : binaryFilenames) {
      if (!checkIfBinaryExists(executor, binaryFilename)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if a binary bundled in the jar exists.
   * @param executor mapper executor
   * @param binaryFilename program to check
   * @return true if the binary exists
   */
  private boolean checkIfBinaryExists(final MapperExecutor executor,
      final String binaryFilename) throws IOException {

    return executor.isExecutable(binaryFilename);
  }

  private void checkMapperBinaries() throws IOException {

    // Check if indexer binary exists
    if (!checkIfBinaryExists(this.executor,
        getProvider().getIndexerExecutables(this))) {
      throw new IOException("Unable to find mapper "
          + this.mapper.getName() + " version " + this.version + " (flavor: "
          + (this.flavor == null ? "not defined" : this.flavor) + ")");

    }

    // Check if mapper binary exists
    if (!checkIfBinaryExists(this.executor,
        getProvider().getMapperExecutableName(this))) {
      throw new IOException("Unable to find mapper "
          + this.mapper.getName() + " version " + version + " (flavor: "
          + (this.flavor == null ? "not defined" : this.flavor) + ")");

    }

    // Check if flavor binary exists
    if (!getProvider().checkIfFlavorExists(this)) {
      throw new IOException("Unable to find mapper "
          + this.mapper.getName() + " version " + version + " (flavor: "
          + (this.flavor == null ? "not defined" : this.flavor) + ")");
    }

  }

  public String getBinaryVersion() {

    if (!this.mapperInstalled) {

    }

    return getProvider().readBinaryVersion(this);
  }

  //
  // MaperIndexArchive creation
  //

  /**
   * Create an instance of MapperIndex.
   * @param archiveIndexFile archive index file
   * @param indexOutputDir index output directory
   * @return a new instance of MapperIndexArchive
   * @throws IOException if an error occurs while installing the mapper
   */
  public MapperIndex newMapperIndex(final File archiveIndexFile,
      final File indexOutputDir) throws IOException {

    return new MapperIndex(this, new FileInputStream(archiveIndexFile),
        indexOutputDir);
  }

  /**
   * Create an instance of MapperIndex.
   * @param in archive index file input stream
   * @param indexOutputDir index output directory
   * @return a new instance of MapperIndexArchive
   * @throws IOException if an error occurs while installing the mapper
   */
  public MapperIndex newMapperIndex(final InputStream in,
      final File indexOutputDir) throws IOException {

    // Install the mapper
    installMapper();

    return new MapperIndex(this, in, indexOutputDir);
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param mapper the mapper
   * @param executor the executor
   * @param version the version of the mapper to use
   * @param flavor the flavor of the mapper to use
   * @param temporaryDirectory the temporary directory
   * @throws IOException if an error occurs while checking the mapper
   */
  MapperInstance(Mapper mapper, MapperExecutor executor, final String version,
      final String flavor, final File temporaryDirectory) throws IOException {

    requireNonNull(mapper, "mapper cannot be null");
    requireNonNull(executor, "executor cannot be null");
    requireNonNull(temporaryDirectory, "temporaryDirectory cannot be null");

    this.mapper = mapper;
    this.executor = executor;
    this.version = version;
    this.flavor = flavor;
    this.temporaryDirectory = temporaryDirectory;

    getLogger().fine("Use executor: " + this.executor);

    // Check mapper binaries
    checkMapperBinaries();
  }

}
