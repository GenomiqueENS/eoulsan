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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.EoulsanRuntime.getSettings;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanMode.HADOOP_COMPATIBLE;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanMode.HADOOP_ONLY;
import static fr.ens.biologie.genomique.eoulsan.annotations.EoulsanMode.LOCAL_ONLY;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.annotations.EoulsanMode;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.steps.GalaxyToolStep;
import fr.ens.biologie.genomique.eoulsan.util.ClassPathResourceLoader;
import fr.ens.biologie.genomique.eoulsan.util.FileResourceLoader;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This class define a registry for steps.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ModuleRegistry {

  private static final String RESOURCE_PREFIX =
      "META-INF/services/registrytoolshed/";
  private static final String GALAXY_TOOL_SUBDIR = "galaxytools";

  private static ModuleRegistry instance;
  private final ModuleService service;
  private final GalaxyToolStepClassPathLoader galaxyClassPathLoader;
  private final GalaxyToolStepFileResourceLoader galaxyFileLoader;

  //
  // Inner classes
  //

  /**
   * This class define a resource loader for resource defined in the file
   * system.
   */
  private static final class GalaxyToolStepFileResourceLoader
      extends FileResourceLoader<GalaxyToolStep> {

    @Override
    protected String getExtension() {

      return ".xml";
    }

    @Override
    protected GalaxyToolStep load(final InputStream in, final String source)
        throws IOException, EoulsanException {

      checkNotNull(in, "in argument cannot be null");

      try {
        return new GalaxyToolStep(in, source);
      } catch (EoulsanException e) {
        throw new EoulsanException(
            "Unable to load Galaxy tool step: " + source);
      }
    }

    /**
     * Get the default format directory.
     * @return the default format directory
     */
    private static DataFile getDefaultFormatDirectory() {

      final Main main = Main.getInstance();

      if (main == null) {
        return new DataFile(GALAXY_TOOL_SUBDIR);
      }

      return new DataFile(
          new File(main.getEoulsanDirectory(), GALAXY_TOOL_SUBDIR));
    }

    @Override
    protected String getResourceName(final GalaxyToolStep resource) {

      checkNotNull(resource, "resource argument cannot be null");

      return resource.getName();
    }

    //
    // Constructors
    //

    /**
     * Constructor.
     * @param resourcePaths paths where searching for the resources.
     */
    public GalaxyToolStepFileResourceLoader(final String resourcePaths) {

      super(GalaxyToolStep.class, getDefaultFormatDirectory());

      if (resourcePaths != null) {
        addResourcePaths(resourcePaths);
      }
    }

  }

  /**
   * This class define a resource loader for resource defined in the class path.
   */
  private static final class GalaxyToolStepClassPathLoader
      extends ClassPathResourceLoader<GalaxyToolStep> {

    @Override
    protected GalaxyToolStep load(final InputStream in, final String source)
        throws IOException, EoulsanException {

      return new GalaxyToolStep(in, source);
    }

    @Override
    protected String getResourceName(final GalaxyToolStep resource) {

      checkNotNull(resource, "resource argument cannot be null");

      return resource.getName();
    }

    //
    // Constructor
    //

    public GalaxyToolStepClassPathLoader() {

      super(GalaxyToolStep.class, RESOURCE_PREFIX);
    }
  }

  //
  // Singleton method
  //

  /**
   * Retrieve the singleton static instance of StepRegistry.
   * @return A StepRegistry instance
   */
  public static synchronized ModuleRegistry getInstance() {

    if (instance == null) {
      instance = new ModuleRegistry();

      // Load the available steps
      instance.reload();
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
  public Module loadStep(final String stepName, final String version) {

    final List<Module> stepsFound = new ArrayList<>();

    stepsFound.addAll(this.service.newServices(stepName));
    stepsFound.addAll(this.galaxyClassPathLoader.loadResources(stepName));
    stepsFound.addAll(this.galaxyFileLoader.loadResources(stepName));

    // Filter steps
    filterSteps(stepsFound, Strings.nullToEmpty(version).trim());

    // Sort steps
    sortSteps(stepsFound);

    if (stepsFound.isEmpty()) {
      return null;
    }

    return stepsFound.get(stepsFound.size() - 1);
  }

  /**
   * Reload the list of available steps.
   */
  public void reload() {

    this.service.reload();
    this.galaxyClassPathLoader.reload();
    this.galaxyFileLoader.reload();

    // Log steps defined in jars
    for (Map.Entry<String, String> e : this.service.getServiceClasses()
        .entries()) {

      getLogger()
          .config("Found step: " + e.getKey() + " (" + e.getValue() + ")");
    }

    // Log Galaxy tool steps
    final List<GalaxyToolStep> stepsFound = new ArrayList<>();
    stepsFound.addAll(this.galaxyClassPathLoader.loadAllResources());
    stepsFound.addAll(this.galaxyFileLoader.loadAllResources());

    for (GalaxyToolStep s : stepsFound) {

      getLogger().config("Found step: "
          + s.getName() + " (Galaxy tool, source: " + s.getSource() + ")");
    }
  }

  /**
   * Filter the steps on their version.
   * @param steps steps to filter
   * @param version required version
   */
  private void filterSteps(final List<Module> steps, final String version) {

    // Do no filter if no version has been specified
    if (steps == null || "".equals(version)) {
      return;
    }

    final List<Module> toRemove = new ArrayList<>();

    // For each step
    for (Module step : steps) {

      // Get the version
      Version stepVersion = step.getVersion();

      // Discard null version
      if (stepVersion == null) {
        continue;
      }

      // Keep only the step with the right version
      if (!stepVersion.toString().equals(version)) {
        toRemove.add(step);
      }
    }

    // Remove all the entries
    steps.removeAll(toRemove);
  }

  /**
   * Sort the steps.
   * @param steps list of step to sort
   */
  private void sortSteps(final List<Module> steps) {

    // Do nothing if the list of step is null
    if (steps == null) {
      return;
    }

    // Sort the steps
    Collections.sort(steps, new Comparator<Module>() {

      @Override
      public int compare(final Module s1, final Module s2) {

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

      private int compareStepModes(final Module s1, final Module s2) {

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

      private int compareStepVersions(final Module s1, final Module s2) {

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
  private ModuleRegistry() {

    this.service = new ModuleService();
    this.galaxyClassPathLoader = new GalaxyToolStepClassPathLoader();
    this.galaxyFileLoader =
        new GalaxyToolStepFileResourceLoader(getSettings().getGalaxyToolPath());
  }

}
