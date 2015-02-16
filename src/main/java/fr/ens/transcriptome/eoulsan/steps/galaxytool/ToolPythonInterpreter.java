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
package fr.ens.transcriptome.eoulsan.steps.galaxytool;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.testng.collections.Sets;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;

// TODO: Auto-generated Javadoc
/**
 * This class create a python interpreter which can build a command line tool
 * from command tag from Galaxy tool XML file.
 * @author Sandrine Perrin
 * @since 2.1
 */
public class ToolPythonInterpreter {

  /** The Constant NEW_LINE. */
  public final static Splitter NEW_LINE = Splitter.onPattern("[\r\n]")
      .trimResults().omitEmptyStrings();

  /** The Constant VAR_CMD_NAME. */
  public static final String VAR_CMD_NAME = "cmd";

  /** The Constant INSTANCE_NAME. */
  public static final String INSTANCE_NAME = "mapVariables";

  /** The Constant CALL_METHOD. */
  public static final String CALL_METHOD = INSTANCE_NAME + ".get";

  /** The variable names. */
  private final Set<String> variableNames = Sets.newHashSet();

  /** The Constant DEFAULT_VALUE_NULL. */
  static final String DEFAULT_VALUE_NULL = "no_authorized";

  /** The python script with java code. */
  private String pythonScriptWithJavaCode = null;

  private boolean isCommandLineTranslate = false;

  /**
   * Execute script.
   * @param definedVariableCommand the registry
   * @return the string
   * @throws EoulsanException the eoulsan exception
   */
  public String executeScript(final Map<String, String> definedVariableCommand)
      throws EoulsanException {

    if (!isCommandLineTranslate) {
      throw new EoulsanException(
          "Command tag has not been translate in script python, Can not be interpreted.");
    }

    Preconditions.checkNotNull(this.pythonScriptWithJavaCode,
        "Not found python script to interprete.");

    Preconditions.checkNotNull(this.variableNames,
        "None variables setting for python script.");

    if (this.variableNames.isEmpty()) {
      throw new EoulsanException(
          "Not found variables in python script to interprete.");
    }

    // TODO
    // System.out.println("script: " + script);
    // System.out.println("variables "
    // + Joiner.on("\n\t").withKeyValueSeparator("=").join(registry));

    final PythonInterpreter interp = new PythonInterpreter();

    // Init variable cmd
    interp.set(VAR_CMD_NAME, new String());
    interp.set(INSTANCE_NAME, definedVariableCommand);

    // Add script
    interp.exec(this.pythonScriptWithJavaCode);

    // Retrieve standard output
    final PyObject cmd = interp.get(VAR_CMD_NAME);

    // TODO
    // System.out.println("cmd: " + cmd.asString());

    return cmd.asString();
  }

  /**
   * Translate command xml in python.
   * @param cmdTag the content command tag.
   * @throws EoulsanException if the translation fails.
   */
  void translateCommandXMLInPython(final String cmdTag) throws EoulsanException {

    if (isCommandLineTranslate) {
      return;
    }

    // Split on line
    final List<String> rawCommandTag = NEW_LINE.splitToList(cmdTag);

    // Init translator
    final TranslatorStringToPython translator =
        new TranslatorStringToPython(rawCommandTag);

    // Receive all variables names found in command tag
    this.variableNames.addAll(translator.getVariableNames());

    // Receive code python for building command line after replace variables by
    // values
    this.pythonScriptWithJavaCode = translator.getTranslatedCommandInPython();

    isCommandLineTranslate = true;
  }

  /**
   * Gets the variable names.
   * @return the variable names
   */
  public Set<String> getVariableNames() {

    if (this.variableNames.isEmpty()) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(this.variableNames);
  }

  /**
   * Comparison parameters xml variables command.
   * @param toolInterpreter TODO
   * @param parametersXML the parameters xml
   * @return the map
   * @throws EoulsanException the eoulsan exception
   */
  Map<String, String> comparisonVariablesFromXMLToCommand(
      ToolInterpreter toolInterpreter, final Map<String, String> parametersXML)
      throws EoulsanException {

    final Map<String, String> results = new HashMap<>();

    // Parsing variable name found in command tag
    for (final String variableName : this.variableNames) {
      // Check exist
      if (parametersXML.get(variableName) == null) {
        results.put(variableName, DEFAULT_VALUE_NULL);
      }
    }
    return Collections.unmodifiableMap(results);
  }

}
