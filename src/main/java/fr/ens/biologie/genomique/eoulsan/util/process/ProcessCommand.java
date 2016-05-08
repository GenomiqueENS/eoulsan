package fr.ens.biologie.genomique.eoulsan.util.process;

import java.io.File;
import java.io.IOException;

/**
 * This interface define a command that will be executed.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface ProcessCommand {

  /**
   * Test if the executable command is available
   * @return true if the command is available
   */
  boolean isAvailable();

  /**
   * Test if the executable command is installed.
   * @return true if the command is installed
   */
  boolean isInstalled();

  /**
   * Install the executable for the command.
   * @return The path of where the command is installed
   * @throws IOException if an error occurs while installing the command
   */
  String install() throws IOException;

  /**
   * Execute the command.
   * @return a RunningProcess object
   * @throws IOException if an error occurs while starting the command
   */
  RunningProcess execute() throws IOException;

}
