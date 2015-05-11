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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.StepContext;

// TODO: Auto-generated Javadoc
/**
 * This class create a python interpreter which can build a command line tool
 * from command tag from Galaxy tool XML file.
 * @author Sandrine Perrin
 * @since 2.1
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

  private final Map<String, String> variablesCommand;

  private final ToolData tool;

  private final StepContext context;

  /**
   * Execute script.
   * @return the string
   * @throws EoulsanException the eoulsan exception
   */
  public ToolExecutorResult executeScript() throws EoulsanException {

    if (!isCommandLineTranslate) {
      throw new EoulsanException(
          "Command tag has not been translate in script python, Can not be interpreted.");
    }

    // Interpreter python script
    final String commandLine = interpreterScript();

    // TODO
    System.out.println("final command line " + commandLine);

    final ToolExecutor executor =
        new ToolExecutor(this.context, commandLine, this.tool.getToolName(),
            this.tool.getToolVersion());

    final ToolExecutorResult result = executor.execute();

    return result;

  }

  private String interpreterScript() throws EoulsanException {

    checkNotNull(this.pythonScriptWithJavaCode,
        "Not found python script to interprete.");

    checkNotNull(this.variablesCommand,
        "None variables setting for python script.");

    if (this.variablesCommand.isEmpty())
      // TODO Auto-generated method stub
      return null;

    final Map<String, String> variablesCommandFinal =
        addMissingVariableFromCommandLine();

    // TODO
    // System.out.println("script: " + script);
    // System.out.println("variables "
    // + Joiner.on("\n\t").withKeyValueSeparator("=").join(registry));

    final PythonInterpreter interpreter = new PythonInterpreter();

    // Init variable cmd
    interpreter.set(VAR_CMD_NAME, new String());
    interpreter.set(INSTANCE_NAME, variablesCommandFinal);

    // Add script
    interpreter.exec(this.pythonScriptWithJavaCode);

    // Retrieve standard output
    final PyObject cmd = interpreter.get(VAR_CMD_NAME);

    // TODO
    // System.out.println("cmd: " + cmd.asString());

    return addInterpreter(cmd.asString());
  }

  private String addInterpreter(final String cmd) {

    checkNotNull(cmd, "Command line can not be null");

    // Add interpreter if exists
    if (this.tool.isInterpreterSetting()) {

      return this.tool.getInterpreter()
          + " " + this.tool.getToolExecutable() + "/" + cmd.trim();

    } else {

      return cmd.trim();
    }

  }

  /**
   * Translate command xml in python.
   * @param cmdTag the content command tag.
   * @throws EoulsanException if the translation fails.
   */
  private void translateCommandXMLInPython() throws EoulsanException {

    if (isCommandLineTranslate) {
      return;
    }

    // Receive code python for building command line after replace variables by
    // values
    this.pythonScriptWithJavaCode = translator.getTranslatedCommandInPython();

    // TODO
    // System.out.println("DEBUG completed command with variable \t"
    // + this.pythonScriptWithJavaCode);

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

    final Map<String, String> results = Maps.newHashMap(variablesCommand);

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

    if (!isCommandLineTranslate) {
      throw new EoulsanException(
          "Can not get variable before translate command tag in script Python.");
    }

    // Receive all variables names found in command tag
    return translator.getVariableNames();

  }

  /**
   * Comparison parameters xml variables command.
   * @param toolInterpreter TODO
   * @param parametersXML the parameters xml
   * @return the map
   * @throws EoulsanException the eoulsan exception
   */
  private Map<String, String> compareVariablesFromXMLToCommand()
      throws EoulsanException {

    final Map<String, String> results = new HashMap<>();

    // Parsing variable name found in command tag
    for (final String variableName : getVariableNames()) {

      // Check exist
      if (this.variablesCommand.get(variableName) == null) {
        results.put(variableName, DEFAULT_VALUE_NULL);
      }

    }
    return Collections.unmodifiableMap(results);
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new tool python interpreter.
   * @param tool the tool
   * @param variablesCommand the variables command
   * @throws EoulsanException
   */
  public ToolPythonInterpreter(final StepContext context, final ToolData tool,
      final Map<String, String> variablesCommand) throws EoulsanException {

    Preconditions.checkNotNull(tool,
        "Tool instance from Galaxy Tool can not be null for interpretation.");

    // TODO
    System.out.println("cons python inter variables size "
        + variablesCommand.size());

    Preconditions
        .checkArgument(variablesCommand.size() != 0,
            "Tool instance from Galaxy Tool not found variables for interpretation");

    this.tool = tool;
    this.variablesCommand = variablesCommand;
    this.context = context;

    // Init translator
    this.translator =
        new TranslatorStringToPython(this.tool.getCmdTagContent());

    translateCommandXMLInPython();
  }

}
