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
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.ReporterIncrementer;

public class SOAPReadsMapper extends AbstractSequenceReadsMapper {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static final String DEFAULT_ARGUMENTS = "-r 2 -l 28";

  private static final String SYNC = SOAPReadsMapper.class.getName();

  private File outputFile;
  private File unmapFile;
  private File unpairedFile;

  @Override
  public String getMapperName() {

    return "SOAP";
  }

  @Override
  public DataFormat getArchiveFormat() {

    return DataFormats.SOAP_INDEX_ZIP;
  }

  @Override
  protected String getIndexerExecutable() {

    return "2bwt-builder";
  }

  @Override
  protected String getIndexerCommand(String indexerPathname,
      String genomePathname) {

    return indexerPathname + " " + genomePathname;
  }

  @Override
  protected void internalMap(final File readsFile, final File archiveIndexDir)
      throws IOException {

    final String soapPath;

    synchronized (SYNC) {
      soapPath = BinariesInstaller.install("soap");
    }

    this.outputFile =
        FileUtils.createTempFile(readsFile.getParentFile(), "soap-outputFile-",
            ".soap");
    this.unmapFile =
        FileUtils.createTempFile(readsFile.getParentFile(), "soap-outputFile-",
            ".unmap");

    // Build the command line
    final String cmd =
        soapPath
            + " " + getMapperArguments() + " -p " + getThreadsNumber() + " -a "
            + readsFile.getAbsolutePath()
            + " -D "
            // + ambFile.substring(0, ambFile.length() - 4)
            + getIndexPath(archiveIndexDir, ".index.amb", 4) + " -o "
            + outputFile.getAbsolutePath() + " -u "
            + unmapFile.getAbsolutePath() + " > /dev/null 2> /dev/null";

    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for SOAP execution: " + exitValue);
    }

  }

  @Override
  protected void internalMap(File readsFile1, File readsFile2,
      File archiveIndexDir) throws IOException {

    final String soapPath;

    synchronized (SYNC) {
      soapPath = BinariesInstaller.install("soap");
    }

    this.outputFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".soap");
    this.unmapFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".unmap");
    this.unpairedFile =
        FileUtils.createTempFile(readsFile1.getParentFile(), "soap-output-",
            ".unpaired");

    // Build the command line
    final String cmd =
        soapPath
            + " " + getMapperArguments() + " -p " + getThreadsNumber() + " -a "
            + readsFile1.getAbsolutePath()
            + " -b "
            + readsFile2.getAbsolutePath()
            + " -D "
            // + ambFile.substring(0, ambFile.length() - 4)
            + getIndexPath(archiveIndexDir, ".index.amb", 4) + " -o "
            + outputFile.getAbsolutePath() + " -u "
            + unmapFile.getAbsolutePath() + " -2 "
            + unpairedFile.getAbsolutePath() + " > /dev/null 2> /dev/null";

    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for SOAP execution: " + exitValue);
    }

  }

  @Override
  public File getSAMFile() throws IOException {

    final File resultFile =
        FileUtils.createTempFile(this.outputFile.getParentFile(),
            "soap-output-", ".sam");

    final SOAP2SAM convert =
        new SOAP2SAM(this.outputFile, this.unmapFile, resultFile);
    convert.convert(isPairEnd());

    return resultFile;
  }

  @Override
  public void clean() {

    deleteFile(this.outputFile);
    deleteFile(this.unmapFile);
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
