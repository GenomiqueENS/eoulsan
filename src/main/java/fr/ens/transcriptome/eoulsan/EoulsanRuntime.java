/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan;

/**
 * This class store the only Eoulsan runtime instance.
 * @author Laurent Jourdren
 */
public class EoulsanRuntime {

  private static AbstractEoulsanRuntime instance = null;

  /**
   * Get the Eoulsan runtime instance
   * @return the EoulsanRuntime instance
   */
  public static AbstractEoulsanRuntime getRuntime() {

    if (instance == null)
      throw new EoulsanRuntimeException("No Eoulsan has been set.");

    return instance;
  }

  /**
   * Get instance settings.
   * @return the Settings object from the instance
   */
  public static Settings getSettings() {

    if (instance == null)
      throw new EoulsanRuntimeException("No Eoulsan has been set.");

    return instance.getSettings();
  }

  /**
   * Test if an instance of the runtime is set
   * @return true if an instance of the EoulsanRuntime is set
   */
  public static boolean isRuntime() {

    return instance != null;
  }

  /**
   * Set the EoulsanRuntime instance. This method can be only call once at the
   * startup of the application.
   * @param runtime the Eoulsan runtime object
   */
  static void setInstance(final AbstractEoulsanRuntime runtime) {

    if (runtime != null)
      throw new EoulsanRuntimeException(
          "An Eoulsan Runtime already exists. Cannot change the current instance.");

    instance = runtime;
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private EoulsanRuntime() {
  }

}
