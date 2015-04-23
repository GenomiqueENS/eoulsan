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

package fr.ens.transcriptome.eoulsan.core;

import static fr.ens.transcriptome.eoulsan.annotations.EoulsanMode.HADOOP_COMPATIBLE;
import static fr.ens.transcriptome.eoulsan.annotations.EoulsanMode.HADOOP_ONLY;
import static fr.ens.transcriptome.eoulsan.annotations.EoulsanMode.LOCAL_ONLY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Strings;

import fr.ens.transcriptome.eoulsan.annotations.EoulsanMode;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a registry for steps.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class StepRegistry {

  private static StepRegistry instance;
  private StepService service = new StepService();

  //
  // Singleton method
  //

  /**
   * Retrieve the singleton static instance of StepRegistry.
   * @return A StepRegistry instance
   */
  public static synchronized StepRegistry getInstance() {

    if (instance == null) {
      instance = new StepRegistry();
    }

    return instance;
  }

  //
  // Instances methods
  //

  /**
   * Load a step.
   * @param stepName name of the required step
   * @param version version of the required step
   * @return a step object or null if the requested step has been not found
   */
  public Step loadStep(final String stepName, final String version) {

    final List<Step> stepsFound = new ArrayList<>();

    stepsFound.addAll(service.newServices(stepName));

    // Filter steps
    filterSteps(stepsFound, Strings.nullToEmpty(version).trim());

    // Sort steps
    sortSteps(stepsFound);

    if (stepsFound.isEmpty()) {
      return null;
    }

    return stepsFound.get(0);
  }

  /**
   * Reload the list of available steps.
   */
  public void reload() {

    service.reload();
  }

  /**
   * Filter the steps on their version.
   * @param steps steps to filter
   * @param version required version
   */
  private void filterSteps(final List<Step> steps, final String version) {

    // Do no filter if no version has been specified
    if (steps == null || "".equals(version)) {
      return;
    }

    Step toKeep = null;

    // For each step
    for (Step step : steps) {

      // Get the version
      Version stepVersion = step.getVersion();

      // Discard null version
      if (stepVersion == null) {
        continue;
      }

      // Keep only the step with the right version
      if (stepVersion.toString().equals(version)) {
        toKeep = step;

        // Keep only the first occurrence
        break;
      }
    }

    // Remove all the entries
    steps.clear();

    // Add if exist the step with the right version
    if (toKeep != null) {
      steps.add(toKeep);
    }
  }

  /**
   * Sort the steps.
   * @param steps list of step to sort
   */
  private void sortSteps(final List<Step> steps) {

    // Do nothing if the list of step is null
    if (steps == null) {
      return;
    }

    // Sort the steps
    Collections.sort(steps, new Comparator<Step>() {

      @Override
      public int compare(final Step s1, final Step s2) {

        if (s1 == null) {
          return 1;
        }

        if (s2 == null) {
          return -1;
        }

        int result = compareStepModes(s1, s2);

        if (result != 0) {
          return result;
        }

        return compareStepVersions(s1, s2);
      }

      private int compareStepModes(final Step s1, final Step s2) {

        final EoulsanMode mode1 = EoulsanMode.getEoulsanMode(s1.getClass());
        final EoulsanMode mode2 = EoulsanMode.getEoulsanMode(s2.getClass());

        int result = compareModes(mode1, mode2, HADOOP_ONLY);

        if (result != 0) {
          return result;
        }

        result = compareModes(mode1, mode2, HADOOP_COMPATIBLE);

        if (result != 0) {
          return result;
        }

        return compareModes(mode1, mode2, LOCAL_ONLY);
      }

      private int compareModes(EoulsanMode mode1, EoulsanMode mode2,
          EoulsanMode modeToCompare) {

        if (mode1 == modeToCompare && mode2 != modeToCompare) {
          return 1;
        }

        if (mode2 == modeToCompare && mode1 != modeToCompare) {
          return -1;
        }

        return 0;
      }

      private int compareStepVersions(final Step s1, final Step s2) {

        final Version v1 = s1.getVersion();
        final Version v2 = s2.getVersion();

        if (v1 == null) {
          return 1;
        }

        if (v2 == null) {
          return -1;
        }

        return v1.compareTo(v2);
      }

    });

  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private StepRegistry() {

    // Load the available steps
    reload();
  }

}
