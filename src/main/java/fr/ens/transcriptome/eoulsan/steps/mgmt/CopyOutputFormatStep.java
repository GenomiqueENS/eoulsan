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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * Copy output files of a step with a specified format to the output directory.
 * @author Laurent Jourdren
 * @since 1.3
 */
@HadoopCompatible
public class CopyOutputFormatStep extends AbstractStep {

  public static final String STEP_NAME = "_copyoutputformat";
  public static final String FORMAT_PARAMETER = "format";

  private Set<DataFormat> formats = Sets.newHashSet();

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public Set<DataFormat> getInputFormats() {

    return Collections.unmodifiableSet(this.formats);
  }

  @Override
  public void configure(Set<Parameter> stepParameters) throws EoulsanException {
    for (Parameter p : stepParameters) {

      if (FORMAT_PARAMETER.equals(p.getName())) {

        final DataFormatRegistry registry = DataFormatRegistry.getInstance();

        for (String formatName : Splitter.on(',').split(p.getValue())) {

          final DataFormat format = registry.getDataFormatFromName(formatName);

          if (format != null)
            this.formats.add(format);
        }
      }
    }

    if (this.formats.isEmpty())
      new EoulsanException("No format set.");

  }

  @Override
  public StepResult execute(final Design design, final StepContext context,
      final StepStatus status) {

    try {

      // Copy files for each sample
      for (Sample sample : design.getSamples()) {

        for (DataFormat format : this.formats) {

          // Test if there is only one file per analysis for the format
          if (format.isOneFilePerAnalysis()) {
            copyFormat(context, format, design.getSamples().get(0));
          } else {
            copyFormat(context, format, sample);
          }
        }
        status.setSampleProgress(sample, 1.0);
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

    final DataFile outputDir = new DataFile(context.getStepWorkingPathname());

    // Handle standard case
    if (format.getMaxFilesCount() == 1) {

      final DataFile in = context.getInputDataFile(format, sample);
      if (!in.exists())
        throw new FileNotFoundException("input file not found: " + in);

      // Copy file
      FileUtils.copy(in.rawOpen(),
          new DataFile(outputDir, in.getName()).rawCreate());
    } else {

      // Handle multi file format like fastq
      final int count = context.getInputDataFileCount(format, sample);
      for (int i = 0; i < count; i++) {

        final DataFile in = context.getInputDataFile(format, sample, i);
        if (!in.exists())
          throw new FileNotFoundException("input file not found: " + in);

        // Copy file
        FileUtils.copy(in.rawOpen(),
            new DataFile(outputDir, in.getName()).rawCreate());
      }
    }
  }

}
