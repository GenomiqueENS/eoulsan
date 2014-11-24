package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.VariableRegistry.INSTANCE_NAME;

import java.util.List;
import java.util.Map;

import org.python.core.PyObject;
import org.python.google.common.base.Joiner;
import org.python.util.PythonInterpreter;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class ToolPythonInterpreter {

  private static final String LINE_SEPARATOR = System.getProperties()
      .getProperty("line.separator");
  public final static Splitter NEW_LINE = Splitter.onPattern("[\r\n]")
      .trimResults().omitEmptyStrings();
  public static final String VAR_CMD_NAME = "cmd";

  public String executeScript(final String script,
      final Map<String, String> registry) {

    // TODO
    System.out.println("script: " + script);
    System.out.println("variables "
        + Joiner.on("\n\t").withKeyValueSeparator("=").join(registry));

    final PythonInterpreter interp = new PythonInterpreter();

    // Init variable cmd
    interp.set(VAR_CMD_NAME, new String());
    interp.set(INSTANCE_NAME, registry);

    // Add script
    interp.exec(script);

    // Retrieve standard output
    PyObject cmd = interp.get(VAR_CMD_NAME);

    // TODO
    System.out.println("cmd: " + cmd.asString());

    return cmd.asString();
  }

  public String parseCommandString(String cmdTag) {
    // Split on line
    final List<String> rawCommandInList = NEW_LINE.splitToList(cmdTag);

    final List<String> commandInList = Lists.newArrayList();

    // Build line script python
    for (final String line : rawCommandInList) {
      final LineScriptJython newLine = new LineScriptJython(line);
      commandInList.add(newLine.asString());

    }

    return Joiner.on(LINE_SEPARATOR).join(commandInList).trim();
  }

}
