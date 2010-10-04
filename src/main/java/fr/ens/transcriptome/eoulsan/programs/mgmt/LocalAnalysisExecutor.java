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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.io.DesignReader;
import fr.ens.transcriptome.eoulsan.design.io.SimpleDesignReader;
import fr.ens.transcriptome.eoulsan.programs.anadiff.local.AnaDiffLocalMain;
import fr.ens.transcriptome.eoulsan.programs.expression.local.ExpressionLocalStep;
import fr.ens.transcriptome.eoulsan.programs.mapping.local.FilterReadsLocalStep;
import fr.ens.transcriptome.eoulsan.programs.mapping.local.FilterSamplesLocalStep;
import fr.ens.transcriptome.eoulsan.programs.mapping.local.SoapMapReadsLocalStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define an executor for local mode.
 * @author Laurent Jourdren
 */
public class LocalAnalysisExecutor extends Executor {

  /** Logger */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private final File designFile;
  private final StepsRegistery registery = new StepsRegistery();

  @Override
  protected Step getStep(String stepName) {

    return this.registery.getStep(stepName);
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
          new File(this.designFile.getParent(), getInfo().getExecutionName());

      if (!logDir.exists())
        logDir.mkdirs();

      final String logFilename = result.getStep().getLogName();

      final Writer writer =
          FileUtils
              .createBufferedWriter(new File(logDir, logFilename + ".log"));

      writer.write(result.getLogMessage());
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
  public LocalAnalysisExecutor(final Command command, final File designFile) {

    if (command == null)
      throw new NullPointerException("The command is null");

    setCommand(command);

    if (designFile == null)
      throw new NullPointerException("The design file is null");

    this.designFile = designFile;
    getInfo().setBasePathname(
        designFile.getAbsoluteFile().getParentFile().getAbsolutePath());

    //
    // Register local steps
    //

    this.registery.addStepType(FilterReadsLocalStep.class);
    this.registery.addStepType(SoapMapReadsLocalStep.class);
    this.registery.addStepType(FilterSamplesLocalStep.class);
    this.registery.addStepType(ExpressionLocalStep.class);
    this.registery.addStepType(AnaDiffLocalMain.class);

  }

}
