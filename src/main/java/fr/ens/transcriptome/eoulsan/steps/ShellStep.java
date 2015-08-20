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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.LocalOnly;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a step that execute a shell command. It use the user shell
 * to execute the command. The launched command is : $SHELL -c "command"
 * @author Laurent Jourdren
 */
@LocalOnly
public class ShellStep extends AbstractStep {

  private static final String STEP_NAME = "shell";

  private String command;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step allow to execute shell commands";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "command":
        this.command = p.getValue();
        break;

      default:
        throw new EoulsanException(
            "Unknown parameter for step " + getName() + ": " + p.getName());
      }
    }

    if (this.command == null || this.command.trim().isEmpty()) {
      throw new EoulsanException("No command defined.");
    }
  }

  @Override
  public StepResult execute(final StepContext context,
      final StepStatus status) {

    try {

      getLogger().info("Execute: " + this.command);

      final Process p =
          new ProcessBuilder(System.getenv("SHELL"), "-c", this.command)
              .start();

      final int exitCode = p.waitFor();

      // If exit code is not 0 throw an exception
      if (exitCode != 0) {
        throw new IOException("Finish process with exit code: " + exitCode);
      }

      // Write log file
      return status.createStepResult();

    } catch (IOException | InterruptedException e) {
      return status.createStepResult(e, "Error while running command ("
          + this.command + "): " + e.getMessage());
    }

  }
}
