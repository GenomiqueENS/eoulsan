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

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.BinariesInstaller;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.ProcessUtils;

/**
 * Wrapper class for SOAP. TODO set the number of thread to use TODO Handle Path
 * object
 * @author Laurent Jourdren
 */
public class SOAPWrapper {

  private static final boolean DEBUG = true;

  private static String soapPath;
  private static String indexerPath;

  public static void init() throws IOException {

    if (soapPath == null)
      soapPath = BinariesInstaller.install("soap");
  }

  public static File makeIndex(final File genomeFile, final boolean copyFile)
      throws IOException {

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

    final File tmpGenomeFile = new File(tmpDir, genomeFile.getName());

    if (copyFile)
      FileUtils.copyFile(genomeFile, tmpGenomeFile);
    else
      genomeFile.renameTo(tmpDir);

    final String cmd = indexerPath + " " + tmpGenomeFile.getAbsolutePath();

    ProcessUtils.exec(cmd, DEBUG);

    final File indexZipFile =
        File.createTempFile(Globals.APP_NAME_LOWER_CASE + "-soapgenomeindex",
            ".zip");

    // Zip index files
    FileUtils.createZip(tmpDir, indexZipFile);

    // Remove temporary directory
    FileUtils.removeDirectory(tmpDir);

    return indexZipFile;
  }

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
            + unmapFile.getAbsolutePath();
    if (DEBUG)
      System.out.println(cmd);
    ProcessUtils.exec(cmd, DEBUG);
  }

}
