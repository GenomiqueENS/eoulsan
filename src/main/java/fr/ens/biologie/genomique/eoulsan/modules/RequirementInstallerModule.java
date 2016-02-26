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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.modules;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.requirements.AbstractRequirement;
import fr.ens.biologie.genomique.eoulsan.requirements.Requirement;
import fr.ens.biologie.genomique.eoulsan.requirements.RequirementService;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class allow to install a requirement.
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
public class RequirementInstallerModule extends AbstractModule {

  public static final String STEP_NAME = "requirementinstaller";

  private Requirement requirement;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public ParallelizationMode getParallelizationMode() {

    return ParallelizationMode.NOT_NEEDED;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String requirementName = null;

    // Get the requirement name
    for (Parameter p : stepParameters) {

      if (AbstractRequirement.NAME_PARAMETER.equals(p.getName())) {
        requirementName = p.getValue();
      }
    }

    // Get an instance of the requirement
    this.requirement =
        RequirementService.getInstance().newService(requirementName);

    if (this.requirement == null) {
      Modules.invalidConfiguration(context,
          "Unknown requirement: " + requirementName);
    }

    // Configure the requirement
    this.requirement.configure(stepParameters);

    if (!this.requirement.isInstallable()) {
      Modules.invalidConfiguration(context,
          "The requirement is not installable: " + requirementName);
    }

  }

  @Override
  public StepResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      // Install the requirement
      this.requirement.install(status);

    } catch (EoulsanException e) {
      return status.createStepResult(e);
    }

    return status.createStepResult();
  }

}
