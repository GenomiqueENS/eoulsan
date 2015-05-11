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

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;

/**
 * This class define a step based on a Command object (workflow file).
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CommandWorkflowStep extends AbstractWorkflowStep {

  /** Serialization version UID. */
  private static final long serialVersionUID = -1736197317540226070L;

  //
  // Step lifetime methods
  //

  //
  // Constructors
  //

  /**
   * Constructor that create a step with nothing to execute like ROOT_STEP,
   * DESIGN_STEP and FIRST_STEP.
   * @param workflow the workflow of the step
   * @param type the type of the step
   */
  public CommandWorkflowStep(final AbstractWorkflow workflow,
      final StepType type) {

    super(workflow, type);
  }

  /**
   * Create a Generator Workflow step.
   * @param workflow the workflow object
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  public CommandWorkflowStep(final AbstractWorkflow workflow,
      final DataFormat format) throws EoulsanException {

    super(workflow, format);
  }

  /**
   * Create a step for a standard step.
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param stepName Step name
   * @param stepVersion step version
   * @param skip true to skip execution of the step
   * @param parameters parameters of the step
   * @throws EoulsanException id an error occurs while creating the step
   */
  public CommandWorkflowStep(final AbstractWorkflow workflow, final String id,
      final String stepName, final String stepVersion,
      final Set<Parameter> parameters, final boolean skip,
      final boolean copyResultsToOutput) throws EoulsanException {

    super(workflow, id, stepName, stepVersion, skip, copyResultsToOutput,
        parameters);
  }

}
