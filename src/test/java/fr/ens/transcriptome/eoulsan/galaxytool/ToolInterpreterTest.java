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

public class ToolInterpreterTest {

  public final static Splitter SPLITTER = Splitter.on("=").trimResults()
      .omitEmptyStrings();

  public final static Splitter SPLITTER_KEY = Splitter.on(".").trimResults()
      .omitEmptyStrings();

  public final static Splitter SPLITTER_SPACE = Splitter.on(" ").trimResults()
      .omitEmptyStrings();

  private static final String NEW_TEST_KEY = "test_description";

  private static final File SRC_DIR = new File(new File(".").getAbsolutePath(),
      "src/main/java/META-INF/services/registrytoolshed/");

  @Test
  public void testToolInterpreter() throws FileNotFoundException, Exception {

    final File matrixTestToolshed =
        new File("src/test/java/files/testdatatoolshedgalaxy.txt");

    String line = "";
    ToolTest tt = null;

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

        if (key.equals(NEW_TEST_KEY)) {

          if (tt != null) {
            // Execute current test
            System.out.println("Launch test");
            tt.launchTest();
          }

          // Init new test
          System.out.println("Init new test");
          tt = new ToolTest(value);

        } else {

          // Update current test
          System.out.println("--- add data " + key + " -> " + value);
          tt.addData(key, value);
        }
      }

      // Launch last test setting in file
      if (tt != null) {
        System.out.println("Launch latest ");
        tt.launchTest();
      }

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  //
  // Internal Class
  //

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
        final File e = new File(value);
        if (e.exists())
          this.executableTool = e;

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

    public void launchTest() throws FileNotFoundException, EoulsanException {

      System.out.println("read xml "
          + toolXML.getAbsolutePath() + " exist " + toolXML.exists());

      // Init tool interpreter
      final GalaxyToolInterpreter interpreter =
          new GalaxyToolInterpreter(new FileInputStream(toolXML));

      // Configure
      interpreter.configure(setStepParameters);

      // Compile variable from parameter workflow and Tool file
      // compileParameters(interpreter);

      System.out.println("Tool interpreter create \n"
          + interpreter + "\n"
          + Joiner.on("\t").withKeyValueSeparator(":").join(variablesCommand));

      for (final ToolElement ptg : interpreter.getInputs().values()) {

        if (!ptg.isFile()) {

          System.out.println("TEST tool name "
              + ptg.getName() + " is include in "
              + this.variablesCommand.containsKey(ptg.getName()));

          if (!this.variablesCommand.containsKey(ptg.getName())) {
            System.out.println("TEST add param "
                + ptg.getName() + " - " + ptg.getValue());
            this.variablesCommand.put(ptg.getName(), ptg.getValue());
          }
        }
      }

      // Execute
      final ToolData toolData = interpreter.getToolData();
      System.out.println("Step parameters "
          + Joiner.on(" - ").join(setStepParameters));
      System.out.println("=> tool data generated \n\t" + toolData);

      final ToolPythonInterpreter tpi =
          new ToolPythonInterpreter(null, toolData, this.variablesCommand);

      compareCommandLine(tpi.interpreteScript());

    }

    private void compileParameters(final GalaxyToolInterpreter interpreter) {

      for (final ToolElement ptg : interpreter.getInputs().values()) {

        if (!ptg.isFile()) {
          this.variablesCommand.put(ptg.getName(), ptg.getValue());
        }
      }

      for (final ToolElement ptg : interpreter.getOutputs().values()) {

        if (!ptg.isFile()) {
          this.variablesCommand.put(ptg.getName(), ptg.getValue());
        }
      }

    }

    private void compareCommandLine(final String commandLine) {

      System.out.println("Compare expected \t"
          + this.command + "\nwith generate command \t" + commandLine);

      // Compare command
      final List<String> commandBuildByInterpreter =
          SPLITTER_SPACE.splitToList(commandLine);

      final List<String> commandExpected =
          SPLITTER_SPACE.splitToList(this.command);

      int length = commandExpected.size();

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

    private void addParam(final String key, final String value) {

      Preconditions.checkNotNull(key, "key for parameter");

      this.setStepParameters.add(new Parameter(key, value));

    }

    //
    // Constructor
    //

    ToolTest(final String description) {
      this.description = description;

      this.setStepParameters = new HashSet<Parameter>();
      this.variablesCommand = new HashMap<>();
    }
  }

}
