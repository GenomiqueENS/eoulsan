package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static fr.ens.biologie.genomique.eoulsan.EoulsanRuntime.getSettings;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import fr.ens.biologie.genomique.kenetre.util.StringUtils;
import fr.ens.biologie.genomique.kenetre.util.SystemUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class define a generic executor interpreter. The path of the executor is first searched in
 * Eoulsan settings, and not set, it will be search in the system PATH.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class GenericExecutorInterpreter extends AbstractExecutorInterpreter {

  private static final String GALAXY_TOOL_INTERPRETER_SETTING_PREFIX =
      "main.galaxy.tool.interpreter.";

  private final String name;
  private final File path;

  @Override
  public String getName() {

    return this.name;
  }

  /**
   * Get the interpreter path.
   *
   * @return a File with the interpreter path
   */
  protected File getInterpreterPath() {

    return this.path;
  }

  @Override
  public List<String> createCommandLine(final String arguments) {

    requireNonNull(arguments, "arguments argument cannot be null");

    final List<String> result = new ArrayList<>();
    result.add(getInterpreterPath().getAbsolutePath());
    result.addAll(StringUtils.splitShellCommandLine(arguments));

    return Collections.unmodifiableList(result);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("name", this.name)
        .add("path", this.path)
        .toString();
  }

  //
  // Static methods
  //

  /**
   * Search the interpreter path in the Eoulsan settings.
   *
   * @param interpreterName the name of the interpreter
   * @return a File with the interpreter path or null if the interpreter path has not been defined
   *     in the Eoulsan settings
   */
  private static File getInterpreterPathFromConfiguration(final String interpreterName) {

    final String value =
        getSettings().getSetting(GALAXY_TOOL_INTERPRETER_SETTING_PREFIX + interpreterName);

    return value != null ? Path.of(value).toFile() : null;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   *
   * @param interpreterName the name of the interpreter
   */
  public GenericExecutorInterpreter(final String interpreterName) {

    requireNonNull(interpreterName, "interpreterName argument cannot be null");

    this.name = interpreterName;

    File path = getInterpreterPathFromConfiguration(interpreterName);

    if (path == null) {
      path = SystemUtils.searchExecutableInPATH(interpreterName);
    }

    if (path == null) {
      path = Path.of("/usr/bin/" + interpreterName).toFile();
    }

    this.path = path;
  }
}
