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

package fr.ens.transcriptome.eoulsan.steps;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.annotations.NoLog;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This step is a step that always fail. This step is only used for debugging
 * purpose.
 * @since 2.0
 * @author Laurent Jourdren
 */
@LocalOnly
@ReuseStepInstance
@NoLog
public class FailStep extends AbstractStep {

  public static final String STEP_NAME = "fail";

  private int delay = 0;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return InputPortsBuilder.singleInputPort(DataFormats.DUMMY_TXT);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "delay":
        this.delay = p.getIntValueGreaterOrEqualsTo(0);
        break;

      default:
        throw new EoulsanException(
            "Unknown parameter for step " + getName() + ": " + p.getName());
      }
    }

    // Check delay value
    if (this.delay < 0) {
      throw new EoulsanException("Delay cannot be lower than 0: " + this.delay);
    }

  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    try {
      Thread.sleep(delay * 1000);
    } catch (InterruptedException e) {
      context.getLogger()
          .warning("Thread.sleep() interrupted: " + e.getMessage());
    }

    return status.createStepResult(
        new EoulsanException("Fail of the step required by user"));
  }

}
