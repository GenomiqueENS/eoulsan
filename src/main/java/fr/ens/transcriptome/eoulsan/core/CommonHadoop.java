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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define common constants and other methods specific to Hadoop mode.
 * @author Laurent Jourdren
 */
public class CommonHadoop {

  public static final String AWS_S3_SECRET_ACCESS_KEY_PARAM_NAME =
      "fs.s3n.awsSecretAccessKey";
  public static final String AWS_S3_ACCESSKEY_ID_PARAM_KEY =
      "fs.s3n.awsAccessKeyId";

  public static final int CHECK_COMPLETION_TIME = 5000;
  public static final String HADOOP_PARAMETER_PREFIX = "hadoop.conf.";

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
        PathUtils.listPathsBySuffix(basePath, Common.FASTA_EXTENSION, conf);
    if (genomePaths.size() == 0)
      throw new IOException("Genome file not found.");
    if (genomePaths.size() > 1)
      throw new IOException("More than one genome file found.");

    return genomePaths.get(0);
  }

  /**
   * Write log data.
   * @param logPath Path of the log file
   * @param data data to write
   * @throws IOException if an error occurs while writing log file
   */
  public static void writeLog(final Path logPath, final long startTime,
      final String data) throws IOException {

    final FileSystem fs = logPath.getFileSystem(new Configuration());
    Common.writeLog(fs.create(logPath), startTime, data);
  }

  /**
   * Return the path of entry data either a directory or a file with an
   * extension
   * @param path the path of the data
   * @param extension the extension of the file
   * @return the path of the data
   * @throws IOException if an error occurs while testing files
   */
  public static Path selectDirectoryOrFile(final Path path,
      final String extension) throws IOException {

    final Configuration conf = new Configuration();

    if (PathUtils.isExistingDirectoryFile(path, conf))
      return path;

    final Path filePath =
        new Path(path.getParent(), path.getName() + extension);

    if (PathUtils.isFile(filePath, conf))
      return filePath;

    return null;
  }

  /**
   * Print the contents of a Configuration object
   * @param conf Configuration object
   * @param prefix prefix used for keys filtering
   */
  public static void printConfiguration(final Configuration conf) {

    printConfiguration(conf, null);
  }

  /**
   * Print the contents of a Configuration object
   * @param conf Configuration object
   * @param prefix prefix used for keys filtering
   */
  public static void printConfiguration(final Configuration conf,
      final String prefix) {

    if (conf == null)
      return;

    final Iterator<Map.Entry<String, String>> it = conf.iterator();

    System.out.println("=== Start print configuration ===");

    while (it.hasNext()) {

      final Map.Entry<String, String> e = it.next();

      if (prefix == null || e.getKey().startsWith(prefix))
        System.out.println(e.getKey() + "\t" + e.getValue());

    }
    System.out.println("=== End print configuration ===");

  }

  /**
   * Create a new Configuration object from global parameters
   * @param globalParameters global parameter as a set of Parameter object
   * @return a new Configuration object
   */
  public static final Configuration createConfiguration(
      final Set<Parameter> globalParameters) throws EoulsanException {

    if (globalParameters == null)
      return null;

    final Configuration conf = new Configuration();

    for (Parameter p : globalParameters) {

      final String keyName = p.getName();

      if (keyName.startsWith(HADOOP_PARAMETER_PREFIX)) {

        final String hadoopKey =
            keyName.substring(HADOOP_PARAMETER_PREFIX.length());
        conf.set(hadoopKey, p.getStringValue());
      }
    }

    return conf;
  }

  /**
   * Create a new Configuration object from global parameters
   * @param settings Settings of the application
   * @return a new Configuration object
   */
  public static final Configuration createConfigurationFromSettings(
      final Settings settings) {

    if (settings == null)
      return null;

    final Configuration conf = new Configuration();

    for (String keyName : settings.getSettingsNames()) {

      if (keyName.startsWith(HADOOP_PARAMETER_PREFIX)) {

        final String hadoopKey =
            keyName.substring(HADOOP_PARAMETER_PREFIX.length());

        conf.set(hadoopKey, settings.getSetting(keyName));
      }
    }

    return conf;
  }

}
