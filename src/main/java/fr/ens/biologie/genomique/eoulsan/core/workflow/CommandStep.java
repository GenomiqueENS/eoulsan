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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static fr.ens.biologie.genomique.eoulsan.core.workflow.CommandWorkflow.EMPTY_PARAMETERS;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import java.util.Set;

/**
 * This class define a step based on a Command object (workflow file).
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class CommandStep extends AbstractStep {

  /** Serialization version UID. */
  private static final long serialVersionUID = -1736197317540226070L;

  //
  // Step lifetime methods
  //

  //
  // Constructors
  //

  /**
   * Constructor that create a step with nothing to execute like ROOT_STEP, DESIGN_STEP and
   * FIRST_STEP.
   *
   * @param workflow the workflow of the step
   * @param type the type of the step
   */
  public CommandStep(final AbstractWorkflow workflow, final StepType type) {

    super(workflow, type);
  }

  /**
   * Create a Generator Workflow step.
   *
   * @param workflow the workflow object
   * @param format DataFormat
   * @throws EoulsanException if an error occurs while configuring the generator
   */
  public CommandStep(final AbstractWorkflow workflow, final DataFormat format)
      throws EoulsanException {

    super(workflow, format);
  }

  /**
   * Create a step for a standard step.
   *
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param moduleName module name
   * @param stepVersion step version
   * @param parameters parameters of the step
   * @param skip true to skip execution of the step
   * @param discardOutput discard output value
   * @param requiredMemory required memory
   * @param requiredProcessors required processors
   * @param dataProduct data product
   * @throws EoulsanException id an error occurs while creating the step
   */
  public CommandStep(
      final AbstractWorkflow workflow,
      final String id,
      final String moduleName,
      final String stepVersion,
      final Set<Parameter> parameters,
      final boolean skip,
      final Step.DiscardOutput discardOutput,
      final int requiredMemory,
      final int requiredProcessors,
      final String dataProduct)
      throws EoulsanException {

    super(
        workflow,
        id,
        moduleName,
        stepVersion,
        skip,
        discardOutput,
        parameters,
        requiredMemory,
        requiredProcessors,
        dataProduct);
  }

  /**
   * Create a step for a standard step.
   *
   * @param workflow workflow of the step
   * @param id identifier of the step
   * @param moduleName module name
   * @param stepVersion step version
   * @param parameters parameters of the step
   * @param skip true to skip execution of the step
   * @param discardOutput discard output value
   * @param requiredMemory required memory
   * @param requiredProcessors required processors
   * @param dataProduct data product
   * @param outputDirectory output directory
   * @throws EoulsanException id an error occurs while creating the step
   */
  public CommandStep(
      final AbstractWorkflow workflow,
      final String id,
      final String moduleName,
      final String stepVersion,
      final Set<Parameter> parameters,
      final boolean skip,
      final Step.DiscardOutput discardOutput,
      final int requiredMemory,
      final int requiredProcessors,
      final String dataProduct,
      final DataFile outputDirectory)
      throws EoulsanException {

    super(
        workflow,
        id,
        moduleName,
        stepVersion,
        skip,
        discardOutput,
        parameters,
        requiredMemory,
        requiredProcessors,
        dataProduct,
        outputDirectory);
  }

  /**
   * Create a step for a standard step from an existing step object.
   *
   * @param workflow workflow of the step
   * @param module module object
   * @throws EoulsanException id an error occurs while creating the step
   */
  public CommandStep(final AbstractWorkflow workflow, final Module module) throws EoulsanException {

    this(workflow, module, EMPTY_PARAMETERS);
  }

  /**
   * Create a step for a standard step from an existing step object.
   *
   * @param workflow workflow of the step
   * @param module module object
   * @param parameters parameters of the step
   * @throws EoulsanException id an error occurs while creating the step
   */
  public CommandStep(
      final AbstractWorkflow workflow, final Module module, final Set<Parameter> parameters)
      throws EoulsanException {

    this(
        workflow,
        module.getName(),
        module.getName(),
        module.getVersion().toString(),
        parameters,
        false,
        Step.DiscardOutput.NO,
        -1,
        -1,
        "");
  }
}
