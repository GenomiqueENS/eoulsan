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
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public class BWAReadsMapper extends AbstractSequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String MAPPER_EXECUTABLE = "bwa";
  private static final String INDEXER_EXECUTABLE = MAPPER_EXECUTABLE;

  private static final int MIN_BWTSW_ALGO_GENOME_SIZE = 1 * 1024 * 1024 * 1024;
  public static final String DEFAULT_ARGUMENTS = "-l 28";

  private static final String SYNC = BWAReadsMapper.class.getName();
  private static final String MAPPER_NAME = "BWA";
  private static final String PREFIX_FILES = "bwa";
  private static final String SUFFIX_OUTPUT = ".sai";

  private File archiveIndex;
  private File outputFile;
  private File readsFile;

  private File outputFile1;
  private File readsFile1;
  private File outputFile2;
  private File readsFile2;

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = BinariesInstaller.install(MAPPER_EXECUTABLE);
      }

      final String cmd = execPath;

      final String s = ProcessUtils.execToString(cmd, true, false);

      if (s == null)
        return null;

      final String[] lines = s.split("\n");

      for (int i = 0; i < lines.length; i++)
        if (lines[i].startsWith("Version:")) {

          final String[] tokens = lines[i].split(":");
          if (tokens.length > 1)
            return tokens[1].trim();
        }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  protected String getIndexerCommand(String indexerPathname,
      String genomePathname) {

    final File genomeFile = new File(genomePathname);

    if (genomeFile.length() >= MIN_BWTSW_ALGO_GENOME_SIZE) {
      return indexerPathname + " index -a bwtsw " + genomePathname;
    }

    return indexerPathname + " index " + genomePathname;
  }

  @Override
  protected String getIndexerExecutable() {

    return INDEXER_EXECUTABLE;
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.BWA_INDEX_ZIP;
  }

  @Override
  protected void internalMap(final File readsFile, final File archiveIndex)
      throws IOException {

    this.archiveIndex = archiveIndex;
    this.readsFile = readsFile;

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = BinariesInstaller.install(MAPPER_EXECUTABLE);
    }

    this.outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    execAln(bwaPath, getMapperArguments(), getThreadsNumber(),
        outputFile.getAbsolutePath(), getIndexPath(archiveIndex),
        readsFile.getAbsolutePath());

  }

  private String getIndexPath(final File archiveIndexDir) throws IOException {

    return getIndexPath(archiveIndexDir, ".bwt", 4);
  }

  @Override
  protected void internalMap(final File readsFile1, final File readsFile2,
      final File archiveIndexDir) throws IOException {

    this.archiveIndex = archiveIndexDir;
    this.readsFile1 = readsFile1;
    this.readsFile2 = readsFile2;

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = BinariesInstaller.install("bwa");
    }

    System.out.println("=== aln 1 ===");

    this.outputFile1 =
        FileUtils.createTempFile(readsFile1.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    execAln(bwaPath, getMapperArguments(), getThreadsNumber(),
        outputFile1.getAbsolutePath(), getIndexPath(archiveIndexDir),
        readsFile1.getAbsolutePath());

    System.out.println("=== aln 2 ===");

    this.outputFile2 =
        FileUtils.createTempFile(readsFile2.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    execAln(bwaPath, getMapperArguments(), getThreadsNumber(),
        outputFile2.getAbsolutePath(), getIndexPath(archiveIndexDir),
        readsFile2.getAbsolutePath());

  }

  private static void execAln(final String bwaPath, final String args,
      final int threads, final String outputFilename,
      final String indexPathname, final String readsFilename)
      throws IOException {

    final String cmd =
        bwaPath
            + " aln " + args + " -t " + threads + " -f " + outputFilename + " "
            + " " + indexPathname + " " + readsFilename
            + " > /dev/null 2> /dev/null";

    System.out.println("cmd: " + cmd);
    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

  }

  @Override
  public File getSAMFile(final GenomeDescription gd) throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = BinariesInstaller.install("bwa");
    }

    final String cmd;
    final File resultFile;

    if (isPairEnd()) {

      resultFile =
          FileUtils.createTempFile(this.outputFile1.getParentFile(),
              PREFIX_FILES + "-output-", ".sam");

      cmd =
          bwaPath
              + " sampe -P -f " + resultFile.getAbsolutePath() + " "
              + getIndexPath(archiveIndex) + " "
              + outputFile1.getAbsolutePath() + " "
              + outputFile2.getAbsolutePath() + " "
              + readsFile1.getAbsolutePath() + " "
              + readsFile2.getAbsolutePath() + " > /dev/null 2> /dev/null";

    } else {

      resultFile =
          FileUtils.createTempFile(this.outputFile.getParentFile(),
              PREFIX_FILES + "-output-", ".sam");

      // Build the command line
      cmd =
          bwaPath
              + " samse -f " + resultFile.getAbsolutePath() + " "
              + getIndexPath(archiveIndex) + " " + outputFile.getAbsolutePath()
              + " " + readsFile.getAbsolutePath() + " > /dev/null 2> /dev/null";
    }

    System.out.println("cmd: " + cmd);
    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    return resultFile;
  }

  @Override
  public void clean() {

    deleteFile(this.outputFile);
  }

  //
  // Init
  //

  @Override
  public void init(final boolean pairEnd, final FastqFormat fastqFormat,
      final ReporterIncrementer incrementer, final String counterGroup) {

    super.init(pairEnd, fastqFormat, incrementer, counterGroup);
    setMapperArguments(DEFAULT_ARGUMENTS);
  }

}
