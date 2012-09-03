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
 * of the Institut de Biologie de l'√âcole Normale Sup√©rieure and
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

package fr.ens.transcriptome.eoulsan.core.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.core.Command;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepType;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class NewWorkflow {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private static final Set<Parameter> EMPTY_PARAMETERS = Collections.emptySet();

  private List<WorkflowStep> steps;

  private final Command command;
  private final Design design;
  private final Context context;
  private final boolean hadoopMode;

  private final Set<DataFormat> generatorAdded = Sets.newHashSet();

  //
  // Add steps
  //

  /**
   * Create the list of steps
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void addMainSteps() throws EoulsanException {

    this.steps = new ArrayList<WorkflowStep>();
    final Command c = this.command;

    for (String stepId : c.getStepIds()) {

      final String stepName = c.getStepName(stepId);
      final Set<Parameter> stepParameters = c.getStepParameters(stepId);
      final boolean skip = c.isStepSkipped(stepId);

      LOGGER.info("Create "
          + (skip ? "skipped step" : "step ") + stepId + " (" + stepName
          + ") step.");
      this.steps.add(new WorkflowStep(design, context, stepId, stepName,
          stepParameters, skip));
    }
  }

  /**
   * Add some steps at the start of the Workflow.
   * @param firstSteps list of steps to add
   * @throws EoulsanException if an error occurs while adding a step
   */
  private void addFirstSteps(final List<Step> firstSteps)
      throws EoulsanException {

    if (firstSteps != null)
      for (Step step : Utils.listWithoutNull(firstSteps))
        this.steps.add(
            0,
            new WorkflowStep(this.design, this.context, step.getName(), step
                .getName(), EMPTY_PARAMETERS, false));

    // Add the first step. Generators cannot be added after this step
    this.steps.add(0, new WorkflowStep(design, context, StepType.FIRST_STEP));

    // Add the design step
    this.steps.add(0, new WorkflowStep(design, context, StepType.DESIGN_STEP));
  }

  /**
   * Add some steps at the end of the Workflow.
   * @param endSteps list of steps to add
   * @throws EoulsanException if an error occurs while adding a step
   */
  private void addEndSteps(final List<Step> endSteps) throws EoulsanException {

    if (endSteps == null)
      return;

    for (Step step : Utils.listWithoutNull(endSteps))
      this.steps.add(new WorkflowStep(this.design, this.context,
          step.getName(), step.getName(), EMPTY_PARAMETERS, false));
  }

  /**
   * Initialize the steps of the Workflow
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void init() throws EoulsanException {

    final Command c = this.command;
    final Set<Parameter> globalParameters = c.getGlobalParameters();

    final Settings settings = EoulsanRuntime.getSettings();

    // Add globals parameters to Settings
    LOGGER.info("Init all steps with global parameters: " + globalParameters);
    for (Parameter p : globalParameters)
      settings.setSetting(p.getName(), p.getStringValue());

    // Configure all the steps
    for (WorkflowStep step : this.steps)
      step.configure();
  }

  private Set<DataType> getDesignDataTypes() {

    final Set<DataType> result = Sets.newHashSet();
    final List<String> fields = this.design.getMetadataFieldsNames();

    final DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String fieldName : fields) {
      DataType dt = registry.getDataTypeForDesignField(fieldName);

      if (dt != null)
        result.add(dt);
    }

    return result;
  }

  private void searchInputDataFormat() throws EoulsanException {

    final Set<DataType> dataTypesFromDesign = getDesignDataTypes();

    for (int i = this.steps.size() - 1; i >= 0; i--) {

      final WorkflowStep step = this.steps.get(i);

      ListMultimap<DataType, DataFormat> lmm =
          step.getInputDataFormatByDataTypes();

      for (DataType dt : lmm.keySet()) {

        boolean found = false;
        List<DataFormat> generatorAvaillables = Lists.newArrayList();

        for (DataFormat df : lmm.get(dt)) {

          if (!found)
            for (int j = i - 1; j >= 0; j--) {

              final WorkflowStep stepTested = this.steps.get(j);

              // The tested step is a standard/generator step
              if ((stepTested.getType() == StepType.STANDARD_STEP || stepTested
                  .getType() == StepType.GENERATOR_STEP)
                  && stepTested.getOutputDataFormats().contains(df)) {

                step.addInputFormatLocation(df, stepTested);
                found = true;
                break;
              }

              // The tested step is the design step
              if (stepTested.getType() == StepType.DESIGN_STEP
                  && dataTypesFromDesign.contains(dt)) {

                for (DataFormat df2 : lmm.get(dt))
                  step.addInputFormatLocation(df2, stepTested);
                found = true;
                break;
              }

            }

          // A generator is available for the DataType
          if (df.isGenerator() && !this.generatorAdded.contains(df))
            generatorAvaillables.add(df);

        }

        if (!found) {

          // Add generator if needed
          if (!generatorAvaillables.isEmpty()) {

            this.steps.add(1, new WorkflowStep(this.design, this.context,
                generatorAvaillables.get(0)));
            searchInputDataFormat();
            return;
          }

          else
            throw new EoulsanException("Cannot found \""
                + dt.getName() + "\" for step " + step.getId() + ".");
        }
      }
    }

  }

  //
  // Convert S3 URL methods
  //

  /**
   * Convert the S3 URLs to S3N URLs in source, genome and annotation fields of
   * the design.
   */
  private void convertDesignS3URLs() {

    for (Sample s : this.design.getSamples()) {

      // Convert read file URL
      final List<String> readsSources =
          Lists.newArrayList(s.getMetadata().getReads());
      for (int i = 0; i < readsSources.size(); i++)
        readsSources.set(i, convertS3URL(readsSources.get(i)));
      s.getMetadata().setReads(readsSources);

      // Convert genome file URL
      if (s.getMetadata().isGenomeField())
        s.getMetadata().setGenome(convertS3URL(s.getMetadata().getGenome()));

      // Convert annotation file URL
      if (s.getMetadata().isAnnotationField())
        s.getMetadata().setAnnotation(
            convertS3URL(s.getMetadata().getAnnotation()));
    }
  }

  /**
   * Convert a s3:// URL to a s3n:// URL
   * @param url input URL
   * @return converted URL
   */
  private String convertS3URL(final String url) {

    return StringUtils.replacePrefix(url, "s3:/", "s3n:/");
  }

  //
  // Constructor
  //

  public NewWorkflow(final Command command, final List<Step> firstSteps,
      final List<Step> endSteps, final Design design, final Context context,
      final boolean hadoopMode) throws EoulsanException {

    if (command == null)
      throw new NullPointerException("The command is null.");

    if (design == null)
      throw new NullPointerException("The design is null.");

    if (context == null)
      throw new NullPointerException("The execution context is null.");

    this.command = command;
    this.design = design;
    this.context = context;
    this.hadoopMode = hadoopMode;

    // Convert s3:// urls to s3n:// urls
    convertDesignS3URLs();

    // Create the basic steps
    addMainSteps();

    // Add first steps
    addFirstSteps(firstSteps);

    // Add end steps
    addEndSteps(endSteps);

    // initialize steps
    init();

    // TODO Set manually defined input format source

    // Search others input format sources
    searchInputDataFormat();

    // TODO check if output files does not exists
    // TODO check if input files exists
  }

}
