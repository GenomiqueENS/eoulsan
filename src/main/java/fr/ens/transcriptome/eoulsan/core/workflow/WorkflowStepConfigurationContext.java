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

import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.AbstractEoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;

/**
 * This class define a concrete implementation of the configuration context of a
 * step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class WorkflowStepConfigurationContext implements
    StepConfigurationContext {

  private final WorkflowContext workflowContext;
  private final AbstractWorkflowStep step;

  private static class WorkflowStepWrapper implements WorkflowStep {

    private static final long serialVersionUID = 6362676600557016431L;

    private final AbstractWorkflowStep step;

    @Override
    public Workflow getWorkflow() {

      throw new IllegalStateException(
          "This method cannot be used from a configuration context");
    }

    @Override
    public int getNumber() {

      return this.step.getNumber();
    }

    @Override
    public String getId() {

      return this.step.getId();
    }

    @Override
    public boolean isSkip() {

      return this.step.isSkip();
    }

    @Override
    public StepType getType() {

      return this.step.getType();
    }

    @Override
    public String getStepName() {

      return this.step.getStepName();
    }

    @Override
    public String getStepVersion() {

      return this.step.getStepVersion();
    }

    @Override
    public Set<Parameter> getParameters() {

      return this.step.getParameters();
    }

    @Override
    public InputPorts getInputPorts() {

      throw new IllegalStateException(
          "This method cannot be used from a configuration context");
    }

    @Override
    public OutputPorts getOutputPorts() {

      throw new IllegalStateException(
          "This method cannot be used from a configuration context");
    }

    @Override
    public StepState getState() {

      return this.step.getState();
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param step step
     */
    private WorkflowStepWrapper(final AbstractWorkflowStep step) {

      this.step = step;
    }
  }

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
  public WorkflowStep getCurrentStep() {
    return new WorkflowStepWrapper(this.step);
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

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step step related to the context
   */
  WorkflowStepConfigurationContext(final AbstractWorkflowStep step) {

    checkNotNull(step, "step cannot be null");

    this.workflowContext = step.getAbstractWorkflow().getWorkflowContext();
    this.step = step;
  }

}
