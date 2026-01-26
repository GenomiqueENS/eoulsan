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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.kenetre.log.GenericLogger;

/**
 * This class define a concrete implementation of the configuration context of a
 * step.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepConfigurationContextImpl implements StepConfigurationContext {

  private final WorkflowContext workflowContext;
  private final AbstractStep step;

  private static class WorkflowStepWrapper implements Step {

    private static final long serialVersionUID = 6362676600557016431L;

    private final AbstractStep step;

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
    public String getModuleName() {

      return this.step.getModuleName();
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
    public int getRequiredMemory() {

      return this.step.getRequiredMemory();
    }

    @Override
    public int getRequiredProcessors() {

      return this.step.getRequiredProcessors();
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

    @Override
    public Checker getChecker() {

      return this.step.getChecker();
    }

    //
    // Constructor
    //

    /**
     * Constructor.
     * @param step step
     */
    private WorkflowStepWrapper(final AbstractStep step) {

      this.step = step;
    }
  }

  //
  // Getters
  //

  /**
   * Get the local working directory.
   * @return Returns the local working Path
   */
  public DataFile getLocalWorkingDirectory() {

    return this.workflowContext.getLocalWorkingDirectory();
  }

  /**
   * Get the Hadoop working directory.
   * @return Returns the Hadoop working directory
   */
  public DataFile getHadoopWorkingDirectory() {

    return this.workflowContext.getHadoopWorkingDirectory();
  }

  /**
   * Get the job directory.
   * @return Returns the job directory
   */
  @Override
  public DataFile getJobDirectory() {

    return this.workflowContext.getJobDirectory();
  }

  /**
   * Get the task output directory.
   * @return Returns the task output directory
   */
  public DataFile getTaskOutputDirectory() {

    return this.workflowContext.getTaskDirectory();
  }

  @Override
  public DataFile getOutputDirectory() {
    return this.workflowContext.getOutputDirectory();
  }

  @Override
  public DataFile getStepOutputDirectory() {

    return this.step.getStepOutputDirectory();
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
  public DataFile getDesignFile() {
    return this.workflowContext.getDesignFile();
  }

  @Override
  public DataFile getWorkflowFile() {
    return this.workflowContext.getWorkflowFile();
  }

  /**
   * Get the application jar file.
   * @return Returns the jar file
   */
  public DataFile getJarPathname() {
    return this.workflowContext.getJarFile();
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
  public Step getCurrentStep() {
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

  @Override
  public GenericLogger getGenericLogger() {

    return this.workflowContext.getGenericLogger();
  }

  @Override
  public File getLocalTempDirectory() {

    return EoulsanRuntime.getRuntime().getTempDirectory();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param step step related to the context
   */
  StepConfigurationContextImpl(final AbstractStep step) {

    requireNonNull(step, "step cannot be null");

    this.workflowContext = step.getAbstractWorkflow().getWorkflowContext();
    this.step = step;
  }

}
