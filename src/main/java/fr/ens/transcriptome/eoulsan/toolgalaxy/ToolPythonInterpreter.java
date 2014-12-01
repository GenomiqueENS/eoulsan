package fr.ens.transcriptome.eoulsan.toolgalaxy;

import static fr.ens.transcriptome.eoulsan.toolgalaxy.VariableRegistry.INSTANCE_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.core.PyObject;
import org.python.google.common.base.Joiner;
import org.python.util.PythonInterpreter;
import org.testng.collections.Sets;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class ToolPythonInterpreter {

  private static final String LINE_SEPARATOR = System.getProperties()
      .getProperty("line.separator");
  public final static Splitter NEW_LINE = Splitter.onPattern("[\r\n]")
      .trimResults().omitEmptyStrings();
  public static final String VAR_CMD_NAME = "cmd";

  private final Set<String> variableNames = Sets.newHashSet();

  public String executeScript(final String script,
      final Map<String, String> registry) {

    // TODO
    System.out.println("script: " + script);
//    System.out.println("variables "
//        + Joiner.on("\n\t").withKeyValueSeparator("=").join(registry));

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
      // TODO
      // final LineScriptJython newLine = new LineScriptJython(line);
      final ScriptLineJython newLine = new ScriptLineJython(line);
      commandInList.add(newLine.asString());

      this.variableNames.addAll(newLine.getVariableNames());
    }

    return Joiner.on(LINE_SEPARATOR).join(commandInList).trim();
  }

  public Set<String> getVariableNames() {

    if (this.variableNames.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(variableNames);
  }
}
