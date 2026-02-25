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

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.EoulsanRuntimeException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.core.Step;
import java.util.HashMap;
import java.util.Map;

/**
 * This class store module instances and avoid storing this instance in Step objects that are
 * serialized.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepInstances {

  private static StepInstances instance;

  private final Map<Step, Module> steps = new HashMap<>();

  /**
   * Get a module instance.
   *
   * @param step the step
   * @return a module instance
   * @throws EoulsanRuntimeException if an error occurs while loading the module
   */
  public Module getModule(final Step step) {

    requireNonNull(step, "Step is null");
    final String stepName = step.getModuleName();
    final String stepVersion = step.getStepVersion();

    try {
      return getModule(step, stepName, stepVersion);
    } catch (EoulsanException e) {
      throw new EoulsanRuntimeException(e);
    }
  }

  /**
   * Get a module instance.
   *
   * @param step workflow step
   * @param moduleName module name
   * @param moduleVersion module version
   * @return a Module instance
   * @throws EoulsanException if an error occurs while loading the step
   */
  public Module getModule(final Step step, final String moduleName, final String moduleVersion)
      throws EoulsanException {

    requireNonNull(moduleName, "Step name is null");

    if (!this.steps.containsKey(step)) {

      // Load module
      final Module moduleInstance = loadModule(moduleName, moduleVersion);

      // Register module instance
      registerStep(step, moduleInstance);

      // return module instance
      return moduleInstance;
    }

    return this.steps.get(step);
  }

  /**
   * Register a step instance.
   *
   * @param step the step
   * @param module module instance
   */
  public void registerStep(final Step step, final Module module) {

    requireNonNull(step, "workflow step is null");
    requireNonNull(module, "module is null");

    this.steps.put(step, module);
  }

  /**
   * Remove a step instance.
   *
   * @param step workflow step
   */
  public void removeStep(final Step step) {

    requireNonNull(step);

    this.steps.remove(step);
  }

  //
  // Static methods
  //

  /**
   * Singleton method.
   *
   * @return the singleton
   */
  public static StepInstances getInstance() {

    if (instance == null) {
      instance = new StepInstances();
    }

    return instance;
  }

  //
  // Static methods
  //

  /**
   * Get a Module object from its name.
   *
   * @param moduleName name of the step
   * @param moduleVersion version of the step
   * @return a Module object
   * @throws EoulsanException if the module does not exits
   */
  private static Module loadModule(final String moduleName, final String moduleVersion)
      throws EoulsanException {

    if (moduleName == null) {
      throw new EoulsanException("Step name is null");
    }

    final String lower = moduleName.trim().toLowerCase(Globals.DEFAULT_LOCALE);

    final Module result = ModuleRegistry.getInstance().loadModule(lower, moduleVersion);

    if (result == null) {
      throw new EoulsanException(
          "Unknown module: "
              + lower
              + ("".equals(moduleVersion) ? "" : " (version required: " + moduleVersion + ")"));
    }

    return result;
  }

  //
  // Constructor
  //

  /** Private constructor. */
  private StepInstances() {}
}
