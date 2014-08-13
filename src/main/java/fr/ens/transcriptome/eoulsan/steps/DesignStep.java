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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.OutputPort;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.core.workflow.DataUtils;
import fr.ens.transcriptome.eoulsan.data.Data;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.io.CompressionType;

/**
 * This class define a design step.
 * @since 2.0
 * @author Laurent Jourdren
 */
public class DesignStep extends AbstractStep {

  private final Design design;
  private final CheckerStep checkerStep;
  private OutputPorts outputPorts;

  @Override
  public String getName() {
    return "design";
  }

  @Override
  public boolean isCreateLogFiles() {
    return false;
  }

  @Override
  public OutputPorts getOutputPorts() {

    return this.outputPorts;
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    final Set<String> fieldNames =
        Sets.newHashSet(this.design.getMetadataFieldsNames());
    final OutputPortsBuilder builder = new OutputPortsBuilder();

    for (DataFormat format : DataFormatRegistry.getInstance().getAllFormats()) {

      if (fieldNames.contains(format.getDesignFieldName())) {

        builder.addPort(format.getDesignFieldName(),
            !format.isOneFilePerAnalysis(), format, CompressionType.NONE);
      }
    }

    // Create the output ports
    this.outputPorts = builder.create();

    // Configure Checker input ports
    this.checkerStep.configureInputPorts(this.outputPorts);
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    final Set<DataFile> files = Sets.newHashSet();
    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (Sample sample : this.design.getSamples()) {

      for (OutputPort port : getOutputPorts()) {

        final boolean oneFilePerDesign =
            port.getFormat().isOneFilePerAnalysis();

        // Create DataFile object(s)
        List<DataFile> dataFiles = getDesignDatafilesPort(sample, port);

        // Check if file has not been already processed
        DataFile f = dataFiles.get(0);
        if (files.contains(f)) {
          continue;
        }
        files.add(f);

        // Get the data object
        final Data dataList =
            context.getOutputData(port.getName(), port.getName());
        final Data data;

        // Set metadata
        if (port.isList()) {

          data = dataList.addDataToList(sample.getName());

          final Map<String, String> dataMetadata = data.getMetadata();
          for (String fieldName : this.design.getMetadataFieldsNames()) {

            if (!getOutputPorts().contains(fieldName)) {
              dataMetadata.put(fieldName,
                  sample.getMetadata().getField(fieldName));
            }
          }
        } else {
          data = dataList;
        }

        // Set the DataFile(s) in the Data object
        if (port.getFormat().getMaxFilesCount() == 1) {
          // Mono file data
          DataUtils.setDataFile(data, f);
        } else {
          // Multi-file data
          for (int i = 0; i < dataFiles.size(); i++) {
            DataUtils.setDataFile(data, i, dataFiles.get(i));
          }
        }
      }

    }

    return status.createStepResult();
  }

  /**
   * Create a list of data files from a sample and a port
   * @param sample the sample
   * @param port the port
   * @return a list with the data files
   */
  private List<DataFile> getDesignDatafilesPort(final Sample sample,
      final OutputPort port) {

    checkNotNull(sample, "sample argument cannot be null");
    checkNotNull(port, "port argument cannot be null");

    final List<DataFile> result = Lists.newArrayList();

    // Get the design field name for the port
    String fieldName = null;
    for (String f : sample.getMetadata().getFields()) {
      if (port.getName().equals(f.trim().toLowerCase())) {
        fieldName = f;
        break;
      }
    }

    // Get the values in the design for the sample
    final List<String> fieldValues =
        sample.getMetadata().getFieldAsList(fieldName);

    for (String value : fieldValues) {
      result.add(new DataFile(value));
    }

    return result;
  }

  //
  // Constructor
  //

  /**
   * Constructor.
   * @param design design
   * @param checkerStep the checker step instance
   */
  public DesignStep(final Design design, final CheckerStep checkerStep) {

    checkNotNull(design, "design argument cannot be null");
    checkNotNull(checkerStep, "checkerStep argument cannot be null");

    this.design = design;
    this.checkerStep = checkerStep;
  }

}
