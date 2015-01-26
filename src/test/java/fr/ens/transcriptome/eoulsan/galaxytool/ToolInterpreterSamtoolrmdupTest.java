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
package fr.ens.transcriptome.eoulsan.galaxytool;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.GalaxyToolStep;
import fr.ens.transcriptome.eoulsan.util.FileUtils;

public class ToolInterpreterSamtoolrmdupTest {

  private final File dir = new File(new File(".").getAbsolutePath(),
      "src/main/java/files/toolshed/");

  private final File dirSample = new File(new File(".").getAbsolutePath()
      + "/src/test/java/files");

  private final File resultFile = new File(
      "/tmp/GALAXY_TOOL_mapper_results_2_OUT.bam");

  private final File toolFile = new File(this.dir,
      "samtools_rmdup/1.0.1/samtools_rmdup.xml");

  private GalaxyToolStep galaxyToolStep;

  // Create input stream
  private final boolean isPE = true;

  private Map<DataFormat, DataFile> inputData;
  private Map<DataFormat, DataFile> outputData;

  private StepStatus status;

  @Before
  public void setUp() {

    // Remove result file
    this.resultFile.delete();

    this.inputData = getInputData();
    this.outputData = getOutputData();

    this.status = this.createStepStatus();

    InputStream is;
    try {
      is = FileUtils.createInputStream(this.toolFile);
      this.galaxyToolStep = new GalaxyToolStep(is);

    } catch (FileNotFoundException | EoulsanException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @Test
  public void executionTestPE() {

    // Call GalaxyToolStep
    this.galaxyToolStep.configure(null, getStepParameters(this.isPE));

    final int exitValue =
        this.galaxyToolStep.execute(this.inputData, this.outputData,
            this.status);

    assertTrue("Exit value? ", exitValue == 0);
    assertTrue("Result file exist ? ", this.resultFile.exists());
    assertTrue("Result file not empty ? ", this.resultFile.length() > 0);

  }

  @Test
  public void executionTestSE() {

    // Call GalaxyToolStep
    this.galaxyToolStep.configure(null, getStepParameters(!this.isPE));

    final int exitValue =
        this.galaxyToolStep.execute(this.inputData, this.outputData,
            this.status);

    assertTrue("Exit value? ", exitValue == 0);
    assertTrue("Result file exist ? ", this.resultFile.exists());
    assertTrue("Result file not empty ? ", this.resultFile.length() > 0);

  }

  /**
   * Gets the step parameters.
   * @param isPE the is PE
   * @return the step parameters
   */
  private Set<Parameter> getStepParameters(final boolean isPE) {

    final Set<Parameter> stepParameters = new HashSet<>();
    final String nameSpace = "bam_paired_end_type.";

    final String selectValue = isPE ? "PE" : "SE";

    // Condition if
    stepParameters.add(new Parameter(
        nameSpace + "bam_paired_end_type_selector", selectValue));

    return stepParameters;
  }

  /**
   * Gets the input data.
   * @return the input data
   */
  private final Map<DataFormat, DataFile> getInputData() {
    final Map<DataFormat, DataFile> inputData = new HashMap<>();

    inputData.put(DataFormats.MAPPER_RESULTS_BAM, new DataFile(new File(
        this.dirSample, "mapper_results_2.bam")));

    return inputData;
  }

  /**
   * Gets the output data.
   * @return the output data
   */
  private final Map<DataFormat, DataFile> getOutputData() {
    final Map<DataFormat, DataFile> inputData = new HashMap<>();

    final File resultFile =
        new File("/tmp/GALAXY_TOOL_mapper_results_2_OUT.bam");
    inputData.put(DataFormats.MAPPER_RESULTS_BAM, new DataFile(resultFile));

    return inputData;
  }

  private StepStatus createStepStatus() {

    final StepStatus stepStatus = null;

    return stepStatus;
  }

}
