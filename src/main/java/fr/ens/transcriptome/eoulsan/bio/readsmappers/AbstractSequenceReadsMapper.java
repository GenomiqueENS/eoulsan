package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.checkExistingStandardFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.UnSynchronizedBufferedWriter;

/**
 * This class abstract implements a generic Mapper.
 * @author Laurent Jourdren
 * @author Maria Bernard
 */
public abstract class AbstractSequenceReadsMapper implements
    SequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String SYNC =
      AbstractSequenceReadsMapper.class.getName();

  protected abstract String getIndexerExecutable();

  protected abstract String getIndexerCommand(final String indexerPathname,
      final String genomePathname);

  private File readsFile1;
  private File readsFile2;

  private UnSynchronizedBufferedWriter readsWriter1;
  private UnSynchronizedBufferedWriter readsWriter2;

  private boolean noReadWritten = true;
  private boolean pairEnd = false;

  private int threadsNumber;
  private String mapperArguments;

  private int entriesWritten;

  private ReporterIncrementer incrementer;
  private String counterGroup;

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

  /**
   * Test if the mapper is in pair end mode.
   * @return true if the mapper is in pair end mode
   */
  public boolean isPairEnd() {

    return this.pairEnd;
  }

  //
  // Setters
  //

  @Override
  public void setThreadsNumber(final int threadsNumber) {

    this.threadsNumber = threadsNumber;
  }

  @Override
  public void setMapperArguments(final String arguments) {

    if (arguments == null) {
      this.mapperArguments = "";
    } else {

      this.mapperArguments = arguments;
    }
  }

  //
  // Index creation
  //

  private void makeIndex(final File genomeFile, final File outputDir)
      throws IOException {

    checkNotNull(genomeFile, "genome file is null");
    checkNotNull(outputDir, "output directory is null");

    LOGGER.info("Start computing "
        + getMapperName() + " index for " + genomeFile);
    final long startTime = System.currentTimeMillis();

    final String indexerPath;

    synchronized (SYNC) {
      indexerPath = BinariesInstaller.install(getIndexerExecutable());
    }

    if (!outputDir.exists() && !outputDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    final File tmpGenomeFile = new File(outputDir, genomeFile.getName());

    // Create temporary symbolic link for genome
    FileUtils.createSymbolicLink(genomeFile, tmpGenomeFile);

    // Compute the index
    // ProcessUtils.exec(getIndexerCommand(indexerPath, tmpGenomeFile
    // .getAbsolutePath(), null), EoulsanRuntime.getSettings().isDebug());

    // Build the command line
    final String cmd =
        getIndexerCommand(indexerPath, tmpGenomeFile.getAbsolutePath())
            + " > /dev/null 2> /dev/null";

    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for index creation execution: "
          + exitValue);
    }

    // Remove symbolic link
    if (!tmpGenomeFile.delete()) {
      LOGGER.warning("Cannot remove symbolic link while after creating "
          + getMapperName() + " index");
    }

    final long endTime = System.currentTimeMillis();

    LOGGER.info("Create the "
        + getMapperName() + " index in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));

  }

  @Override
  public void makeArchiveIndex(final File genomeFile,
      final File archiveOutputFile) throws IOException {

    LOGGER.info("Start index computation");

    final File tmpDir =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE
            + "-" + getMapperName().toLowerCase() + "-genomeindexdir-", "");

    if (!(tmpDir.delete())) {
      throw new IOException("Could not delete temp file ("
          + tmpDir.getAbsolutePath() + ")");
    }

    if (!tmpDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    makeIndex(genomeFile, tmpDir);

    // Zip index files
    FileUtils.createZip(tmpDir, archiveOutputFile);

    // Remove temporary directory
    FileUtils.removeDirectory(tmpDir);

    LOGGER.info("End index computation");
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

    LOGGER.info("Copy genome to local disk before computating index");

    final File genomeTmpFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-genome", "");
    FileUtils.copy(is, FileUtils.createOutputStream(genomeTmpFile));

    makeArchiveIndex(genomeTmpFile, archiveOutputFile);

    if (!genomeTmpFile.delete()) {
      LOGGER.warning("Cannot delete temporary index zip file");
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

    if (!archiveIndexFile.exists())
      throw new IOException("No index for the mapper found: "
          + archiveIndexFile);

    // Uncompress archive if necessary
    if (!archiveIndexDir.exists()) {

      if (!archiveIndexDir.mkdir())
        throw new IOException("Can't create directory for "
            + getMapperName() + " index: " + archiveIndexDir);

      FileUtils.unzip(archiveIndexFile, archiveIndexDir);
    }

    FileUtils.checkExistingDirectoryFile(archiveIndexDir, getMapperName()
        + " index directory");

  }

  //
  // Entries
  //

  @Override
  public void closeInput() throws IOException {

    checkState(!this.noReadWritten,
        "Can not close writer that has not been created.");
    checkState(this.readsWriter1 != null,
        "Can not close writer that has not been created.");

    this.readsWriter1.close();

    if (isPairEnd()) {

      checkState(this.readsWriter2 != null,
          "Can not close writer that has not been created.");
      this.readsWriter2.close();
    }

    LOGGER.info("Write " + entriesWritten + " reads for mapping");
  }

  private void checkWritePairEnd() throws IOException {

    checkState(isPairEnd(), "Can not write pair-end read in single end mode.");

    if (noReadWritten) {

      this.readsFile1 =
          FileUtils.createTempFile(Globals.APP_NAME_LOWER_CASE + "-reads1-",
              ".fq");
      this.readsFile2 =
          FileUtils.createTempFile(Globals.APP_NAME_LOWER_CASE + "-reads2-",
              ".fq");

      this.readsWriter1 = FileUtils.createBufferedWriter(this.readsFile1);
      this.readsWriter2 = FileUtils.createBufferedWriter(this.readsFile2);

      this.noReadWritten = false;
    }
  }

  private void checkWriteSingleEnd() throws IOException {

    checkState(!isPairEnd(), "Can not write single-end read in pair-end mode.");

    if (noReadWritten) {

      this.readsFile1 =
          FileUtils.createTempFile(Globals.APP_NAME_LOWER_CASE + "-reads1-",
              ".fq");

      this.readsWriter1 = FileUtils.createBufferedWriter(this.readsFile1);

      this.noReadWritten = false;
    }

  }

  @Override
  public void writeInputEntry(final ReadSequence read1, final ReadSequence read2)
      throws IOException {

    checkWritePairEnd();

    if (read1 == null && read2 == null) {
      return;
    }

    if (read1 == null || read2 == null) {

      throw new IllegalStateException(
          "One of the two read of the pair-end is null");
    }

    this.readsWriter1.write(read1.toFastQ());
    this.readsWriter2.write(read2.toFastQ());

    entriesWritten++;
    inputReadsIncr();
  }

  @Override
  public void writeInputEntry(ReadSequence read) throws IOException {

    checkWriteSingleEnd();

    if (read == null) {
      return;
    }

    this.readsWriter1.write(read.toFastQ());
    entriesWritten++;
    inputReadsIncr();
  }

  @Override
  public void writeInputEntry(final String sequenceName, final String sequence,
      final String quality) throws IOException {

    checkWriteSingleEnd();

    if (sequenceName == null || sequence == null || quality == null) {
      return;
    }

    this.readsWriter1.write(ReadSequence.toFastQ(sequenceName, sequence,
        quality));
    entriesWritten++;
    inputReadsIncr();
  }

  @Override
  public void writeInputEntry(final String sequenceName1,
      final String sequence1, final String quality1,
      final String sequenceName2, final String sequence2, final String quality2)
      throws IOException {

    checkWritePairEnd();

    if (sequenceName1 == null
        || sequence1 == null || quality1 == null || sequenceName2 == null
        || sequence2 == null || quality2 == null) {
      return;
    }

    this.readsWriter1.write(ReadSequence.toFastQ(sequenceName1, sequence1,
        quality1));
    this.readsWriter2.write(ReadSequence.toFastQ(sequenceName2, sequence2,
        quality2));

    entriesWritten++;
    inputReadsIncr();
  }

  //
  // Mapping
  //

  @Override
  public void map(final File archiveIndexFile, final File archiveIndexDir)
      throws IOException {

    if (isPairEnd()) {
      map(this.readsFile1, this.readsFile2, archiveIndexFile, archiveIndexDir);
    } else {
      map(this.readsFile1, archiveIndexFile, archiveIndexDir);
    }
  }

  @Override
  public final void map(final File readsFile1, final File readsFile2,
      final File archiveIndexFile, final File archiveIndexDir)
      throws IOException {

    LOGGER.info("Mapping with " + getMapperName() + " in pair-end mode");

    checkState(isPairEnd(), "Cannot map a single reads file in pair-end mode.");
    checkNotNull(readsFile1, "readsFile1 is null");
    checkNotNull(readsFile2, "readsFile2 is null");
    checkNotNull(archiveIndexFile, "archiveIndexFile is null");
    checkNotNull(archiveIndexDir, "archiveIndexDir is null");

    checkExistingStandardFile(readsFile1,
        "readsFile1 not exits or is not a standard file.");
    checkExistingStandardFile(readsFile2,
        "readsFile2 not exits or is not a standard file.");
    checkExistingStandardFile(archiveIndexFile,
        "The archive index file not exits or is not a standard file.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Process to mapping
    internalMap(readsFile1, readsFile2, archiveIndexDir);
  }

  @Override
  public final void map(final File readsFile, final File archiveIndexFile,
      final File archiveIndexDir) throws IOException {

    LOGGER.info("Mapping with " + getMapperName() + " in single-end mode");

    checkState(!isPairEnd(), "Cannot map a single reads file in pair-end mode.");
    checkNotNull(readsFile, "readsFile1 is null");
    checkNotNull(archiveIndexFile, "archiveIndex is null");
    checkNotNull(archiveIndexDir, "archiveIndexDir is null");
    checkExistingStandardFile(readsFile,
        "readsFile1 not exits or is not a standard file.");
    checkExistingStandardFile(archiveIndexFile,
        "The archive index file not exits or is not a standard file.");

    // Unzip archive index if necessary
    unzipArchiveIndexFile(archiveIndexFile, archiveIndexDir);

    // Process to mapping
    internalMap(readsFile, archiveIndexDir);
  }

  protected abstract void internalMap(final File readsFile1,
      final File readsFile2, final File archiveIndex) throws IOException;

  protected abstract void internalMap(final File readsFile,
      final File archiveIndex) throws IOException;

  protected void deleteFile(final File file) {

    if (file != null && file.exists()) {

      if (!file.delete()) {

        LOGGER
            .warning("Cannot delete file while cleaning mapper temporary file: "
                + file);
      }
    }
  }

  //
  // Incrementors
  //

  private void inputReadsIncr() {

    this.incrementer.incrCounter(this.counterGroup, "mapper input reads", 1);
  }

  //
  // Init
  //

  /**
   * Initialize mapper.
   * @param pairEnd true if the mapper is in pair end mode.
   * @param incrementer Objet to use to increment counters
   * @param counterGroup counter name group
   */
  @Override
  public void init(final boolean pairEnd,
      final ReporterIncrementer incrementer, final String counterGroup) {

    checkNotNull(incrementer, "incrementer is null");
    checkNotNull(counterGroup, "counterGroup is null");

    this.pairEnd = pairEnd;
    this.incrementer = incrementer;
    this.counterGroup = counterGroup;
  }

}
