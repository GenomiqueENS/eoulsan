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

import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.DEFAULT_SINGLE_INPUT_PORT_NAME;
import static fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.DEFAULT_SINGLE_OUTPUT_PORT_NAME;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.annotations.LocalOnly;
import fr.ens.biologie.genomique.eoulsan.annotations.NoLog;
import fr.ens.biologie.genomique.eoulsan.annotations.NoOutputDirectory;
import fr.ens.biologie.genomique.eoulsan.annotations.ReuseModuleInstance;
import fr.ens.biologie.genomique.eoulsan.core.DataUtils;
import fr.ens.biologie.genomique.eoulsan.core.FileNaming;
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
import fr.ens.biologie.genomique.eoulsan.data.DataFiles;
import fr.ens.biologie.genomique.eoulsan.data.DataFormat;
import fr.ens.biologie.genomique.eoulsan.data.DataFormatRegistry;
import fr.ens.biologie.genomique.eoulsan.data.protocols.DataProtocol;
import fr.ens.biologie.genomique.eoulsan.data.protocols.StorageDataProtocol;
import fr.ens.biologie.genomique.kenetre.io.CompressionType;

/**
 * Copy input files of a format in another location or in different compression
 * format.
 * @author Laurent Jourdren
 * @since 2.0
 */
@LocalOnly
@ReuseModuleInstance
@NoLog
@NoOutputDirectory
public class CopyInputDataModule extends AbstractModule {

  public static final String MODULE_NAME = "_copyinputformat";
  public static final String FORMAT_PARAMETER = "format";
  public static final String OUTPUT_COMPRESSION_PARAMETER =
      "output.compression";
  public static final String OUTPUT_COMPRESSIONS_ALLOWED_PARAMETER =
      "output.compressions.allowed";

  private DataFormat format;
  private CompressionType outputCompression;
  private EnumSet<CompressionType> outputCompressionsAllowed =
      EnumSet.allOf(CompressionType.class);

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

