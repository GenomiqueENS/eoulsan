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

package fr.ens.transcriptome.eoulsan.core.workflow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.core.Step;
import fr.ens.transcriptome.eoulsan.core.StepRegistry;

/**
 * This class store step instances and avoid storing this instance in
 * WorkflowStep objects that are serialized.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepInstances {

  private static StepInstances instance;

  private final Map<WorkflowStep, Step> steps = new HashMap<>();

  /**
   * Get a step instance.
   * @param step workflow step
   * @return a step instance
   * @throws EoulsanRuntimeException if an error occurs while loading the step
   */
  public Step getStep(final WorkflowStep step) {

    checkNotNull(step, "Step is null");
    final String stepName = step.getStepName();
    final String stepVersion = step.getStepVersion();

    try {
      return getStep(step, stepName, stepVersion);
    } catch (EoulsanException e) {
      throw new EoulsanRuntimeException(e);
    }
  }

  /**
   * Get a step instance.
   * @param workflowStep workflow step
   * @param stepVersion step version
   * @return a step instance
   * @throws EoulsanException if an error occurs while loading the step
   */
  public Step getStep(final WorkflowStep workflowStep, final String stepName,
      final String stepVersion) throws EoulsanException {

    checkNotNull(stepName, "Step name is null");

    if (!this.steps.containsKey(workflowStep)) {

      // Load step
      final Step stepInstance = loadStep(stepName, stepVersion);

      // Register step instance
      registerStep(workflowStep, stepInstance);

      // return step instance
      return stepInstance;
    }

    return this.steps.get(workflowStep);
  }

  /**
   * Register a step instance.
   * @param workflowStep workflow step
   * @param stepInstance step instance
   */
  public void registerStep(final WorkflowStep workflowStep,
      final Step stepInstance) {

    checkNotNull(workflowStep, "workflow step is null");
    checkNotNull(stepInstance, "stepInstance is null");

    this.steps.put(workflowStep, stepInstance);
  }

  /**
   * Remove a step instance.
   * @param workflowStep workflow step
   */
  public void removeStep(final WorkflowStep workflowStep) {

    checkNotNull(workflowStep);

    this.steps.remove(workflowStep);
  }

  //
  // Static methods
  //

  public static StepInstances getInstance() {

    if (instance == null) {
      instance = new StepInstances();
    }

    return instance;
  }

  //
  // Static methods
  //

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @param stepVersion version of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private static Step loadStep(final String stepName, final String stepVersion)
      throws EoulsanException {

    if (stepName == null) {
      throw new EoulsanException("Step name is null");
    }

    final String lower = stepName.trim().toLowerCase();

    final Step result = StepRegistry.getInstance().loadStep(lower, stepVersion);

    if (result == null) {
      throw new EoulsanException("Unknown step: "
          + lower + ("".equals(stepVersion) ? "" : " version " + stepVersion));
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepInstances() {
  }

}
