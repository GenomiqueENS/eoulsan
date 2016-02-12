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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import static com.google.common.base.Preconditions.checkState;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.util.FileUtils.checkExistingStandardFile;
import static fr.ens.biologie.genomique.eoulsan.util.Utils.checkNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Strings;
import com.spotify.docker.client.DockerClient;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.bio.FastqFormat;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.bio.io.FastqReader;
import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.MapperExecutor.Result;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class abstract implements a generic Mapper.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public abstract class AbstractSequenceReadsMapper
    implements SequenceReadsMapper {

  private static final String SYNC =
      AbstractSequenceReadsMapper.class.getName();

  static final String SHORT_INDEX_FLAVOR = "standard";
  static final String LARGE_INDEX_FLAVOR = "large-index";
  static final String DEFAULT_FLAVOR = SHORT_INDEX_FLAVOR;

  private InputStream archiveIndexFileInputStream;
  private File archiveIndexDir;

  private FastqFormat fastqFormat = FastqFormat.FASTQ_SANGER;

  private String mapperVersionToUse = getDefaultPackageVersion();
  private String flavorToUse = DEFAULT_FLAVOR;
  private String flavor = DEFAULT_FLAVOR;
  private boolean useBundledBinaries = true;
  private String mapperDockerImage = "";
  private int threadsNumber;
  private String mapperArguments = null;
  private String indexerArguments = null;
  private File tempDir = EoulsanRuntime.getSettings().getTempDirectoryFile();
  private boolean multipleInstancesEnabled;
  private DockerClient dockerClient;

  private ReporterIncrementer incrementer;
  private String counterGroup;

  private boolean binariesReady;
  private boolean initialized;
  private IOException mappingException;
  private MapperExecutor executor;

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
    return this.flavorToUse;
  }

  @Override
  public String getMapperVersionToUse() {

    return this.mapperVersionToUse;
  }

  @Override
  public String getMapperFlavor() {

    checkIfFlavorExists();
    return this.flavor;
  }

  @Override
  public boolean isUseBundledBinaries() {

    return this.useBundledBinaries;
  }

  @Override
  public String getMapperDockerImage() {

    return this.mapperDockerImage;
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
  public String getIndexerArguments() {

    return this.indexerArguments;
  }

  /**
   * Get the default mapper arguments.
   * @return the default mapper arguments
   */
  protected abstract String getDefaultMapperArguments();

  @Override
  public List<String> getListMapperArguments() {

    return getListArguments(getMapperArguments());
  }

  @Override
  public List<String> getListIndexerArguments() {

    return getListArguments(getIndexerArguments());
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

  @Override
  public boolean isMultipleInstancesAllowed() {

    return false;
  }

  @Override
  public boolean isMultipleInstancesEnabled() {

    return this.multipleInstancesEnabled;
  }

  /**
   * Convenient method to directly get the absolute path for the temporary
   * directory.
   * @return the absolute path to the temporary directory as a string
   */
  protected String getTempDirectoryPath() {

    return getTempDirectory().getAbsolutePath();
  }

  @Override
  public DockerClient getDockerClient() {

    return this.dockerClient;
  }

  /**
   * Get mapper executor.
   * @return the mapper executor
   */
  protected MapperExecutor getExecutor() {

    return this.executor;
  }

  //
  // Setters
  //

  @Override
  public void setMapperFlavorToUse(final String flavor) {

    checkState(!this.binariesReady, "Mapper has been initialized");

    this.flavorToUse = Strings.emptyToNull(flavor);
  }

  @Override
  public void setMapperVersionToUse(final String version) {

    checkState(!this.binariesReady, "Mapper has been initialized");

    this.mapperVersionToUse = Strings.emptyToNull(version) == null
        ? getDefaultPackageVersion() : version;
  }

  @Override
  public void setUseBundledBinaries(final boolean use) {

    checkState(!this.binariesReady, "Mapper has been initialized");

    this.useBundledBinaries = use;
  }

  @Override
  public void setMapperDockerImage(final String dockerImage) {

    checkState(!this.binariesReady, "Mapper has been initialized");

    if (dockerImage == null) {
      this.mapperDockerImage = "";
    } else {
      this.mapperDockerImage = dockerImage.trim();
    }
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
  public void setIndexerArguments(final String arguments) {

    if (arguments == null) {
      this.indexerArguments = "";
    } else {

      this.indexerArguments = arguments;
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

  /**
   * Set the "real" flavor of the mapper.
   * @param flavor the flavor to set
   */
  protected void setFlavor(final String flavor) {

    checkState(!this.initialized, "Mapper has been initialized");

    if (flavor != null) {
      this.flavor = flavor;
    }
  }

  @Override
  public void setMultipleInstancesEnabled(final boolean enable) {

    checkState(!this.initialized, "Mapper has been initialized");

    if (isMultipleInstancesAllowed() && enable == true) {
      this.multipleInstancesEnabled = true;
    } else {
      this.multipleInstancesEnabled = false;
    }
  }

  @Override
  public void setDockerClient(final DockerClient dockerClient) {

    checkState(!this.initialized, "Mapper has been initialized");

    this.dockerClient = dockerClient;
  }

  //
  // Get mapper version
  //

  @Override
  public final String getMapperVersion() {

    // Prepare binaries
    try {
      prepareBinaries();
    } catch (IOException e) {
      return null;
    }

    return internalGetMapperVersion();
  }

  /**
   * Get mapper version.
   * @return a string with the version of the mapper
   */
  protected abstract String internalGetMapperVersion();

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

  private void makeIndex(final File genomeFile, final File outputDir)
      throws IOException {

    checkNotNull(genomeFile, "genome file is null");
    checkNotNull(outputDir, "output directory is null");

    final File unCompressedGenomeFile =
        uncompressGenomeIfNecessary(genomeFile, outputDir);

    getLogger().fine("Start computing "
        + getMapperName() + " index for " + unCompressedGenomeFile);

    final long startTime = System.currentTimeMillis();

    final String indexerPath;

    synchronized (SYNC) {
      indexerPath = install(getIndexerExecutables());
    }

    if (!outputDir.exists() && !outputDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    final File tmpGenomeFile =
        new File(outputDir, unCompressedGenomeFile.getName());

    // Create temporary symbolic link for genome
    if (!unCompressedGenomeFile.equals(tmpGenomeFile)) {

      try {

        Files.createSymbolicLink(tmpGenomeFile.toPath(),
            unCompressedGenomeFile.toPath());
      } catch (IOException e) {
        throw new IOException("Unable to create the symbolic link in "
            + tmpGenomeFile + " directory for " + unCompressedGenomeFile);
      }
    }

    // Build the command line and compute the index
    final List<String> cmd = new ArrayList<>();
    cmd.addAll(getIndexerCommand(indexerPath, tmpGenomeFile.getAbsolutePath()));

    getLogger().fine(cmd.toString());

    final int exitValue =
        this.executor.execute(cmd, tmpGenomeFile.getParentFile(), false, false,
            unCompressedGenomeFile, tmpGenomeFile).waitFor();

    if (exitValue != 0) {
      throw new IOException(
          "Bad error result for index creation execution: " + exitValue);
    }

    // Remove symbolic link
    if (!tmpGenomeFile.delete()) {
      getLogger().warning("Cannot remove symbolic link while after creating "
          + getMapperName() + " index");
    }

    final long endTime = System.currentTimeMillis();

    getLogger().fine("Create the "
        + getMapperName() + " index in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));
  }

  @Override
  public void makeArchiveIndex(final File genomeFile,
      final File archiveOutputFile) throws IOException {

    getLogger().fine("Start index computation");

    final String indexTmpDirPrefix = Globals.APP_NAME_LOWER_CASE
        + "-" + getMapperName().toLowerCase() + "-genomeindexdir-";

    getLogger().fine("Want to create a temporary directory with prefix: "
        + indexTmpDirPrefix + " in " + getTempDirectory());

    final File indexTmpDir = Files
        .createTempDirectory(getTempDirectory().toPath(), indexTmpDirPrefix)
        .toFile();

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

    final File genomeTmpFile = File.createTempFile(
        Globals.APP_NAME_LOWER_CASE + "-genome", ".fasta", getTempDirectory());
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

    if (indexFiles == null || indexFiles.length == 0) {
      throw new IOException("Unable to get index file for "
          + getMapperName() + " with \"" + extension
          + "\" extension in directory: " + archiveIndexDir);
    }

    if (indexFiles.length > 1) {
      throw new IOException("More than one index file for "
          + getMapperName() + " with \"" + extension
          + "\" extension in directory: " + archiveIndexDir);
    }

    // Get the path to the index
    final String bwtFile = indexFiles[0].getAbsolutePath();

    return bwtFile.substring(0, bwtFile.length() - extensionLength);
  }

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
    } catch (IOException e) {
      throw e;
    } finally {

      lock.release();
      lockIs.close();
      lockFile.delete();
    }

    FileUtils.checkExistingDirectoryFile(archiveIndexDir,
        getMapperName() + " index directory");
  }

  //
  // Mapping with File
  //

  @Override
  public final MapperProcess mapSE(final DataFile readsFile)
      throws IOException {

    checkNotNull(readsFile, "readsFile is null");

    if (!readsFile.exists()) {
      throw new IOException("readsFile1 not exits");
    }

    getLogger().fine("FASTQ file to map: " + readsFile);

    return mapSE(readsFile.open());
  }

  @Override
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
  private final MapperProcess mapSE(final InputStream in) throws IOException {

    checkNotNull(in, "in argument is null");

    // Check if the mapper has been initialized
    checkState(this.initialized, "Mapper has not been initialized");

    getLogger().fine("Mapping with " + getMapperName() + " in single-end mode");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFileInputStream,
        this.archiveIndexDir);

    // Process to mapping
    final MapperProcess mapperProcess = mapSE();

    // Copy reads file to named pipe
    writeFirstPairEntries(in, mapperProcess);

    return mapperProcess;
  }

  @Override
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

  @Override
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
  private final MapperProcess mapPE(final InputStream in1,
      final InputStream in2) throws IOException {

    checkNotNull(in1, "in1 argument is null");
    checkNotNull(in2, "in2 argument is null");

    // Check if the mapper has been initialized
    checkState(this.initialized, "Mapper has not been initialized");

    getLogger().fine("Mapping with " + getMapperName() + " in pair-end mode");

    checkNotNull(in1, "readsFile1 is null");
    checkNotNull(in2, "readsFile2 is null");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFileInputStream,
        this.archiveIndexDir);

    // Process to mapping
    final MapperProcess mapperProcess = mapPE();

    // Copy reads files to named pipes
    writeFirstPairEntries(in1, mapperProcess);
    writeSecondPairEntries(in2, mapperProcess);

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

  @Override
  public void throwMappingException() throws IOException {

    if (this.mappingException != null) {
      throw this.mappingException;
    }
  }

  //
  // Mapping with streams
  //

  @Override
  public final MapperProcess mapPE() throws IOException {

    // Check if the mapper has been initialized
    checkState(this.initialized, "Mapper has not been initialized");

    getLogger().fine("Mapping with " + getMapperName() + " in single-end mode");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFileInputStream,
        this.archiveIndexDir);

    // Process to mapping
    final MapperProcess result = internalMapPE(this.archiveIndexDir);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    return result;
  }

  @Override
  public final MapperProcess mapSE() throws IOException {

    // Check if the mapper has been initialized
    checkState(this.initialized, "Mapper has not been initialized");

    getLogger().fine("Mapping with " + getMapperName() + " in single-end mode");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(this.archiveIndexFileInputStream,
        this.archiveIndexDir);

    // Process to mapping
    final MapperProcess result = internalMapSE(this.archiveIndexDir);

    // Set counter
    result.setIncrementer(this.incrementer, this.counterGroup);

    return result;
  }

  protected abstract MapperProcess internalMapPE(final File archiveIndex)
      throws IOException;

  protected abstract MapperProcess internalMapSE(final File archiveIndex)
      throws IOException;

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

    // Set the executor to use
    if (!this.mapperDockerImage.isEmpty() && this.dockerClient != null) {
      this.executor = new DockerMapperExecutor(getDockerClient(),
          getMapperDockerImage(), getTempDirectory());
    } else if (isUseBundledBinaries()) {
      this.executor = new BundledMapperExecutor(getSoftwarePackage(),
          getMapperVersionToUse(), getTempDirectory());
    } else {
      this.executor = new PathMapperExecutor();
    }

    getLogger().fine("Use executor: " + this.executor);

    if (!checkIfBinaryExists(getIndexerExecutables())) {
      throw new IOException("Unable to find mapper "
          + getMapperName() + " version " + this.mapperVersionToUse
          + " (flavor: "
          + (this.flavorToUse == null ? "not defined" : this.flavorToUse)
          + ")");

    }

    if (!checkIfFlavorExists()) {
      throw new IOException("Unable to find mapper "
          + getMapperName() + " version " + this.mapperVersionToUse
          + " (flavor: "
          + (this.flavorToUse == null ? "not defined" : this.flavorToUse)
          + ")");
    }

    this.binariesReady = true;
  }

  @Override
  public void init(final DataFile archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
          throws IOException {

    checkNotNull(archiveIndexFile, "archiveIndexFile is null");

    if (!archiveIndexFile.exists()) {
      throw new IOException("The archive index file not exits");
    }

    getLogger().fine("Mapper index archive file: " + archiveIndexFile);

    init(archiveIndexFile.open(), archiveIndexDir, incrementer, counterGroup);
  }

  @Override
  public void init(final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
          throws IOException {

    checkNotNull(archiveIndexFile, "archiveIndexFile is null");

    checkExistingStandardFile(archiveIndexFile,
        "The archive index file not exits or is not a standard file");

    getLogger().fine("Mapper index archive file: " + archiveIndexFile);

    init(new FileInputStream(archiveIndexFile), archiveIndexDir, incrementer,
        counterGroup);
  }

  @Override
  public void init(final InputStream archiveIndexInputStream,
      final File archiveIndexDir, final ReporterIncrementer incrementer,
      final String counterGroup) throws IOException {

    checkState(!this.initialized, "Mapper has already been initialized");

    checkNotNull(incrementer, "incrementer is null");
    checkNotNull(counterGroup, "counterGroup is null");

    checkNotNull(archiveIndexInputStream, "archiveIndexInputStream is null");
    checkNotNull(archiveIndexDir, "archiveIndexDir is null");

    this.archiveIndexFileInputStream = archiveIndexInputStream;
    this.archiveIndexDir = archiveIndexDir;
    this.incrementer = incrementer;
    this.counterGroup = counterGroup;

    // Prepare binaries
    prepareBinaries();

    this.initialized = true;
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

    return this.executor.install(binaryFilename);
  }

  /**
   * Check if binaries bundled in the jar exists.
   * @param binaryFilenames program to check
   * @return true if the binary exists
   */
  protected boolean checkIfBinaryExists(final String... binaryFilenames)
      throws IOException {

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
   */
  protected boolean checkIfBinaryExists(final String binaryFilename)
      throws IOException {

    return this.executor.isExecutable(binaryFilename);
  }

  /**
   * Convert a string that contains a list of arguments to a list of strings.
   * @param s the string to convert
   * @return a list of string
   */
  private static final List<String> getListArguments(final String s) {

    if (s == null) {
      return Collections.emptyList();
    }

    // Split the mapper arguments
    final String[] tabMapperArguments = s.trim().split(" ");

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
   * Execute a command and get its output.
   * @param command the command to execute
   * @return a string with the output
   * @throws IOException if an error occurs while executing the command
   */
  protected String executeToString(final List<String> command)
      throws IOException {

    final Result result = this.executor.execute(command, null, true, true);

    final StringBuilder sb = new StringBuilder();

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(result.getInputStream()))) {

      String line;

      while ((line = reader.readLine()) != null) {
        sb.append(line);
        sb.append('\n');
      }
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   */
  protected AbstractSequenceReadsMapper() {

    setMapperArguments(getDefaultMapperArguments());
  }

}
