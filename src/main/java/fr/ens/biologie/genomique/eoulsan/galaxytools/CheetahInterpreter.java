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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */
package fr.ens.biologie.genomique.eoulsan.galaxytools;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashMap;
import java.util.Map;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import com.google.common.collect.Maps;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;

/**
 * This class create a Cheetah interpreter, it can build a command line tool
 * from command tag from Galaxy tool XML file.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class CheetahInterpreter {

  /** The Constant VAR_CMD_NAME. */
  public static final String VAR_CMD_NAME = "cmd";

  /** The Constant INSTANCE_NAME. */
  public static final String PYTHON_VARIABLES_DICT_NAME = "galaxy_dict";

  /** The Constant CALL_METHOD. */
  public static final String CALL_METHOD = PYTHON_VARIABLES_DICT_NAME + ".get";

  /** The Constant DEFAULT_VALUE_NULL. */
  static final String DEFAULT_VALUE_NULL = "no_authorized";

  /** The python script with java code. */
  private String pythonCode = null;

  private final CheetahToPythonTranslator translator;

  private final Map<String, String> variables;

  /**
   * Execute script by Python interpreter and replace variable name by value.
   * @return final command line
   * @throws EoulsanException if an error throws by interpretation.
   */
  public String execute() throws EoulsanException {

    // Translate command in Cheetah syntax in Python script
    this.pythonCode = translator.translate();

    try (final PythonInterpreter interpreter = new PythonInterpreter()) {

      // Init variable cmd
      interpreter.set(VAR_CMD_NAME, "");
      interpreter.set(PYTHON_VARIABLES_DICT_NAME, createVariablesDict());

      // Add script
      interpreter.exec(this.pythonCode);

      // Retrieve standard output
      final PyObject cmd = interpreter.get(VAR_CMD_NAME);

      return cmd.asString().trim();
    }
  }

  /**
   * Adds the missing variable from command line, this can be extract from
   * parsing XML, which can be fail python interpreter execution.
   * @return the map all variable needed to interpreter python script.
   * @throws EoulsanException the Eoulsan exception
   */
  private Map<String, String> createVariablesDict() throws EoulsanException {

    final Map<String, String> result = Maps.newHashMap(this.variables);

    // Parsing variable name found in command tag
    for (final String variableName : this.translator.getVariableNames()) {

      // Check exist
      if (this.variables.get(variableName) == null) {
        result.put(variableName, DEFAULT_VALUE_NULL);
      }
    }

    return result;
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
  public CheetahInterpreter(final String script,
      final Map<String, String> variables) throws EoulsanException {

    checkNotNull(variables, "No variable set for Cheetah interpreter");

    checkState(!variables.isEmpty(),
        "Tool instance from Galaxy Tool not found variables for interpretation");

    // this.tool = tool;
    this.variables = new HashMap<>(variables);

    // Init translator
    this.translator = new CheetahToPythonTranslator(script);
  }

}
