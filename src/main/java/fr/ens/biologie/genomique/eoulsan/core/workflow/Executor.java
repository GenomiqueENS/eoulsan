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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.Globals.APP_VERSION;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntime;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.Settings;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.Workflow;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.io.DefaultDesignReader;
import fr.ens.biologie.genomique.eoulsan.ui.UI;
import fr.ens.biologie.genomique.eoulsan.ui.UIService;
import fr.ens.biologie.genomique.eoulsan.util.SystemUtils;
import fr.ens.biologie.genomique.eoulsan.util.hadoop.HadoopInfo;

/**
 * This class is the executor for running all the steps of an analysis.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class Executor {

  private final ExecutorArguments arguments;
  private final CommandWorkflowModel command;
  private final Design design;

  //
  // Check methods
  //

  /**
   * Check design.
   * @throws EoulsanException if there is an issue with the design
   */
  private void checkDesign() throws EoulsanException {

    if (this.design == null) {
      throw new EoulsanException("The design is null");
    }

    // Check samples count
    if (this.design.getSamples().isEmpty()) {
      throw new EoulsanException(
          "Nothing to do, no samples found in design file");
    }

    getLogger().info("Found "
        + this.design.getSamples().size() + " sample(s) in design file");
  }

  //
  // Execute methods
  //

  /**
   * run Eoulsan.
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute() throws EoulsanException {

    execute(null, null);
  }

  /**
   * run Eoulsan.
   * @param firstSteps steps to add at the begin the workflow
   * @param lastSteps steps to add at the end the workflow
   * @throws EoulsanException if an error occurs while creating of executing
   *           steps
   */
  public void execute(final List<Module> firstSteps,
      final List<Module> lastSteps) throws EoulsanException {

    // Add general executor info
    logInfo(this.arguments, this.command);

    // Add Hadoop info in Hadoop mode
    if (EoulsanRuntime.getRuntime().getMode().isHadoopMode()) {
      HadoopInfo.logHadoopSysInfo();
    }

    // Check base path
    if (this.arguments.getLocalWorkingPathname() == null) {
      throw new EoulsanException("The base path is null");
    }

    // Check design
    checkDesign();

    // Get Eoulsan settings
    final Settings settings = EoulsanRuntime.getSettings();

    // Update settings with global parameters
    updateSettingsWithGlobalParameters(settings, this.command);

    // Add default Eoulsan external modules
    configureEoulsanTools(settings);

    // Create Workflow
    final CommandWorkflow workflow = new CommandWorkflow(this.arguments,
        this.command, firstSteps, lastSteps, this.design);

    // Check directories (log, working, output, temporary...)
    workflow.checkDirectories();

    // Get UI
    final UI ui = startUI(workflow);

    // Enable listen workflow events by ui
    StepObserverRegistry.getInstance().addObserver(ui);

    getLogger()
        .info("Start analysis at " + new Date(System.currentTimeMillis()));

    // Execute Workflow
    workflow.execute();
  }

  //
  // Utility methods
  //

  /**
   * Start the UI.
   * @param workflow the workflow
   * @return the UI object
   * @throws EoulsanException if an error occurs while stating the UI
   */
  private UI startUI(final Workflow workflow) throws EoulsanException {

    // Get the UI name
    final String uiName = EoulsanRuntime.getSettings().getUIName();

    if (uiName == null) {
      throw new EoulsanException("No UI name defined.");
    }

    // Get the UIService
    final UIService uiService = UIService.getInstance();

    // Test the UI exists
    if (!uiService.isService(uiName)) {
      throw new EoulsanException("Unknown UI name: " + uiName);
    }

    // Get the UI
    final UI ui = uiService.newService(uiName);

    // Initialize UI
    ui.init(workflow);

    return ui;
  }

  /**
   * Log some information about the current execution.
   * @param execArgs execution information
   * @param command workflow file content
   */
  private static void logInfo(final ExecutorArguments execArgs,
      final CommandWorkflowModel command) {

    getLogger().info("Design file path: " + execArgs.getDesignPathname());
    getLogger().info("Workflow file path: " + execArgs.getWorkflowPathname());

    getLogger().info("Workflow Author: " + command.getAuthor());
    getLogger().info("Workflow Description: " + command.getDescription());
    getLogger().info("Workflow Name: " + command.getName());

    getLogger().info("Job Id: " + execArgs.getJobId());
    getLogger().info("Job UUID: " + execArgs.getJobUUID());
    getLogger().info("Job Description: " + execArgs.getJobDescription());
    getLogger().info("Job Environment: " + execArgs.getJobEnvironment());

    getLogger().info("Job Base path: " + execArgs.getLocalWorkingPathname());
    getLogger().info("Job Output path: " + execArgs.getOutputPathname());
    getLogger().info("Job Log path: " + execArgs.getJobPathname());
  }

  /**
   * Load the design.
   * @param arguments executor arguments
   * @return the design
   * @throws EoulsanException if an error occurs while loading the design
   */
  private static Design loadDesign(final ExecutorArguments arguments)
      throws EoulsanException {

    try {

      // Load design
      getLogger().info("Read design file");

      // Get input stream of design file from arguments object
      final InputStream is = arguments.openDesignFile();
      requireNonNull(is, "The input stream for design file is null");

      // Read design file and return the design object
      return new DefaultDesignReader(is).read();

    } catch (IOException e) {
      throw new EoulsanException(
          "Error while reading design file: " + e.getMessage(), e);
    }
  }

  /**
   * Load workflow model.
   * @param arguments executor arguments
   * @param design design
   * @return the workflow model
   * @throws EoulsanException if an error occurs while creating the model
   */
  private static CommandWorkflowModel loadCommand(
      final ExecutorArguments arguments, final Design design)
      throws EoulsanException {

    try {

      // Get input stream of workflow file from arguments object
      final InputStream is = arguments.openParamFile();
      requireNonNull(is, "The input stream for workflow file is null");

      // Parse workflow file
      final CommandWorkflowParser pp = new CommandWorkflowParser(is);

      // Add command constants
      pp.addConstants(arguments);

      // Add design header entries
      for (Map.Entry<String, String> e : design.getMetadata().entrySet()) {
        pp.addConstant("design.header." + e.getKey(), e.getValue());
      }

      return pp.parse();
    } catch (IOException e) {
      throw new EoulsanException(e);
    }
  }

  /**
   * Update Eoulsan settings with global parameters.
   * @param settings the Eoulsan settings
   * @param command the command object
   */
  private static void updateSettingsWithGlobalParameters(
      final Settings settings, final CommandWorkflowModel command) {

    final Set<Parameter> globalParameters = command.getGlobalParameters();

    // Add globals parameters to Settings
    getLogger()
        .info("Init all steps with global parameters: " + globalParameters);
    for (Parameter p : globalParameters) {
      settings.setSetting(p.getName(), p.getStringValue());
    }
  }

  /**
   * Configure default standard Galaxy modules and external formats.
   * @param settings the Eoulsan settings
   */
  private static void configureEoulsanTools(final Settings settings) {

    // Is standard external modules enabled?
    if (!settings.isUseStandardExternalModules()) {
      return;
    }

    // Is internet connection active?
    if (SystemUtils.isActiveConnection(Globals.INTERNET_CHECK_SERVER,
        Globals.INTERNET_CHECK_PORT, 5000)) {

      // Define the branch to use
      String branch = "master";
      if (APP_VERSION.getMajor() > 1) {
        branch =
            "branch" + APP_VERSION.getMajor() + "." + APP_VERSION.getMinor();
      }

      // Define default external Galaxy tools Path
      String defaultGalaxyToolPath =
          Globals.EOULSAN_TOOLS_WEBSITE_URL + "/" + branch + "/galaxytools";

      // Define default external format Path
      String defaultDataFormatPath =
          Globals.EOULSAN_TOOLS_WEBSITE_URL + "/" + branch + "/formats";

      // Add standard galaxy tools from Eoulsan tools GitHub repository
      List<String> galaxyToolPathList =
          new ArrayList<>(settings.getGalaxyToolPaths());
      galaxyToolPathList.add(defaultGalaxyToolPath);
      settings.setGalaxyToolsPaths(galaxyToolPathList);

      // Add standard format from Eoulsan tools GitHub repository
      List<String> dataFormatPathList =
          new ArrayList<>(settings.getDataFormatPaths());
      dataFormatPathList.add(defaultDataFormatPath);
      settings.setDataFormatPaths(dataFormatPathList);
    }

  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param arguments arguments for the Executor
   * @throws EoulsanException if an error occurs while loading and parsing
   *           design and workflow files
   */
  public Executor(final ExecutorArguments arguments) throws EoulsanException {

    requireNonNull(arguments, "The arguments of the executor is null");

    this.arguments = arguments;
    this.design = loadDesign(arguments);
    this.command = loadCommand(arguments, this.design);
  }

}
