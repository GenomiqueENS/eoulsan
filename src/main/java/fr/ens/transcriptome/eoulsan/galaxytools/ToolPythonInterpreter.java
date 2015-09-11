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
package fr.ens.transcriptome.eoulsan.galaxytools;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;

/**
 * This class create a Cheetah interpreter, it can build a command line tool
 * from command tag from Galaxy tool XML file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolPythonInterpreter {

  /** The Constant VAR_CMD_NAME. */
  public static final String VAR_CMD_NAME = "cmd";

  /** The Constant INSTANCE_NAME. */
  public static final String INSTANCE_NAME = "mapVariables";

  /** The Constant CALL_METHOD. */
  public static final String CALL_METHOD = INSTANCE_NAME + ".get";

  // /** The variable names. */
  // private final Set<String> variableNamesInCommand = Sets.newHashSet();

  /** The Constant DEFAULT_VALUE_NULL. */
  static final String DEFAULT_VALUE_NULL = "no_authorized";

  /** The python script with java code. */
  private String pythonScriptWithJavaCode = null;

  /** The is command line translate. */
  private boolean isCommandLineTranslate = false;

  private final TranslatorStringToPython translator;

  private final Map<String, String> variables;

  /**
   * Interprete script by Python interpreter and replace variable name by value.
   * @return final command line
   * @throws EoulsanException if an error throws by interpretation.
   */
  public String interpretScript() throws EoulsanException {

    checkNotNull(this.pythonScriptWithJavaCode,
        "Not found python script to interprete.");

    checkNotNull(this.variables, "None variables setting for python script.");

    // if (this.variablesCommand.isEmpty())
    // // TODO
    // return null;

    final Map<String, String> variablesCommandFinal =
        addMissingVariableFromCommandLine();

    try (final PythonInterpreter interpreter = new PythonInterpreter()) {

      // Init variable cmd
      interpreter.set(VAR_CMD_NAME, "");
      interpreter.set(INSTANCE_NAME, variablesCommandFinal);

      // Add script
      interpreter.exec(this.pythonScriptWithJavaCode);

      // Retrieve standard output
      final PyObject cmd = interpreter.get(VAR_CMD_NAME);

      return cmd.asString().trim();
    }
  }

  /**
   * Translate command XML in Python.
   * @param cmdTag the content command tag.
   * @throws EoulsanException if the translation fails.
   */
  private void translateCommandXMLInPython() throws EoulsanException {

    if (isCommandLineTranslate) {
      return;
    }

    // Receive code Python for building command line after replace variables by
    // values
    this.pythonScriptWithJavaCode = translator.getTranslatedCommandInPython();

    isCommandLineTranslate = true;
  }

  /**
   * Adds the missing variable from command line, this can be extract from
   * parsing XML, which can be fail python interpreter execution.
   * @return the map all variable needed to interpreter python script.
   * @throws EoulsanException the Eoulsan exception
   */
  private Map<String, String> addMissingVariableFromCommandLine()
      throws EoulsanException {

    final Map<String, String> results = Maps.newHashMap(variables);

    // Compare with variable from command tag
    // Add variable not found in xml tag, corresponding to dataset value from
    // external file
    final Map<String, String> missingVariables =
        compareVariablesFromXMLToCommand();

    results.putAll(missingVariables);

    return Collections.unmodifiableMap(results);
  }

  /**
   * Gets the variable names.
   * @return the variable names
   * @throws EoulsanException
   */
  private Set<String> getVariableNames() throws EoulsanException {

    checkState(isCommandLineTranslate,
        "Can not get variable before translate command tag in script Python.");

    // Receive all variables names found in command tag
    return translator.getVariableNames();

  }

  /**
   * Comparison parameters xml variables command.
   * @return the map
   * @throws EoulsanException the eoulsan exception
   */
  private Map<String, String> compareVariablesFromXMLToCommand()
      throws EoulsanException {

    final Map<String, String> results = new HashMap<>();

    // Parsing variable name found in command tag
    for (final String variableName : getVariableNames()) {

      // Check exist
      if (this.variables.get(variableName) == null) {
        results.put(variableName, DEFAULT_VALUE_NULL);
      }

    }
    return Collections.unmodifiableMap(results);
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new tool Cheetah script interpreter.
   * @param script the Cheetah script to execute
   * @param variables the variables of the script
   * @throws EoulsanException
   */
  public ToolPythonInterpreter(final String script,
      final Map<String, String> variables) throws EoulsanException {

    checkState(!variables.isEmpty(),
        "Tool instance from Galaxy Tool not found variables for interpretation");

    // this.tool = tool;
    this.variables = new HashMap<>(variables);

    // Init translator
    this.translator = new TranslatorStringToPython(script);

    // Translate command in Cheetah syntax in Python script
    translateCommandXMLInPython();
  }

}
