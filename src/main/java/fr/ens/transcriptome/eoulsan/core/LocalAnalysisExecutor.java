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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.steps.anadiff.local.AnaDiffLocalMain;
import fr.ens.transcriptome.eoulsan.steps.expression.local.ExpressionLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.local.FilterReadsLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.local.FilterSamplesLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.local.SoapMapReadsLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.local.ExecInfoLogStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an executor for local mode.
 * @author Laurent Jourdren
 */
public class LocalAnalysisExecutor extends Executor {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private AbstractExecutorInfo info = new LocalExecutorInfo();
  private final File designFile;

  @Override
  protected AbstractExecutorInfo getExecutorInfo() {

    return this.info;
  }

  @Override
  protected Design loadDesign() throws EoulsanException {

    if (this.designFile == null)
      throw new EoulsanException("The design file is null.");

    if (!this.designFile.exists())
      throw new EoulsanException("The design file does not exist: "
          + this.designFile);

    DesignReader dr = new SimpleDesignReader(designFile);
    return dr.read();
  }

  @Override
  protected void writeStepLogs(final StepResult result) {

    if (result == null)
      return;

    try {

      final File logDir =
          new File(this.designFile.getParent(), getExecutorInfo()
              .getExecutionName());

      if (!logDir.exists())
        if (!logDir.mkdirs()) {
          throw new IOException("Can not create log directory: "
              + logDir.getAbsolutePath());
        }

      final String logFilename = result.getStep().getLogName();

      final Writer writer =
          FileUtils
              .createBufferedWriter(new File(logDir, logFilename + ".log"));

      if (result.getLogMessage() != null)
        writer.write(result.getLogMessage());
      else
        writer.write("Nothing to log.");
      writer.close();

    } catch (IOException e) {

      logger.severe("Unable to create log file for "
          + result.getStep() + " step.");
      System.err.println("Unable to create log file for "
          + result.getStep() + " step.");
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor
   * @param command command to execute
   * @param design the path to the design filename
   */
  public LocalAnalysisExecutor(final Command command, final File designFile,
      final File paramFile) {

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (designFile == null)
      throw new NullPointerException("The design file is null");

    this.designFile = designFile;

    final AbstractExecutorInfo info = getExecutorInfo();
    info.setBasePathname(designFile.getAbsoluteFile().getParentFile()
        .getAbsolutePath());
    info.setDesignPathname(designFile.getAbsolutePath());
    info.setParameterPathname(paramFile.getAbsolutePath());

    final File logDir =
        new File(designFile.getAbsoluteFile().getParent().toString()
            + "/" + info.getExecutionName());

    final File outputDir =
        new File(designFile.getAbsoluteFile().getParent().toString()
            + "/" + info.getExecutionName());

    info.setOutputPathname(outputDir.getAbsolutePath());
    info.setLogPathname(logDir.getAbsolutePath());

    //
    // Register local steps
    //

    final StepsRegistery registery = StepsRegistery.getInstance();

    registery.addStepType(ExecInfoLogStep.class);
    registery.addStepType(FilterReadsLocalStep.class);
    registery.addStepType(SoapMapReadsLocalStep.class);
    registery.addStepType(FilterSamplesLocalStep.class);
    registery.addStepType(ExpressionLocalStep.class);
    registery.addStepType(AnaDiffLocalMain.class);
  }

}
