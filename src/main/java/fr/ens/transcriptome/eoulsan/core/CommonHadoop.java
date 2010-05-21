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

package fr.ens.transcriptome.eoulsan.core;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define common constants and other methods specific to Hadoop mode.
 * @author Laurent Jourdren
 */
public class CommonHadoop extends Common {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  public static final int CHECK_COMPLETION_TIME = 5000;
  public static final String SAMPLE_FILE_PREFIX = "sample_";
  public static final String GENOME_FILE_PREFIX = "genome_";
  public static final String GENOME_SOAP_INDEX_FILE_PREFIX =
      "genome_soap_index_";
  public static final String GENOME_SOAP_INDEX_FILE_SUFFIX = ".zip";
  public static final String ANNOTATION_FILE_PREFIX = "annotation_";

  /**
   * Retrieve the genome file name from the files of a directory
   * @param basePath Base path directory
   * @param conf Hadoop configuration
   * @return the genome file path
   * @throws IOException if the genome file can't be identified
   */
  public static Path getGenomeFilePath(final Path basePath,
      final Configuration conf) throws IOException {

    if (basePath == null)
      throw new NullPointerException("Base path is null");

    final List<Path> genomePaths =
        PathUtils.listPathsBySuffix(basePath, CommonHadoop.FASTA_EXTENSION,
            conf);
    if (genomePaths.size() == 0)
      throw new IOException("Genome file not found.");
    if (genomePaths.size() > 1)
      throw new IOException("More than one genome file found.");

    return genomePaths.get(0);
  }

  /**
   * Show an error message and exit program.
   * @param message
   */
  public static void error(final String message) {

    logger.severe(message);
    System.err.println(message);
    System.exit(1);
  }

  /**
   * Show an error message and exit program.
   * @param message
   */
  public static void error(final String message, final Exception e) {

    error(message + e.getMessage());
  }

  /**
   * Write log data.
   * @param logPath Path of the log file
   * @param data data to write
   * @throws IOException if an error occurs while writing log file
   */
  public static void writeLog(final Path logPath, final long startTime,
      final String data) throws IOException {

    FileSystem fs = PathUtils.getFileSystem(logPath, new Configuration());
    writeLog(fs.create(logPath), startTime, data);
  }

  public static Path selectDirectoryOrFile(final Path path,
      final String extension) {

    final Configuration conf = new Configuration();

    try {

      if (PathUtils.isExistingDirectoryFile(path, conf))
        return path;

      final Path filePath =
          new Path(path.getParent(), path.getName() + extension);

      if (PathUtils.isFile(filePath, conf))
        return filePath;

    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
    }

    return null;
  }

}
