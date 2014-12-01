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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.singleInputPort;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.NONE;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByContentEncoding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.annotations.ReuseStepInstance;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.splitermergers.Splitter;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define a generic splitter step
 * @author Laurent Jourdren
 * @since 2.0
 */
@HadoopCompatible
@ReuseStepInstance
public class SplitterStep extends AbstractStep {

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
    private final List<Data> list = new ArrayList<>();

    private Data getData(final int index) {

      checkState(index <= this.list.size(), "invalid index: "
          + index + " (maximum expected index: " + this.list.size() + ")");

      checkArgument(index >= 0, "index argument cannot be lower than 0");

      if (index == this.list.size()) {
        this.list.add(this.data.addDataToList(this.data.getName(), index));
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

        int count = 0;

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

          final Data d = getData(this.count++);

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
    public SplitterIterator(final Data data) {

      this.data = data;
    }

  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return "splitter";
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

    return new OutputPortsBuilder().addPort("output", true,
        this.splitter.getFormat(), this.compression).create();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    final Set<Parameter> splitterParameters = new HashSet<>();

    for (Parameter p : stepParameters) {

      if ("format".equals(p.getName())) {

        // Get format
        final DataFormat format =
            DataFormatRegistry.getInstance()
                .getDataFormatFromName(p.getValue());

        // Check if the format exists
        if (format == null) {
          throw new EoulsanException("Unknown format: " + p.getValue());
        }

        // Check if a splitter exists for the format
        if (!format.isSplitter()) {
          throw new EoulsanException("No splitter exists for format: "
              + format.getName());
        }

        // Set the splitter
        this.splitter = format.getSplitter();

      } else if ("compression".equals(p.getName())) {

        this.compression = getCompressionTypeByContentEncoding(p.getValue());
      } else {
        splitterParameters.add(p);
      }
    }

    // Check if a format has been set
    if (this.splitter == null) {
      throw new EoulsanException("No format set for splitter");
    }

    // Configure the splitter
    this.splitter.configure(splitterParameters);
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    final DataFormat format = this.splitter.getFormat();

    // Get input and output data
    final Data inData = context.getInputData(format);
    final Data outData = context.getOutputData(format, inData);

    try {

      if (inData.getPart() != -1) {
        throw new EoulsanException("Cannot split already split data");
      }

      // If Mono-file format
      if (format.getMaxFilesCount() == 1) {

        // Launch splitting
        this.splitter.split(inData.getDataFile(),
            new SplitterIterator(outData).getIterator());

      } else {

        // For each file of the multi-file format
        for (int fileIndex = 0; fileIndex < inData.getDataFileCount(); fileIndex++) {

          // Launch splitting
          this.splitter.split(inData.getDataFile(fileIndex),
              new SplitterIterator(outData).getIterator(fileIndex));
        }
      }

      // Successful result
      return status.createStepResult();
    } catch (IOException e) {

      // Fail of the step
      return status.createStepResult(e);
    } catch (EoulsanException e) {

      // Fail of the step
      return status.createStepResult(e);
    }
  }
}
