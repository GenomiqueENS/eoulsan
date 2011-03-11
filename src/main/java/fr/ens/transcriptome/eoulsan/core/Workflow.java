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

package fr.ens.transcriptome.eoulsan.core;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.Settings;
import fr.ens.transcriptome.eoulsan.checkers.CheckStore;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.data.DataFormats;
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class manage the workflow.
 * @author Laurent Jourdren
 */
class Workflow implements WorkflowDescription {

  /** Logger */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

  private List<Step> steps;

  private final Command command;
  private final Design design;
  private final Context context;
  private final boolean hadoopMode;

  private final Map<Integer, Set<DataFormat>> globalInputDataFormats =
      Maps.newHashMap();
  private final Map<Integer, Set<DataFormat>> globalOutputDataFormats =
      Maps.newHashMap();

  //
  // Getters
  //

  @Override
  public List<Step> getSteps() {

    return Collections.unmodifiableList(this.steps);
  }

  @Override
  public Set<DataFormat> getGlobalInputDataFormat(final Sample sample) {

    if (sample == null)
      throw new NullPointerException("Sample is null");

    return this.globalInputDataFormats.get(sample.getId());
  }

  @Override
  public Set<DataFormat> getGlobalOutputDataFormat(final Sample sample) {

    if (sample == null)
      throw new NullPointerException("Sample is null");

    return this.globalOutputDataFormats.get(sample.getId());
  }

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private Step getStep(String stepName) {

    return StepService.getInstance(this.hadoopMode).getStep(stepName);
  }

  //
  // Add additional steps
  //

  /**
   * Add some steps at the start of the Workflow.
   * @param firstSteps list of steps to add
   */
  public void addFirstSteps(final List<Step> firstSteps) {

    if (firstSteps == null)
      return;

    this.steps.addAll(0, Utils.listWithoutNull(firstSteps));
  }

  /**
   * Add some steps at the end of the Workflow.
   * @param endSteps list of steps to add
   */
  public void addEndSteps(final List<Step> endSteps) {

    if (endSteps == null)
      return;

    this.steps.addAll(Utils.listWithoutNull(endSteps));
  }

  //
  // Checks methods
  //

