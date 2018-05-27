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

package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DF;
import org.apache.hadoop.util.VersionInfo;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.HadoopEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.util.LinuxCpuInfo;
import fr.ens.biologie.genomique.eoulsan.util.LinuxMemInfo;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class show in log some Hadoop information.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class HadoopInfo {

  private static final String ROOT_PATH = "/";
  private static final String VAR_PATH = "/var";
  private static final String TMP_PATH = "/tmp";

  public static void logHadoopSysInfo() {

    if (EoulsanRuntime.getRuntime().getMode() != EoulsanExecMode.AMAZON) {
      return;
    }

    logHadoopVersionInfo();

    // Get Eoulsan Hadoop runtime
    HadoopEoulsanRuntime runtime =
        (HadoopEoulsanRuntime) EoulsanRuntime.getRuntime();

    if (EoulsanRuntime.getSettings().isDebug()) {
      sysInfo(runtime.getConfiguration());
    }
  }

  //
  // Other methods
  //

  private static void sysInfo(final Configuration conf) {

    try {

      parseCpuinfo();
      parseMeminfo();

      df(new File(ROOT_PATH), conf);
      df(new File(TMP_PATH), conf);
      df(new File(VAR_PATH), conf);

      // Log the usage of the hadoop temporary directory partition
      final String hadoopTmp = conf.get("hadoop.tmp.dir");
      if (hadoopTmp != null) {
        df(new File(hadoopTmp), conf);
      }

      // Log the usage of the Java temporary directory partition
      final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
      if (tmpDir != null && tmpDir.exists() && tmpDir.isDirectory()) {
        df(tmpDir, conf);
      }
    } catch (IOException e) {

      getLogger()
          .warning("Error while get system information: " + e.getMessage());
    }
  }

  private static void parseCpuinfo() throws IOException {

    final LinuxCpuInfo cpuinfo = new LinuxCpuInfo();

    final String modelName = cpuinfo.getModelName();
    final String processor = cpuinfo.getProcessor();
    final String cpuMHz = cpuinfo.getCPUMHz();
    final String bogomips = cpuinfo.getBogoMips();
    final String cores = cpuinfo.getCores();

    getLogger().info(
        "SYSINFO CPU model name: " + (modelName == null ? "NA" : modelName));
    getLogger().info("SYSINFO CPU count: "
        + (processor == null
            ? "NA" : "" + (Integer.parseInt(processor.trim()) + 1)));
    getLogger().info("SYSINFO CPU cores: " + (cores == null ? "NA" : cores));
    getLogger().info(
        "SYSINFO CPU clock: " + (cpuMHz == null ? "NA" : cpuMHz) + " MHz");
    getLogger()
        .info("SYSINFO Bogomips: " + (bogomips == null ? "NA" : bogomips));
  }

  private static void parseMeminfo() throws IOException {

    final LinuxMemInfo meminfo = new LinuxMemInfo();
    final String memTotal = meminfo.getMemTotal();

    getLogger()
        .info("SYSINFO Mem Total: " + (memTotal == null ? "NA" : memTotal));
  }

  private static void df(final File f, final Configuration conf)
      throws IOException {

    DF df = new DF(f, conf);

    getLogger().info("SYSINFO "
        + f + " " + StringUtils.sizeToHumanReadable(df.getCapacity())
        + " capacity, " + StringUtils.sizeToHumanReadable(df.getUsed())
        + " used, " + StringUtils.sizeToHumanReadable(df.getAvailable())
        + " available, " + df.getPercentUsed() + "% used");

  }

  private static void logHadoopVersionInfo() {

    getLogger().info("SYSINFO Hadoop version: " + VersionInfo.getVersion());
    getLogger().info("SYSINFO Hadoop revision: " + VersionInfo.getRevision());
    getLogger().info("SYSINFO Hadoop date: " + VersionInfo.getDate());
    getLogger().info("SYSINFO Hadoop user: " + VersionInfo.getUser());
    getLogger().info("SYSINFO Hadoop url: " + VersionInfo.getUrl());
  }

}
