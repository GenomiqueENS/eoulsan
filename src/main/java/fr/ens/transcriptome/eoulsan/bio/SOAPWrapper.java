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

package fr.ens.transcriptome.eoulsan.bio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Wrapper class for SOAP. TODO set the number of thread to use TODO Handle Path
 * object
 * @author Laurent Jourdren
 */
public final class SOAPWrapper {

  /** Logger. */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
  private static final boolean DEBUG = true;

  private static String soapPath;
  private static String indexerPath;

  private static final String SYNC = SOAPWrapper.class.getName();

  /**
   * Create the soap index in a directory.
   * @param genomeFile path to the genome file
   * @param outputDir output directory for the index
   * @throws IOException if an error occurs while creating the index
   */
  private static void makeIndex(final File genomeFile, final File outputDir)
      throws IOException {

    if (genomeFile == null) {
      throw new IllegalArgumentException("genome file is null");
    }

    if (outputDir == null) {
      throw new IllegalArgumentException("output directory is null");
    }

    LOGGER.info("Start computing SOAP index for " + genomeFile);
    final long startTime = System.currentTimeMillis();

    if (indexerPath == null) {
      synchronized (SYNC) {
        indexerPath = BinariesInstaller.install("2bwt-builder");
      }
    }

    if (!outputDir.exists() && !outputDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    final File tmpGenomeFile = new File(outputDir, genomeFile.getName());

    // Create temporary symbolic link for genome
    FileUtils.createSymbolicLink(genomeFile, tmpGenomeFile);

    // Compute the index
    final String cmd = indexerPath + " " + tmpGenomeFile.getAbsolutePath();
    ProcessUtils.exec(cmd, DEBUG);

    // Remove symbolic link
    if (!tmpGenomeFile.delete()) {
      LOGGER
          .warning("Cannot remove symbolic link while after creating SOAP index");
    }

    final long endTime = System.currentTimeMillis();

    LOGGER.info("Create the SOAP index in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));

  }

  /**
   * Create the soap index in a zip archive.
   * @param is InputStream to use for the genome file
   * @throws IOException if an error occurs while creating the index
   */
  public static File makeIndexInZipFile(final File tempDir, final InputStream is)
      throws IOException {

    LOGGER.info("Copy genome to local disk before computating index");

    final File genomeTmpFile =
        FileUtils.createTempFile(tempDir, Globals.APP_NAME_LOWER_CASE
            + "-genome", "");
    FileUtils.copy(is, FileUtils.createOutputStream(genomeTmpFile));

    final File result = makeIndexInZipFile(tempDir, genomeTmpFile);

    if (!genomeTmpFile.delete()) {
      LOGGER.warning("Cannot delete temporary index zip file");
    }

    return result;
  }

  /**
   * Create the soap index in a zip archive.
   * @param genomeFile path to the genome file
   * @return a File object with the path of the result zip file
   * @throws IOException if an error occurs while creating the index
   */
  private static File makeIndexInZipFile(final File tempDir,
      final File genomeFile) throws IOException {

    LOGGER.info("Start index computation");

    if (indexerPath == null) {
      synchronized (SYNC) {
        indexerPath = BinariesInstaller.install("2bwt-builder");
      }
    }

    final File tmpDir =
        FileUtils.createTempFile(tempDir, Globals.APP_NAME_LOWER_CASE
            + "-soap-genomeindexdir", "");

    if (!(tmpDir.delete())) {
      throw new IOException("Could not delete temp file ("
          + tmpDir.getAbsolutePath() + ")");
    }

    if (!tmpDir.mkdir()) {
      throw new IOException("Unable to create directory for genome index");
    }

    makeIndex(genomeFile, tmpDir);

    final File indexZipFile =
        FileUtils.createTempFile(tempDir, Globals.APP_NAME_LOWER_CASE
            + "-soapgenomeindex", ".zip");

    // Zip index files
    FileUtils.createZip(tmpDir, indexZipFile);

    // Remove temporary directory
    FileUtils.removeDirectory(tmpDir);

    LOGGER.info("End index computation");

    return indexZipFile;
  }

  /**
   * Map reads using soap.
   * @param readsFile reads file
   * @param soapIndexDir soap index file
   * @param outputFile output alignment file
   * @param unmapFile output unmap file
   * @param soapArgs soap arguments
   * @param nbSoapThreads number of threads to use
   * @throws IOException if an error occurs while mapping reads
   */
  public static void map(final File readsFile, final File soapIndexDir,
      final File outputFile, final File unmapFile, final String soapArgs,
      final int nbSoapThreads) throws IOException {

    if (soapPath == null) {
      synchronized (SYNC) {
        soapPath = BinariesInstaller.install("soap");
      }
    }

    FileUtils.checkExistingDirectoryFile(soapIndexDir, "SOAP index directory");

    final File[] indexFiles =
        FileUtils.listFilesByExtension(soapIndexDir, ".index.amb");

    if (indexFiles == null || indexFiles.length != 1) {
      throw new IOException("Unable to get index file for SOAP");
    }

    // Get the path to the index
    final String ambFile = indexFiles[0].getAbsolutePath();

    // Build the command line
    final String cmd =
        soapPath
            + " " + soapArgs + " -p " + nbSoapThreads + " -a "
            + readsFile.getAbsolutePath() + " -D "
            + ambFile.substring(0, ambFile.length() - 4) + " -o "
            + outputFile.getAbsolutePath() + " -u "
            + unmapFile.getAbsolutePath() + " > /dev/null 2> /dev/null";

    LOGGER.info(cmd);

    final int exitValue = ProcessUtils.sh(cmd);

    if (exitValue != 0) {
      throw new IOException("Bad error result for SOAP execution: " + exitValue);
    }
  }

  /**
   * Create a soap command line for mapping reads using soap in pipe mode.
   * @param soapIndexDir soap index file
   * @param soapArgs soap arguments
   * @param nbSoapThreads number of threads to use
   * @throws IOException if an error occurs while mapping reads
   */
  public static String mapPipe(final File soapIndexDir, final String soapArgs,
      final int nbSoapThreads) throws IOException {

    if (soapPath == null) {
      synchronized (SYNC) {
        soapPath = BinariesInstaller.install("soap-pipe");
      }
    }

    FileUtils.checkExistingDirectoryFile(soapIndexDir, "SOAP index directory");

    final File[] indexFiles =
        FileUtils.listFilesByExtension(soapIndexDir, ".index.amb");

    if (indexFiles == null || indexFiles.length != 1) {
      throw new IOException("Unable to get index file for SOAP");
    }

    // Get the path to the index
    final String ambFile = indexFiles[0].getAbsolutePath();

    // Build the command line
    final String cmd =
        soapPath
            + " -P " + soapArgs + " -p " + nbSoapThreads + " -a " + "fakefq.fq"
            + " -D " + ambFile.substring(0, ambFile.length() - 4) + " -o "
            + "fakealign.txt" + " -u " + "fakeunmap.fasta";

    LOGGER.info(cmd);

    // final String soapOutput = ProcessUtils.execToString(cmd);
    // logger.info(soapOutput);
    // ProcessUtils.sh(cmd);
    return cmd;
  }

  //
  // Constructor
  //

  /**
   * No constructor.
   */
  private SOAPWrapper() {

    throw new IllegalStateException();
  }
}
