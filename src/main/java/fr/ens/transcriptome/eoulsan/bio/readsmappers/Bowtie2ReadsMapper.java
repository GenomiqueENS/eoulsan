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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.GenomeDescription;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Bowtie2ReadsMapper extends AbstractSequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String MAPPER_EXECUTABLE = "bowtie2";
  private static final String MAPPER_EXECUTABLE_BIN = "bowtie2-align";
  private static final String INDEXER_EXECUTABLE = "bowtie2-build";

  public static final String DEFAULT_ARGUMENTS = "";

  private static final String SYNC = BowtieReadsMapper.class.getName();
  private static final String MAPPER_NAME = "Bowtie2";

  private File outputFile;

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
      final String bowtiePath;

      synchronized (SYNC) {
        bowtiePath = install(MAPPER_EXECUTABLE_BIN, MAPPER_EXECUTABLE);
      }

      final String cmd = bowtiePath + " --version";

      final String s = ProcessUtils.execToString(cmd);
      final String[] lines = s.split("\n");
      if (lines.length == 0)
        return null;

      final String[] tokens = lines[0].split(" version ");
      if (tokens.length > 1)
        return tokens[1].trim();

      return null;

    } catch (IOException e) {

      return null;
    }
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.BOWTIE2_INDEX_ZIP;
  }

  @Override
  protected String getIndexerCommand(String indexerPathname,
      String genomePathname) {

    File genomeDir = new File(genomePathname).getParentFile();

    return "cd "
        + genomeDir.getAbsolutePath() + " && " + indexerPathname + " "
        + genomePathname + " genome";
  }

  @Override
  protected String getIndexerExecutable() {

    return INDEXER_EXECUTABLE;
  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(MAPPER_EXECUTABLE_BIN, MAPPER_EXECUTABLE);
    }

    final String bt2 =
        new File(getIndexPath(archiveIndexDir, ".rev.1.bt2",
            ".rev.1.bt2".length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();
    cmd.add(bowtiePath);
    cmd.add(getBowtieQualityArgument(getFastqFormat()));
    cmd.add(getMapperArguments());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add("-x");
    cmd.add(bt2);
    cmd.add("-1");
    cmd.add(readsFile1.getAbsolutePath());
    cmd.add("-2");
    cmd.add(readsFile2.getAbsolutePath());
    cmd.add("-S");
    cmd.add(outputFile.getAbsolutePath());
    cmd.add("2>");
    cmd.add("/dev/null");

    // Old version cmd = "cd "
    // + archiveIndexDir.getAbsolutePath() + " && " + bowtiePath + " "
    // + getBowtieQualityArgument(getFastqFormat()) + " "
    // + getMapperArguments() + " -p " + getThreadsNumber() + " -x " + bt2
    // + " -1 " + readsFile1.getAbsolutePath() + " -2 "
    // + readsFile2.getAbsolutePath() + " -S "
    // + outputFile.getAbsolutePath() + " 2> /dev/null";

    LOGGER.info(cmd.toString());

    final int exitValue = sh(cmd, archiveIndexDir);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    this.outputFile = outputFile;

  }

  @Override
  protected void internalMap(File readsFile, File archiveIndexDir)
      throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = install(MAPPER_EXECUTABLE_BIN, MAPPER_EXECUTABLE);
    }

    final String bt2 =
        new File(getIndexPath(archiveIndexDir, ".rev.1.bt2",
            ".rev.1.bt2".length())).getName();
    // TODO

    final File outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final List<String> cmd = new ArrayList<String>();
    cmd.add(bowtiePath);
    cmd.add(getBowtieQualityArgument(getFastqFormat()));
    cmd.add(getMapperArguments());
    cmd.add("-p");
    cmd.add(getThreadsNumber() + "");
    cmd.add("-x");
    cmd.add(bt2);
    cmd.add("-q");
    cmd.add(readsFile.getAbsolutePath());
    cmd.add("-S");
    cmd.add(outputFile.getAbsolutePath());
    cmd.add("2>");
    cmd.add("/dev/null");

    // Old version : cmd = "cd "
    // + archiveIndexDir.getAbsolutePath() + " && " + bowtiePath + " "
    // + getBowtieQualityArgument(getFastqFormat()) + " "
    // + getMapperArguments() + " -p " + getThreadsNumber() + " -x " + bt2
    // + " -q " + readsFile.getAbsolutePath() + " -S "
    // + outputFile.getAbsolutePath() + " 2> /dev/null";

    LOGGER.info(cmd.toString());

    final int exitValue = sh(cmd, archiveIndexDir);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    this.outputFile = outputFile;
  }

  private static final String getBowtieQualityArgument(final FastqFormat format) {

    switch (format) {

    case FASTQ_SOLEXA:
      // TODO BOWTIE do not support solexa quality scores
      return "--solexa-quals";

    case FASTQ_ILLUMINA:
    case FASTQ_ILLUMINA_1_5:
      return "--phred64";

    case FASTQ_SANGER:
    default:
      return "--phred33";
    }
  }

  @Override
  public void clean() {
  }

  @Override
  public File getSAMFile(final GenomeDescription gd) throws IOException {

    return this.outputFile;
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
