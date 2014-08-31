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

package fr.ens.transcriptome.eoulsan;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.StreamHandler;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DF;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.VersionInfo;

import fr.ens.transcriptome.eoulsan.util.LinuxCpuInfo;
import fr.ens.transcriptome.eoulsan.util.LinuxMemInfo;
import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * Main class in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class MainHadoop extends Main {

  private static final String LAUNCH_MODE_NAME = "hadoop";

  private static final String ROOT_PATH = "/";
  private static final String VAR_PATH = "/var";
  private static final String TMP_PATH = "/tmp";

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
  protected Handler getLogHandler(final String logFile) throws IOException {

    final Path loggerPath = new Path(logFile);
    final FileSystem loggerFs = loggerPath.getFileSystem(this.conf);

    // Create parent directory if necessary
    if (!loggerFs.exists(loggerPath.getParent()))
      loggerFs.mkdirs(loggerPath.getParent());

    return new StreamHandler(loggerFs.create(loggerPath), Globals.LOG_FORMATTER);
  }

  @Override
  protected void sysInfoLog() {

    try {

      parseCpuinfo();
      parseMeminfo();

      df(new File(ROOT_PATH), conf);
      df(new File(TMP_PATH), conf);
      df(new File(VAR_PATH), conf);

      // Log the usage of the hadoop temporary directory partition
      final String hadoopTmp = conf.get("hadoop.tmp.dir");
      if (hadoopTmp != null)
        df(new File(hadoopTmp), conf);

      // Log the usage of the Java temporary directory partition
      final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      if (tmpDir != null && tmpDir.exists() && tmpDir.isDirectory())
        df(tmpDir, conf);

      // Log Hadoop information
      HadoopInfo();

    } catch (IOException e) {
      getLogger().severe(
          "Error while getting system information: " + e.getMessage());
    }

  }

  /**
   * Parse information from /etc/cpuinfo
   * @throws IOException if an error occurs while parsing information
   */
  private static final void parseCpuinfo() throws IOException {

    final LinuxCpuInfo cpuinfo = new LinuxCpuInfo();

    final String modelName = cpuinfo.getModelName();
    final String processor = cpuinfo.getProcessor();
    final String cpuMHz = cpuinfo.getCPUMHz();
    final String bogomips = cpuinfo.getBogoMips();
    final String cores = cpuinfo.getCores();

    getLogger().info(
        "SYSINFO CPU model name: " + (modelName == null ? "NA" : modelName));
    getLogger().info(
        "SYSINFO CPU count: "
            + (processor == null ? "NA" : ""
                + (Integer.parseInt(processor.trim()) + 1)));
    getLogger().info("SYSINFO CPU cores: " + (cores == null ? "NA" : cores));
    getLogger().info(
        "SYSINFO CPU clock: " + (cpuMHz == null ? "NA" : cpuMHz) + " MHz");
    getLogger().info(
        "SYSINFO Bogomips: " + (bogomips == null ? "NA" : bogomips));
  }

  /**
   * Parse information from /etc/meminfo
   * @throws IOException if an error occurs while parsing information
   */
  private static final void parseMeminfo() throws IOException {

    final LinuxMemInfo meminfo = new LinuxMemInfo();
    final String memTotal = meminfo.getMemTotal();

    getLogger().info(
        "SYSINFO Mem Total: " + (memTotal == null ? "NA" : memTotal));
  }

  /**
   * Log disk free information.
   * @param f file
   * @param conf Hadoop configuration
   * @throws IOException if an error occurs
   */
  private static final void df(final File f, final Configuration conf)
      throws IOException {

    DF df = new DF(f, conf);

    getLogger().info(
        "SYSINFO "
            + f + " " + StringUtils.sizeToHumanReadable(df.getCapacity())
            + " capacity, " + StringUtils.sizeToHumanReadable(df.getUsed())
            + " used, " + StringUtils.sizeToHumanReadable(df.getAvailable())
            + " available, " + df.getPercentUsed() + "% used");

  }

  /**
   * Log some Hadoop information.
   */
  private static final void HadoopInfo() {

    getLogger().info("SYSINFO Hadoop version: " + VersionInfo.getVersion());
    getLogger().info("SYSINFO Hadoop revision: " + VersionInfo.getRevision());
    getLogger().info("SYSINFO Hadoop date: " + VersionInfo.getDate());
    getLogger().info("SYSINFO Hadoop user: " + VersionInfo.getUser());
    getLogger().info("SYSINFO Hadoop url: " + VersionInfo.getUrl());
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
