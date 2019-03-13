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

package fr.ens.biologie.genomique.eoulsan;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.StreamHandler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.VersionInfo;

import fr.ens.biologie.genomique.eoulsan.Infos.Info;

/**
 * Main class in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MainHadoop extends Main {

  private static final String LAUNCH_MODE_NAME = "hadoop";

  private Configuration conf;

  @Override
  protected void initializeRuntime(final Settings settings) {

    this.conf =
        HadoopEoulsanRuntime.newEoulsanRuntime(settings).getConfiguration();
  }

  @Override
  protected String getHelpEoulsanCommand() {

    return "hadoop jar " + Globals.APP_NAME_LOWER_CASE + ".jar";
  }

  @Override
  protected Handler getLogHandler(final URI logFile) throws IOException {

    if (logFile == null) {
      throw new NullPointerException("The log file is null");
    }

    final Path loggerPath = new Path(logFile);
    final FileSystem loggerFs = loggerPath.getFileSystem(this.conf);

    final Path parentPath = loggerPath.getParent();

    // Create parent directory if necessary
    if (!loggerFs.exists(loggerPath.getParent())) {
      if (!loggerFs.mkdirs(loggerPath.getParent())) {
        throw new IOException("Unable to create directory "
            + parentPath + " for log file:" + logFile);
      }
    }

    return new StreamHandler(loggerFs.create(loggerPath),
        Globals.LOG_FORMATTER);
  }

  @Override
  protected void sysInfoLog() {

    // Log standard system properties
    super.sysInfoLog();

    try {

      Infos.log(Level.INFO, Infos.cpuInfo());
      Infos.log(Level.INFO, Infos.memInfo());
      Infos.log(Level.INFO, Infos.partitionInfo(EoulsanRuntime.getSettings()));

      // Log the usage of the hadoop temporary directory partition
      final String hadoopTmp = this.conf.get("hadoop.tmp.dir");
      if (hadoopTmp != null) {
        Infos.log(Level.INFO,
            singletonList(Infos.diskFreeInfo(new File(hadoopTmp))));
      }

      // Log the usage of the Java temporary directory partition
      final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      if (tmpDir != null && tmpDir.exists() && tmpDir.isDirectory()) {
        Infos.log(Level.INFO, Infos.diskFreeInfo(new File(hadoopTmp)));
      }

    } catch (IOException e) {
      getLogger()
          .severe("Error while getting system information: " + e.getMessage());
    }

    // Log Hadoop information
    Infos.log(Level.INFO, new Info("Hadoop version", VersionInfo.getVersion()));
    Infos.log(Level.INFO,
        new Info("Hadoop revision", VersionInfo.getRevision()));
    Infos.log(Level.INFO, new Info("Hadoop date", VersionInfo.getDate()));
    Infos.log(Level.INFO, new Info("Hadoop user", VersionInfo.getUser()));
    Infos.log(Level.INFO, new Info("Hadoop url", VersionInfo.getUrl()));
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param args command line arguments
   */
  protected MainHadoop(final String[] args) {

    super(LAUNCH_MODE_NAME, args);
  }

}
