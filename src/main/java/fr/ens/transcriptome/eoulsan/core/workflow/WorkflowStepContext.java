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

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.EoulsanRuntimeException;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Data;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class define a step context.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class WorkflowStepContext implements StepContext, Serializable {

  /** Serialization version UID. */
  private static final long serialVersionUID = 8288158811122533646L;

  private final WorkflowContext workflowContext;
  private final AbstractWorkflowStep step;

  //
  // Getters
  //

  @Override
  public String getLocalWorkingPathname() {

    return this.workflowContext.getLocalWorkingPathname();
  }

  @Override
  public String getHadoopWorkingPathname() {

    return this.workflowContext.getHadoopWorkingPathname();
  }

  @Override
  public String getLogPathname() {

    return this.workflowContext.getLogPathname();
  }

  @Override
  public String getOutputPathname() {
    return this.workflowContext.getOutputPathname();
  }

  @Override
  public String getStepWorkingPathname() {

    return this.step.getStepWorkingDir().getSource();
  }

  @Override
  public String getJobId() {
    return this.workflowContext.getJobId();
  }

  @Override
  public String getJobHost() {
    return this.workflowContext.getJobHost();
  }

  @Override
  public long getContextCreationTime() {
    return this.workflowContext.getContextCreationTime();
  }

  @Override
  public String getDesignPathname() {
    return this.workflowContext.getDesignPathname();
  }

  @Override
  public String getWorkflowPathname() {
    return this.workflowContext.getWorkflowPathname();
  }

  @Override
  public String getJarPathname() {
    return this.workflowContext.getJarPathname();
  }

  @Override
  public String getJobUUID() {
    return this.workflowContext.getJobUUID();
  }

  @Override
  public String getJobDescription() {
    return this.workflowContext.getJobDescription();
  }

  @Override
  public String getJobEnvironment() {
    return this.workflowContext.getJobEnvironment();
  }

  @Override
  public String getCommandName() {
    return this.workflowContext.getCommandName();
  }

  @Override
  public String getCommandDescription() {
    return this.workflowContext.getCommandDescription();
  }

  @Override
  public String getCommandAuthor() {
    return this.workflowContext.getCommandAuthor();
  }

  @Override
  public Workflow getWorkflow() {

    return this.workflowContext.getWorkflow();
  }

  @Override
  public WorkflowStep getCurrentStep() {
    return this.step;
  }

  //
  // Other methods
  //

  @Override
  public AbstractEoulsanRuntime getRuntime() {

    return this.workflowContext.getRuntime();
  }

  @Override
  public Settings getSettings() {

    return this.workflowContext.getSettings();
  }

  @Override
  public Logger getLogger() {

    return this.workflowContext.getLogger();
  }


  @Override
  public Data getInputData(final String portName, final Sample sample) {

    return this.step.getInputPortData(portName, sample);
  }

@Override
  public Data getInputData(final DataFormat format, final Sample sample) {

  return getInputData(getInputPortNameForFormat(format), sample);
}



  @Override
  public Data getOutputData(final String portName, final Sample sample) {

    return this.step.getOutputPortData(portName, sample);
  }

  @Override
  public Data getOutputData(final DataFormat format, final Sample sample) {

    return getInputData(getOutputPortNameForFormat(format), sample);
  }




  //
  // Private methods
  //

  private String getInputPortNameForFormat(final DataFormat format) {

    checkNotNull(format, "The format is null");

    final List<WorkflowInputPort> ports =
        this.step.getInputPorts().getPortsWithDataFormat(format);

    switch (ports.size()) {

    case 0:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId() + " do not provide an input port with format: "
          + format);
    case 1:
      return ports.get(0).getName();
    default:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId()
          + " provide more than one input port with format: " + format);
    }
  }

  private String getOutputPortNameForFormat(final DataFormat format) {

    checkNotNull(format, "The format is null");

    final List<WorkflowOutputPort> ports =
        this.step.getOutputPorts().getPortsWithDataFormat(format);

    switch (ports.size()) {

    case 0:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId() + " do not provide an output port with format: "
          + format);
    case 1:
      return ports.get(0).getName();
    default:
      throw new EoulsanRuntimeException("The step "
          + this.step.getId()
          + " provide more than one output port with format: " + format);
    }
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step step related to the context
   */
  WorkflowStepContext(final WorkflowContext workflowContext,
      final AbstractWorkflowStep step) {

    checkNotNull(workflowContext, "workflow context cannot be null");
    checkNotNull(step, "step cannot be null");

    this.workflowContext = workflowContext;
    this.step = step;
  }

}
