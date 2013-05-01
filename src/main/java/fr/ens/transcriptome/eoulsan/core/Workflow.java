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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
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
import fr.ens.transcriptome.eoulsan.data.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.FirstStep;
import fr.ens.transcriptome.eoulsan.steps.Step;
import fr.ens.transcriptome.eoulsan.util.StringUtils;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class manage the workflow.
 * @since 1.0
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

  private final Map<Integer, Set<DataFormat>> globalInputDataFormats = Maps
      .newHashMap();
  private final Map<Integer, Set<DataFormat>> globalOutputDataFormats = Maps
      .newHashMap();

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

    return StepService.getInstance(this.hadoopMode).newService(stepName);
  }

  //
  // Add additional steps
  //

  /**
   * Add some steps at the start of the Workflow.
   * @param firstSteps list of steps to add
   */
  public void addFirstSteps(final List<Step> firstSteps) {

    if (firstSteps != null)
      this.steps.addAll(0, Utils.listWithoutNull(firstSteps));

    // Add the first step. Generators cannot be added after this step
    this.steps.add(0, new FirstStep());
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

  private static final class Cart {

    final Set<DataFormat> cart = newHashSet();
    final Set<DataFormat> cartUsed = newHashSet();
    final Set<DataFormat> cartReUsed = newHashSet();
    final Set<DataFormat> cartGenerated = newHashSet();
    final Set<DataFormat> cartNotGenerated = newHashSet();
    final Set<DataFormat> cartOnlyGenerated = newHashSet();
  }

  private void scanWorkflow() throws EoulsanException {

    final Set<DataFile> checkedDatafile = newHashSet();
    final Map<DataFormat, Checker> checkers = newHashMap();

    boolean firstSample = true;

    for (Sample s : this.design.getSamples()) {

      final Cart cart = new Cart();

      // Fill cart with DataFormats provided by the sample in the design file
      fillCartWithDesignFiles(cart, s);

      for (Step step : this.steps) {

        // Register DataFormats
        if (firstSample)
          checkStepInOutFormat(step);

        // Check Input
        final Map<DataType, Set<DataFormat>> map =
            getDataFormatByDataType(step.getInputFormats());

        if (map != null && map.size() > 0)
          for (Map.Entry<DataType, Set<DataFormat>> e : map.entrySet()) {

            int foundInCart = 0;
            int foundFile = 0;
            boolean canBeGenerated = false;

            for (DataFormat df : e.getValue()) {

              if (df.isGenerator())
                canBeGenerated = true;

              if (cart.cart.contains(df)) {
                cart.cartUsed.add(df);
                if (df.isChecker())
                  checkers.put(df, df.getChecker());

                if (cart.cartGenerated.contains(df))
                  cart.cartReUsed.add(df);

                foundInCart++;
              } else
                foundFile = swCheckExistingFiles(df, s, cart, foundFile);

            }

            if (foundInCart == 0 && foundFile == 0) {

              if (canBeGenerated) {
                cart.cartUsed.add(e.getValue().iterator().next());
              } else
                throw new EoulsanException("For sample "
                    + s.getId() + " in step " + step.getName()
                    + ", no input data of " + e.getKey()
                    + " type found in the workflow.");
            }

            if (foundFile > 1 || foundInCart > 1)
              throw new EoulsanException("For sample "
                  + s.getId() + " in step " + step.getName()
                  + ", more than one format of the same data (" + e.getKey()
                  + ") found in the workflow.");
          }

        // Check Output
        if (step.getOutputFormats() != null)
          for (DataFormat df : step.getOutputFormats()) {
            cart.cartGenerated.add(df);
            cart.cart.add(df);
          }

      }

      // Check Input data
      cart.cartNotGenerated.addAll(cart.cartUsed);
      cart.cartNotGenerated.removeAll(cart.cartGenerated);

      for (DataFormat df : cart.cartNotGenerated) {

        // Get DataFile

        final List<DataFile> files = swGetAllDataFiles(df, s);

        for (DataFile file : files)
          if (!checkedDatafile.contains(file)) {
            if (!file.exists()) {

              if (df.isGenerator()) {

                final Step generator = df.getGenerator();

                this.steps.add(swfindGeneratorInsertionPosition(df), generator);
                LOGGER.info("Add generator step: " + generator.getName());
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
      for (DataFormat df : cart.cartGenerated) {

        for (DataFile file : swGetAllDataFiles(df, s))
          if (!checkedDatafile.contains(file)) {
            if (file.exists())
              throw new EoulsanException("For sample "
                  + s.getId() + ", generated \"" + df.getFormatName()
                  + "\" already exists (" + file + ").");
            checkedDatafile.add(file);
          }
      }

      cart.cartOnlyGenerated.addAll(cart.cartGenerated);
      cart.cartOnlyGenerated.removeAll(cart.cartReUsed);

      globalInputDataFormats.put(s.getId(),
          Collections.unmodifiableSet(cart.cartNotGenerated));
      globalOutputDataFormats.put(s.getId(),
          Collections.unmodifiableSet(cart.cartGenerated));

      if (firstSample)
        firstSample = false;
    }

    // Run checkers
    runChecker(checkers);
  }

  private DataFile swGetFirstDataFile(final DataFormat format,
      final Sample sample) {

    if (format.getMaxFilesCount() > 1)
      return this.context.getOtherDataFile(format, sample, 0);

    return this.context.getOtherDataFile(format, sample);
  }

  private List<DataFile> swGetAllDataFiles(final DataFormat format,
      final Sample sample) {

    if (format.getMaxFilesCount() == 1)
      return singletonList(this.context.getOtherDataFile(format, sample));

    final List<DataFile> result = newArrayList();

    final int count = this.context.getDataFileCount(format, sample);
    for (int i = 0; i < count; i++)
      result.add(this.context.getOtherDataFile(format, sample, i));

    return result;
  }

  private int swCheckExistingFiles(final DataFormat df, final Sample sample,
      final Cart cart, final int foundFile) {

    int result = foundFile;

    final DataFile file = swGetFirstDataFile(df, sample);

    if (file.exists()) {
      cart.cart.add(df);
      cart.cartNotGenerated.add(df);
      cart.cartUsed.add(df);
      result++;
    }

    return result;
  }

  private int swfindGeneratorInsertionPosition(final DataFormat df) {

    if (df == null)
      return -1;

    for (int i = 0; i < this.steps.size(); i++) {

      final Step step = this.steps.get(i);

      // Generator must be added before a terminal step
      if (step.isTerminalStep() || step instanceof FirstStep)
        return i;

      final DataFormat[] dfs = step.getInputFormats();
      if (dfs == null)
        continue;

      if (Arrays.asList(dfs).contains(df))
        return i;
    }

    return -1;
  }

  private void runChecker(final Map<DataFormat, Checker> checkers)
      throws EoulsanException {

    final CheckStore checkStore = CheckStore.getCheckStore();

    for (Step step : this.steps) {

      if (step.getInputFormats() != null)
        for (DataFormat df : step.getInputFormats()) {

          if (checkers.containsKey(df)) {

            Checker c = checkers.get(df);

            c.configure(this.command.getStepParameters(step.getName()));
            LOGGER.info("Start checking "
                + df.getFormatName() + " for " + step.getName() + " step.");

            final long startTime = System.currentTimeMillis();
            c.check(this.design, this.context, checkStore);
            final long endTime = System.currentTimeMillis();

            LOGGER.info("End of checking of "
                + df.getFormatName() + " in "
                + StringUtils.toTimeHumanReadable(endTime - startTime) + ".");
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

  private void fillCartWithDesignFiles(final Cart cart, final Sample s) {

    final List<String> fieldnames = s.getMetadata().getFields();
    DataFormatRegistry registry = DataFormatRegistry.getInstance();

    for (String fieldname : fieldnames) {

      DataType dt = registry.getDataTypeForDesignField(fieldname);

      if (dt != null) {

        final List<String> fieldValues =
            s.getMetadata().getFieldAsList(fieldname);

        if (fieldValues.size() > 0) {

          DataFile file = new DataFile(fieldValues.get(0));
          final DataFormat df = file.getDataFormat(dt);
          if (df != null)
            cart.cart.add(df);
        }
      }
    }

  }

  /**
   * Check all the steps, inputs and outputs of the workflow.
   * @throws EoulsanException if error while checking the workflow
   */
  public void check() throws EoulsanException {

    // Scan Workflow
    scanWorkflow();
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
   * Log the global parameter with obfuscated values if needed.
   * @param globalParameters globalParameters
   */
  private void logGlobalsParameters(Set<Parameter> globalParameters) {

    final Set<String> keysToObfuscate =
        context.getSettings().getSettingsKeyToObfuscated();

    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    for (Parameter p : globalParameters) {

      if (sb.length() > 1)
        sb.append(", ");

      sb.append(p.getName());
      sb.append('=');

      if (keysToObfuscate.contains(p.getValue()))
        sb.append("xxxx value not shown xxxx");
      else
        sb.append(p.getStringValue());
    }

    sb.append(']');

    LOGGER.info("Init all steps with global parameters: " + sb.toString());
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
    logGlobalsParameters(globalParameters);
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

    // Convert s3:// urls to s3n:// urls
    convertDesignS3URLs();

    // Create the basic steps
    createSteps();
  }

}
