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

package fr.ens.transcriptome.eoulsan.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.DF;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.VersionInfo;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanLogger;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.steps.StepResult;
import fr.ens.transcriptome.eoulsan.util.LinuxCpuInfo;
import fr.ens.transcriptome.eoulsan.util.LinuxMemInfo;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.hadoop.PathUtils;

/**
 * This class define an executor for hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class HadoopExecutor extends Executor {

  /** Logger */
  private static final Logger LOGGER = EoulsanLogger.getLogger();

  private Configuration conf;

  private static final String ROOT_PATH = "/";
  private static final String VAR_PATH = "/var";
  private static final String TMP_PATH = "/tmp";

  @Override
  protected void writeStepLogs(final StepResult result) {

    if (result == null || result.getStep() == null)
      return;

    final boolean debug = EoulsanRuntime.getSettings().isDebug();

    try {

      final Path logPath = new Path(getArguments().getLogPathname());
      final Path basePath = new Path(getArguments().getBasePathname());
      final FileSystem logFs = logPath.getFileSystem(this.conf);
      final FileSystem baseFs = basePath.getFileSystem(this.conf);

      if (!logFs.exists(logPath))
        logFs.mkdirs(logPath);

      final String logFilename = result.getStep().getId();

      // Write logs in the log directory
      writeResultLog(new Path(logPath, logFilename + ".log"), logFs, result);
      if (debug)
        writeErrorLog(new Path(logPath, logFilename + ".err"), logFs, result);

      // Write logs in the base directory
      writeResultLog(new Path(basePath, logFilename + ".log"), baseFs, result);
      if (debug)
        writeErrorLog(new Path(basePath, logFilename + ".err"), baseFs, result);

      // Write the catlog of the base path
      if (debug)
        writeDirectoryCatalog(new Path(logPath, logFilename + ".cat"), logFs);

    } catch (IOException e) {

      Common.showAndLogErrorMessage("Unable to create log file for "
          + result.getStep() + " step.");
    }

  }

  @Override
  protected void checkTemporaryDirectory() {

    // Do nothing
  }

  private void writeResultLog(final Path logPath, final FileSystem fs,
      final StepResult result) throws IOException {

    final Writer writer =
        new OutputStreamWriter(fs.create(logPath),
            Globals.DEFAULT_FILE_ENCODING);

    final String data = result.getLogMessage();

    if (data != null)
      writer.write(data);
    writer.close();
  }

  private void writeErrorLog(final Path logPath, final FileSystem fs,
      final StepResult result) throws IOException {

    final Writer writer =
        new OutputStreamWriter(fs.create(logPath),
            Globals.DEFAULT_FILE_ENCODING);

    final String data = result.getErrorMessage();
    final Exception e = result.getException();

    if (data != null)
      writer.write(data);

    if (e != null) {

      writer.write("\n");
      writer.write("Exception: " + e.getClass().getName() + "\n");
      writer.write("Message: " + e.getMessage() + "\n");
      writer.write("StackTrace:\n");
      e.printStackTrace(new PrintWriter(writer));
    }

    writer.close();
  }

  private void writeDirectoryCatalog(final Path catPath, final FileSystem fs)
      throws IOException {

    final Writer writer =
        new OutputStreamWriter(fs.create(catPath),
            Globals.DEFAULT_FILE_ENCODING);

    final Path basePath = new Path(getArguments().getBasePathname());
    final FileSystem baseFs = basePath.getFileSystem(this.conf);

    final StringBuilder sb = new StringBuilder();

    final FileStatus[] files = baseFs.listStatus(basePath);

    long count = 0;

    if (files != null)
      for (FileStatus f : files) {

        if (f.isDir())
          sb.append("D ");
        else
          sb.append("  ");

        sb.append(new Date(f.getModificationTime()));
        sb.append("\t");

        sb.append(String.format("%10d", f.getLen()));
        sb.append("\t");

        sb.append(f.getPath().getName());
        sb.append("\n");

        count += f.getLen();
      }
    sb.append(count);
    sb.append(" bytes in ");
    sb.append(basePath);
    sb.append(" directory.\n");

    writer.write(sb.toString());
    writer.close();
  }

  @Override
  protected void logSysInfo() {

    HadoopInfo();

    if (EoulsanRuntime.getSettings().isDebug())
      sysInfo(conf);
  }

  //
  // Other methods
  //

  private void sysInfo(final Configuration conf) {

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
    } catch (IOException e) {

      LOGGER.warning("Error while get system information: " + e.getMessage());
    }
  }

  private static final void parseCpuinfo() throws IOException {

    final LinuxCpuInfo cpuinfo = new LinuxCpuInfo();

    final String modelName = cpuinfo.getModelName();
    final String processor = cpuinfo.getProcessor();
    final String cpuMHz = cpuinfo.getCPUMHz();
    final String bogomips = cpuinfo.getBogoMips();
    final String cores = cpuinfo.getCores();

    LOGGER.info("SYSINFO CPU model name: "
        + (modelName == null ? "NA" : modelName));
    LOGGER.info("SYSINFO CPU count: "
        + (processor == null ? "NA" : ""
            + (Integer.parseInt(processor.trim()) + 1)));
    LOGGER.info("SYSINFO CPU cores: " + (cores == null ? "NA" : cores));
    LOGGER.info("SYSINFO CPU clock: "
        + (cpuMHz == null ? "NA" : cpuMHz) + " MHz");
    LOGGER.info("SYSINFO Bogomips: " + (bogomips == null ? "NA" : bogomips));
  }

  private static final void parseMeminfo() throws IOException {

    final LinuxMemInfo meminfo = new LinuxMemInfo();
    final String memTotal = meminfo.getMemTotal();

    LOGGER.info("SYSINFO Mem Total: " + (memTotal == null ? "NA" : memTotal));
  }

  private static final void df(final File f, final Configuration conf)
      throws IOException {

    DF df = new DF(f, conf);

    LOGGER.info("SYSINFO "
        + f + " " + StringUtils.sizeToHumanReadable(df.getCapacity())
        + " capacity, " + StringUtils.sizeToHumanReadable(df.getUsed())
        + " used, " + StringUtils.sizeToHumanReadable(df.getAvailable())
        + " available, " + df.getPercentUsed() + "% used");

  }

  private static final void HadoopInfo() {

    LOGGER.info("SYSINFO Hadoop version: " + VersionInfo.getVersion());
    LOGGER.info("SYSINFO Hadoop revision: " + VersionInfo.getRevision());
    LOGGER.info("SYSINFO Hadoop date: " + VersionInfo.getDate());
    LOGGER.info("SYSINFO Hadoop user: " + VersionInfo.getUser());
    LOGGER.info("SYSINFO Hadoop url: " + VersionInfo.getUrl());
  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param arguments executor arguments
   * @param conf Hadoop configuration
   * @throws EoulsanException if an error occurs while loading the design file
   *           or the workflow file
   */
  public HadoopExecutor(final ExecutorArguments arguments,
      final Configuration conf) throws IOException, EoulsanException {

    super(arguments);

    if (conf == null)
      throw new NullPointerException("The configuration is null.");

    this.conf = conf;

    // Create Log directory if necessary
    final Path logPath = new Path(arguments.getLogPathname());
    if (!logPath.getFileSystem(conf).exists(logPath))
      PathUtils.mkdirs(logPath, conf);

    // Create Output directory if necessary
    final Path outputPath = new Path(arguments.getOutputPathname());
    if (!outputPath.getFileSystem(conf).exists(outputPath))
      PathUtils.mkdirs(outputPath, conf);
  }

}
