package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

/**
 * This class define the default interpreter. This interpreter use
 * <code>/bin/sh -c</code> to create the command line.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class DefaultToolExecutorInterpreter extends
    AbstractToolExecutorInterpreter {

  @Override
  public String getName() {

    return "default";
  }

  @Override
  public List<String> createCommandLine(final List<String> arguments) {

    checkNotNull(arguments, "arguments argument cannot be null");

    return Arrays.asList("/bin/sh", "-c", Joiner.on(' ').join(arguments));
  }

}
