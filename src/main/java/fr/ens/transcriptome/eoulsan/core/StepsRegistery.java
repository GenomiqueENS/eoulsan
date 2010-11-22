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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.Common;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.steps.anadiff.local.AnaDiffLocalStep;
import fr.ens.transcriptome.eoulsan.steps.expression.hadoop.ExpressionHadoopStep;
import fr.ens.transcriptome.eoulsan.steps.expression.local.ExpressionLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.FilterAndSoapMapReadsHadoopStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.FilterReadsHadoopStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.FilterSamplesHadoopStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.hadoop.SoapMapReadsHadoopStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.local.FilterReadsLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.local.FilterSamplesLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mapping.local.SoapMapReadsLocalStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.CopyDesignAndParametersToOutputStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.hadoop.InitGlobalLoggerStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.local.ExecInfoLogStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataDownloadStep;
import fr.ens.transcriptome.eoulsan.steps.mgmt.upload.HDFSDataUploadStep;

/**
 * This class register all the step that can be used par the application.
 * @author Laurent Jourdren
 */
public class StepsRegistery {

  /** Logger. */
  private static Logger logger = Logger.getLogger(Globals.APP_NAME);

  private static StepsRegistery instance;
  private Map<String, Class<?>> registry = new HashMap<String, Class<?>>();

  //
  // Static method
  //

  /**
   * Get the instance of the StepRegistery.
   * @return the singleton for StepRegistery
   */
  public static StepsRegistery getInstance() {

    if (instance == null)
      instance = new StepsRegistery();

    return instance;
  }

  //
  // Instance methods
  //

  /**
   * Add a step
   * @param clazz Class of the step
   */
  public void addStepType(final Class<?> clazz) {

    addStepType(null, clazz);
  }

  /**
   * Add a step
   * @param stepName name of the step
   * @param clazz Class of the step
   */
  public void addStepType(final String stepName, final Class<?> clazz) {

    if (clazz == null)
      return;

    final Step s = testClassType(clazz);

    if (s != null) {

      final String lowerName =
          (stepName == null ? s.getName() : stepName).trim().toLowerCase();

      if (this.registry.containsKey(lowerName))
        logger.warning("Step "
            + s.getName() + " already exits, override previous step.");

      this.registry.put(lowerName, clazz);
      logger.finest("Add " + s.getName() + " to step registery");
    } else
      logger.warning("Addon " + clazz.getName() + " is not a step class");
  }

  /**
   * Add a step.
   * @param stepName name of the step to add
   * @param className class name of the step to add
   */
  @SuppressWarnings("static-access")
  public void addStepType(final String stepName, final String className) {

    if (stepName == null || "".equals(stepName) || className == null)
      return;

    try {
      Class<?> clazz = StepsRegistery.class.forName(className);

      addStepType(stepName, clazz);

      logger.info("Add external measurement: " + stepName);

    } catch (ClassNotFoundException e) {

      logger.severe("Cannot find " + className + " for step addon");
      throw new RuntimeException("Cannot find " + className + " for step addon");

    }
  }

  private Step testClassType(final Class<?> clazz) {

    if (clazz == null)
      return null;

    try {

      final Object o = clazz.newInstance();

      if (o instanceof Step)
        return (Step) o;

      return null;

    } catch (InstantiationException e) {
      Common.showAndLogErrorMessage("Can't create instance of "
          + clazz.getName()
          + ". Maybe your class doesn't have a void constructor.");
    } catch (IllegalAccessException e) {
      Common.showAndLogErrorMessage("Can't access to " + clazz.getName());
    }

    return null;
  }

  /**
   * Get a new instance of a step from its name.
   * @param name The name of the step to get
   * @return a new instance of a step or null if the requested step doesn't
   *         exists
   */
  public Step getStep(final String name) {

    if (name == null)
      return null;

    Class<?> clazz = this.registry.get(name.toLowerCase());

    if (clazz == null)
      return null;

    try {

      return (Step) clazz.newInstance();

    } catch (InstantiationException e) {
      System.err.println("Unable to instantiate "
          + name + " filter. Maybe this step doesn't have a void constructor.");
      logger.severe("Unable to instantiate "
          + name + " filter. Maybe this step doesn't have a void constructor.");
      return null;
    } catch (IllegalAccessException e) {

      return null;
    }

  }

  /**
   * Test if a step exists in the registry.
   * @param stepName step to test
   * @return true if the step exits in the registry
   */
  public boolean isStep(final String stepName) {

    if (stepName == null)
      return false;

    return this.registry.containsKey(stepName.toLowerCase());
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepsRegistery() {

    if (EoulsanRuntime.getRuntime().isHadoopMode()) {

      //
      // Register Hadoop steps
      //

      addStepType(HDFSDataUploadStep.class);
      addStepType(FilterReadsHadoopStep.class);
      addStepType(SoapMapReadsHadoopStep.class);
      addStepType(FilterAndSoapMapReadsHadoopStep.class);
      addStepType(FilterSamplesHadoopStep.class);
      addStepType(ExpressionHadoopStep.class);
      addStepType(HDFSDataDownloadStep.class);
      addStepType(CopyDesignAndParametersToOutputStep.class);
      addStepType(InitGlobalLoggerStep.class);
    }

    else {

      //
      // Register Local steps
      //

      addStepType(ExecInfoLogStep.class);
      addStepType(FilterReadsLocalStep.class);
      addStepType(SoapMapReadsLocalStep.class);
      addStepType(FilterSamplesLocalStep.class);
      addStepType(ExpressionLocalStep.class);
      addStepType(AnaDiffLocalStep.class);
    }

  }

}
