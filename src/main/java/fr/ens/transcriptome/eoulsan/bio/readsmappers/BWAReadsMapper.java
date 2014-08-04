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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA;
import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA_1_5;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.bio.SAMParserLine;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the BWA mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BWAReadsMapper extends AbstractSequenceReadsMapper {

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
  public boolean isSplitsAllowed() {

    return true;
  }

  @Override
  public String getMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath =
            BinariesInstaller
                .install(MAPPER_EXECUTABLE, getTempDirectoryPath());
      }

      final String cmd = execPath;

      final String s = ProcessUtils.execToString(cmd, true, false);
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
  protected List<String> getIndexerCommand(String indexerPathname,
      String genomePathname) {

    final File genomeFile = new File(genomePathname);
    List<String> cmd = new ArrayList<String>();
    if (genomeFile.length() >= MIN_BWTSW_ALGO_GENOME_SIZE) {
      cmd.add(indexerPathname);
      cmd.add("index");
      cmd.add("-a");
      cmd.add("bwtsw");
      cmd.add(genomePathname);

      return cmd;
    }

    cmd.add(indexerPathname);
    cmd.add("index");
    cmd.add(genomePathname);

    return cmd;
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
      bwaPath = install(MAPPER_EXECUTABLE);
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
      bwaPath = install("bwa");
    }

    getLogger().fine("first pair member alignement");

    this.outputFile1 =
        FileUtils.createTempFile(readsFile1.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    execAln(bwaPath, getMapperArguments(), getThreadsNumber(),
        outputFile1.getAbsolutePath(), getIndexPath(archiveIndexDir),
        readsFile1.getAbsolutePath());

    getLogger().fine("first second member alignement");

    this.outputFile2 =
        FileUtils.createTempFile(readsFile2.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    execAln(bwaPath, getMapperArguments(), getThreadsNumber(),
        outputFile2.getAbsolutePath(), getIndexPath(archiveIndexDir),
        readsFile2.getAbsolutePath());

  }

  @Override
  protected void internalMap(final File readsFile1, final File readsFile2,
      final File archiveIndex, final SAMParserLine parserLine)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void internalMap(final File readsFile, final File archiveIndex,
      final SAMParserLine parserLine) throws IOException {
    throw new UnsupportedOperationException();
  }

  private void execAln(final String bwaPath, final String args,
      final int threads, final String outputFilename,
      final String indexPathname, final String readsFilename)
      throws IOException {

    final boolean illuminaFastq =
        getFastqFormat() == FASTQ_ILLUMINA
            || getFastqFormat() == FASTQ_ILLUMINA_1_5;

    final List<String> cmd = new ArrayList<String>();
    cmd.add(bwaPath);
    cmd.add("aln");
    if (illuminaFastq)
      cmd.add("-I");
    cmd.add(args);
    cmd.add("-t");
    cmd.add(threads + "");
    cmd.add("-f");
    cmd.add(outputFilename);
    cmd.add(indexPathname);
    cmd.add(readsFilename);

    getLogger().info(cmd.toString());

    final int exitValue = sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

  }

  @Override
  public File getSAMFile(final GenomeDescription gd) throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install("bwa");
    }

    final List<String> cmd = new ArrayList<String>();
    final File resultFile;

    if (isPairEnd()) {

      resultFile =
          FileUtils.createTempFile(this.outputFile1.getParentFile(),
              PREFIX_FILES + "-output-", ".sam");

      cmd.add(bwaPath);
      cmd.add("sampe");
      cmd.add("-P");
      cmd.add("-f");
      cmd.add(resultFile.getAbsolutePath());
      cmd.add(getIndexPath(archiveIndex));
      cmd.add(outputFile1.getAbsolutePath());
      cmd.add(outputFile2.getAbsolutePath());
      cmd.add(readsFile1.getAbsolutePath());
      cmd.add(readsFile2.getAbsolutePath());

    } else {

      resultFile =
          FileUtils.createTempFile(this.outputFile.getParentFile(),
              PREFIX_FILES + "-output-", ".sam");

      // Build the command line
      cmd.add(bwaPath);
      cmd.add("sampe");
      cmd.add("-f");
      cmd.add(resultFile.getAbsolutePath());
      cmd.add(getIndexPath(archiveIndex));
      cmd.add(outputFile.getAbsolutePath());
      cmd.add(readsFile.getAbsolutePath());

    }

    System.out.println("cmd: " + cmd);
    getLogger().info(cmd.toString());

    final int exitValue = sh(cmd);

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
      final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    super.init(pairEnd, fastqFormat, archiveIndexFile, archiveIndexDir,
        incrementer, counterGroup);
    setMapperArguments(DEFAULT_ARGUMENTS);
  }

}
