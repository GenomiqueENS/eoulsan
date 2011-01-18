/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public class BowtieReadsMapper extends AbstractSequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String DEFAULT_ARGUMENTS = "";

  private static final String SYNC = BowtieReadsMapper.class.getName();
  private static final String MAPPER_NAME = "Bowtie";

  private File outputFile;

  @Override
  public String getMapperName() {

    return MAPPER_NAME;
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

    return "bowtie-build";
  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir) throws IOException {

    final String bowtiePath;

    synchronized (SYNC) {
      bowtiePath = BinariesInstaller.install("bowtie");
    }

    final String ebwt =
        new File(getIndexPath(archiveIndexDir, ".rev.1.ebwt", ".rev.1.ebwt"
            .length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), getMapperName()
            .toLowerCase()
            + "-outputFile-", ".sam");

    // Build the command line
    final String cmd =
        "cd "
            + archiveIndexDir.getAbsolutePath() + " && " + bowtiePath + " -S "
            + getMapperArguments() + " -p " + getThreadsNumber() + " " + ebwt
            + " -1 " + readsFile1.getAbsolutePath() + " -2 "
            + readsFile2.getAbsolutePath() + " > "
            + outputFile.getAbsolutePath() + " 2> /dev/null";

    System.out.println("cmd: " + cmd);
    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

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
      bowtiePath = BinariesInstaller.install("bowtie");
    }

    final String ebwt =
        new File(getIndexPath(archiveIndexDir, ".rev.1.ebwt", ".rev.1.ebwt"
            .length())).getName();

    final File outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), getMapperName()
            .toLowerCase()
            + "-outputFile-", ".sam");

    // Build the command line
    final String cmd =
        "cd "
            + archiveIndexDir.getAbsolutePath() + " && " + bowtiePath + " -S "
            + getMapperArguments() + " -p " + getThreadsNumber() + " " + ebwt
            + " -q " + readsFile.getAbsolutePath() + " > "
            + outputFile.getAbsolutePath() + " 2> /dev/null";

    System.out.println("cmd: " + cmd);
    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for "
          + MAPPER_NAME + " execution: " + exitValue);
    }

    this.outputFile = outputFile;
  }

  @Override
  public void clean() {
  }

  @Override
  public File getSAMFile() throws IOException {

    return this.outputFile;
  }

  //
  // Init
  //

  @Override
  public void init(final boolean pairEnd,
      final ReporterIncrementer incrementer, final String counterGroup) {

    super.init(pairEnd, incrementer, counterGroup);
    setMapperArguments(DEFAULT_ARGUMENTS);
  }

}
