package fr.ens.transcriptome.eoulsan.core;

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
public class SOAPWrapper {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);
  private static final boolean DEBUG = true;

  private static String soapPath;
  private static String indexerPath;

  /**
   * Create the soap index in a directory
   * @param genomeFile path to the genome file
   * @param outputDir output directory for the index
   * @throws IOException if an error occurs while creating the index
   */
  public static void makeIndex(final File genomeFile, final File outputDir)
      throws IOException {

    if (genomeFile == null)
      throw new NullPointerException("genome file is null");

    if (outputDir == null)
      throw new NullPointerException("output directory is null");

    logger.info("Start computing SOAP index for " + genomeFile);
    final long startTime = System.currentTimeMillis();

    if (indexerPath == null)
      indexerPath = BinariesInstaller.install("2bwt-builder");

    if (!outputDir.exists())
      if (!outputDir.mkdir())
        throw new IOException("Unable to create directory for genome index");

    final File tmpGenomeFile = new File(outputDir, genomeFile.getName());

    // Create temporary symbolic link for genome
    FileUtils.createSymbolicLink(genomeFile, tmpGenomeFile);

    // Compute the index
    final String cmd = indexerPath + " " + tmpGenomeFile.getAbsolutePath();
    ProcessUtils.exec(cmd, DEBUG);

    // Remove symbolic link
    tmpGenomeFile.delete();

    final long endTime = System.currentTimeMillis();

    logger.info("Create the SOAP index in "
        + StringUtils.toTimeHumanReadable(endTime - startTime));

  }

  /**
   * Create the soap index in a zip archive
   * @param genomeFile path to the genome file
   * @throws IOException if an error occurs while creating the index
   */
  public static File makeIndexInZipFile(final InputStream is)
      throws IOException {

    logger.info("Copy genome to local disk before computating index");
    
    File genomeTmpFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-genome", "");
    FileUtils.copy(is, FileUtils.createOutputStream(genomeTmpFile));

    final File result = makeIndexInZipFile(genomeTmpFile);

    genomeTmpFile.delete();

    return result;
  }

  /**
   * Create the soap index in a zip archive
   * @param genomeFile path to the genome file
   * @throws IOException if an error occurs while creating the index
   */
  public static File makeIndexInZipFile(final File genomeFile)
      throws IOException {

    logger.info("Start index computation");
    
    if (indexerPath == null)
      indexerPath = BinariesInstaller.install("2bwt-builder");

    final File tmpDir =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE
            + "-soap-genomeindexdir", "");

    if (!(tmpDir.delete()))
      throw new IOException("Could not delete temp file ("
          + tmpDir.getAbsolutePath() + ")");

    if (!tmpDir.mkdir())
      throw new IOException("Unable to create directory for genome index");

    makeIndex(genomeFile, tmpDir);

    final File indexZipFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-soapgenomeindex",
            ".zip");

    // Zip index files
    FileUtils.createZip(tmpDir, indexZipFile);

    // Remove temporary directory
    FileUtils.removeDirectory(tmpDir);

    logger.info("End index computation");
    
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

    if (soapPath == null)
      soapPath = BinariesInstaller.install("soap");

    FileUtils.checkExistingDirectoryFile(soapIndexDir, "SOAP index directory");

    final File[] indexFiles =
        FileUtils.listFilesByExtension(soapIndexDir, ".index.amb");

    if (indexFiles == null || indexFiles.length != 1)
      throw new IOException("Unable to get index file for SOAP");

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

    logger.info(cmd);

    // final String soapOutput = ProcessUtils.execToString(cmd);
    // logger.info(soapOutput);
    ProcessUtils.sh(cmd);
  }

  /**
   * Create a soap command line for mapping reads using soap in pipe mode.
   * @param soapIndexDir soap index file
   * @param outputFile output alignment file
   * @param unmapFile output unmap file
   * @param soapArgs soap arguments
   * @param nbSoapThreads number of threads to use
   * @throws IOException if an error occurs while mapping reads
   */
  public static String mapPipe(final File soapIndexDir, final String soapArgs,
      final int nbSoapThreads) throws IOException {

    if (soapPath == null)
      soapPath = BinariesInstaller.install("soap-pipe");

    FileUtils.checkExistingDirectoryFile(soapIndexDir, "SOAP index directory");

    final File[] indexFiles =
        FileUtils.listFilesByExtension(soapIndexDir, ".index.amb");

    if (indexFiles == null || indexFiles.length != 1)
      throw new IOException("Unable to get index file for SOAP");

    // Get the path to the index
    final String ambFile = indexFiles[0].getAbsolutePath();

    // Build the command line
    final String cmd =
        soapPath
            + " -P " + soapArgs + " -p " + nbSoapThreads + " -a " + "fakefq.fq"
            + " -D " + ambFile.substring(0, ambFile.length() - 4) + " -o "
            + "fakealign.txt" + " -u " + "fakeunmap.fasta";

    logger.info(cmd);

    // final String soapOutput = ProcessUtils.execToString(cmd);
    // logger.info(soapOutput);
    // ProcessUtils.sh(cmd);
    return cmd;
  }

}
