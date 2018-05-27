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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeDebug;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.DataToolElement;
import fr.ens.biologie.genomique.eoulsan.galaxytools.elements.ToolElement;
import fr.ens.biologie.genomique.eoulsan.util.GuavaCompatibility;

/**
 * The class define unit tests on GalaxyToolStep, it check if the command line
 * build from tool shed XML and parameters correspond to the expected syntax. It
 * use an extra file which set data useful to create command line and expected
 * value of this. Syntax example: _____________________________________________
 * test_description=grep python script_________________________________________
 * toolshedxml.path=grep.xml___________________________________________________
 * param.key1=value1___________________________________________________________
 * param.key3=value2___________________________________________________________
 * output.key3=value3__________________________________________________________
 * command.expected=python grep.py_____________________________________________
 * @author Sandrine Perrin
 * @since 2.0
 */
public class GalaxyToolInterpreterTest {

  public final static Splitter SPLITTER =
      Splitter.on("=").trimResults().omitEmptyStrings();

  public final static Splitter SPLITTER_KEY =
      Splitter.on(".").trimResults().omitEmptyStrings();

  public final static Splitter SPLITTER_SPACE =
      Splitter.on(" ").trimResults().omitEmptyStrings();

  /** Key for value test description, it marks too the start on a new test. */
  private static final String NEW_TEST_KEY = "test_description";

  /** Directory path which contains all tool shed XML file. */
  private static final String SRC_DIR = "/galaxytools";

  private static final String SRC_TESTS_SETTING =
      SRC_DIR + "/testdatatoolshedgalaxy.txt";

  @Before
  public void setUp() throws Exception {

    EoulsanRuntimeDebug.initDebugEoulsanRuntime();
  }

