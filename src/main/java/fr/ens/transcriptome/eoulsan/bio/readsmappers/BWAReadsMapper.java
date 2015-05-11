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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the BWA mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BWAReadsMapper extends AbstractSequenceReadsMapper {

  public static final String MAPPER_NAME = "BWA";
  private static final String DEFAULT_PACKAGE_VERSION = "0.6.2";
  private static final String MAPPER_EXECUTABLE = "bwa";
  private static final String INDEXER_EXECUTABLE = MAPPER_EXECUTABLE;

  private static final int MIN_BWTSW_GENOME_SIZE = 1 * 1024 * 1024 * 1024;
  public static final String DEFAULT_ARGUMENTS = "-l 28";

  private static final String SYNC = BWAReadsMapper.class.getName();
  private static final String PREFIX_FILES = "bwa";
  private static final String SUFFIX_OUTPUT = ".sai";

  private File archiveIndex;

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
  }

  @Override
  protected String getDefaultPackageVersion() {

    return DEFAULT_PACKAGE_VERSION;
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
        execPath = install(MAPPER_EXECUTABLE);
      }

      final String cmd = execPath;

      final String s = ProcessUtils.execToString(cmd, true, false);
      final String[] lines = s.split("\n");

      for (String line : lines) {
        if (line.startsWith("Version:")) {

          final String[] tokens = line.split(":");
          if (tokens.length > 1) {
            return tokens[1].trim();
          }
        }
      }

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  protected List<String> getIndexerCommand(final String indexerPathname,
      final String genomePathname) {

    final File genomeFile = new File(genomePathname);
    List<String> cmd = new ArrayList<>();
    if (genomeFile.length() >= MIN_BWTSW_GENOME_SIZE) {
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
  protected String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  @Override
  protected InputStream internalMapSE(final File readsFile,
      final File archiveIndex, final GenomeDescription genomedescription)
      throws IOException {

    this.archiveIndex = archiveIndex;

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install(MAPPER_EXECUTABLE);
    }

    // Temporary result file
    final File tmpFile =
        FileUtils.createTempFile(readsFile.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessSE(bwaPath, indexPath, readsFile, tmpFile, true)
        .getStout();
  }

  private String getIndexPath(final File archiveIndexDir) throws IOException {

    return getIndexPath(archiveIndexDir, ".bwt", 4);
  }

  @Override
  protected InputStream internalMapPE(final File readsFile1,
      final File readsFile2, final File archiveIndexDir,
      final GenomeDescription genomeDescription) throws IOException {

    this.archiveIndex = archiveIndexDir;

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install("bwa");
    }

    // Temporary result file 1
    final File tmpFile1 =
        FileUtils.createTempFile(readsFile1.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    // Temporary result file 2
    final File tmpFile2 =
        FileUtils.createTempFile(readsFile2.getParentFile(), PREFIX_FILES
            + "-output-", SUFFIX_OUTPUT);

    // Path to index
    final String indexPath = getIndexPath(this.archiveIndex);

    return createMapperProcessPE(bwaPath, indexPath, readsFile1, readsFile2,
        tmpFile1, tmpFile2, true).getStout();
  }

  @Override
  protected MapperProcess internalMapSE(final File archiveIndex,
      final GenomeDescription gd) throws IOException {
    this.archiveIndex = archiveIndex;

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install(MAPPER_EXECUTABLE);
    }

    // Temporary result file
    final File tmpFile =
        FileUtils.createTempFile(
            EoulsanRuntime.getRuntime().getTempDirectory(), PREFIX_FILES
                + "-output-", SUFFIX_OUTPUT);

    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessSE(bwaPath, indexPath, null, tmpFile, false);
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndex,
      final GenomeDescription gd) throws IOException {

    this.archiveIndex = archiveIndex;

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install("bwa");
    }

    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    // Temporary result file 1
    final File tmpFile1 =
        FileUtils.createTempFile(tmpDir, PREFIX_FILES + "-output-",
            SUFFIX_OUTPUT);

    // Temporary result file 2
    final File tmpFile2 =
        FileUtils.createTempFile(tmpDir, PREFIX_FILES + "-output-",
            SUFFIX_OUTPUT);
    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessPE(bwaPath, indexPath, null, null, tmpFile1,
        tmpFile2, false);
  }

  private MapperProcess createMapperProcessSE(final String bwaPath,
      final String indexPath, final File readsFile, final File tmpFile,
      final boolean fileMode) throws IOException {

    return new MapperProcess(this, fileMode, false, false) {

      @Override
      protected List<List<String>> createCommandLines() {

        final boolean illuminaFastq =
            getFastqFormat() == FASTQ_ILLUMINA
                || getFastqFormat() == FASTQ_ILLUMINA_1_5;

        final List<String> cmd1 = new ArrayList<>();
        cmd1.add(bwaPath);
        cmd1.add("aln");
        if (illuminaFastq) {
          cmd1.add("-I");
        }
        cmd1.add(getMapperArguments());
        cmd1.add("-t");
        cmd1.add(getThreadsNumber() + "");
        cmd1.add("-f");
        cmd1.add(tmpFile.getAbsolutePath());
        cmd1.add(indexPath);
        if (fileMode) {
          cmd1.add(readsFile.getAbsolutePath());
        } else {
          cmd1.add(getTmpInputFile1().getAbsolutePath());
        }

        final List<String> cmd2 = new ArrayList<>();

        // Build the command line
        cmd2.add(bwaPath);
        cmd2.add("samse");
        cmd2.add(indexPath);
        cmd2.add(tmpFile.getAbsolutePath());

        // TODO fix version Laurent
        // cmd2.add(readsFile.getAbsolutePath());
        if (fileMode) {
          cmd2.add(readsFile.getAbsolutePath());
        } else {
          cmd2.add(getTmpInputFile1().getAbsolutePath());
        }

        final List<List<String>> result = new ArrayList<>();
        result.add(cmd1);
        result.add(cmd2);

        return result;
      }

      @Override
      protected void clean() {

        if (!tmpFile.delete()) {
          getLogger().warning("Cannot remove BWA temporary file: " + tmpFile);
        }
      }

    };

  }

  private MapperProcess createMapperProcessPE(final String bwaPath,
      final String indexPath, final File readsFile1, final File readsFile2,
      final File tmpFile1, final File tmpFile2, final boolean fileMode)
      throws IOException {

    return new MapperProcess(this, fileMode, false, true) {

      @Override
      protected List<List<String>> createCommandLines() {

        final boolean illuminaFastq =
            getFastqFormat() == FASTQ_ILLUMINA
                || getFastqFormat() == FASTQ_ILLUMINA_1_5;

        final List<String> cmd1 = new ArrayList<>();
        cmd1.add(bwaPath);
        cmd1.add("aln");
        if (illuminaFastq) {
          cmd1.add("-I");
        }
        cmd1.add(getMapperArguments());
        cmd1.add("-t");
        cmd1.add(getThreadsNumber() + "");
        cmd1.add("-f");
        cmd1.add(tmpFile1.getAbsolutePath());
        cmd1.add(indexPath);
        if (fileMode) {
          cmd1.add(readsFile1.getAbsolutePath());
        } else {
          cmd1.add(getTmpInputFile1().getAbsolutePath());
        }

        final List<String> cmd2 = new ArrayList<>();
        cmd2.add(bwaPath);
        cmd2.add("aln");
        if (illuminaFastq) {
          cmd2.add("-I");
        }
        cmd2.add(getMapperArguments());
        cmd2.add("-t");
        cmd2.add(getThreadsNumber() + "");
        cmd2.add("-f");
        cmd2.add(tmpFile2.getAbsolutePath());
        cmd2.add(indexPath);
        if (fileMode) {
          cmd2.add(readsFile2.getAbsolutePath());
        } else {
          cmd2.add(getTmpInputFile2().getAbsolutePath());
        }

        final List<String> cmd3 = new ArrayList<>();

        // Build the command line
        cmd3.add(bwaPath);
        cmd3.add("sampe");
        cmd3.add(indexPath);
        cmd3.add(tmpFile1.getAbsolutePath());
        cmd3.add(tmpFile2.getAbsolutePath());
        if (fileMode) {
          cmd3.add(readsFile1.getAbsolutePath());
          cmd3.add(readsFile2.getAbsolutePath());
        } else {
          cmd3.add(getTmpInputFile1().getAbsolutePath());
          cmd3.add(getTmpInputFile2().getAbsolutePath());
        }

        final List<List<String>> result = new ArrayList<>();
        result.add(cmd1);
        result.add(cmd2);
        result.add(cmd3);

        return result;
      }

      @Override
      protected void clean() {

        if (tmpFile1.delete()) {
          getLogger().warning("Cannot remove BWA temporary file: " + tmpFile1);
        }

        if (tmpFile2.delete()) {
          getLogger().warning("Cannot remove BWA temporary file: " + tmpFile2);
        }
      }

    };
  }

  //
  // Init
  //

  @Override
  public void init(final File archiveIndexFile, final File archiveIndexDir,
      final ReporterIncrementer incrementer, final String counterGroup)
      throws IOException {

    super.init(archiveIndexFile, archiveIndexDir, incrementer, counterGroup);
  }

}
