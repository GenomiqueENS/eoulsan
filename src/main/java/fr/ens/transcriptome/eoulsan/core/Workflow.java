package fr.ens.transcriptome.eoulsan.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.checkers.CheckStore;
import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.datatypes.DataFile;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormat;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormatRegistry;
import fr.ens.transcriptome.eoulsan.datatypes.DataFormats;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.util.Utils;

/**
 * This class manage the workflow.
 * @author Laurent Jourdren
 */
class Workflow implements WorkflowDescription {

  /** Logger */
  private static final Logger logger = Logger.getLogger(Globals.APP_NAME);

  private final StepsRegistery registery = StepsRegistery.getInstance();

  private List<Step> steps;

  private Command command;
  private Design design;
  private ExecutorInfo info;

  private final Map<Sample, Set<DataFormat>> globalInputDataFormats =
      new HashMap<Sample, Set<DataFormat>>();
  private final Map<Sample, Set<DataFormat>> globalOutputDataFormats =
      new HashMap<Sample, Set<DataFormat>>();

  // private Set<DataType> dataTypesNotGenerated = new HashSet<DataType>();
  // private Set<DataType> dataTypesOnlyGenerated = new HashSet<DataType>();
  // private Set<DataType> dataTypesReUsed = new HashSet<DataType>();
  // private Set<DataType> dataTypesUsed = new HashSet<DataType>();
  // private Set<DataType> dataTypesGenerated = new HashSet<DataType>();
  // private List<DataType> dataTypesUsedOrder = new ArrayList<DataType>();
  //
  // private Set<DataFormat> dataFormatsNotGenerated = new
  // HashSet<DataFormat>();
  // private Set<DataFormat> dataFormatsOnlyGenerated = new
  // HashSet<DataFormat>();
  // private Set<DataFormat> dataFormatsReUsed = new HashSet<DataFormat>();
  // private Set<DataFormat> dataFormatsUsed = new HashSet<DataFormat>();
  // private Set<DataFormat> dataFormatsGenerated = new HashSet<DataFormat>();
  // private List<DataFormat> dataFormatsUsedOrder = new
  // ArrayList<DataFormat>();

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

