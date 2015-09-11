package fr.ens.transcriptome.eoulsan.galaxytools.executorinterpreters;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.EoulsanRuntime.getSettings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.util.StringUtils;

/**
 * This class define a generic executor interpreter. The path of the executor is
 * first searched in Eoulsan settings, and not set, it will be search in the
 * system PATH.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class GenericExecutorInterpreter extends AbstractExecutorInterpreter {

  private static final String GALAXY_TOOL_INTERPRETER_SETTING_PREFIX =
      "main.galaxy.tool.interpreter.";

  private String name;
  private File path;

  @Override
  public String getName() {

    return this.name;
  }

  /**
   * Get the interpreter path.
   * @return a File with the interpreter path
   */
  protected File getInterpreterPath() {

    return this.path;
  }

  @Override
  public List<String> createCommandLine(final String arguments) {

    checkNotNull(arguments, "arguments argument cannot be null");

    final List<String> result = new ArrayList<>();
    result.add(getInterpreterPath().getAbsolutePath());
    result.addAll(StringUtils.splitShellCommandLine(arguments));

    return Collections.unmodifiableList(result);
  }

  //
  // Static methods
  //

  /**
   * Search the interpreter path in the Eoulsan settings.
   * @param interpreterName the name of the interpreter
   * @return a File with the interpreter path or null if the interpreter path
   *         has not been defined in the Eoulsan settings
   */
  private static File getInterpreterPathFromConfiguration(
      final String interpreterName) {

    final String value = getSettings()
        .getSetting(GALAXY_TOOL_INTERPRETER_SETTING_PREFIX + interpreterName);

    return value != null ? new File(value) : null;
  }

  /**
   * Search the interpreter path in the system PATH.
   * @param interpreterName the name of the interpreter
   * @return a File with the interpreter path or null if the interpreter path
   *         has not been defined in the system PATH
   */
  private static File searchInterpreterInPATH(final String interpreterName) {

    final String pathEnv = System.getenv().get("PATH");

    if (pathEnv == null) {
      return null;
    }

    for (String dirname : Splitter.on(File.pathSeparatorChar).split(pathEnv)) {

      final File dir = new File(dirname);

      final File file = new File(dir, interpreterName);

      if (dir.isDirectory() && file.exists() && file.canExecute()) {
        return file;
      }

    }

    return null;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param interpreterName the name of the interpreter
   */
  public GenericExecutorInterpreter(final String interpreterName) {

    checkNotNull(interpreterName, "interpreterName argument cannot be null");

    this.name = interpreterName;

    File path = getInterpreterPathFromConfiguration(interpreterName);

    if (path == null) {
      path = searchInterpreterInPATH(interpreterName);
    }

    if (path == null) {
      path = new File("/usr/bin/" + interpreterName);
    }

    this.path = path;
  }

}
