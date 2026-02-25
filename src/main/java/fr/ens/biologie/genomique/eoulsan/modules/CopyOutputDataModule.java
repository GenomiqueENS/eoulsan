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

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.NoLog;
import fr.ens.biologie.genomique.eoulsan.annotations.NoOutputDirectory;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.DataUtils;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Modules;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskContext;
import fr.ens.biologie.genomique.eoulsan.core.TaskResult;
import fr.ens.biologie.genomique.eoulsan.core.TaskStatus;
import fr.ens.biologie.genomique.eoulsan.data.Data;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.kenetre.util.Version;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Copy output files of a step with a specified format to the output directory.
 *
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
@ReuseModuleInstance
@NoLog
@NoOutputDirectory
public class CopyOutputDataModule extends AbstractModule {

  public static final String MODULE_NAME = "_copyoutputformat";
  public static final String PORTS_PARAMETER = "ports";
  public static final String FORMATS_PARAMETER = "formats";

  private String portName;
  private DataFormat format;

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

    return new InputPortsBuilder().addPort(this.portName, this.format).create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort(this.portName, this.format).create();
  }

  @Override
  public void configure(final StepConfigurationContext context, final Set<Parameter> stepParameters)
      throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {
        case FORMATS_PARAMETER:
          final DataFormatRegistry registry = DataFormatRegistry.getInstance();

          final DataFormat format = registry.getDataFormatFromName(p.getValue());

          if (format == null) {
            Modules.badParameterValue(context, p, "Unknown format: " + p.getValue());
          }

          this.format = format;
          break;

        case PORTS_PARAMETER:
          this.portName = p.getValue();
          break;

        default:
          Modules.unknownParameter(context, p);
      }
    }
  }

  @Override
  public TaskResult execute(final TaskContext context, final TaskStatus status) {

    try {

      final InputPorts inputPorts = context.getCurrentStep().getInputPorts();

      for (String portName : inputPorts.getPortNames()) {

        final Data inData = context.getInputData(portName);
        final Data outData = context.getOutputData(portName, inData);

        copyData(context, inData, outData);
        status.setProgress(1.0);
      }

    } catch (IOException e) {
      return status.createTaskResult(e);
    }
    return status.createTaskResult();
  }

  //
  // Other methods
  //

  /**
   * Check input and output files.
   *
   * @param inFile input file
   * @param outFile output file
   * @throws IOException if copy cannot be started
   */
  private static void checkFiles(final DataFile inFile, final DataFile outFile) throws IOException {

    if (inFile.equals(outFile)) {
      throw new IOException("Cannot copy file on itself: " + inFile);
    }

    if (!inFile.exists()) {
      throw new FileNotFoundException("Input file not found: " + inFile);
    }

    if (outFile.exists()) {
      throw new IOException("Output file already exists: " + outFile);
    }
  }

  /**
   * Copy files for a format and a samples.
   *
   * @param context step context
   * @param inData input data
   * @param outData output data
   * @throws IOException if an error occurs while copying
   */
  private void copyData(final TaskContext context, final Data inData, final Data outData)
      throws IOException {

    final DataFile outputDir = context.getStepOutputDirectory();

    // Handle standard case
    if (inData.getFormat().getMaxFilesCount() == 1) {

      final DataFile in = inData.getDataFile();
      final DataFile out = new DataFile(outputDir, in.getName());

      // Check input and output files
      checkFiles(in, out);

      // Copy file
      DataFiles.symlinkOrCopy(in, out, true);

      // Set the DataFile in the output data object
      DataUtils.setDataFile(outData, out);

    } else {

      final int count = inData.getDataFileCount();
      final List<DataFile> outFiles = new ArrayList<>();

      // Handle multi file format like FASTQ files
      for (int i = 0; i < count; i++) {

        final DataFile in = inData.getDataFile(i);
        final DataFile out = new DataFile(outputDir, in.getName());
        outFiles.add(out);

        // Check input and output files
        checkFiles(in, out);

        // Copy file
        DataFiles.symlinkOrCopy(in, out, true);
      }

      // Set the DataFile in the output data object
      DataUtils.setDataFiles(outData, outFiles);
    }
  }
}