  /**
   * Test tool interpreter, it read the extra file. To set a test, it give XML
   * name file, parameter value, command line expected.
   * @throws Exception if an error occurs during setting or execution on a test
   */
  @Test
  public void testToolInterpreter() throws Exception {

    // Extract extra file, contains key=value
    final InputStream srcTestsSetting =
        this.getClass().getResourceAsStream(SRC_TESTS_SETTING);

    String line = "";
    ToolTest tt = null;

    // Read file
    try (final BufferedReader br =
        new BufferedReader(new InputStreamReader(srcTestsSetting))) {

      while ((line = br.readLine()) != null) {

        final String lineTrimmed = line.trim();

        // Skip empty line or comment
        if (lineTrimmed.isEmpty()
            || lineTrimmed.startsWith("#") || !lineTrimmed.contains("=")) {
          continue;
        }

        final int pos = lineTrimmed.indexOf("=");

        if (pos < 0)
          throw new Exception(
              "Invalid entry key=value in file with " + lineTrimmed);

        final String key = lineTrimmed.substring(0, pos);
        final String value =
            (pos < lineTrimmed.length() ? lineTrimmed.substring(pos + 1) : "");

        // Check if it is a new test
        if (key.equals(NEW_TEST_KEY)) {

          if (tt != null) {

            // Execute current test
            tt.launchTest();
          }

          // Init new test
          tt = new ToolTest(value);

        } else {

          // Update current test
          tt.addData(key, value);
        }
      }

      // Launch last test setting in file
      if (tt != null) {
        tt.launchTest();
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  //
  // Internal Class
  //

  /**
   * The class define a test corresponding to a tool and parameters associated.
   * @author Sandrine Perrin
   * @since 2.0
   */
  final class ToolTest {

    /** Keys expected from description file */
    private static final String TOOLSHEDXML_PATH_KEY = "toolshedxml";
    private static final String EXECUTABLE_PATH_KEY = "executable";

    private static final String COMMAND_KEY = "command";
    private static final String INPUT_KEY = "input";
    private static final String OUTPUT_KEY = "output";
    private static final String PARAM_KEY = "param";
    private static final String OTHER_KEY = "other";

    private final String description;
    private String toolXMLPath;
    private String executableToolPath;

    private String command;
    private final Set<Parameter> setStepParameters = new HashSet<>();
    private final Map<String, String> inputCommandVariables = new HashMap<>();
    private final Map<String, String> outputCommandVariables = new HashMap<>();
    private final Map<String, String> otherCommandVariables = new HashMap<>();

    /**
     * Adds the data from extra file in test instance.
     * @param key the key
     * @param value the value
     * @throws EoulsanException it occurs if a data is invalid.
     */
    public void addData(final String key, final String value)
        throws EoulsanException {

      final String keyPrefix =
          GuavaCompatibility.splitToList(SPLITTER_KEY, key).get(0);
      final String nameVariable = key.substring(key.indexOf('.') + 1);

      switch (keyPrefix) {
      case TOOLSHEDXML_PATH_KEY:
        // File save in test directory files
        this.toolXMLPath = SRC_DIR + "/" + value;

        break;

      case EXECUTABLE_PATH_KEY:
        this.executableToolPath = value;
        break;

      case COMMAND_KEY:
        this.command = value;
        break;

      case INPUT_KEY:
        this.inputCommandVariables.put(nameVariable, value);
        break;

      case OUTPUT_KEY:
        this.outputCommandVariables.put(nameVariable, value);
        break;

      case PARAM_KEY:
        addParam(nameVariable, value);
        break;

      case OTHER_KEY:
        this.otherCommandVariables.put(nameVariable, value);
        break;

      default:
      }

    }

    /**
     * Launch test.
     * @throws FileNotFoundException the XML file is not found
     * @throws EoulsanException if an error occurs during setting or execution
     *           on a test
     */
    public void launchTest() throws FileNotFoundException, EoulsanException {

      // Check if command executed is setting
      if (this.command == null || this.command.isEmpty()) {
        throw new EoulsanException(
            "UnitTest on GalaxyTool: missing command line expected, test can not be launch.");
      }

      // Init tool interpreter
      final InputStream is = this.getClass().getResourceAsStream(toolXMLPath);
      assertNotNull("Resource not found: " + toolXMLPath, is);
      final GalaxyToolInterpreter interpreter =
          new GalaxyToolInterpreter(is, toolXMLPath);

      // Configure interpreter with parameters setting in workflow Eoulsan file
      interpreter.configure(setStepParameters);

      // Compile variable from parameter workflow and Tool file
      compileParameters(interpreter);

      // Extract instance on toolData which contains all data useful from XML
      // file
      final ToolInfo toolData = interpreter.getToolInfo();

      // Check input data names
      int inputCount = 0;
      for (Map.Entry<String, ToolElement> e : interpreter.getInputs()
          .entrySet()) {

        if (e.getValue() instanceof DataToolElement) {
          inputCount++;
          assertTrue(this.inputCommandVariables.containsKey(e.getKey())
              || this.inputCommandVariables.containsKey(
                  GalaxyToolInterpreter.removeNamespace(e.getKey())));
        }
      }
      assertEquals(this.inputCommandVariables.size(), inputCount);

      // Check output data names
      int outputCount = 0;
      for (Map.Entry<String, ToolElement> e : interpreter.getOutputs()
          .entrySet()) {

        if (e.getValue() instanceof DataToolElement) {

          outputCount++;
          assertTrue(this.outputCommandVariables.containsKey(e.getKey())
              || this.outputCommandVariables.containsKey(
                  GalaxyToolInterpreter.removeNamespace(e.getKey())));
        }
      }
      assertEquals(this.outputCommandVariables.size(), outputCount);

      final Map<String, String> variables = new HashMap<>();
      variables.putAll(this.otherCommandVariables);
      variables.putAll(this.inputCommandVariables);
      variables.putAll(this.outputCommandVariables);

      // Init Tool python interpreter
      final CheetahInterpreter tpi =
          new CheetahInterpreter(toolData.getCheetahScript(), variables);

      // Create command line and compare with command expected
      compareCommandLine(tpi.execute());

    }

    /**
     * Compile parameters in interpreter instance, replace actions executed by
     * GalaxyTool from StepContext instance, which is extract from the workflow.
     * @param interpreter the interpreter instance.
     */
    private void compileParameters(final GalaxyToolInterpreter interpreter) {

      // Replace actions realize in GalaxyToolInterpreter from StepContext
      // instance
      for (final ToolElement ptg : interpreter.getInputs().values()) {

        if (!(ptg instanceof DataToolElement)) {

          // Update list variables needed to build command line
          if (!this.otherCommandVariables.containsKey(ptg.getName())) {
            this.otherCommandVariables.put(ptg.getName(), ptg.getValue());
          }
        }
      }
    }

    /**
     * Compare command line, word by word from expected version to generate by
     * Python interpreter.
     * @param commandLine the command line to compare
     */
    private void compareCommandLine(final String commandLine) {

      // Compare command
      final List<String> commandBuildByInterpreter =
          GuavaCompatibility.splitToList(SPLITTER_SPACE, commandLine);

      final List<String> commandExpected =
          GuavaCompatibility.splitToList(SPLITTER_SPACE, this.command);

      int length = commandExpected.size();

      // Compare length
      assertTrue("Number words requiered is invalid, expected "
          + length + " obtains by PythonInterpreter "
          + commandBuildByInterpreter.size() + ": " + commandBuildByInterpreter,
          length == commandBuildByInterpreter.size());

      // Compare word by word
      assertTrue(
          "Expected: "
              + commandExpected + " but got " + commandBuildByInterpreter,
          Objects.deepEquals(commandExpected, commandBuildByInterpreter));
    }

    /**
     * Adds the parameter in test, it should prefixed with param in extra file,
     * corresponding to value extract from workflow XML file.
     * @param key the key
     * @param value the value
     */
    private void addParam(final String key, final String value) {

      Preconditions.checkNotNull(key, "key for parameter");

      this.setStepParameters.add(new Parameter(key, value));

    }

    //
    // Constructor
    //

    /**
     * Constructor
     * @param description the description
     */
    ToolTest(final String description) {
      this.description = description;
    }
  }

}
