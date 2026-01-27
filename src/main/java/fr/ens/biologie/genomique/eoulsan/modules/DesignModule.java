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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.NoLog;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.DataUtils;
import fr.ens.biologie.genomique.eoulsan.core.Naming;
import fr.ens.biologie.genomique.eoulsan.core.OutputPort;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.ParallelizationMode;
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
import fr.ens.biologie.genomique.eoulsan.data.DataFormats;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocol;
import fr.ens.biologie.genomique.eoulsan.data.protocols.StorageDataProtocol;
import fr.ens.biologie.genomique.eoulsan.design.Design;
import fr.ens.biologie.genomique.eoulsan.design.DesignUtils;
import fr.ens.biologie.genomique.eoulsan.design.Sample;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;

/**
 * This class define a design module.
 * @since 2.0
 * @author Laurent Jourdren
 */
@ReuseModuleInstance
@NoLog
public class DesignModule extends AbstractModule {

  public static final String MODULE_NAME = "design";

  private final Design design;
  private final CheckerModule checkerModule;
  private OutputPorts outputPorts;
  private final Set<String> designPortNames = new HashSet<>();
  private final Set<String> samplePortNames = new HashSet<>();

  @Override
  public String getName() {

    return MODULE_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public OutputPorts getOutputPorts() {

    return this.outputPorts;
  }

  @Override
  public ParallelizationMode getParallelizationMode() {

    return ParallelizationMode.NOT_NEEDED;
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    // Get the metadata keys of the design and the samples
    final Set<String> designMetadataKeys = this.design.getMetadata().keySet();
    final Set<String> sampleMetadataKeys =
        Sets.newHashSet(DesignUtils.getAllSamplesMetadataKeys(this.design));

    final OutputPortsBuilder builder = new OutputPortsBuilder();

    for (DataFormat format : DataFormatRegistry.getInstance().getAllFormats()) {

      // Search in Design metadata
      if (designMetadataKeys.contains(format.getDesignMetadataKeyName())) {

        final String key = format.getDesignMetadataKeyName();

        builder.addPort(key, !format.isOneFilePerAnalysis(), format,
            compressionTypeOfDesignMetadata(key));

        this.designPortNames.add(key);
      }

      // Search in Sample metadata
      if (sampleMetadataKeys.contains(format.getSampleMetadataKeyName())) {

        final String key = format.getSampleMetadataKeyName();

        builder.addPort(key, !format.isOneFilePerAnalysis(), format,
            compressionTypeOfField(key));

        this.samplePortNames.add(key);
      }

    }

    // Create the output ports
    this.outputPorts = builder.create();

    // Configure Checker input ports
    this.checkerModule.configureInputPorts(this.outputPorts);
  }

  /**
   * Get the compression of a field of the design. The compression returned is
   * the first compression found in the field.
   * @param fieldname the name of the field
   * @return a compression type
   */
  private CompressionType compressionTypeOfField(final String fieldname) {

    for (Sample sample : this.design.getSamples()) {

      final String fieldValue = sample.getMetadata().get(fieldname);

      if (fieldValue != null) {

        final DataFile file = new DataFile(fieldValue);
        final CompressionType fileCompression = file.getCompressionType();

        if (fileCompression != CompressionType.NONE) {
          return fileCompression;
        }
      }
    }

    return CompressionType.NONE;
  }

  /**
   * Get the compression of a metadata of the design.
   * @param key the key of the metadata
   * @return a compression type
   */
  private CompressionType compressionTypeOfDesignMetadata(final String key) {

    final String value = this.design.getMetadata().get(key);

    if (value != null) {

      final DataFile file = getUnderLyingDataFile(new DataFile(value));
      final CompressionType fileCompression = file.getCompressionType();

      if (fileCompression != CompressionType.NONE) {
        return fileCompression;
      }
    }

    return CompressionType.NONE;
  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    final Set<DataFile> files = new HashSet<>();
    final Set<String> dataNames = new HashSet<>();

    for (String portName : this.designPortNames) {

      final OutputPort port = getOutputPorts().getPort(portName);

      // Create DataFile object(s)
      List<DataFile> dataFiles = getDesignDatafilesPort(this.design, port);

      // Check if file has not been already processed
      DataFile f = dataFiles.get(0);
      if (files.contains(f)) {
        continue;
      }
      files.add(f);

      // Get the data object
      final Data data = context.getOutputData(port.getName(), port.getName());

      // Set the DataFile(s) in the Data object
      if (port.getFormat().getMaxFilesCount() == 1) {
        // Mono file data
        DataUtils.setDataFile(data, f);
      } else {
        // Multi-file data
        DataUtils.setDataFiles(data, dataFiles);
      }

    }

    for (Sample sample : this.design.getSamples()) {

      for (String portName : this.samplePortNames) {

        final OutputPort port = getOutputPorts().getPort(portName);

        // Create DataFile object(s)
        List<DataFile> dataFiles = getSampleDatafilesPort(sample, port);

        // Check if file has not been already processed
        DataFile f = dataFiles.get(0);
        if (files.contains(f)) {
          continue;
        }
        files.add(f);

        // Define the name of the port
        final String dataListName;
        if (port.isList() || port.getFormat().getMaxFilesCount() > 1) {
          dataListName = port.getName();
        } else {
          dataListName = Naming.toValidName(f.getBasename());
        }

        // Get the data object
        final Data dataList =
            context.getOutputData(port.getName(), dataListName);
        final Data data;

        // Set metadata
        if (port.isList()) {

          final String dataName = Naming.toValidName(sample.getId());

          // Check if the data name has already used
          if (dataNames.contains(dataName)) {
            return status.createTaskResult(new EoulsanException(
                "The design contains two or more sample with the same name after renaming: "
                    + dataName + " ( original sample name: " + sample.getId()
                    + ")"));
          }
          dataNames.add(dataName);

          // Add a new data to the list
          data = dataList.addDataToList(dataName);

          // Set the metadata
          DataUtils.setDataMetaData(data, sample);

        } else {
          data = dataList;
        }

        // Set the DataFile(s) in the Data object
        if (port.getFormat().getMaxFilesCount() == 1) {
          // Mono file data
          DataUtils.setDataFile(data, f);
        } else {
          // Multi-file data
          DataUtils.setDataFiles(data, dataFiles);

          // Set paired-end metadata
          if (DataFormats.READS_FASTQ.equals(port.getFormat())
              && dataFiles.size() > 1) {
            data.getMetadata().setPairedEnd(true);
          }

        }
      }

    }

    return status.createTaskResult();
  }

  /**
   * Create a list of data files from a sample and a port
   * @param sample the sample
   * @param port the port
   * @return a list with the data files
   */
  private List<DataFile> getSampleDatafilesPort(final Sample sample,
      final OutputPort port) {

    requireNonNull(sample, "sample argument cannot be null");
    requireNonNull(port, "port argument cannot be null");

    final List<DataFile> result = new ArrayList<>();

    // Get the design field name for the port
    String fieldName = null;
    for (String f : sample.getMetadata().keySet()) {
      if (port.getName().equals(f.trim().toLowerCase(Globals.DEFAULT_LOCALE))) {
        fieldName = f;
        break;
      }
    }

    // Get the values in the design for the sample
    final List<String> fieldValues = sample.getMetadata().getAsList(fieldName);

    for (String value : fieldValues) {
      result.add(new DataFile(value));
    }

    return result;
  }

  /**
   * Create a list of data files from a sample and a port
   * @param design the design
   * @param port the port
   * @return a list with the data files
   */
  private List<DataFile> getDesignDatafilesPort(final Design design,
      final OutputPort port) {

    requireNonNull(design, "design argument cannot be null");
    requireNonNull(port, "port argument cannot be null");

    final List<DataFile> result = new ArrayList<>();

    // Get the design field name for the port
    String fieldName = null;
    for (String f : design.getMetadata().keySet()) {
      if (port.getName().equals(f.trim().toLowerCase(Globals.DEFAULT_LOCALE))) {
        fieldName = f;
        break;
      }
    }

    // Get the values in the design for the sample
    final List<String> fieldValues = design.getMetadata().getAsList(fieldName);

    for (String value : fieldValues) {
      result.add(new DataFile(value));
    }

    return result;
  }

  /**
   * Get the underlying file if the file is available via a StorageDataProtocol.
   * @param file the input file
   * @return the underlying file if exist or the original file
   */
  private static DataFile getUnderLyingDataFile(final DataFile file) {

    if (file == null) {
      return null;
    }

    try {
      DataProtocol protocol = file.getProtocol();

      if (protocol != null && protocol instanceof StorageDataProtocol) {

        return ((StorageDataProtocol) protocol).getUnderLyingData(file);
      }
    } catch (IOException e) {
      return file;
    }

    return file;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param design design
   * @param checkeModule the checker module instance
   */
  public DesignModule(final Design design, final CheckerModule checkeModule) {

    requireNonNull(design, "design argument cannot be null");
    requireNonNull(checkeModule, "checkerModule argument cannot be null");

    this.design = design;
    this.checkerModule = checkeModule;
  }

}
