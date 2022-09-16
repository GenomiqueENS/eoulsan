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
 * of the Institut de Biologie de l'École normale supérieure and
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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.kenetre.util.GuavaCompatibility;

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

  private final String cheetahScript;
  private final Map<String, String> variables;

  //
  // Inner class
  //

  /**
   * This class define a Python dictionary that __str__() returns can be
   * defined.
   */
  private static class PyStrDictionary extends PyDictionary {

    private static final long serialVersionUID = 1L;

    private PyString value;

    /**
     * The the value.
     * @param value the value
     */
    public void setValue(final String value) {

      this.value = new PyString(value);
    }

    @Override
    public PyString __str__() {

      if (this.value != null) {
        return this.value;
      }

      return super.__str__();
    }

    //
    // Constructor
    //

    /**
     * Default constructor.
     */
    public PyStrDictionary() {
    }

    /**
     * Constructor.
     * @param value the value of the returns of __str___
     */
    public PyStrDictionary(final String value) {
      setValue(value);
    }

  }

  /**
   * Execute script by Python interpreter and replace variable name by value.
   * @return final command line
   * @throws EoulsanException if an error throws by interpretation.
   */
  public String execute() throws EoulsanException {

    try (final PythonInterpreter interpreter = new PythonInterpreter()) {

      final PyObject nameSpace = createNameSpace(this.variables);

      interpreter.set("template", this.cheetahScript);
      interpreter.set("nameSpace", nameSpace);

      final String pythonScript = "from Cheetah.Template import Template\n"
          + "result = str(Template(template, searchList=[nameSpace]))";

      interpreter.exec(pythonScript);

      // Retrieve standard output
      final PyObject cmd = interpreter.get("result");

      return cmd.asString().replace('\n', ' ').trim();
    }
  }

  /**
   * Create the dictionary that contains all the placeholders for Cheetah.
   * @param plateholders the placeholders
   * @return a modified Python dictionnary
   */
  private static PyStrDictionary createNameSpace(
      final Map<String, String> plateholders) {

    final PyStrDictionary result = new PyStrDictionary();

    if (plateholders != null) {

      for (Map.Entry<String, String> e : plateholders.entrySet()) {

        List<String> fields =
            GuavaCompatibility.splitToList(Splitter.on('.'), e.getKey());

        PyStrDictionary dict = result;

        for (int i = 0; i < fields.size() - 1; i++) {

          final String f = fields.get(i);

          if (dict.containsKey(f)) {

            Object o = dict.get(f);
            if (o instanceof String) {
              final PyStrDictionary newDict = new PyStrDictionary((String) o);
              dict.put(f, newDict);
              dict = newDict;
            } else {
              dict = (PyStrDictionary) dict.get(f);
            }
          } else {
            final PyStrDictionary newDict = new PyStrDictionary();
            dict.put(f, newDict);
            dict = newDict;
          }
        }

        final String key = fields.get(fields.size() - 1);

        if (dict.containsKey(key)) {
          ((PyStrDictionary) dict.get(key)).setValue(e.getValue());
        } else {
          dict.put(key, e.getValue());
        }
      }
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Instantiates a new tool Cheetah script interpreter.
   * @param cheetahScript the Cheetah script to execute
   * @param variables the variables of the script
   * @throws EoulsanException if the constructor fails
   */
  public CheetahInterpreter(final String cheetahScript,
      final Map<String, String> variables) throws EoulsanException {

    requireNonNull(variables, "No variable set for Cheetah interpreter");

    checkState(!variables.isEmpty(),
        "Tool instance from Galaxy Tool not found variables for interpretation");

    this.cheetahScript = cheetahScript;
    this.variables = new HashMap<>(variables);
  }

}
