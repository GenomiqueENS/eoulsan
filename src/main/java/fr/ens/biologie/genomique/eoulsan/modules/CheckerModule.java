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

package fr.ens.biologie.genomique.eoulsan.modules;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.checkers.CheckStore;
import fr.ens.biologie.genomique.eoulsan.checkers.Checker;
import fr.ens.biologie.genomique.eoulsan.core.DataUtils;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPort;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;

/**
 * This class is a module that launch checkers.
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
@ReuseModuleInstance
public class CheckerModule extends AbstractModule {

  public static final String MODULE_NAME = "checker";

  private static CheckerModule instance;

  private final Map<DataFormat, Checker> checkers = new HashMap<>();
  private final Map<DataFormat, Set<Parameter>> checkerConfiguration =
      new HashMap<>();
  private InputPorts inputPorts = InputPortsBuilder.noInputPort();
  private boolean inputPortsConfigured;

  /**
   * Configure input port of the checker from the output ports of the design
   * step.
   * @param designOutputPorts output ports of the design step
   */
  void configureInputPorts(final OutputPorts designOutputPorts) {

    checkState(!this.inputPortsConfigured,
        "inputPorts has been already configured");

    final InputPortsBuilder builder = new InputPortsBuilder();

    for (OutputPort port : designOutputPorts) {

      final String portName = port.getName();
      final DataFormat format = port.getFormat();

      if (format.isChecker()) {

        if (!this.checkers.containsKey(format)) {
          builder.addPort(portName, true, format);
          this.checkers.put(format, format.getChecker());
        }
      }
    }

    this.inputPorts = builder.create();

    this.inputPortsConfigured = true;
  }

  /**
   * This method allow to configure a checker from the configure method of other
   * steps, that's why this method is static.
   * @param format checker format to configure
   * @param parameters parameter of the checker
   */
  public static void configureChecker(final DataFormat format,
                                      final Set<Parameter> parameters) {

    requireNonNull(format, "format argument cannot be null");
    requireNonNull(parameters, "parameter argument cannot be null");
    checkArgument(format.isChecker(),
        "No checker exists for format: " + format.getName());
    checkState(instance != null,
        "Instance of CheckerModule has not been yet created");

    instance.checkerConfiguration.put(format, Sets.newHashSet(parameters));
  }

  //
  // Module methods
  //

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return this.inputPorts;
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    // Get the CheckStore
    final CheckStore checkStore = CheckStore.getCheckStore();

    int count = 0;

    try {

      final List<Checker> checkerList = createDependenciesList();

      // For all input ports of the step
      for (Checker checker : checkerList) {

        // Get the format of the checker
        final DataFormat format = checker.getFormat();

        // Configure checker if specific configuration exists
        if (this.checkerConfiguration.containsKey(format)) {
          checker.configure(this.checkerConfiguration.get(format));
        }

        for (Data data : context.getInputData(format).getListElements()) {

          context.getLogger()
              .info("Start checker "
                  + checker.getName() + " to check: "
                  + DataUtils.getDataFiles(data));

          // Check the data
          checker.check(data, checkStore);

          context.getLogger()
              .info("End of checker "
                  + checker.getName() + " to check: "
                  + DataUtils.getDataFiles(data));
        }

        count++;
        status.setProgress(((double) count) / checkerList.size());
      }

    } catch (EoulsanException e) {

      return status.createTaskResult(e);
    } finally {

      // Clear the checker
      this.checkers.clear();
      this.checkerConfiguration.clear();
    }

    return status.createTaskResult();
  }

  /**
   * Create the dependencies list of the checker.
   * @return a list of Checker object correctly ordered to avoid missing
   *         dependencies
   * @throws EoulsanException if dependencies order cannot be defined
   */
  private List<Checker> createDependenciesList() throws EoulsanException {

    List<Checker> list = Lists.newArrayList(this.checkers.values());
    List<Checker> result = new ArrayList<>();

    final Map<Checker, Set<Checker>> dependencies = new HashMap<>();
    final Set<Checker> added = new HashSet<>();

    // Create the dependencies map
    for (Checker c : list) {

      if (c == null) {
        continue;
      }

      final Set<Checker> deps = new HashSet<>();
      for (DataFormat format : c.getCheckersRequired()) {

        if (this.checkers.containsKey(format)) {
          deps.add(this.checkers.get(format));
        }
      }

      if (deps.size() == 0) {
        result.add(c);
        added.add(c);
      } else {
        dependencies.put(c, deps);
      }
    }

    // Resolve dependencies
    while (result.size() != list.size()) {

      final Set<Checker> toRemove = new HashSet<>();
      for (Map.Entry<Checker, Set<Checker>> e : dependencies.entrySet()) {
        e.getValue().removeAll(added);
        if (e.getValue().size() == 0) {
          toRemove.add(e.getKey());
        }
      }

      if (toRemove.size() == 0) {
        throw new EoulsanException("Unable to resolve checker dependencies");
      }

      for (Checker c : toRemove) {
        dependencies.remove(c);
        result.add(c);
        added.add(c);
      }
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   */
  public CheckerModule() {

    checkState(instance == null,
        "Instance of CheckerModule has been already created");
    instance = this;
  }

}
