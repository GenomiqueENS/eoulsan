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
 * of the Institut de Biologie de l'√âcole Normale Sup√©rieure and
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

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepService;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.Step;

public class CommandWorkflowStep extends AbstractWorkflowStep {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  //
  // Step lifetime methods
  //

  @Override
  public void configure() throws EoulsanException {

    if (getState() != StepState.CREATED)
      throw new IllegalStateException("Illegal step state for configuration: "
          + getState());

    // Configure only standard steps and generator steps
    if (getType() == StepType.STANDARD_STEP
        || getType() == StepType.GENERATOR_STEP) {

      LOGGER.info("Configure "
          + getId() + " step with step parameters: " + getParameters());

      if (getType() == StepType.STANDARD_STEP)
        getStep().configure(getParameters());

      // Get output formats
      final DataFormat[] dfOut = getStep().getOutputFormats();
      if (dfOut != null)
        for (DataFormat df : dfOut)
          registerOutputFormat(df);

      // Get input format
      final DataFormat[] dfIn = getStep().getInputFormats();
      if (dfIn != null)
        for (DataFormat df : dfIn)
          registerInputFormat(df);

    }

    setState(StepState.CONFIGURED);
  }

  //
  // Static methods
  //

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private final static Step loadStep(final String stepName)
      throws EoulsanException {

    if (stepName == null)
      throw new EoulsanException("Step name is null");

    final String lower = stepName.trim().toLowerCase();
    final boolean hadoopMode = EoulsanRuntime.getRuntime().isHadoopMode();

    final Step result = StepService.getInstance(hadoopMode).getStep(lower);

    if (result == null)
      throw new EoulsanException("Unknown step: " + lower);

    return result;
  }

  //
  // Constructors
  //

  public CommandWorkflowStep(final AbstractWorkflow workflow,
      final StepType type) {

    super(workflow, type);
  }

  /**
   * Create a Generator Workflow step.
   * @param design design object
   * @param context context object
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  public CommandWorkflowStep(final AbstractWorkflow workflow,
      final DataFormat format) throws EoulsanException {

    super(workflow, format);
  }

  public CommandWorkflowStep(final AbstractWorkflow workflow, final String id,
      final String stepName, final Set<Parameter> stepParameters,
      final boolean skip) throws EoulsanException {

    super(workflow, id, loadStep(stepName), skip, stepParameters);
  }

}
