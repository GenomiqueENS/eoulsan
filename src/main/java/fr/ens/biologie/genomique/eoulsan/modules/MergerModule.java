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

import static fr.ens.biologie.genomique.eoulsan.io.CompressionType.NONE;
import static fr.ens.biologie.genomique.eoulsan.io.CompressionType.getCompressionTypeByContentEncoding;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.DataUtils;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.core.Version;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.splitermergers.Merger;

/**
 * This class define a generic merger module.
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
@ReuseModuleInstance
public class MergerModule extends AbstractModule {

  public static final String MODULE_NAME = "merger";

  private Merger merger;
  private CompressionType compression = NONE;

  //
  // Inner class
  //

  /**
   * This inner class allow to create iterator needed by SplitterMerger.merge()
   * method.
   */
  private final class MergerIterator {

    private final ListMultimap<String, Data> map = ArrayListMultimap.create();
    private int maxFileIndex = 1;

    public Set<String> getDataNames() {

      return this.map.keySet();
    }

    public List<Data> getListData(final String dataName) {

      return this.map.get(dataName);
    }

    public int getMaxFileIndex() {

      return this.maxFileIndex;
    }

    public Iterator<DataFile> getIterator(final String dataName)
        throws EoulsanException {

      return getIterator(dataName, -1);
    }

    public Iterator<DataFile> getIterator(final String dataName,
        final int fileIndex) throws EoulsanException {

      final List<Data> list = Lists.newArrayList(this.map.get(dataName));

      // Sort Data by their part number
      list.sort(Comparator.comparingInt(Data::getPart));

      // Check if two data has the same part number
      if (checkForPartDuplicates()) {
        final Set<Integer> partNumbers = new HashSet<>();
        for (Data data : list) {

          if (partNumbers.contains(data.getPart())) {
            throw new EoulsanException(
                "Found two or more data with the same part: " + data.getName());
          }
          partNumbers.add(data.getPart());
        }
      }

      final Iterator<Data> it = list.iterator();

      // Create the iterator itself
      return new Iterator<DataFile>() {

        @Override
        public boolean hasNext() {

          return it.hasNext();
        }

        @Override
        public DataFile next() {

          if (fileIndex == -1) {
            return it.next().getDataFile();
          } else {
            return it.next().getDataFile(fileIndex);
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }

    /**
     * Check that two keys cannot produce the same data name.
     * @throws EoulsanException if two keys share the same data name
     */
    private void checkKeys() throws EoulsanException {

      final Set<String> validNames = new HashSet<>();

      for (String name : getDataNames()) {

        final String validName = Naming.toValidName(name);

        if (validNames.contains(validName)) {
          throw new EoulsanException(
              "Two merger keys share the same data name ("
                  + name + " -> " + validName + ")");
        }

        validNames.add(validName);
      }
    }

    /**
     * Constructor.
     * @param data the data
     * @throws EoulsanException if two keys share the same data name
     */
    public MergerIterator(final Data data) throws EoulsanException {

      for (Data d : data.getListElements()) {

        final String key = getMapKey(d);

        if (key != null) {

          this.map.put(key, d);

          if (d.getDataFileCount() > this.maxFileIndex) {
            this.maxFileIndex = d.getDataFileCount();
          }
        }
      }

      // Check keys
      checkKeys();
    }

  }

  //
  // Protected methods
  //

  /**
   * Define the key to use for replicate merging.
   * @param data data to merge
   * @return the merging key
   */
  protected String getMapKey(final Data data) {

    return data.getName();
  }

  protected boolean checkForPartDuplicates() {

    return true;
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

    return new InputPortsBuilder()
        .addPort("input", true, this.merger.getFormat()).create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder()
        .addPort("output", true, this.merger.getFormat(), this.compression)
        .create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    final Set<Parameter> mergerParameters = new HashSet<>();

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

        // Check if a merger exists for the format
        if (!format.isMerger()) {
          Modules.badParameterValue(context, p,
              "No splitter exists for format: " + format.getName());
        }

        // Set the merger
        this.merger = format.getMerger();

        break;

      case "compression":
        this.compression = getCompressionTypeByContentEncoding(p.getValue());
        break;

      default:
        mergerParameters.add(p);
        break;
      }
    }

    // Check if a format has been set
    if (this.merger == null) {
      Modules.invalidConfiguration(context, "No format set for merge");
    }

    // Configure the merger
    this.merger.configure(mergerParameters);
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final DataFormat format = this.merger.getFormat();

    // Get input and output data
    final Data inListData = context.getInputData(format);
    final Data outListData = context.getOutputData(format, inListData);

    try {

      final MergerIterator it = new MergerIterator(inListData);

      for (String dataName : it.getDataNames()) {

        final Data outData =
            outListData.addDataToList(Naming.toValidName(dataName));

        // Set metadata for output data
        DataUtils.setDataMetadata(outData, it.getListData(dataName));

        // If Mono-file format
        if (format.getMaxFilesCount() == 1) {

          // Get output file
          final DataFile outFile = outData.getDataFile();

          // Launch merger
          this.merger.merge(it.getIterator(dataName), outFile);
        } else {

          // For each file of the multi-file format
          for (int fileIndex = 0; fileIndex < it
              .getMaxFileIndex(); fileIndex++) {

            // Get output file
            final DataFile outFile = outData.getDataFile(fileIndex);

            // Launch splitting
            this.merger.merge(it.getIterator(dataName, fileIndex), outFile);
          }
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
