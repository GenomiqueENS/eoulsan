package fr.ens.biologie.genomique.eoulsan.galaxytools.executorinterpreters;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Objects;

/**
 * This class define the default executor interpreter. This interpreter use
 * <code>/bin/sh -c</code> to create the command line.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DefaultExecutorInterpreter extends AbstractExecutorInterpreter {

  public static final String INTERPRETER_NAME = "default";

  @Override
  public String getName() {

    return INTERPRETER_NAME;
  }

  @Override
  public List<String> createCommandLine(final String arguments) {

    requireNonNull(arguments, "arguments argument cannot be null");

    return Arrays.asList("/bin/sh", "-c", arguments);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {

    return Objects.toStringHelper(this).add("name", getName()).toString();
  }

}
