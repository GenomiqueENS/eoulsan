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

package fr.ens.biologie.genomique.eoulsan.modules.diffana;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.modules.diffana.NormalizationModule.DESEQ1_DOCKER_IMAGE;
import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.modules.AbstractModule;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.DiffAna.DispersionFitType;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.DiffAna.DispersionMethod;
import fr.ens.biologie.genomique.eoulsan.modules.diffana.DiffAna.DispersionSharingMode;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.util.r.RExecutor;

/**
 * This class define the module of differential analysis in local mode.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Vivien Deshaies
 */
@LocalOnly
public class DiffAnaModule extends AbstractModule {

  private static final String DISP_EST_METHOD_PARAMETER_NAME =
      "disp.est.method";
  private static final String DISP_EST_FIT_TYPE_PARAMETER_NAME =
      "disp.est.fit.type";
  private static final String DISP_EST_SHARING_MODE_PARAMETER_NAME =
      "disp.est.sharing.mode";

  private static final String MODULE_NAME = "diffana";

  // parameters and there default values
  private DispersionMethod dispEstMethod = DispersionMethod.POOLED;
  private DispersionFitType dispEstFitType = DispersionFitType.LOCAL;
  private DispersionSharingMode dispEstSharingMode =
      DispersionSharingMode.MAXIMUM;

  private final Set<Requirement> requirements = new HashSet<>();
  private RExecutor executor;

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public String getDescription() {

    return "This class compute the differential analysis for the experiment.";
  }

  @Override
  public InputPorts getInputPorts() {
    return new InputPortsBuilder()
        .addPort(DEFAULT_SINGLE_INPUT_PORT_NAME, true, EXPRESSION_RESULTS_TSV)
        .create();
  }

  @Override
  public Set<Requirement> getRequirements() {

    return unmodifiableSet(this.requirements);
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      final Design design = context.getWorkflow().getDesign();

      // Launch differential analysis
      final DiffAna diffana = new DiffAna(this.executor, design,
          this.dispEstMethod, this.dispEstSharingMode, this.dispEstFitType);
      diffana.run(context, context.getInputData(EXPRESSION_RESULTS_TSV));

      // Write log file
      return status.createTaskResult();

    } catch (EoulsanException e) {

      return status.createTaskResult(e,
          "Error while analysis data: " + e.getMessage());
    }
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Parse R executor parameters
    final Set<Parameter> parameters = new HashSet<>(stepParameters);
    this.executor = RModuleCommonConfiguration.parseRExecutorParameter(context,
        parameters, this.requirements, DESEQ1_DOCKER_IMAGE);

    for (Parameter p : parameters) {

      switch (p.getName()) {

      case DISP_EST_METHOD_PARAMETER_NAME:

        this.dispEstMethod =
            DispersionMethod.getDispEstMethodFromName(p.getStringValue());

        if (this.dispEstMethod == null) {
          Modules.badParameterValue(context, p,
              "Unknown dispersion estimation method");
        }
        break;

      case DISP_EST_FIT_TYPE_PARAMETER_NAME:

        this.dispEstFitType =
            DispersionFitType.getDispEstFitTypeFromName(p.getStringValue());

        if (this.dispEstFitType == null) {
          Modules.badParameterValue(context, p,
              "Unknown dispersion estimation fitType");
        }
        break;

      case DISP_EST_SHARING_MODE_PARAMETER_NAME:

        this.dispEstSharingMode = DispersionSharingMode
            .getDispEstSharingModeFromName(p.getStringValue());

        if (this.dispEstSharingMode == null) {
          Modules.badParameterValue(context, p,
              "Unknown dispersion estimation sharing mode");
        }
        break;

      default:
        Modules.unknownParameter(context, p);
      }
    }

    // Log Step parameters
    getLogger().info("In "
        + getName() + ", dispersion estimation method="
        + this.dispEstMethod.getName());
    getLogger().info("In "
        + getName() + ", dispersion estimation sharing mode="
        + this.dispEstSharingMode.getName());
    getLogger().info("In "
        + getName() + ", dispersion estimation fit type="
        + this.dispEstFitType.getName());
  }
}
