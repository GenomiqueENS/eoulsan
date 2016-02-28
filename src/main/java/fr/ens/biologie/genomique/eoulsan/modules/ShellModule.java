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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import java.io.IOException;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.RequiresAllPreviousSteps;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.StepResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class define a step that execute a shell command. It use the user shell
 * to execute the command. The launched command is : $SHELL -c "command"
 * @author Laurent Jourdren
 */
@LocalOnly
@RequiresAllPreviousSteps
public class ShellModule extends AbstractModule {

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
        Modules.unknownParameter(context, p);
      }
    }

    if (this.command == null || this.command.trim().isEmpty()) {
      Modules.invalidConfiguration(context, "No command defined");
    }
  }

  @Override
  public StepResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      // Define the shell interpreter to use
      String shellInterpreter = System.getenv("SHELL");
      if (shellInterpreter == null) {
        shellInterpreter = "/bin/sh";
      }

      getLogger().info("Execute: " + this.command);
      getLogger().info("Shell interpreter: " + shellInterpreter);

      final Process p =
          new ProcessBuilder(shellInterpreter, "-c", this.command).start();

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
