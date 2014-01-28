package fr.ens.transcriptome.eoulsan;

import static java.lang.System.setOut;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.actions.Action;
import fr.ens.transcriptome.eoulsan.actions.ActionService;
import fr.ens.transcriptome.eoulsan.actions.ValidationAction;

public class ValidationActionDemo {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  public static void main(String[] args) {

    final String pathEoulsanNewVersion = "/home/sperrin/home-net/eoulsan";
    final String listDatasets =
        "/home/sperrin/Documents/test_eoulsan/dataset_source/expected";
    final String pathOutputDirectory =
        "/home/sperrin/Documents/test_eoulsan/dataset_source";
    final String jobDescription = "validation_test";

    // Set the default local for all the application
    Globals.setDefaultLocale();

    // Set default log level
    LOGGER.setLevel(Globals.LOG_LEVEL);
    LOGGER.getParent().getHandlers()[0].setFormatter(Globals.LOG_FORMATTER);

    // Select the application execution mode
    final String eoulsanMode = System.getProperty(Globals.LAUNCH_MODE_PROPERTY);

    Main main;
    // if (eoulsanMode != null && eoulsanMode.equals("local")) {
    main = new MainCLI(new String[] {"validation"});
    // } else {
    // main = new MainHadoop(args);
    // }

    // // Get the action to execute
    // final Action action = main.getAction();
    Action action0 = ActionService.getInstance().getAction("validation");

    ValidationAction action = (ValidationAction) action0;
    
    // Get the Eoulsan settings
    final Settings settings = EoulsanRuntime.getSettings();

    // Test if action can be executed with current platform
    if (!settings.isBypassPlatformChecking()
        && !action.isCurrentArchCompatible()) {
      Common.showErrorMessageAndExit(Globals.WELCOME_MSG
          + "\nThe " + action.getName() + " of " + Globals.APP_NAME
          + " is not available for your platform. Required platforms: "
          + Main.availableArchsToString() + ".");

    }

    // Run action
    action.run(pathEoulsanNewVersion, listDatasets, pathOutputDirectory,
        jobDescription);
  }

}
