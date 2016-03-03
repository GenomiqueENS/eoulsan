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

import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA;
import static fr.ens.biologie.genomique.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA_1_5;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.ReporterIncrementer;

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
  private static final String SAI_EXTENSION = ".sai";
  private static final String FASTQ_EXTENSION = ".fq";

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
  public String internalGetMapperVersion() {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = install(MAPPER_EXECUTABLE);
      }

      final List<String> cmd = Lists.newArrayList(execPath);

      final String s = executeToString(cmd);
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

  private String getIndexPath(final File archiveIndexDir) throws IOException {

    return getIndexPath(archiveIndexDir, ".bwt", 4);
  }

  //
  // Utility methods
  //

  @Override
  protected MapperProcess internalMapSE(final File archiveIndex)
      throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install(MAPPER_EXECUTABLE);
    }

    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessSE(bwaPath, indexPath);
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndex)
      throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install(MAPPER_EXECUTABLE);
    }

    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessPE(bwaPath, indexPath);
  }

  private MapperProcess createMapperProcessSE(final String bwaPath,
      final String indexPath) throws IOException {

    return new MapperProcess(this, false) {

      private File saiFile;
      private File fastqFile;
      private FastqWriterThread writer;

      protected void additionalInit() throws IOException {

        final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

        final String uuid = getUUID();

        this.saiFile =
            new File(tmpDir, PREFIX_FILES + "-sai-" + uuid + SAI_EXTENSION);

        this.fastqFile =
            new File(tmpDir, PREFIX_FILES + "-fastq-" + uuid + FASTQ_EXTENSION);

        // Create named pipes
        FileUtils.createNamedPipe(this.saiFile);

        // Add fastq copy file and sai file to files to remove
        addFilesToRemove(saiFile, this.fastqFile);

        // Create FASTQ writer
        this.writer = new FastqWriterThread(this.fastqFile,
            "BWA sampe writeFirstPairEntries thread");
      }

      @Override
      public void writeEntry(final String name, final String sequence,
          final String quality) throws IOException {

        super.writeEntry(name, sequence, quality);
        this.writer.write(ReadSequence.toFastQ(name, sequence, quality) + '\n');
      }

      @Override
      public void writeEntry1(final ReadSequence read) throws IOException {

        super.writeEntry1(read);
        this.writer.write(read.toFastQ() + '\n');
      }

      @Override
      public void closeEntriesWriter() throws IOException {

        super.closeWriter1();
        this.writer.close();
      }

      @Override
      public void closeWriter1() throws IOException {

        super.closeWriter1();
        this.writer.close();
      }

      @Override
      protected List<List<String>> createCommandLines() {

        final boolean illuminaFastq = getFastqFormat() == FASTQ_ILLUMINA
            || getFastqFormat() == FASTQ_ILLUMINA_1_5;

        final List<String> cmd1 = new ArrayList<>();
        cmd1.add(bwaPath);
        cmd1.add("aln");

        if (illuminaFastq) {
          cmd1.add("-I");
        }

        // Set the user options
        cmd1.addAll(getListMapperArguments());

        cmd1.add("-t");
        cmd1.add(getThreadsNumber() + "");
        cmd1.add("-f");
        cmd1.add(this.saiFile.getAbsolutePath());
        cmd1.add(indexPath);
        cmd1.add(getNamedPipeFile1().getAbsolutePath());

        final List<String> cmd2 = new ArrayList<>();

        // Build the command line
        cmd2.add(bwaPath);
        cmd2.add("samse");
        cmd2.add(indexPath);
        cmd2.add(this.saiFile.getAbsolutePath());
        cmd2.add(this.fastqFile.getAbsolutePath());

        final List<List<String>> result = new ArrayList<>();
        result.add(cmd1);
        result.add(cmd2);

        return result;
      }

    };

  }

  private MapperProcess createMapperProcessPE(final String bwaPath,
      final String indexPath) throws IOException {

    return new MapperProcess(this, true) {

      private File saiFile1;
      private File saiFile2;
      private File fastqFile1;
      private File fastqFile2;
      private FastqWriterThread writer1;
      private FastqWriterThread writer2;

      protected void additionalInit() throws IOException {

        final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

        final String uuid = getUUID();

        this.saiFile1 =
            new File(tmpDir, PREFIX_FILES + "-sai1-" + uuid + SAI_EXTENSION);
        this.saiFile2 =
            new File(tmpDir, PREFIX_FILES + "-sai2-" + uuid + SAI_EXTENSION);

        this.fastqFile1 = new File(tmpDir,
            PREFIX_FILES + "-fastq1-" + uuid + FASTQ_EXTENSION);

        this.fastqFile2 = new File(tmpDir,
            PREFIX_FILES + "-fastq2-" + uuid + FASTQ_EXTENSION);

        // Create named pipes
        FileUtils.createNamedPipe(this.saiFile1);
        FileUtils.createNamedPipe(this.saiFile2);

        // Add fastq copy file and sai file to files to remove
        addFilesToRemove(this.saiFile1, this.saiFile2, this.fastqFile1,
            this.fastqFile2);

        // Create writer on FASTQ files
        this.writer1 = new FastqWriterThread(this.fastqFile1,
            "BWA sampe writeFirstPairEntries thread");
        this.writer2 = new FastqWriterThread(this.fastqFile2,
            "BWA sampe writeSecondPairEntries thread");
      }

      @Override
      public void writeEntry(final String name1, final String sequence1,
          final String quality1, final String name2, final String sequence2,
          final String quality2) throws IOException {

        super.writeEntry(name1, sequence1, quality1, name2, sequence2,
            quality2);
        this.writer1
            .write(ReadSequence.toFastQ(name1, sequence1, quality2) + '\n');
        this.writer2
            .write(ReadSequence.toFastQ(name2, sequence2, quality2) + '\n');
      }

      @Override
      public void writeEntry1(final ReadSequence read) throws IOException {

        super.writeEntry1(read);
        this.writer1.write(read.toFastQ() + '\n');
      }

      @Override
      public void writeEntry2(final ReadSequence read) throws IOException {

        super.writeEntry2(read);
        this.writer2.write(read.toFastQ() + '\n');
      }

      @Override
      public void closeEntriesWriter() throws IOException {

        super.closeWriter1();
        super.closeWriter2();
        this.writer1.close();
        this.writer2.close();
      }

      @Override
      public void closeWriter1() throws IOException {

        super.closeWriter1();
        this.writer1.close();
      }

      @Override
      public void closeWriter2() throws IOException {

        super.closeWriter2();
        this.writer2.close();
      }

      @Override
      protected List<List<String>> createCommandLines() {

        final boolean illuminaFastq = getFastqFormat() == FASTQ_ILLUMINA
            || getFastqFormat() == FASTQ_ILLUMINA_1_5;

        final List<String> cmd1 = new ArrayList<>();
        cmd1.add(bwaPath);
        cmd1.add("aln");
        if (illuminaFastq) {
          cmd1.add("-I");
        }

        // There are 2 bwa aln processes in paired-end mode, so we divided the
        // number of threads by 2
        final int threadNumber =
            getThreadsNumber() > 1 ? getThreadsNumber() / 2 : 1;

        // Set the user options
        cmd1.addAll(getListMapperArguments());

        cmd1.add("-t");
        cmd1.add(threadNumber + "");
        cmd1.add("-f");
        cmd1.add(this.saiFile1.getAbsolutePath());
        cmd1.add(indexPath);
        cmd1.add(getNamedPipeFile1().getAbsolutePath());

        final List<String> cmd2 = new ArrayList<>();
        cmd2.add(bwaPath);
        cmd2.add("aln");
        if (illuminaFastq) {
          cmd2.add("-I");
        }

        // Set the user options
        cmd2.addAll(getListMapperArguments());

        cmd2.add("-t");
        cmd2.add(threadNumber + "");
        cmd2.add("-f");
        cmd2.add(this.saiFile2.getAbsolutePath());
        cmd2.add(indexPath);
        cmd2.add(getNamedPipeFile2().getAbsolutePath());

        final List<String> cmd3 = new ArrayList<>();

        // Build the command line
        cmd3.add(bwaPath);
        cmd3.add("sampe");
        cmd3.add(indexPath);
        cmd3.add(this.saiFile1.getAbsolutePath());
        cmd3.add(this.saiFile2.getAbsolutePath());
        cmd3.add(this.fastqFile1.getAbsolutePath());
        cmd3.add(this.fastqFile2.getAbsolutePath());

        final List<List<String>> result = new ArrayList<>();
        result.add(cmd1);
        result.add(cmd2);
        result.add(cmd3);

        return result;
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