    return this.globalInputDataFormats.get(sample);
  }

  @Override
  public Set<DataFormat> getGlobalOutputDataFormat(final Sample sample) {

    if (sample == null)
      throw new NullPointerException("Sample is null");

    return this.globalOutputDataFormats.get(sample);
  }

  /**
   * Get a Step object from its name.
   * @param stepName name of the step
   * @return a Step object
   * @throws EoulsanException if the step does not exits
   */
  private Step getStep(String stepName) {

    return this.registery.getStep(stepName);
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

  // private void fillDataTypeCategories() throws EoulsanException {
  //
  // dataTypesNotGenerated.clear();
  // dataTypesOnlyGenerated.clear();
  // dataTypesUsed.clear();
  // dataTypesReUsed.clear();
  // dataTypesOnlyGenerated.clear();
  // dataTypesGenerated.clear();
  // dataTypesUsedOrder.clear();
  //
  // dataFormatsNotGenerated.clear();
  // dataFormatsOnlyGenerated.clear();
  // dataFormatsUsed.clear();
  // dataFormatsReUsed.clear();
  // dataFormatsOnlyGenerated.clear();
  // dataFormatsGenerated.clear();
  // dataFormatsUsedOrder.clear();
  //
  // for (Step s : this.steps) {
  //
  // // Input DataType
  // for (DataType dt : Utils.unmodifiableSet(s.getInputTypes())) {
  //
  // this.dataTypesUsed.add(dt);
  // this.dataTypesUsedOrder.add(dt);
  // if (this.dataTypesGenerated.contains(dt))
  // this.dataTypesReUsed.add(dt);
  // }
  //
  // // Input DataFormat
  // for (DataFormat df : Utils.unmodifiableSet(s.getInputFormats())) {
  //
  // this.dataFormatsUsed.add(df);
  // this.dataFormatsUsedOrder.add(df);
  // if (this.dataFormatsGenerated.contains(df))
  // this.dataFormatsReUsed.add(df);
  // }
  //
  // // Output DataType
  // for (DataType dt : Utils.unmodifiableSet(s.getOutputType())) {
  //
  // if (this.dataTypesUsed.contains(dt))
  // throw new EoulsanException("Step "
  // + s.getName() + " generate \"" + dt.getName()
  // + "\" ressource after it is needed.");
  // this.dataTypesGenerated.add(dt);
  // }
  //
  // // Output DataFormat
  // for (DataFormat df : Utils.unmodifiableSet(s.getOutputFormats())) {
  // if (this.dataFormatsUsed.contains(df))
  // throw new EoulsanException("Step "
  // + s.getName() + " generate \"" + df.getFormatName()
  // + "\" ressource after it is needed.");
  // this.dataFormatsGenerated.add(df);
  // }
  // }
  //
  // dataTypesNotGenerated.addAll(dataTypesUsed);
  // dataTypesNotGenerated.removeAll(dataTypesGenerated);
  // dataTypesOnlyGenerated.addAll(dataTypesGenerated);
  // dataTypesOnlyGenerated.removeAll(dataTypesReUsed);
  //
  // dataFormatsNotGenerated.addAll(dataFormatsUsed);
  // dataFormatsNotGenerated.removeAll(dataFormatsGenerated);
  // dataFormatsOnlyGenerated.addAll(dataFormatsGenerated);
  // dataFormatsOnlyGenerated.removeAll(dataFormatsReUsed);
  // }
  //
  // private void checkIfOutputsExists() throws EoulsanException {
  //
  // final Design design = this.design;
  // final ExecutorInfo info = this.info;
  //
  // final Set<DataFormat> formats =
  // new HashSet<DataFormat>(this.dataFormatsGenerated);
  //
  // for (DataFormat df : formats)
  // for (Sample sample : design.getSamples()) {
  //
  // final String dataFilename = info.getDataFilename(df, sample);
  // if (!checkDataFilename(dataFilename))
  // throw new EoulsanException("No data for \""
  // + df + "\" type and sample \"" + sample.getName());
  // }
  //
  // }
  //
  // private void checkIfInputsExists() throws EoulsanException {
  //
  // final Design design = this.design;
  // final ExecutorInfo info = this.info;
  //
  // if (design.getSampleCount() == 0)
  // throw new EoulsanException("No Sample available in design.");
  //
  // int lastStepCount = 0;
  // SampleMetadata sm = design.getSamples().get(0).getMetadata();
  // Set<DataType> types;
  //
  // do {
  //
  // lastStepCount = steps.size();
  // fillDataTypeCategories();
  // types = new HashSet<DataType>(this.dataTypesNotGenerated);
  //
  // // Remove reads DataSource, always provided by design file
  // types.remove(DataTypes.READS);
  //
  // // Remove Genome if provided by design file
  // if (sm.isGenomeField())
  // types.remove(DataTypes.GENOME);
  //
  // // Remove annotation if provided by design file
  // if (sm.isAnnotationField())
  // types.remove(DataTypes.ANNOTATION);
  //
  // if (types.size() > 0)
  //
  // for (DataType dt : types) {
  //
  // final List<String> dataFilenamesNotFound = new ArrayList<String>();
  //
  // // Search for DataFile of DataType for samples of the design
  // for (Sample s : design.getSamples()) {
  //
  // final String dataFilename = info.getDataFilename(dt, s);
  // if (!checkDataFilename(dataFilename))
  // dataFilenamesNotFound.add(dataFilename);
  // }
  //
  // // If all the files of this DataType are present for this samples : OK
  // if (dt.isOneFilePerAnalysis() && dataFilenamesNotFound.size() == 1) {
  // types.remove(dt);
  // continue;
  // }
  //
  // if (!dt.isOneFilePerAnalysis() && dataFilenamesNotFound.size() > 0) {
  //
  // // If all the files of this DataType are present for this samples :
  // // OK
  // if (dataFilenamesNotFound.size() == design.getSampleCount()) {
  // types.remove(dt);
  // continue;
  // }
  // throw new EoulsanException("Source \""
  // + dataFilenamesNotFound.get(0) + " not found.");
  // }
  //
  // // No file is provided of this DataType in the design
  // // May be adding a DataType generator will solve the issue
  //
  // final Step s = dt.getGenerator();
  // if (s != null)
  // steps.add(0, s);
  //
  // }
  //
  // } while (steps.size() > lastStepCount);
  //
  // if (types.size() > 0)
  // throw new EoulsanException("No data for \""
  // + types.iterator().next().toString() + "\" found.");
  //
  // }
  //
  // private boolean checkDataFilename(final String dataFilename) {
  //
  // final DataFile df = new DataFile(dataFilename);
  //
  // return df.exists();
  // }
  //
  // private void checkInputData(final Step step,
  // final Set<DataType> typesToCheck, final CheckStorage checkInfo)
  // throws EoulsanException {
  //
  // final Design design = this.design;
  // final ExecutorInfo info = this.info;
  //
  // final DataType[] inTypes = step.getInputTypes();
  //
  // if (inTypes == null)
  // return;
  //
  // final Command c = this.command;
  //
  // Set<DataType> typesToRemove = new HashSet<DataType>();
  //
  // for (DataType dt : inTypes)
  // if (typesToCheck.contains(dt)) {
  //
  // Checker checker = dt.getChecker();
  // if (c == null)
  // continue;
  //
  // // Configure checker
  // checker.configure(c.getStepParameters(step.getName()), c
  // .getGlobalParameters());
  //
  // // Check
  // checker.check(design, info, checkInfo);
  //
  // typesToRemove.add(dt);
  // }
  //
  // typesToCheck.removeAll(typesToRemove);
  // }
  //
  // private void checkInputsData() throws EoulsanException {
  //
  // final List<Step> steps = this.steps;
  //
  // final Set<DataFormat> formatsToCheck =
  // new HashSet<DataFormat>(this.dataFormatsNotGenerated);
  // final Set<DataFormat> formatsToCheckWithChecker = new
  // HashSet<DataFormat>();
  //
  // for (DataFormat df : formatsToCheck) {
  //
  // final Checker checker = df.getChecker();
  // if (checker != null)
  // formatsToCheckWithChecker.add(df);
  //
  // }
  //
  // final CheckStorage checkInfo = new CheckStorage();
  //
  // for (Step s : steps)
  // checkInputData(s, formatsToCheckWithChecker, checkInfo);
  //
  // }

  private void scanWorkflow() throws EoulsanException {

    final ExecutorInfo info = this.info;

    final Set<DataFile> checkedDatafile = new HashSet<DataFile>();
    final Map<DataFormat, Checker> checkers =
        new HashMap<DataFormat, Checker>();

    final DataFormatRegistry dfRegistry = DataFormatRegistry.getInstance();
    dfRegistry.register(DataFormats.READS_FASTQ);
    dfRegistry.register(DataFormats.READS_TFQ);
    dfRegistry.register(DataFormats.GENOME_FASTA);
    dfRegistry.register(DataFormats.ANNOTATION_GFF);

    boolean firstSample = true;

    for (Sample s : this.design.getSamples()) {

      final Set<DataFormat> cart = new HashSet<DataFormat>();
      final Set<DataFormat> cartUsed = new HashSet<DataFormat>();
      final Set<DataFormat> cartReUsed = new HashSet<DataFormat>();
      final Set<DataFormat> cartGenerated = new HashSet<DataFormat>();
      final Set<DataFormat> cartNotGenerated = new HashSet<DataFormat>();
      final Set<DataFormat> cartOnlyGenerated = new HashSet<DataFormat>();

      // Add reads to the cart
      cart.add(getReadsDataFormat(s));

      // Add genome to the cart
      cart.add(getGenomeDataFormat(s));

      // Add annotation to the cart
      cart.add(getAnnotationDataFormat(s));

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

        final DataFile file = info.getDataFile(df, s);
        if (!checkedDatafile.contains(file)) {
          if (!info.getDataFile(df, s).exists()) {

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

        final DataFile file = info.getDataFile(df, s);
        if (!checkedDatafile.contains(file)) {
          if (info.getDataFile(df, s).exists())
            throw new EoulsanException("For sample "
                + s.getId() + ", generated \"" + df.getFormatName()
                + "\" already exists.");
          checkedDatafile.add(file);
        }
      }

      cartOnlyGenerated.addAll(cartGenerated);
      cartOnlyGenerated.removeAll(cartReUsed);

      globalInputDataFormats.put(s, Collections
          .unmodifiableSet(cartNotGenerated));
      globalOutputDataFormats.put(s, Collections
          .unmodifiableSet(cartOnlyGenerated));

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

            c.configure(this.command.getStepParameters(step.getName()),
                this.command.getGlobalParameters());
            c.check(this.design, this.info, checkStore);

          }
        }
    }
  }

  private void checkStepInOutFormat(final Step step) throws EoulsanException {

    final DataFormatRegistry dfRegistry = DataFormatRegistry.getInstance();
    dfRegistry.register(step.getInputFormats());
    dfRegistry.register(step.getOutputFormats());

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

    DataFormat readsDF;
    try {
      readsDF = new DataFile(genomeSource).getMetaData().getDataFormat();

      if (readsDF == null)
        throw new EoulsanException("No DataFormat found for genome file: "
            + s.getSource());

      return readsDF;

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

    DataFormat annotDF;
    try {
      annotDF = new DataFile(annotationSource).getMetaData().getDataFormat();

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
      logger.info("Create " + stepName + " step.");
      this.steps.add(findStep(stepName));
    }
  }

  /**
   * Initialize the steps of the Workflow
   * @throws EoulsanException if an error occurs while creating the step
   */
  public void init() throws EoulsanException {

    final Command c = this.command;

    for (Step s : this.steps) {

      final String stepName = s.getName();

      logger.info("Configure " + stepName + " step.");
      s.configure(c.getStepParameters(stepName), c.getGlobalParameters());
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

  //
  // Constructor
  //

  public Workflow(final Command command, final Design design,
      final ExecutorInfo info) throws EoulsanException {

    if (command == null)
      throw new NullPointerException("The command is null.");

    if (design == null)
      throw new NullPointerException("The design is null.");

    if (info == null)
      throw new NullPointerException("The ExecutorInfo is null.");

    this.command = command;
    this.design = design;
    this.info = info;

    // Create the basic steps
    createSteps();
  }

}
