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
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

/**
 * This class define a wrapper on the Bowtie mapper.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class BowtieReadsMapper extends AbstractSequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final String MAPPER_EXECUTABLE = "bowtie";
  private static final String INDEXER_EXECUTABLE = "bowtie-build";

  public static final String DEFAULT_ARGUMENTS = "-k 10";

  private static final String SYNC = BowtieReadsMapper.class.getName();
  private static final String MAPPER_NAME = "Bowtie";

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
        bowtiePath = install(MAPPER_EXECUTABLE);
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

    return DataFormats.BOWTIE_INDEX_ZIP;
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
      bowtiePath = install(MAPPER_EXECUTABLE);
    }

    final String ebwt =
        new File(getIndexPath(archiveIndexDir, ".rev.1.ebwt",
            ".rev.1.ebwt".length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final String cmd =
        "cd "
            + archiveIndexDir.getAbsolutePath() + " && " + bowtiePath + " -S "
            + getBowtieQualityArgument(getFastqFormat()) + " "
            + getMapperArguments() + " -p " + getThreadsNumber() + " " + ebwt
            + " -1 " + readsFile1.getAbsolutePath() + " -2 "
            + readsFile2.getAbsolutePath() + " > "
            + outputFile.getAbsolutePath() + " 2> /dev/null";

    LOGGER.info(cmd);

    final int exitValue = sh(cmd);

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
      bowtiePath = install(MAPPER_EXECUTABLE);
    }

    final String ebwt =
        new File(getIndexPath(archiveIndexDir, ".rev.1.ebwt",
            ".rev.1.ebwt".length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), getMapperName()
            .toLowerCase() + "-outputFile-", ".sam");

    // Build the command line
    final String cmd =
        "cd "
            + archiveIndexDir.getAbsolutePath() + " && " + bowtiePath + " -S "
            + getBowtieQualityArgument(getFastqFormat()) + " "
            + getMapperArguments() + " -p " + getThreadsNumber() + " " + ebwt
            + " -q " + readsFile.getAbsolutePath() + " > "
            + outputFile.getAbsolutePath() + " 2> /dev/null";

    LOGGER.info(cmd);

    final int exitValue = sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    this.outputFile = outputFile;
  }

  private static final String getBowtieQualityArgument(final FastqFormat format) {

    switch (format) {

    case FASTQ_SOLEXA:
      return "--solexa-quals";

    case FASTQ_ILLUMINA:
    case FASTQ_ILLUMINA_1_5:
      return "--phred64-quals";

    case FASTQ_SANGER:
    default:
      return "--phred33-quals";
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
      final ReporterIncrementer incrementer, final String counterGroup) {

    super.init(pairEnd, fastqFormat, incrementer, counterGroup);
    setMapperArguments(DEFAULT_ARGUMENTS);
  }

}
