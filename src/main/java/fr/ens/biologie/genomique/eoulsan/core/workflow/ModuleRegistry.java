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
import static fr.ens.biologie.genomique.eoulsan.EoulsanRuntime.getSettings;
import static fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode.HADOOP_COMPATIBLE;
import static fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode.HADOOP_ONLY;
import static fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode.LOCAL_ONLY;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Strings;
import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Main;
import fr.ens.biologie.genomique.eoulsan.annotations.ExecutionMode;
import fr.ens.biologie.genomique.eoulsan.core.Module;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.modules.GalaxyToolModule;
import fr.ens.biologie.genomique.eoulsan.util.ClassPathResourceLoader;
import fr.ens.biologie.genomique.eoulsan.util.FileResourceLoader;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class define a registry for modules.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
public class ModuleRegistry {

  private static final String RESOURCE_PREFIX = "META-INF/services/registrytoolshed/";
  private static final String GALAXY_TOOL_SUBDIR = "galaxytools";

  private static ModuleRegistry instance;
  private final ModuleService service;
  private final GalaxyToolModuleClassPathLoader galaxyClassPathLoader;
  private final GalaxyToolModuleFileResourceLoader galaxyFileLoader;
  private final Set<String> javaModuleFound = new LinkedHashSet<>();

  //
  // Inner classes
  //

  /** This class define a resource loader for resource defined in the file system. */
  private static final class GalaxyToolModuleFileResourceLoader
      extends FileResourceLoader<GalaxyToolModule> {

    @Override
    protected String getExtension() {

      return ".xml";
    }

    @Override
    protected GalaxyToolModule load(final InputStream in, final String source)
        throws IOException, EoulsanException {

      requireNonNull(in, "in argument cannot be null");

      try {
        return new GalaxyToolModule(in, source);
      } catch (EoulsanException e) {
        throw new EoulsanException("Unable to load Galaxy tool module: " + source, e);
      }
    }

    /**
     * Get the default format directory.
     *
     * @return the default format directory
     */
    private static DataFile getDefaultFormatDirectory() {

      final Main main = Main.getInstance();

      if (main == null) {
        return new DataFile(GALAXY_TOOL_SUBDIR);
      }

      return new DataFile(main.getEoulsanDirectory(), GALAXY_TOOL_SUBDIR);
    }

    @Override
    protected String getResourceName(final GalaxyToolModule resource) {

      requireNonNull(resource, "resource argument cannot be null");

      return resource.getName();
    }

    //
    // Constructors
    //

    /**
     * Constructor.
     *
     * @param resourcePaths paths where searching for the resources.
     */
    public GalaxyToolModuleFileResourceLoader(final List<String> resourcePaths) {

      super(GalaxyToolModule.class, getDefaultFormatDirectory());

      if (resourcePaths != null) {
        addResourcePaths(resourcePaths);
      }
    }
  }

  /** This class define a resource loader for resource defined in the class path. */
  private static final class GalaxyToolModuleClassPathLoader
      extends ClassPathResourceLoader<GalaxyToolModule> {

    @Override
    protected GalaxyToolModule load(final InputStream in, final String source)
        throws IOException, EoulsanException {

      return new GalaxyToolModule(in, source);
    }

    @Override
    protected String getResourceName(final GalaxyToolModule resource) {

      requireNonNull(resource, "resource argument cannot be null");

      return resource.getName();
    }

    //
    // Constructor
    //

    public GalaxyToolModuleClassPathLoader() {

      super(GalaxyToolModule.class, RESOURCE_PREFIX);
    }
  }

  //
  // Singleton method
  //

  /**
   * Retrieve the singleton static instance of ModuleRegistry.
   *
   * @return A ModuleRegistry instance
   */
  public static synchronized ModuleRegistry getInstance() {

    if (instance == null) {
      instance = new ModuleRegistry();

      // Load the available modules
      instance.reload();
    }

    return instance;
  }

  //
  // Instances methods
  //

