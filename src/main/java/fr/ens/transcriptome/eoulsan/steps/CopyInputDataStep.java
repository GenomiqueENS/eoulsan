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

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.NoLog;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepConfigurationContext;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFiles;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * Copy input files of a format in another location or in different compression
 * format.
 * @author Laurent Jourdren
 * @since 2.0
 */
@HadoopCompatible
@ReuseStepInstance
@NoLog
public class CopyInputDataStep extends AbstractStep {

  public static final String STEP_NAME = "_copyinputformat";
  public static final String FORMAT_PARAMETER = "format";
  public static final String OUTPUT_COMPRESSION_PARAMETER = "compression";

  private DataFormat format;
  private CompressionType outputCompression;

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    return singleInputPort(this.format);
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort("output", this.format,
        this.outputCompression).create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {
    for (Parameter p : stepParameters) {

      if (FORMAT_PARAMETER.equals(p.getName())) {
        this.format =
            DataFormatRegistry.getInstance()
                .getDataFormatFromName(p.getValue());
      } else if (OUTPUT_COMPRESSION_PARAMETER.equals(p.getName())) {
        this.outputCompression = CompressionType.valueOf(p.getValue());
      }
    }

    if (this.format == null) {
      throw new EoulsanException("No format set.");
    }

    if (this.outputCompression == null) {
      throw new EoulsanException("No output compression set.");
    }
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    try {

      final Data inData = context.getInputData(DEFAULT_SINGLE_INPUT_PORT_NAME);
      final Data outData = context.getOutputData("output", inData);

      copyData(inData, outData);
      status.setProgress(1.0);

    } catch (IOException e) {
      return status.createStepResult(e);
    }
    return status.createStepResult();
  }

  //
  // Other methods
  //

  /**
   * Check input and output files.
   * @param inFile input file
   * @param outFile output file
   * @throws IOException if copy cannot be started
   */
  private static void checkFiles(final DataFile inFile, final DataFile outFile)
      throws IOException {

    if (inFile.equals(outFile)) {
      throw new FileNotFoundException("cannot copy file on itself: " + inFile);
    }

    if (!inFile.exists()) {
      throw new FileNotFoundException("input file not found: " + inFile);
    }

    if (outFile.exists()) {
      throw new FileNotFoundException("output file already exists: " + outFile);
    }
  }

  /**
   * Copy files for a format and a samples.
   * @param inData input data
   * @param outData output data
   * @throws IOException if an error occurs while copying
   */
  private void copyData(final Data inData, final Data outData)
      throws IOException {

    final int count = inData.getDataFileCount();

    // Handle standard case
    if (inData.getFormat().getMaxFilesCount() == 1) {

      final DataFile in = inData.getDataFile();
      final DataFile out = outData.getDataFile();

      // Check input and output files
      checkFiles(in, out);

      // Copy file
      DataFiles.symlinkOrCopy(in, out);
    } else {

      // Handle multi file format like FASTQ files

      for (int i = 0; i < count; i++) {

        final DataFile in = inData.getDataFile(i);
        final DataFile out = outData.getDataFile(i);

        // Check input and output files
        checkFiles(in, out);

        // Copy file
        DataFiles.symlinkOrCopy(in, out);
      }
    }
  }

}