  private void scanWorkflow() throws EoulsanException {

    final Context context = this.context;

    final Set<DataFile> checkedDatafile = newHashSet();
    final Map<DataFormat, Checker> checkers = newHashMap();

    final DataFormatRegistry dfRegistry = DataFormatRegistry.getInstance();
    dfRegistry.register(DataFormats.READS_FASTQ);
    dfRegistry.register(DataFormats.READS_TFQ);
    dfRegistry.register(DataFormats.GENOME_FASTA);
    dfRegistry.register(DataFormats.ANNOTATION_GFF);

    boolean firstSample = true;

    for (Sample s : this.design.getSamples()) {

      final Set<DataFormat> cart = newHashSet();
      final Set<DataFormat> cartUsed = newHashSet();
      final Set<DataFormat> cartReUsed = newHashSet();
      final Set<DataFormat> cartGenerated = newHashSet();
      final Set<DataFormat> cartNotGenerated = newHashSet();
      final Set<DataFormat> cartOnlyGenerated = newHashSet();

      // Add reads to the cart
      cart.add(getReadsDataFormat(s));

      // Add genome to the cart
      final DataFormat genomeFormat = getGenomeDataFormat(s);
      if (genomeFormat != null) {
        cart.add(genomeFormat);
      }

      // Add annotation to the cart
      final DataFormat annotationFormat = getAnnotationDataFormat(s);
      if (annotationFormat != null) {
        cart.add(annotationFormat);
      }

      for (Step step : this.steps) {

        // Register DataFormats
        if (firstSample)
          checkStepInOutFormat(step);

        // Check Input
        final Map<DataType, Set<DataFormat>> map =
            getDataFormatByDataType(step.getInputFormats());

        if (map != null && map.size() > 0)
          for (Map.Entry<DataType, Set<DataFormat>> e : map.entrySet()) {

            int found = 0;
            boolean canBeGenerated = false;

            for (DataFormat df : e.getValue()) {

              if (df.isGenerator())
                canBeGenerated = true;

              if (cart.contains(df)) {
                cartUsed.add(df);
                if (df.isChecker())
                  checkers.put(df, df.getChecker());

                if (cartGenerated.contains(df))
                  cartReUsed.add(df);

                found++;
              } else { // To comment to prevent bug
                if (context.getDataFile(df, s).exists()) {
                  cart.add(df);
                  cartNotGenerated.add(df);
                  cartUsed.add(df);
                  found++;
                }
              }

            }

            if (found == 0) {

              if (canBeGenerated) {
                cartUsed.add(e.getValue().iterator().next());
              } else
                throw new EoulsanException("For sample "
                    + s.getId() + " in step " + step.getName()
                    + ", no input data found in the workflow.");
            }

            if (found > 1)
              throw new EoulsanException(
                  "For sample "
                      + s.getId()
                      + " in step "
                      + step.getName()
                      + ", more than one format of the same data found in the workflow.");
          }

        // Check Output
        if (step.getOutputFormats() != null)
          for (DataFormat df : step.getOutputFormats()) {
            cartGenerated.add(df);
            cart.add(df);
          }

      }

      // Check Input data
      cartNotGenerated.addAll(cartUsed);
      cartNotGenerated.removeAll(cartGenerated);

      for (DataFormat df : cartNotGenerated) {

        final DataFile file = context.getDataFile(df, s);
        if (!checkedDatafile.contains(file)) {
          if (!context.getDataFile(df, s).exists()) {

            if (df.isGenerator()) {

              this.steps.add(0, df.getGenerator());
              scanWorkflow();

              return;
            }

            throw new EoulsanException("For sample "
                + s.getId() + ", input \"" + df.getFormatName()
                + "\" not exists.");
          }

          checkedDatafile.add(file);
        }
      }

      // Check if outputs already exists
      for (DataFormat df : cartGenerated) {

        final DataFile file = context.getDataFile(df, s);
        if (!checkedDatafile.contains(file)) {
          if (context.getDataFile(df, s).exists())
            throw new EoulsanException("For sample "
                + s.getId() + ", generated \"" + df.getFormatName()
                + "\" already exists.");
          checkedDatafile.add(file);
        }
      }

      cartOnlyGenerated.addAll(cartGenerated);
      cartOnlyGenerated.removeAll(cartReUsed);

      globalInputDataFormats.put(s.getId(), Collections
          .unmodifiableSet(cartNotGenerated));
      globalOutputDataFormats.put(s.getId(), Collections
          .unmodifiableSet(cartGenerated));

      if (firstSample)
        firstSample = false;
    }

    // Run checkers
    runChecker(checkers);
  }

  private void runChecker(final Map<DataFormat, Checker> checkers)
      throws EoulsanException {

    final CheckStore checkStore = new CheckStore();

    for (Step step : this.steps) {

      if (step.getInputFormats() != null)
        for (DataFormat df : step.getInputFormats()) {

          if (checkers.containsKey(df)) {

            Checker c = checkers.get(df);

            c.configure(this.command.getStepParameters(step.getName()));
            c.check(this.design, this.context, checkStore);

          }
        }
    }
  }

  private void checkStepInOutFormat(final Step step) throws EoulsanException {

    final Map<DataType, Set<DataFormat>> map =
        getDataFormatByDataType(step.getOutputFormats());

    // No output
    if (map == null)
      return;

    for (Map.Entry<DataType, Set<DataFormat>> e : map.entrySet()) {

      if (e.getValue().size() > 1)
        throw new EoulsanException("In Step \""
            + step.getName() + "\" for DataType \"" + e.getKey().getName()
            + "\" found several DataFormat which is forbidden.");

    }

  }

  private Map<DataType, Set<DataFormat>> getDataFormatByDataType(
      DataFormat[] formats) {

    if (formats == null)
      return null;

    final Map<DataType, Set<DataFormat>> result =
        new HashMap<DataType, Set<DataFormat>>();

    for (DataFormat df : formats) {

      final DataType dt = df.getType();

      if (!result.containsKey(dt))
        result.put(dt, new HashSet<DataFormat>());

      result.get(dt).add(df);

    }

    return result;
  }

  private DataFormat getReadsDataFormat(final Sample s) throws EoulsanException {

    final String readsSource = s.getSource();
    if (readsSource == null || "".equals(readsSource))
      throw new EoulsanException("For sample "
          + s.getId() + ", the reads source is null or empty.");

    DataFormat readsDF;
    try {
      readsDF = new DataFile(s.getSource()).getMetaData().getDataFormat();

      if (readsDF == null)
        throw new EoulsanException("No DataFormat found for reads file: "
            + s.getSource());

      return readsDF;

    } catch (IOException e) {
      throw new EoulsanException("Unable to get reads metadata for: "
          + s.getSource());
    }

  }

