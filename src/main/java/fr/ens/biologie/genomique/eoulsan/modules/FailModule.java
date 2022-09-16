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

package fr.ens.biologie.genomique.eoulsan.modules;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.NoLog;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;

/**
 * This module is a module that always fails. This module is only used for
 * debugging purpose.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@ReuseModuleInstance
@NoLog
public class FailModule extends AbstractModule {

  public static final String MODULE_NAME = "fail";

  private int delay = 0;
  private DataFormat inputFormat;

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return this.inputFormat == null
        ? InputPortsBuilder.noInputPort()
        : InputPortsBuilder.singleInputPort(this.inputFormat);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "delay":
        this.delay = p.getIntValueGreaterOrEqualsTo(0);
        break;

      case "input.format":
        this.inputFormat = DataFormatRegistry.getInstance()
            .getDataFormatFromGalaxyFormatNameOrNameOrAlias(
                p.getLowerStringValue());
        if (this.inputFormat == null) {
          Modules.badParameterValue(context, p, "Unknown format");
        }
        break;

      default:
        Modules.unknownParameter(context, p);
      }
    }

    // Check delay value
    if (this.delay < 0) {
      Modules.invalidConfiguration(context,
          "Delay cannot be lower than 0: " + this.delay);
    }

  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {
      Thread.sleep(delay * 1000);
    } catch (InterruptedException e) {
      context.getLogger()
          .warning("Thread.sleep() interrupted: " + e.getMessage());
    }

    return status.createTaskResult(
        new EoulsanException("Fail of the step required by user"));
  }

}
