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

package fr.ens.transcriptome.eoulsan.steps.mgmt;

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import com.google.common.base.Preconditions;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * Copy input files of a format in another location or in different compression
 * format.
 * @author Laurent Jourdren
 * @since 1.3
 */
@HadoopCompatible
public class CopyInputFormatStep extends AbstractStep {

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
  public boolean isCreateLogFiles() {

    return false;
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
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {
    for (Parameter p : stepParameters) {

      if (FORMAT_PARAMETER.equals(p.getName()))
        this.format =
            DataFormatRegistry.getInstance()
                .getDataFormatFromName(p.getValue());
      else if (OUTPUT_COMPRESSION_PARAMETER.equals(p.getName()))
        this.outputCompression = CompressionType.valueOf(p.getValue());
    }

    if (this.format == null)
      throw new EoulsanException("No format set.");

    if (this.outputCompression == null)
      throw new EoulsanException("No output compression set.");
  }

  @Override
  public StepResult execute(final Design design, final StepContext context,
      final StepStatus status) {

    try {

      // Test if there is only one file per analysis for the format
      if (this.format.isOneFilePerAnalysis()) {
        copyFormat(context, format, design.getSamples().get(0));
      } else {

        // Copy files for each sample
        for (Sample sample : design.getSamples()) {
          copyFormat(context, format, sample);
          status.setSampleProgress(sample, 1.0);
        }
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
   * @param format the format
   * @param sample the sample
   * @throws IOException if an error occurs while copying
   */
  private void copyFormat(final StepContext context, final DataFormat format,
      final Sample sample) throws IOException {

    // Handle standard case
    if (format.getMaxFilesCount() == 1) {

      final DataFile in = context.getInputDataFile(format, sample);
      if (!in.exists())
        throw new FileNotFoundException("input file not found: " + in);

      copyDataFile(in, context.getOutputDataFile(format, sample));
    } else {

      // Handle multi file format like fastq
      final int count = context.getInputDataFileCount(format, sample);
      for (int i = 0; i < count; i++) {

        final DataFile in = context.getInputDataFile(format, sample, i);
        if (!in.exists())
          throw new FileNotFoundException("input file not found: " + in);

        copyDataFile(in, context.getOutputDataFile(format, sample, i));
      }
    }
  }

  /**
   * Copy a file.
   * @param in input file
   * @param out ouput file
   * @throws IOException if an error occurs while copying
   */
  private static void copyDataFile(final DataFile in, final DataFile out)
      throws IOException {

    Preconditions.checkNotNull(in, "input file is null");
    Preconditions.checkNotNull(out, "output file is null");

    final CompressionType inType =
        CompressionType.getCompressionTypeByFilename(in.getName());
    final CompressionType outType =
        CompressionType.getCompressionTypeByFilename(out.getName());

    if (inType == outType) {

      FileUtils.copy(in.rawOpen(), out.rawCreate());
      return;
    }

    FileUtils.copy(in.open(), out.create());
  }

}
