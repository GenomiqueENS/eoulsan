package fr.ens.biologie.genomique.eoulsan.util;

import java.io.File;
import java.util.List;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This class an abstract SimpleProcess class.
 * @author Laurent Jourdren
 * @since 2.0
 */
public abstract class AbstractSimpleProcess implements SimpleProcess {

  protected static final String TMP_DIR_ENV_VARIABLE = "TMPDIR";

  @Override
  public int execute(final List<String> commandLine,
      final File executionDirectory, final File temporaryDirectory,
      final File stdoutFile, final File stderrFile) throws EoulsanException {

    return execute(commandLine, executionDirectory, null, temporaryDirectory,
        stdoutFile, stderrFile, false);
  }

}
