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

package fr.ens.transcriptome.eoulsan.steps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Splitter;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.core.workflow.DataUtils;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.util.FileUtils;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * Copy output files of a step with a specified format to the output directory.
 * @author Laurent Jourdren
 * @since 2.0
 */
@HadoopCompatible
@ReuseStepInstance
public class CopyOutputDataStep extends AbstractStep {

  public static final String STEP_NAME = "_copyoutputformat";
  public static final String PORTS_PARAMETER = "ports";
  public static final String FORMATS_PARAMETER = "formats";

  private final List<String> portNames = new ArrayList<>();
  private final List<DataFormat> formats = new ArrayList<>();

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public boolean isCreateLogFiles() {

    return false;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    for (int i = 0; i < this.portNames.size(); i++) {
      builder.addPort(this.portNames.get(i), this.formats.get(i));
    }

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {

    final OutputPortsBuilder builder = new OutputPortsBuilder();

    for (int i = 0; i < this.portNames.size(); i++) {
      builder.addPort(this.portNames.get(i), this.formats.get(i));
    }

    return builder.create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {
    for (Parameter p : stepParameters) {

      if (FORMATS_PARAMETER.equals(p.getName())) {

        final DataFormatRegistry registry = DataFormatRegistry.getInstance();

        for (String formatName : Splitter.on(',').split(p.getValue())) {

          final DataFormat format = registry.getDataFormatFromName(formatName);

          if (format == null) {
            throw new EoulsanException("Unknown format: " + formatName);
          }
          this.formats.add(format);
        }
      } else if (PORTS_PARAMETER.equals(p.getName())) {

        for (String portName : Splitter.on(',').split(p.getValue())) {
          this.portNames.add(portName);
        }
      }

    }

    if (this.formats.isEmpty()) {
      throw new EoulsanException("No format set.");
    }

    if (this.formats.size() != this.portNames.size()) {
      throw new EoulsanException("The number of formats ("
          + this.formats.size() + ") is not the same of the number of ports ("
          + this.portNames.size() + ")");
    }

  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {

      final InputPorts inputPorts = context.getCurrentStep().getInputPorts();

      for (String portName : inputPorts.getPortNames()) {

        final Data inData = context.getInputData(portName);
        final Data outData = context.getOutputData(portName, inData);

        copyFormat(context, inData, outData);

        status.setProgress(1.0);
      }

    } catch (IOException e) {
      return status.createStepResult(e);
    }
    return status.createStepResult();
  }

  //
  // Other methods
  //

  /**
   * Copy files for a format and a samples.
   * @param context step context
   * @param inData input data
   * @param outData output data
   * @throws IOException if an error occurs while copying
   */
  private void copyFormat(final StepContext context, final Data inData,
      final Data outData) throws IOException {

    final DataFile outputDir = new DataFile(context.getStepWorkingPathname());

    // Handle standard case
    if (inData.getFormat().getMaxFilesCount() == 1) {

      final DataFile in = inData.getDataFile();
      final DataFile out = new DataFile(outputDir, in.getName());

      if (!in.exists()) {
        throw new FileNotFoundException("input file not found: " + in);
      }

      // Copy file
      FileUtils.copy(in.rawOpen(), out.rawCreate());

      // Set the DataFile in the output data object
      DataUtils.setDataFile(outData, out);

    } else {

      final int count = inData.getDataFileCount();
      final List<DataFile> outFiles = new ArrayList<>();

      // Handle multi file format like fastq
      for (int i = 0; i < count; i++) {

        final DataFile in = inData.getDataFile(i);
        final DataFile out = new DataFile(outputDir, in.getName());
        outFiles.add(out);

        if (!in.exists()) {
          throw new FileNotFoundException("input file not found: " + in);
        }

        // Copy file
        FileUtils.copy(in.rawOpen(), out.rawCreate());

      }

      // Set the DataFile in the output data object
      DataUtils.setDataFiles(outData, outFiles);
    }
  }

}