    return singleInputPort(this.format);
  }

  @Override
  public OutputPorts getOutputPorts() {

    return new OutputPortsBuilder().addPort(DEFAULT_SINGLE_OUTPUT_PORT_NAME,
        this.format, this.outputCompression).create();
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      switch (p.getName()) {

      case FORMAT_PARAMETER:
        this.format = DataFormatRegistry.getInstance()
            .getDataFormatFromName(p.getValue());
        break;

      case OUTPUT_COMPRESSION_PARAMETER:
        this.outputCompression = CompressionType.valueOf(p.getValue());
        break;

      case OUTPUT_COMPRESSIONS_ALLOWED_PARAMETER:
        this.outputCompressionsAllowed =
            decodeAllowedCompressionsParameterValue(p.getValue());
        break;

      default:
        Modules.unknownParameter(context, p);
      }

    }

    if (this.format == null) {
      Modules.invalidConfiguration(context, "No format set");
    }

    if (this.outputCompression == null) {
      Modules.invalidConfiguration(context, "No output compression set");
    }

    if (this.outputCompressionsAllowed.isEmpty()) {
      throw new EoulsanException(OUTPUT_COMPRESSIONS_ALLOWED_PARAMETER
          + " parameter value cannot be empty");
    }

  }

  @Override
  public TaskResult execute(final TaskContext context,
      final TaskStatus status) {

    try {

      final Data inData = context.getInputData(DEFAULT_SINGLE_INPUT_PORT_NAME);
      final Data outData =
          context.getOutputData(DEFAULT_SINGLE_OUTPUT_PORT_NAME, inData);

      copyData(inData, outData, context);
      status.setProgress(1.0);

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
   * @param inFile input file
   * @param outFile output file
   * @throws IOException if copy cannot be started
   */
  private static void checkFiles(final DataFile inFile, final DataFile outFile)
      throws IOException {

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
   * Get the real underlying file if the file protocol is a StorageDataProtocol
   * instance.
   * @param file the file
   * @return the underlying file if exists or the file itself
   */
  private DataFile getRealDataFile(final DataFile file) {

    try {

      final DataProtocol protocol = file.getProtocol();

      // Get the underlying file if the file protocol is a storage protocol
      if (protocol instanceof StorageDataProtocol) {

        return ((StorageDataProtocol) protocol).getUnderLyingData(file);
      }

      return file;
    } catch (IOException e) {
      return file;
    }
  }

  /**
   * Copy files for a format and a samples.
   * @param inData input data
   * @param outData output data
   * @param context task context
   * @throws IOException if an error occurs while copying
   */
  private void copyData(final Data inData, final Data outData,
      final TaskContext context) throws IOException {

    if (inData.getFormat().getMaxFilesCount() == 1) {

      //
      // Handle standard case
      //

      // Copy the file
      final DataFile outputFile = copyFile(inData.getDataFile(), -1,
          outData.getName(), outData.getPart(), context);

      // Set the file in the data object
      DataUtils.setDataFile(outData, outputFile);
    } else {

      //
      // Handle multi file format like FASTQ files
      //

      // Get the count of input files
      final int count = inData.getDataFileCount();

      // The list of output files
      final List<DataFile> dataFiles = new ArrayList<>();

      for (int i = 0; i < count; i++) {

        // Copy the file
        final DataFile outputFile = copyFile(inData.getDataFile(i), i,
            outData.getName(), outData.getPart(), context);

        dataFiles.add(outputFile);
      }

      // Set the files in the data object
      DataUtils.setDataFiles(outData, dataFiles);
    }
  }

  /**
   * Copy an input file to its destination.
   * @param inputFile the input file
   * @param fileIndex the output file index
   * @param outDataName the output data name
   * @param outDataPart the output part
   * @param context the step context
   * @return the output file
   * @throws IOException if an error occurs while copying the data
   */
  private DataFile copyFile(final DataFile inputFile, final int fileIndex,
      final String outDataName, final int outDataPart,
      final TaskContext context) throws IOException {

    final String stepId = context.getCurrentStep().getId();
    final DataFile outputDir = context.getStepOutputDirectory();

    // Get the real input file
    final DataFile in = getRealDataFile(inputFile);

    // Define the compression of the output
    final CompressionType compression = getOutputCompressionType(in);

    // Define the output filename
    final String outFilename =
        FileNaming.filename(stepId, DEFAULT_SINGLE_OUTPUT_PORT_NAME,
            this.format, outDataName, fileIndex, outDataPart, compression);

    // Define the output file
    final DataFile out = new DataFile(outputDir, outFilename);

    // Check input and output files
    checkFiles(in, out);

    // Copy file
    DataFiles.symlinkOrCopy(in, out, true);

    return out;
  }

  /**
   * Get the compression type to use for the output file.
   * @param inputFile the input file
   * @return the compression type to use for the output file
   */
  private CompressionType getOutputCompressionType(final DataFile inputFile) {

    final CompressionType inCompression = inputFile.getCompressionType();

    if (this.outputCompressionsAllowed.contains(inCompression)) {
      return inCompression;
    }

    if (this.outputCompressionsAllowed.contains(CompressionType.NONE)) {
      return CompressionType.NONE;
    }

    // Get the first allowed compression
    return this.outputCompressionsAllowed.iterator().next();
  }

  /**
   * Method to encode an EnumSet of the allowed compressions parameter in a
   * string.
   * @param outputCompressionAllowed the EnumSet to encode
   * @return a string with the EnumSet encoded
   */
  public static String encodeAllowedCompressionsParameterValue(
      final EnumSet<CompressionType> outputCompressionAllowed) {

    if (outputCompressionAllowed == null) {
      return null;
    }

    return Joiner.on('\t').join(outputCompressionAllowed);
  }

  /**
   * Method to decode the allowed compressions parameter.
   * @param value the parameter value as a string
   * @return the parameter value as an EnumSet
   * @throws EoulsanException if the value parameter is null
   */
  private static EnumSet<CompressionType> decodeAllowedCompressionsParameterValue(
      final String value) throws EoulsanException {

    if (value == null) {
      throw new EoulsanException(
          OUTPUT_COMPRESSIONS_ALLOWED_PARAMETER + " parameter cannot be null");
    }

    final Set<CompressionType> result = new HashSet<>();

    for (String s : Splitter.on('\t').omitEmptyStrings().trimResults()
        .split(value)) {

      final CompressionType compression = CompressionType.valueOf(s);

      if (compression != null) {
        result.add(compression);
      }
    }

    return EnumSet.copyOf(result);
  }

}
