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
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a wrapper on the BWA mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BWAMapperProvider extends AbstractMapperProvider {

  public static final String MAPPER_NAME = "BWA";
  private static final String DEFAULT_VERSION = "0.6.2";
  private static final String MAPPER_EXECUTABLE = "bwa";
  private static final String INDEXER_EXECUTABLE = MAPPER_EXECUTABLE;
  private static final String MEM_FLAVOR = "mem";
  private static final String ALN_FLAVOR = "aln";

  private static final int MIN_BWTSW_GENOME_SIZE = 1024 * 1024 * 1024;
  public static final String DEFAULT_ARGUMENTS = "-l 28";

  private static final String SYNC = BWAMapperProvider.class.getName();
  private static final String PREFIX_FILES = "bwa";
  private static final String SAI_EXTENSION = ".sai";
  private static final String FASTQ_EXTENSION = ".fq";

  @Override
  public String getName() {

    return MAPPER_NAME;
  }

  @Override
  public String getDefaultVersion() {

    return DEFAULT_VERSION;
  }

  @Override
  public String getDefaultFlavor() {
    return ALN_FLAVOR;
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.BWA_INDEX_ZIP;
  }

  @Override
  public String getDefaultMapperArguments() {

    return DEFAULT_ARGUMENTS;
  }

  @Override
  public String readBinaryVersion(final MapperInstance mapperInstance) {

    try {
      final String execPath;

      synchronized (SYNC) {
        execPath = mapperInstance.getExecutor().install(MAPPER_EXECUTABLE);
      }

      final List<String> cmd = Lists.newArrayList(execPath);

      final String s =
          MapperUtils.executeToString(mapperInstance.getExecutor(), cmd);
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
  public List<String> getIndexerExecutables(
      final MapperInstance mapperInstance) {

    return Collections.singletonList(INDEXER_EXECUTABLE);
  }

  @Override
  public String getMapperExecutableName(final MapperInstance mapperInstance) {
    return MAPPER_EXECUTABLE;
  }

  @Override
  public boolean checkIfFlavorExists(final MapperInstance mapperInstance) {

    switch (mapperInstance.getFlavor().trim().toLowerCase()) {

    case ALN_FLAVOR:
    case MEM_FLAVOR:
      return true;

    default:
      return false;
    }
  }

  @Override
  public List<String> getIndexerCommand(final File indexerFile,
      final File genomeFile, final List<String> indexerArguments,
      final int threads) {

    List<String> cmd = new ArrayList<>();
    if (genomeFile.length() >= MIN_BWTSW_GENOME_SIZE) {
      cmd.add("index");
      cmd.add("-a");
      cmd.add("bwtsw");
      cmd.add(genomeFile.getAbsolutePath());

      return cmd;
    }

    cmd.add(indexerFile.getAbsolutePath());
    cmd.add("index");
    cmd.add(genomeFile.getAbsolutePath());

    return cmd;
  }

  private String getIndexPath(final File archiveIndexDir) throws IOException {

    return MapperUtils.getIndexPath(getName(), archiveIndexDir, ".bwt", 4)
        .toString();
  }

  //
  // Utility methods
  //

  @Override
  public MapperProcess mapSE(final EntryMapping mapping, final File inputFile,
      final File errorFile, final File logFile) throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = mapping.getExecutor().install(MAPPER_EXECUTABLE);
    }

    // Path to index
    final String indexPath = getIndexPath(mapping.getIndexDirectory());

    return createMapperProcessSE(mapping, bwaPath, indexPath, inputFile,
        errorFile);
  }

  @Override
  public MapperProcess mapPE(final EntryMapping mapping, final File inputFile1,
      final File inputFile2, final File errorFile, final File logFile)
      throws IOException {

    final String bwaPath;

    synchronized (SYNC) {
      bwaPath = mapping.getExecutor().install(MAPPER_EXECUTABLE);
    }

    // Path to index
    final String indexPath = getIndexPath(mapping.getIndexDirectory());

    return createMapperProcessPE(mapping, bwaPath, indexPath, inputFile1,
        inputFile2, errorFile);
  }

  private MapperProcess createMapperProcessSE(final EntryMapping mapping,
      final String bwaPath, final String indexPath, final File inputFile,
      final File errorFile) throws IOException {

    // Get the BWA algorithm to use
    boolean bwaAln = !MEM_FLAVOR.equals(mapping.getFlavor());

    if (bwaAln) {

      // BWA aln
      return new MapperProcess(mapping.getName(), mapping.getExecutor(),
          mapping.getTemporaryDirectory(), errorFile, false, inputFile) {

        private File saiFile;
        private File fastqFile;
        private FastqWriterThread writer;

        protected void additionalInit() throws IOException {

          final File tmpDir = EoulsanRuntime.getRuntime().getTempDirectory();

          final String uuid = getUUID();

          this.saiFile =
              new File(tmpDir, PREFIX_FILES + "-sai-" + uuid + SAI_EXTENSION);

          // Create named pipes
          FileUtils.createNamedPipe(this.saiFile);

          if (inputFile == null) {

            // Create copy of FASTQ input file
            this.fastqFile = new File(tmpDir,
                PREFIX_FILES + "-fastq-" + uuid + FASTQ_EXTENSION);

            // Create FASTQ writer
            this.writer = new FastqWriterThread(this.fastqFile,
                "BWA samse writeFirstPairEntries thread");
          }

          // Add FASTQ copy file and sai file to files to remove
          addFilesToRemove(saiFile, this.fastqFile);
        }

        @Override
        public void writeEntry(final String name, final String sequence,
            final String quality) throws IOException {

          super.writeEntry(name, sequence, quality);
          this.writer
              .write(ReadSequence.toFastQ(name, sequence, quality) + '\n');
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

          final boolean illuminaFastq =
              mapping.getFastqFormat() == FASTQ_ILLUMINA
                  || mapping.getFastqFormat() == FASTQ_ILLUMINA_1_5;

          final List<String> cmd1 = new ArrayList<>();
          cmd1.add(bwaPath);

          // Select the flavor/algorithm to use
          cmd1.add(ALN_FLAVOR);

          if (illuminaFastq) {
            cmd1.add("-I");
          }

          // Set the user options
          cmd1.addAll(mapping.getMapperArguments());

          cmd1.add("-t");
          cmd1.add(mapping.getThreadNumber() + "");
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
          if (inputFile != null) {
            cmd2.add(inputFile.getAbsolutePath());
          } else {
            cmd2.add(this.fastqFile.getAbsolutePath());
          }

          final List<List<String>> result = new ArrayList<>();
          result.add(cmd1);
          result.add(cmd2);

          return result;
        }

      };
    }

    // BWA mem
    return new MapperProcess(mapping.getName(), mapping.getExecutor(),
        mapping.getTemporaryDirectory(), errorFile, false, inputFile) {

      @Override
      protected List<List<String>> createCommandLines() {

        final List<String> cmd = new ArrayList<>();
        cmd.add(bwaPath);

        // Select the flavor/algorithm to use
        cmd.add(MEM_FLAVOR);

        // Set the user options
        cmd.addAll(mapping.getMapperArguments());

        cmd.add("-t");
        cmd.add(mapping.getThreadNumber() + "");

        // Set the index path
        cmd.add(indexPath);

        // Set the input file
        cmd.add(getNamedPipeFile1().getAbsolutePath());

        return Collections.singletonList(cmd);
      }

    };
  }

  private MapperProcess createMapperProcessPE(final EntryMapping mapping,
      final String bwaPath, final String indexPath, final File inputFile1,
      final File inputFile2, final File errorFile) throws IOException {

    // Get the BWA algorithm to use
    boolean bwaAln = !MEM_FLAVOR.equals(mapping.getFlavor());

    if (bwaAln) {

      // BWA aln
      return new MapperProcess(mapping.getName(), mapping.getExecutor(),
          mapping.getTemporaryDirectory(), errorFile, true, inputFile1,
          inputFile2) {

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

          if (inputFile1 == null) {

            this.fastqFile1 = new File(tmpDir,
                PREFIX_FILES + "-fastq1-" + uuid + FASTQ_EXTENSION);

            this.fastqFile2 = new File(tmpDir,
                PREFIX_FILES + "-fastq2-" + uuid + FASTQ_EXTENSION);

            // Create writer on FASTQ files
            this.writer1 = new FastqWriterThread(this.fastqFile1,
                "BWA sampe writeFirstPairEntries thread");
            this.writer2 = new FastqWriterThread(this.fastqFile2,
                "BWA sampe writeSecondPairEntries thread");
          }

          // Create named pipes
          FileUtils.createNamedPipe(this.saiFile1);
          FileUtils.createNamedPipe(this.saiFile2);

          // Add fastq copy file and sai file to files to remove
          addFilesToRemove(this.saiFile1, this.saiFile2, this.fastqFile1,
              this.fastqFile2);
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

          final boolean illuminaFastq =
              mapping.getFastqFormat() == FASTQ_ILLUMINA
                  || mapping.getFastqFormat() == FASTQ_ILLUMINA_1_5;

          final List<String> cmd1 = new ArrayList<>();
          cmd1.add(bwaPath);

          // Select the flavor/algorithm to use
          if (MEM_FLAVOR.equals(mapping.getFlavor())) {
            cmd1.add(MEM_FLAVOR);
          } else {
            cmd1.add(ALN_FLAVOR);
          }

          if (illuminaFastq) {
            cmd1.add("-I");
          }

          // There are 2 bwa aln processes in paired-end mode, so we divided the
          // number of threads by 2
          final int threadNumber =
              mapping.getThreadNumber() > 1 ? mapping.getThreadNumber() / 2 : 1;

          // Set the user options
          cmd1.addAll(mapping.getMapperArguments());

          cmd1.add("-t");
          cmd1.add(threadNumber + "");
          cmd1.add("-f");
          cmd1.add(this.saiFile1.getAbsolutePath());
          cmd1.add(indexPath);
          cmd1.add(getNamedPipeFile1().getAbsolutePath());

          final List<String> cmd2 = new ArrayList<>();
          cmd2.add(bwaPath);
          cmd2.add(ALN_FLAVOR);
          if (illuminaFastq) {
            cmd2.add("-I");
          }

          // Set the user options
          cmd2.addAll(mapping.getMapperArguments());

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
          if (inputFile1 != null) {
            cmd3.add(inputFile1.getAbsolutePath());
            cmd3.add(inputFile2.getAbsolutePath());
          } else {
            cmd3.add(this.fastqFile1.getAbsolutePath());
            cmd3.add(this.fastqFile2.getAbsolutePath());
          }

          final List<List<String>> result = new ArrayList<>();
          result.add(cmd1);
          result.add(cmd2);
          result.add(cmd3);

          return result;
        }

      };

    } else {

      // BWA mem
      return new MapperProcess(mapping.getName(), mapping.getExecutor(),
          mapping.getTemporaryDirectory(), errorFile, true, inputFile1,
          inputFile2) {

        @Override
        protected List<List<String>> createCommandLines() {

          final List<String> cmd = new ArrayList<>();
          cmd.add(bwaPath);

          // Select the flavor/algorithm to use
          cmd.add(MEM_FLAVOR);

          // Set the user options
          cmd.addAll(mapping.getMapperArguments());

          cmd.add("-t");
          cmd.add(mapping.getThreadNumber() + "");

          // Set the index path
          cmd.add(indexPath);

          // Set the input files
          cmd.add(getNamedPipeFile1().getAbsolutePath());
          cmd.add(getNamedPipeFile2().getAbsolutePath());

          return Collections.singletonList(cmd);
        }

      };
    }
  }

}
