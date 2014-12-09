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
package fr.ens.transcriptome.eoulsan.toolgalaxy;

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

// TODO: Auto-generated Javadoc
/**
 * The Class ToolPythonInterpreter.
 * @author Sandrine Perrin
 * @since 2.4
 */
public class ToolPythonInterpreter {

  /** The Constant LINE_SEPARATOR. */
  private static final String LINE_SEPARATOR = System.getProperties()
      .getProperty("line.separator");
  
  /** The Constant NEW_LINE. */
  public final static Splitter NEW_LINE = Splitter.onPattern("[\r\n]")
      .trimResults().omitEmptyStrings();
  
  /** The Constant VAR_CMD_NAME. */
  public static final String VAR_CMD_NAME = "cmd";

  /** The Constant INSTANCE_NAME. */
  public static final String INSTANCE_NAME = "registry";
  
  /** The Constant CALL_METHOD. */
  public static final String CALL_METHOD = INSTANCE_NAME + ".get";

  /** The variable names. */
  private final Set<String> variableNames = Sets.newHashSet();

  /**
   * Execute script.
   * @param script the script
   * @param registry the registry
   * @return the string
   */
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

  /**
   * Parses the command string.
   * @param cmdTag the cmd tag
   * @return the string
   */
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

  /**
   * Gets the variable names.
   * @return the variable names
   */
  public Set<String> getVariableNames() {

    if (this.variableNames.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(variableNames);
  }
}
