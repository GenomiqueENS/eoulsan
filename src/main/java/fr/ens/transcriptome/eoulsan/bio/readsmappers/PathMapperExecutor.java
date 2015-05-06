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

package fr.ens.transcriptome.eoulsan.bio.readsmappers;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

import fr.ens.transcriptome.eoulsan.bio.readsmappers.BundledMapperExecutor.ProcessResult;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class define a mapper executor that executes process found in the PATH
 * of the system.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class PathMapperExecutor implements MapperExecutor {

  //
  // MapperExecutor methods
  //

  @Override
  public boolean isExecutable(String executable) {

    checkNotNull(executable, "executable argument cannot be null");

    return FileUtils.checkIfExecutableIsInPATH(executable);
  }

  @Override
  public String install(final String executable) throws IOException {

    checkNotNull(executable, "executable argument cannot be null");

    return executable;
  }

  @Override
  public Result execute(List<String> command, File executionDirectory,
      boolean stdout, File... fileUsed) throws IOException {

    ProcessBuilder builder = new ProcessBuilder(command);

    if (executionDirectory != null) {
      builder.directory(executionDirectory);
    }

    getLogger().info(
        "Process command: " + Joiner.on(' ').join(builder.command()));
    getLogger().info("Process directory: " + builder.directory());

    return new ProcessResult(builder.start());
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).toString();
  }

}
