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

package fr.ens.transcriptome.eoulsan.programs.mgmt;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;
import java.util.logging.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.programs.expression.hadoop.ExpressionHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.FilterAndSoapMapReadsHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.FilterReadsHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.FilterSamplesHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mapping.hadoop.SoapMapReadsHadoopMain;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.HDFSDataDownloadStep;
import fr.ens.transcriptome.eoulsan.programs.mgmt.upload.HDFSDataUploadStep;
import fr.ens.transcriptome.eoulsan.util.PathUtils;

/**
 * This class define an executor for hadoop mode.
 * @author Laurent Jourdren
 */
public class HadoopAnalysisExecutor extends Executor {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private Configuration conf;
  private Path designPath;
  private Design design;

  private final StepsRegistery registery = new StepsRegistery();

  @Override
  protected Step getStep(String stepName) {

    return this.registery.getStep(stepName);
  }

  @Override
  protected Design loadDesign() throws EoulsanException {

    if (this.design != null)
      return this.design;

    try {

      final DesignReader dr =
          new SimpleDesignReader(this.designPath.toUri().toURL().openStream());

      return dr.read();

    } catch (IOException e) {
      throw new EoulsanException("Error while reading design file: ", e);
    }
  }

  @Override
  protected void writeStepLogs(final StepResult result) {

    if (result == null)
      return;

    try {

      final Path logPath = new Path(getInfo().getLogPathname());
      final Path basePath = new Path(getInfo().getBasePathname());
      final FileSystem logFs = logPath.getFileSystem(this.conf);
      final FileSystem baseFs = basePath.getFileSystem(this.conf);

      if (!logFs.exists(logPath))
        logFs.mkdirs(logPath);

      final String logFilename = result.getStep().getLogName();

      // Write logs in the log directory
      writeResultLog(new Path(logPath, logFilename + ".log"), logFs, result);
      writeErrorLog(new Path(logPath, logFilename + ".err"), logFs, result);

      // Write logs in the base directory
      writeResultLog(new Path(basePath, logFilename + ".log"), baseFs, result);
      writeErrorLog(new Path(basePath, logFilename + ".err"), baseFs, result);

      // Write the catlog of the base path
      writeDirectoryCatalog(new Path(logPath, logFilename + ".cat"), logFs);

    } catch (IOException e) {

      logger.severe("Unable to create log file for "
          + result.getStep() + " step.");
      System.err.println("Unable to create log file for "
          + result.getStep() + " step.");
    }

  }

  private void writeResultLog(final Path logPath, final FileSystem fs,
      final StepResult result) throws IOException {

    final Writer writer = new OutputStreamWriter(fs.create(logPath));

    final String data = result.getLogMessage();

    if (data != null)
      writer.write(data);
    writer.close();
  }

  private void writeErrorLog(final Path logPath, final FileSystem fs,
      final StepResult result) throws IOException {

    final Writer writer = new OutputStreamWriter(fs.create(logPath));

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

    final Writer writer = new OutputStreamWriter(fs.create(catPath));

    final Path basePath = new Path(getInfo().getBasePathname());
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

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private HadoopAnalysisExecutor(final Configuration conf) {

    if (conf == null)
      throw new NullPointerException("The configuration is null.");

    this.conf = conf;

    //
    // Register local steps
    //

    this.registery.addStepType(HDFSDataUploadStep.class);
    this.registery.addStepType(FilterReadsHadoopMain.class);
    this.registery.addStepType(SoapMapReadsHadoopMain.class);
    this.registery.addStepType(FilterAndSoapMapReadsHadoopMain.class);
    this.registery.addStepType(FilterSamplesHadoopMain.class);
    this.registery.addStepType(ExpressionHadoopMain.class);
    this.registery.addStepType(HDFSDataDownloadStep.class);
  }

  /**
   * Constructor
   * @param command command to execute
   * @param designPathname the path the design file
   */
  public HadoopAnalysisExecutor(final Configuration conf,
      final Command command, final Path designPath) {

    this(conf);

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (designPath == null)
      throw new NullPointerException("The design path is null.");

    this.designPath = designPath;
    getInfo().setBasePathname(designPath.getParent().toString());

  }

  /**
   * Constructor
   * @param command command to execute
   * @param designPathname the path the design file
   * @throws IOException if cannot create log or output directory
   */
  public HadoopAnalysisExecutor(final Configuration conf,
      final Command command, final Design design, final Path designPath,
      final Path basePath) throws IOException {

    this(conf);

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (design == null)
      throw new NullPointerException("The design is null.");

    if (designPath == null)
      throw new NullPointerException("The design path is null.");

    if (basePath == null)
      throw new NullPointerException("The base path is null.");

    this.design = design;
    this.designPath = designPath;

    final SimpleExecutorInfo info = getInfo();

    info.setBasePathname(basePath.toString());

    final Path logPath =
        new Path(this.designPath.getParent().toString()
            + "/" + info.getExecutionName());

    final Path outputPath =
        new Path(this.designPath.getParent().toString()
            + "/" + info.getExecutionName());

    if (!logPath.getFileSystem(conf).exists(logPath))
      PathUtils.mkdirs(logPath, conf);
    if (!outputPath.getFileSystem(conf).exists(outputPath))
      PathUtils.mkdirs(outputPath, conf);

    info.setLogPathname(logPath.toString());
    info.setOutputPathname(outputPath.toString());
  }

}
