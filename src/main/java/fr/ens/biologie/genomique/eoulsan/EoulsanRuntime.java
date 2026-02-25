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

package fr.ens.biologie.genomique.eoulsan;

/**
 * This class store the only Eoulsan runtime instance.
 *
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class EoulsanRuntime {

  private static AbstractEoulsanRuntime instance = null;

  /**
   * Get the Eoulsan runtime instance.
   *
   * @return the EoulsanRuntime instance
   */
  public static AbstractEoulsanRuntime getRuntime() {

    synchronized (EoulsanRuntime.class) {
      if (instance == null) {
        throw new EoulsanRuntimeException("No Eoulsan Runtime has been set.");
      }

      return instance;
    }
  }

  /**
   * Get instance settings.
   *
   * @return the Settings object from the instance
   */
  public static Settings getSettings() {

    if (instance == null) {
      throw new EoulsanRuntimeException("No Eoulsan Runtime has been set.");
    }

    return instance.getSettings();
  }

  /**
   * Test if an instance of the runtime is set.
   *
   * @return true if an instance of the EoulsanRuntime is set
   */
  public static boolean isRuntime() {

    synchronized (EoulsanRuntime.class) {
      return instance != null;
    }
  }

  /**
   * Set the EoulsanRuntime instance. This method can be only call once at the startup of the
   * application.
   *
   * @param runtime the Eoulsan runtime object
   */
  static void setInstance(final AbstractEoulsanRuntime runtime) {

    setInstance(runtime, false);
  }

  /**
   * Set the EoulsanRuntime instance. This method can be only call once at the startup of the
   * application.
   *
   * @param runtime the Eoulsan runtime object
   * @param noException no exception will be thrown if an instance already exists
   */
  static void setInstance(final AbstractEoulsanRuntime runtime, final boolean noException) {

    synchronized (EoulsanRuntime.class) {
      if (instance != null) {

        if (noException) {
          return;
        }

        throw new EoulsanRuntimeException(
            "An Eoulsan Runtime already exists. Cannot change the current instance.");
      }

      instance = runtime;
    }
  }

  //
  // Constructor
  //

  /** Private constructor. */
  private EoulsanRuntime() {

    throw new IllegalStateException();
  }
}
