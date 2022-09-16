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
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.kenetre.io.CompressionType.NONE;
import static fr.ens.biologie.genomique.kenetre.io.CompressionType.getCompressionTypeByContentEncoding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.kenetre.util.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.DataMetadata;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.splitermergers.Splitter;

/**
 * This class define a generic splitter module.
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
@ReuseModuleInstance
public class SplitterModule extends AbstractModule {

  public static final String MODULE_NAME = "splitter";

  private Splitter splitter;
  private CompressionType compression = NONE;

  //
  // Inner class
  //

  /**
   * This inner class allow to create iterator needed by SplitterMerger.split()
   * method.
   */
  private static final class SplitterIterator {

    private final Data data;
    private final DataMetadata metadata;
    private final List<Data> list = new ArrayList<>();

    private Data getData(final int index) {

      checkState(index <= this.list.size(), "invalid index: "
          + index + " (maximum expected index: " + this.list.size() + ")");

      checkArgument(index >= 0, "index argument cannot be lower than 0");

      if (index == this.list.size()) {

        final Data newData =
            this.data.addDataToList(this.data.getName(), index);
        newData.getMetadata().set(this.metadata);
        this.list.add(newData);
      }

      return this.list.get(index);
    }

    Iterator<DataFile> getIterator() {
      return getIterator(-1);
    }

    Iterator<DataFile> getIterator(final int fileIndex) {

      checkArgument(fileIndex >= -1,
          "fileIndex argument cannot be lower than -1");

      return new Iterator<DataFile>() {

        final Map<Integer, Integer> counts = new HashMap<>();

        /**
         * Increment the counter for the current fileIndex.
         * @return the value of the counter before increment the count
         */
        private int incrCount() {

          if (!this.counts.containsKey(fileIndex)) {
            this.counts.put(fileIndex, 0);
          }

          final int result = this.counts.get(fileIndex);
          this.counts.put(fileIndex, result + 1);

          return result;
        }

        @Override
        public boolean hasNext() {
          return true;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }

        @Override
        public DataFile next() {

          final Data d = getData(incrCount());

          if (fileIndex == -1) {
            return d.getDataFile();
          } else {
            return d.getDataFile(fileIndex);
          }
        }
      };
    }

    /**
     * Constructor.
     * @param data the data
     */
    public SplitterIterator(final Data data, final DataMetadata metadata) {

      this.data = data;
      this.metadata = metadata;
    }

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

    return singleInputPort(this.splitter.getFormat());
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder()
        .addPort("output", true, this.splitter.getFormat(), this.compression)
        .create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final Set<Parameter> splitterParameters = new HashSet<>();

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case "format":
        // Get format
        final DataFormat format = DataFormatRegistry.getInstance()
            .getDataFormatFromNameOrAlias(p.getValue());

        // Check if the format exists
        if (format == null) {
          Modules.badParameterValue(context, p,
              "Unknown format: " + p.getValue());
        }

        // Check if a splitter exists for the format
        if (!format.isSplitter()) {
          Modules.badParameterValue(context, p,
              "No splitter exists for format: " + format.getName());
        }

        // Set the splitter
        this.splitter = format.getSplitter();
        break;

      case "compression":
        this.compression = getCompressionTypeByContentEncoding(p.getValue());
        break;

      default:
        splitterParameters.add(p);
        break;
      }
    }

    // Check if a format has been set
    if (this.splitter == null) {
      Modules.invalidConfiguration(context, "No format set for splitter");
    }

    // Configure the splitter
    this.splitter.configure(splitterParameters);
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final DataFormat format = this.splitter.getFormat();

    // Get input and output data
    final Data inData = context.getInputData(format);
    final Data outData = context.getOutputData(format, inData);

    final DataMetadata metadata = inData.getMetadata();

    try {

      if (inData.getPart() != -1) {
        throw new EoulsanException("Cannot split already split data");
      }

      // If Mono-file format
      if (format.getMaxFilesCount() == 1) {

        // Launch splitting
        this.splitter.split(inData.getDataFile(),
            new SplitterIterator(outData, metadata).getIterator());

      } else {

        // Create iterator
        final SplitterIterator iterator =
            new SplitterIterator(outData, metadata);

        // For each file of the multi-file format
        for (int fileIndex = 0; fileIndex < inData
            .getDataFileCount(); fileIndex++) {

          // Launch splitting
          this.splitter.split(inData.getDataFile(fileIndex),
              iterator.getIterator(fileIndex));
        }
      }

      // Successful result
      return status.createTaskResult();
    } catch (IOException | EoulsanException e) {

      // Fail of the task
      return status.createTaskResult(e);
    }
  }
}
