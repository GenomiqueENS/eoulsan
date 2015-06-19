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
package fr.ens.transcriptome.eoulsan.galaxytool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.python.google.common.base.Joiner;
import org.python.google.common.base.Preconditions;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.GalaxyToolInterpreter;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.ToolData;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.ToolPythonInterpreter;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.elements.ToolElement;

/**
 * The class define unit tests on GalaxyToolStep, it check if the command line
 * build from tool shed XML and parameters correspond to the expected syntax. It
 * use an extra file which set data useful to create command line and expected
 * value of this.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class ToolInterpreterTest {

  public final static Splitter SPLITTER = Splitter.on("=").trimResults()
      .omitEmptyStrings();

  public final static Splitter SPLITTER_KEY = Splitter.on(".").trimResults()
      .omitEmptyStrings();

  public final static Splitter SPLITTER_SPACE = Splitter.on(" ").trimResults()
      .omitEmptyStrings();

  /** Key for value test description, it marks too the start on a new test. */
  private static final String NEW_TEST_KEY = "test_description";

  /** Directory path which contains all tool shed XML file. */
  private static final File SRC_DIR = new File(new File(".").getAbsolutePath(),
      "src/main/java/META-INF/services/registrytoolshed/");

  /**
   * Test tool interpreter, it read the extra file. To set a test, it give XML
   * name file, parameter value, command line expected.
   * @throws FileNotFoundException the XML file is not found
   * @throws Exception if an error occurs during setting or execution on a test
   */
  @Test
  public void testToolInterpreter() throws FileNotFoundException, Exception {

    // Extract extra file, contains key=value
    final File matrixTestToolshed =
        new File("src/test/java/files/testdatatoolshedgalaxy.txt");

    String line = "";
    ToolTest tt = null;

    // Read file
    try (final BufferedReader br =
        new BufferedReader(new FileReader(matrixTestToolshed))) {

      while ((line = br.readLine()) != null) {

        final String lineTrimmed = line.trim();

        // Skip empty line or comment
        if (lineTrimmed.isEmpty()
            || lineTrimmed.startsWith("#") || !lineTrimmed.contains("=")) {
          continue;
        }

        final int pos = lineTrimmed.indexOf("=");

        if (pos < 0)
          throw new Exception("Invalid entry key=value in file with "
              + lineTrimmed);

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

    private final String description;
    private File toolXML;
    private File executableTool;

    private String command;
    private Set<Parameter> setStepParameters;
    private Map<String, String> variablesCommand;

    /**
     * Adds the data from extra file in test instance.
     * @param key the key
     * @param value the value
     * @throws EoulsanException it occurs if a data is invalid.
     */
    public void addData(final String key, final String value)
        throws EoulsanException {

      final String keyPrefix = SPLITTER_KEY.splitToList(key).get(0);
      final String nameVariable = key.substring(key.indexOf('.') + 1);

      switch (keyPrefix) {
      case TOOLSHEDXML_PATH_KEY:
        File f;
        // Absolute path
        if (value.startsWith("/")) {
          f = new File(value);
        } else {
          // File save in test directory files
          f = new File(SRC_DIR, value);
        }

        if (f.exists())
          this.toolXML = f;
        else
          throw new EoulsanException("ToolShed XML file does not exist "
              + f.getAbsolutePath());

        break;

      case EXECUTABLE_PATH_KEY:
        this.executableTool = new File(value);
        break;

      case COMMAND_KEY:
        this.command = value;
        break;

      case INPUT_KEY:
      case OUTPUT_KEY:
        this.variablesCommand.put(nameVariable, value);
        break;

      case PARAM_KEY:
        addParam(nameVariable, value);
        break;

      default:
      }

    }

    /**
     * Launch test.
     * @throws FileNotFoundException the XML file is not found
     * @throws Exception if an error occurs during setting or execution on a
     *           test
     */
    public void launchTest() throws FileNotFoundException, EoulsanException {

      // TODO
      System.out.println("read xml "
          + toolXML.getAbsolutePath() + " exist " + toolXML.exists());

      // Check if command executed is setting
      if (this.command == null || this.command.isEmpty()) {
        throw new EoulsanException(
            "UnitTest on GalaxyTool: missing command line expected, test can not be launch.");
      }

      // Init tool interpreter
      final GalaxyToolInterpreter interpreter =
          new GalaxyToolInterpreter(new FileInputStream(toolXML));

      // Configure interpreter with parameters setting in workflow Eoulsan file
      interpreter.configure(setStepParameters);

      // Compile variable from parameter workflow and Tool file
      compileParameters(interpreter);

      // Extract instance on toolData which contains all data useful from XML
      // file
      final ToolData toolData = interpreter.getToolData();

      // TODO
      System.out.println("Step parameters "
          + Joiner.on(" - ").join(setStepParameters));
      System.out.println("=> tool data generated \n\t" + toolData);

      // Init Tool python interpreter
      final ToolPythonInterpreter tpi =
          new ToolPythonInterpreter(null, toolData, this.variablesCommand);

      // Create command line and compare with command expected
      compareCommandLine(tpi.interpreteScript());

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

        if (!ptg.isFile()) {

          // TODO
          // System.out.println("TEST tool name "
          // + ptg.getName() + " is include in "
          // + this.variablesCommand.containsKey(ptg.getName()));

          // Update list variables needed to build command line
          if (!this.variablesCommand.containsKey(ptg.getName())) {

            this.variablesCommand.put(ptg.getName(), ptg.getValue());
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

      // TODO
      System.out.println("Compare expected \t"
          + this.command + "\nwith generate command \t" + commandLine);

      // Compare command
      final List<String> commandBuildByInterpreter =
          SPLITTER_SPACE.splitToList(commandLine);

      final List<String> commandExpected =
          SPLITTER_SPACE.splitToList(this.command);

      int length = commandExpected.size();

      // Compare length
      assertTrue("Number words requiered is invalid, expected "
          + length + " obtains by PythonInterpreter "
          + commandBuildByInterpreter.size(),
          length == commandBuildByInterpreter.size());

      // Compare word by word
      for (int i = 0; i < length; i++) {
        assertEquals(
            "Word not same, expected "
                + commandExpected.get(i) + " vs "
                + commandBuildByInterpreter.get(i), commandExpected.get(i),
            commandBuildByInterpreter.get(i));
      }
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

      this.setStepParameters = new HashSet<Parameter>();
      this.variablesCommand = new HashMap<>();
    }
  }

}
