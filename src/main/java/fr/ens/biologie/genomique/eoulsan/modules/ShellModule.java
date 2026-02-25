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

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.NoOutputDirectory;
import fr.ens.biologie.genomique.eoulsan.annotations.RequiresAllPreviousSteps;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;
import java.util.Set;

/**
 * This class define a module that execute a shell command. It use the user shell to execute the
 * command. The launched command is : $SHELL -c "command"
 *
 * @author Laurent Jourdren
 */
@LocalOnly
@RequiresAllPreviousSteps
@NoOutputDirectory
public class ShellModule extends AbstractModule {

  private static final String MODULE_NAME = "shell";

  private String command;

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public String getDescription() {

    return "This module allow to execute shell commands";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
      throws EoulsanException {

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
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    try {

      // Define the shell interpreter to use
      String shellInterpreter = System.getenv("SHELL");
      if (shellInterpreter == null) {
        shellInterpreter = "/bin/sh";
      }

      getLogger().info("Execute: " + this.command);
      getLogger().info("Shell interpreter: " + shellInterpreter);

      final ProcessBuilder pb = new ProcessBuilder(shellInterpreter, "-c", this.command);

      // Set command line in status
      status.setCommandLine(String.join(" ", pb.command()));

      final int exitCode = pb.start().waitFor();

      // If exit code is not 0 throw an exception
      if (exitCode != 0) {
        throw new IOException("Finish process with exit code: " + exitCode);
      }

      // Write log file
      return status.createTaskResult();

    } catch (IOException | InterruptedException e) {
      return status.createTaskResult(
          e, "Error while running command (" + this.command + "): " + e.getMessage());
    }
  }
}
