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

import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA;
import static fr.ens.transcriptome.eoulsan.bio.FastqFormat.FASTQ_ILLUMINA_1_5;
import static fr.ens.transcriptome.eoulsan.util.FileUtils.createTempFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.bio.ReadSequence;
import fr.ens.transcriptome.eoulsan.bio.io.FastqWriter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
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

  private static final int MIN_BWTSW_ALGO_GENOME_SIZE = 1 * 1024 * 1024 * 1024;
  public static final String DEFAULT_ARGUMENTS = "-l 28";

  private static final String SYNC = BWAReadsMapper.class.getName();
  private static final String PREFIX_FILES = "bwa";
  private static final String SAI_EXTENSION = ".sai";
  private static final String FASTQ_EXTENSION = ".fq";

  private static final class MutableBoolean {
    private boolean value;
  }

  private static final class ExceptionWrapper {
    private IOException exception;
  }

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
  protected String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  private String getIndexPath(final File archiveIndexDir) throws IOException {

    return getIndexPath(archiveIndexDir, ".bwt", 4);
  }

  //
  // Utility methods
  //

  private static final FastqWriter createFastqWriter(final File file)
      throws FileNotFoundException {

    // Create writer on FASTQ file
    @SuppressWarnings("resource")
    final RandomAccessFile raf = new RandomAccessFile(file, "rw");
    return new FastqWriter(Channels.newOutputStream(raf.getChannel()));
  }

  private static Thread createFastqCopyThread(final FastqWriter writer,
      final BlockingDeque<ReadSequence> queue, final MutableBoolean closed,
      final ExceptionWrapper exception) {

    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {

        try {
          while (!closed.value || !queue.isEmpty()) {

            if (!queue.isEmpty()) {

              writer.write(queue.take());
            } else {
              Thread.sleep(500);
            }
          }

          writer.close();

        } catch (IOException e) {
          exception.exception = e;
        } catch (InterruptedException e) {
          exception.exception = new IOException(e);
        }
      }
    });

    t.start();

    return t;
  }

  @Override
  protected MapperProcess internalMapSE(final File archiveIndex)
      throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install(MAPPER_EXECUTABLE);
    }

    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessSE(bwaPath, indexPath, null);
  }

  @Override
  protected MapperProcess internalMapPE(final File archiveIndex)
      throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = install("bwa");
    }

    final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

    // Temporary result file 1
    final File tmpFile1 =
        FileUtils.createTempFile(tmpDir, PREFIX_FILES + "-output-",
            SAI_EXTENSION);

    // Temporary result file 2
    final File tmpFile2 =
        FileUtils.createTempFile(tmpDir, PREFIX_FILES + "-output-",
            SAI_EXTENSION);
    // Path to index
    final String indexPath = getIndexPath(archiveIndex);

    return createMapperProcessPE(bwaPath, indexPath, null, null, tmpFile1,
        tmpFile2);
  }

  private MapperProcess createMapperProcessSE(final String bwaPath,
      final String indexPath, final File readsFile) throws IOException {

    return new MapperProcess(this, false) {

      private File saiFile;
      private File fastqFile;
      private FastqWriter writer;
      private BlockingDeque<ReadSequence> queue;
      private MutableBoolean closed;
      private ExceptionWrapper exception;
      private Thread copyThread;

      protected void additionalInit() throws IOException {

        this.saiFile =
            createTempFile(EoulsanRuntime.getRuntime().getTempDirectory(),
                PREFIX_FILES + "-output-", SAI_EXTENSION);

        this.fastqFile =
            createTempFile(EoulsanRuntime.getRuntime().getTempDirectory(),
                PREFIX_FILES + "-output-", FASTQ_EXTENSION);

        // Delete temporary files
        this.saiFile.delete();
        this.fastqFile.delete();

        // Create named pipes
        FileUtils.createNamedPipe(this.saiFile);
        FileUtils.createNamedPipe(this.fastqFile);

        // Add fastq copy file and sai file to files to remove
        addFilesToRemove(saiFile, this.fastqFile);

        // Create FASTQ writer
        this.writer = createFastqWriter(this.fastqFile);

        // If the files is not initialized here, the fields will be null because
        // this method is called by the super constructor
        this.queue = new LinkedBlockingDeque<>();
        this.closed = new MutableBoolean();
        this.exception = new ExceptionWrapper();

        this.copyThread =
            createFastqCopyThread(this.writer, this.queue, this.closed,
                exception);
      }

      @Override
      public void writeEntry1(final ReadSequence read) throws IOException {

        super.writeEntry1(read);
        this.queue.add(read);

        // Throw exception if occurs
        if (this.exception.exception != null) {
          throw this.exception.exception;
        }
      }

      @Override
      public void closeWriter1() throws IOException {

        super.closeWriter1();
        this.closed.value = true;

        // Wait the end of the copy thread
        try {
          this.copyThread.join();
        } catch (InterruptedException e) {
          throw new IOException(e);
        }

        // Throw exception if occurs
        if (this.exception.exception != null) {
          throw this.exception.exception;
        }

      }

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

        // Set the user options
        cmd1.addAll(getListMapperArguments());

        cmd1.add("-t");
        cmd1.add(getThreadsNumber() + "");
        cmd1.add("-f");
        cmd1.add(saiFile.getAbsolutePath());
        cmd1.add(indexPath);
        cmd1.add(getNamedPipeFile1().getAbsolutePath());

        final List<String> cmd2 = new ArrayList<>();

        // Build the command line
        cmd2.add(bwaPath);
        cmd2.add("samse");
        cmd2.add(indexPath);
        cmd2.add(saiFile.getAbsolutePath());
        cmd2.add(this.fastqFile.getAbsolutePath());

        final List<List<String>> result = new ArrayList<>();
        result.add(cmd1);
        result.add(cmd2);

        return result;
      }

    };

  }

  private MapperProcess createMapperProcessPE(final String bwaPath,
      final String indexPath, final File readsFile1, final File readsFile2,
      final File tmpFile1, final File tmpFile2) throws IOException {

    return new MapperProcess(this, true) {

      private File saiFile;
      private File fastqFile1;
      private File fastqFile2;
      private FastqWriter writer1;
      private FastqWriter writer2;
      private BlockingDeque<ReadSequence> queue1;
      private BlockingDeque<ReadSequence> queue2;

      private MutableBoolean closed1;
      private MutableBoolean closed2;

      private ExceptionWrapper exception1;
      private ExceptionWrapper exception2;
      private Thread copyThread1;
      private Thread copyThread2;

      protected void additionalInit() throws IOException {

        this.saiFile =
            createTempFile(EoulsanRuntime.getRuntime().getTempDirectory(),
                PREFIX_FILES + "-output-", SAI_EXTENSION);

        this.fastqFile1 =
            createTempFile(EoulsanRuntime.getRuntime().getTempDirectory(),
                PREFIX_FILES + "-output1-", FASTQ_EXTENSION);
        this.fastqFile2 =
            createTempFile(EoulsanRuntime.getRuntime().getTempDirectory(),
                PREFIX_FILES + "-output1-", FASTQ_EXTENSION);

        // Delete temporary files
        this.saiFile.delete();
        this.fastqFile1.delete();
        this.fastqFile2.delete();

        // Create named pipes
        FileUtils.createNamedPipe(this.saiFile);
        FileUtils.createNamedPipe(this.fastqFile1);
        FileUtils.createNamedPipe(this.fastqFile2);

        // Add fastq copy file and sai file to files to remove
        addFilesToRemove(saiFile, this.fastqFile1, this.fastqFile2);

        // Create writer on FASTQ files
        this.writer1 = createFastqWriter(this.fastqFile1);
        this.writer1 = createFastqWriter(this.fastqFile2);

        // If the files is not initialized here, the fields will be null because
        // this method is called by the super constructor
        this.queue1 = new LinkedBlockingDeque<>();
        this.queue2 = new LinkedBlockingDeque<>();
        this.closed1 = new MutableBoolean();
        this.closed2 = new MutableBoolean();
        this.exception1 = new ExceptionWrapper();
        this.exception2 = new ExceptionWrapper();
        this.copyThread1 =
            createFastqCopyThread(this.writer1, this.queue1, this.closed1,
                exception1);
        this.copyThread2 =
            createFastqCopyThread(this.writer2, this.queue2, this.closed2,
                exception2);
      }

      @Override
      public void writeEntry1(final ReadSequence read) throws IOException {

        super.writeEntry1(read);
        this.queue1.add(read);

        // Throw exception if occurs
        if (this.exception1.exception != null) {
          throw this.exception1.exception;
        }
      }

      @Override
      public void writeEntry2(final ReadSequence read) throws IOException {

        super.writeEntry1(read);
        this.queue2.add(read);

        // Throw exception if occurs
        if (this.exception2.exception != null) {
          throw this.exception2.exception;
        }
      }

      @Override
      public void closeWriter1() throws IOException {

        super.closeWriter1();
        this.closed1.value = true;

        // Wait the end of the copy thread
        try {
          this.copyThread1.join();
        } catch (InterruptedException e) {
          throw new IOException(e);
        }

        // Throw exception if occurs
        if (this.exception1.exception != null) {
          throw this.exception1.exception;
        }
      }

      @Override
      public void closeWriter2() throws IOException {

        super.closeWriter2();
        this.closed2.value = true;

        // Wait the end of the copy thread
        try {
          this.copyThread2.join();
        } catch (InterruptedException e) {
          throw new IOException(e);
        }

        // Throw exception if occurs
        if (this.exception2.exception != null) {
          throw this.exception2.exception;
        }
      }

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

        // Set the user options
        cmd1.addAll(getListMapperArguments());

        cmd1.add("-t");
        cmd1.add(getThreadsNumber() + "");
        cmd1.add("-f");
        cmd1.add(tmpFile1.getAbsolutePath());
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
        cmd2.add(getThreadsNumber() + "");
        cmd2.add("-f");
        cmd2.add(tmpFile2.getAbsolutePath());
        cmd2.add(indexPath);
        cmd2.add(getNamedPipeFile2().getAbsolutePath());

        final List<String> cmd3 = new ArrayList<>();

        // Build the command line
        cmd3.add(bwaPath);
        cmd3.add("sampe");
        cmd3.add(indexPath);
        cmd3.add(tmpFile1.getAbsolutePath());
        cmd3.add(tmpFile2.getAbsolutePath());
        cmd3.add(getNamedPipeFile1().getAbsolutePath());
        cmd3.add(getNamedPipeFile2().getAbsolutePath());

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