  private DataFormat getGenomeDataFormat(final Sample s)
      throws EoulsanException {

    if (!s.getMetadata().isGenomeField())
      return null;

    final String genomeSource = s.getMetadata().getGenome();

    if (genomeSource == null || "".equals(genomeSource))
      throw new EoulsanException("For sample "
          + s.getId() + ", the genome source is null or empty.");

    final DataFile genomeFile = new DataFile(genomeSource);
    if (!genomeFile.exists())
      return null;

    DataFormat genomeFormat;
    try {
      genomeFormat = genomeFile.getMetaData().getDataFormat();

      if (genomeFormat == null)
        throw new EoulsanException("No DataFormat found for genome file: "
            + s.getSource());

      return genomeFormat;

    } catch (IOException e) {

      throw new EoulsanException("Unable to get genome metadata for: "
          + s.getSource());
    }

  }

  private DataFormat getAnnotationDataFormat(final Sample s)
      throws EoulsanException {

    if (!s.getMetadata().isAnnotationField())
      return null;

    final String annotationSource = s.getMetadata().getAnnotation();

    if (annotationSource == null || "".equals(annotationSource))
      throw new EoulsanException("For sample "
          + s.getId() + ", the annotation source is null or empty.");

    final DataFile annotationFile = new DataFile(annotationSource);
    if (!annotationFile.exists()) {
      return null;
    }

    DataFormat annotDF;
    try {
      annotDF = annotationFile.getMetaData().getDataFormat();

      if (annotDF == null)
        throw new EoulsanException("No DataFormat found for annotation file: "
            + s.getSource());

      return annotDF;

    } catch (IOException e) {
      throw new EoulsanException("Unable to get annotation metadata for: "
          + s.getSource());
    }

  }

  /**
   * Check all the steps, inputs and outputs of the workflow.
   * @throws EoulsanException if error while checking the workflow
   */
  public void check() throws EoulsanException {

    // Scan Workflow
    scanWorkflow();

    // Check steps order, and add DataType generator step if need
    // checkIfInputsExists();

    // Check if outputs exits
    // checkIfOutputsExists();

    // Check inputs data
    // checkInputsData();
  }

  //
  // Step creation and initialization methods
  //

  /**
   * Create the list of steps
   * @throws EoulsanException if an error occurs while creating the step
   */
  private void createSteps() throws EoulsanException {

    this.steps = new ArrayList<Step>();
    final Command c = this.command;

    for (String stepName : c.getStepNames()) {
      LOGGER.info("Create " + stepName + " step.");
      this.steps.add(findStep(stepName));
    }
  }

  /**
   * Initialize the steps of the Workflow
   * @throws EoulsanException if an error occurs while creating the step
   */
  public void init() throws EoulsanException {

    final Command c = this.command;
    final DataFormatRegistry dfRegistry = DataFormatRegistry.getInstance();
    final Set<Parameter> globalParameters = c.getGlobalParameters();

    final Settings settings = EoulsanRuntime.getSettings();

    // Add globals parameters to Settings
    LOGGER.info("Init all step with global parameters: " + globalParameters);
    for (Parameter p : globalParameters)
      settings.setSetting(p.getName(), p.getStringValue());

    for (Step step : this.steps) {

      final String stepName = step.getName();
      final Set<Parameter> stepParameters = c.getStepParameters(stepName);

      LOGGER.info("Configure "
          + stepName + " step with step parameters: " + stepParameters);

      step.configure(stepParameters);

      // Register input and output formats
      dfRegistry.register(step.getInputFormats());
      dfRegistry.register(step.getOutputFormats());
    }

  }

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private final Step findStep(final String stepName) throws EoulsanException {

    if (stepName == null)
      throw new EoulsanException("Step name is null");

    final String lower = stepName.trim().toLowerCase();
    final Step result = getStep(lower);

    if (result == null)
      throw new EoulsanException("Unknown step: " + lower);

    return result;
  }

  @Override
  public final Set<Parameter> getStepParameters(final String stepName) {

    return Collections
        .unmodifiableSet(this.command.getStepParameters(stepName));
  }

  //
  // Constructor
  //

  public Workflow(final Command command, final Design design,
      final Context context, final boolean hadoopMode) throws EoulsanException {

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

    // Create the basic steps
    createSteps();
  }

}