  /**
   * Load a module.
   *
   * @param moduleName name of the required module
   * @param version version of the required module
   * @return a Module object or null if the requested module has been not found
   */
  public Module loadModule(final String moduleName, final String version) {

    final List<Module> modulesFound = new ArrayList<>();

    modulesFound.addAll(this.service.newServices(moduleName));
    modulesFound.addAll(this.galaxyClassPathLoader.loadResources(moduleName));
    modulesFound.addAll(this.galaxyFileLoader.loadResources(moduleName));

    // Filter modules
    filterModules(modulesFound, Strings.nullToEmpty(version).trim());

    // Sort modules
    sortModules(modulesFound);

    if (modulesFound.isEmpty()) {
      return null;
    }

    return modulesFound.get(modulesFound.size() - 1);
  }

  /** Reload the list of available modules. */
  public void reload() {

    this.service.reload();
    this.galaxyClassPathLoader.reload();
    this.galaxyFileLoader.reload();

    this.javaModuleFound.clear();

    // Log modules defined in jars
    for (Map.Entry<String, String> e : this.service.getServiceClasses().entries()) {

      this.javaModuleFound.add(e.getKey());

      getLogger().config("Found module: " + e.getKey() + " (" + e.getValue() + ")");
    }

    // Log Galaxy tool modules
    final List<GalaxyToolModule> modulesFound = new ArrayList<>();
    modulesFound.addAll(this.galaxyClassPathLoader.loadAllResources());
    modulesFound.addAll(this.galaxyFileLoader.loadAllResources());

    for (GalaxyToolModule s : modulesFound) {

      getLogger()
          .config("Found module: " + s.getName() + " (Galaxy tool, source: " + s.getSource() + ")");
    }
  }

  /**
   * Get all the modules.
   *
   * @return a list of all the modules
   */
  public List<Module> getAllModules() {

    List<Module> result = new ArrayList<>();

    // Load all Java modules
    for (String moduleName : this.javaModuleFound) {

      result.addAll(this.service.newServices(moduleName));
    }

    // Load all Galaxy modules
    result.addAll(this.galaxyClassPathLoader.loadAllResources());
    result.addAll(this.galaxyFileLoader.loadAllResources());

    return result;
  }

  /**
   * Filter the modules on their version.
   *
   * @param modules modules to filter
   * @param version required version
   */
  private void filterModules(final List<Module> modules, final String version) {

    // Do no filter if no version has been specified
    if (modules == null || "".equals(version)) {
      return;
    }

    final List<Module> toRemove = new ArrayList<>();

    // For each module
    for (Module module : modules) {

      // Get the version
      Version moduleVersion = module.getVersion();

      // Discard null version
      if (moduleVersion == null) {
        continue;
      }

      // Keep only the module with the right version
      if (!moduleVersion.toString().equals(version)) {
        toRemove.add(module);
      }
    }

    // Remove all the entries
    modules.removeAll(toRemove);
  }

  /**
   * Sort the modules.
   *
   * @param modules list of module to sort
   */
  private void sortModules(final List<Module> modules) {

    // Do nothing if the list of module is null
    if (modules == null) {
      return;
    }

    // Reverse the order of the module to prioritize modules of the first
    // sources
    Collections.reverse(modules);

    // Sort the modules
    modules.sort(
        new Comparator<Module>() {

          @Override
          public int compare(final Module m1, final Module m2) {

            if (m1 == null) {
              return 1;
            }

            if (m2 == null) {
              return -1;
            }

            int result = compareModuleModes(m1, m2);

            if (result != 0) {
              return result;
            }

            return compareModuleVersions(m1, m2);
          }

          private int compareModuleModes(final Module m1, final Module m2) {

            final ExecutionMode mode1 = ExecutionMode.getExecutionMode(m1.getClass());
            final ExecutionMode mode2 = ExecutionMode.getExecutionMode(m2.getClass());

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

          private int compareModes(
              ExecutionMode mode1, ExecutionMode mode2, ExecutionMode modeToCompare) {

            if (mode1 == modeToCompare && mode2 != modeToCompare) {
              return 1;
            }

            if (mode2 == modeToCompare && mode1 != modeToCompare) {
              return -1;
            }

            return 0;
          }

          private int compareModuleVersions(final Module s1, final Module s2) {

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

  /** Private constructor. */
  private ModuleRegistry() {

    this.service = new ModuleService();
    this.galaxyClassPathLoader = new GalaxyToolModuleClassPathLoader();
    this.galaxyFileLoader =
        new GalaxyToolModuleFileResourceLoader(getSettings().getGalaxyToolPaths());
  }
}
