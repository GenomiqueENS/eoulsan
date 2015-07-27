package fr.ens.transcriptome.eoulsan.ui;

import fr.ens.transcriptome.eoulsan.EoulsanRuntime;

/**
 * This class define an abstract UI class.
 * @author Laurent Jourdren
 * @since 2.0
 */
abstract class AbstractUI implements UI {

  private boolean interactiveMode;

  /**
   * Test if Eoulsan is running in an interactive mode.
   * @return true if Eoulsan is running in an interactive mode
   */
  protected boolean isInteractiveMode() {

    return this.interactiveMode;
  }

  //
  // Constructor
  //

  /**
   * Protected constructor.
   */
  protected AbstractUI() {

    if (EoulsanRuntime.getSettings()
        .getBooleanSetting("main.debug.ui.force.interactive.mode")) {
      this.interactiveMode = true;
    } else {
      this.interactiveMode = System.console() != null;
    }
  }

}
