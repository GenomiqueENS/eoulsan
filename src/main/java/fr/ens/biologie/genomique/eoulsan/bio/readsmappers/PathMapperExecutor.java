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

package fr.ens.biologie.genomique.eoulsan.bio.readsmappers;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import fr.ens.biologie.genomique.eoulsan.bio.readsmappers.BundledMapperExecutor.ProcessResult;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;

/**
 * This class define a mapper executor that executes process found in the PATH
 * of the system.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class PathMapperExecutor implements MapperExecutor {

  private MapperLogger logger;

  //
  // MapperExecutor methods
  //

  @Override
  public MapperLogger getLogger() {
    return this.logger;
  }

  @Override
  public boolean isExecutable(String executable) {

    requireNonNull(executable, "executable argument cannot be null");

    return FileUtils.checkIfExecutableIsInPATH(executable);
  }

  @Override
  public String install(final String executable) throws IOException {

    requireNonNull(executable, "executable argument cannot be null");

    return executable;
  }

  @Override
  public Result execute(final List<String> command,
      final File executionDirectory, final boolean stdout,
      final File stdErrFile, final boolean redirectStderr,
      final File... fileUsed) throws IOException {

    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(redirectStderr);

    // Define the redirection of standard error
    if (stdErrFile != null) {
      builder.redirectError(stdErrFile);
    } else if (!redirectStderr) {
      builder.redirectError(new File("/dev/null"));
    }

    if (executionDirectory != null) {
      builder.directory(executionDirectory);
    }

    this.logger
        .info("Process command: " + Joiner.on(' ').join(builder.command()));
    this.logger.info("Process directory: " + builder.directory());

    return new ProcessResult(builder.start());
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).toString();
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param logger the logger to use
   */
  PathMapperExecutor(MapperLogger logger) {

    requireNonNull(logger, "logger argument cannot be null");
    this.logger = logger;
  }

}
