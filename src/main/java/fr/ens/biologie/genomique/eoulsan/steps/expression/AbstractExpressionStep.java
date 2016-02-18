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
 *      http://outils.genomique.biologie.ens.fr/eoulsan
 *
 */

package fr.ens.biologie.genomique.eoulsan.steps.expression;

import static fr.ens.biologie.genomique.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.biologie.genomique.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.biologie.genomique.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;
import static fr.ens.biologie.genomique.eoulsan.steps.Steps.renamedParameter;

import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.Globals;
import fr.ens.biologie.genomique.eoulsan.AbstractEoulsanRuntime.EoulsanExecMode;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.ExpressionCounter;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.ExpressionCounterService;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.HTSeqCounter;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.biologie.genomique.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.biologie.genomique.eoulsan.core.InputPorts;
import fr.ens.biologie.genomique.eoulsan.core.InputPortsBuilder;
import fr.ens.biologie.genomique.eoulsan.core.OutputPorts;
import fr.ens.biologie.genomique.eoulsan.core.Parameter;
import fr.ens.biologie.genomique.eoulsan.core.StepConfigurationContext;
import fr.ens.biologie.genomique.eoulsan.steps.AbstractStep;
import fr.ens.biologie.genomique.eoulsan.steps.CheckerStep;
import fr.ens.biologie.genomique.eoulsan.steps.Steps;
import fr.ens.biologie.genomique.eoulsan.util.Version;

/**
 * This abstract class define and parse arguments for the expression step.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 * @author Claire Wallon
 */
public abstract class AbstractExpressionStep extends AbstractStep {

  public static final String STEP_NAME = "expression";

  private static final String OLD_EOULSAN_COUNTER_NAME = "eoulsanCounter";

  public static final String REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME =
      "remove.ambiguous.cases";

  public static final String OVERLAP_MODE_PARAMETER_NAME = "overlap.mode";

  public static final String STRANDED_PARAMETER_NAME = "stranded";

  public static final String COUNTER_PARAMETER_NAME = "counter";

  public static final String GENOMIC_TYPE_PARAMETER_NAME = "genomic.type";
  public static final String ATTRIBUTE_ID_PARAMETER_NAME = "attribute.id";
  public static final String SPLIT_ATTRIBUTE_VALUES_PARAMETER_NAME =
      "split.attribute.values";

  protected static final String COUNTER_GROUP = "expression";

  private static final String DEFAULT_GENOMIC_TYPE = "exon";
  private static final String DEFAULT_ATTRIBUTE_ID = "PARENT";

  private String genomicType = DEFAULT_GENOMIC_TYPE;
  private String attributeId = DEFAULT_ATTRIBUTE_ID;

  private String counterName;
  private StrandUsage stranded = StrandUsage.NO;
  private OverlapMode overlapmode = OverlapMode.UNION;
  private boolean removeAmbiguousCases = true;
  private boolean splitAttributeValues = false;

  //
  // Getters
  //

  /**
   * Get the genomic type.
   * @return Returns the genomicType
   */
  protected String getGenomicType() {
    return this.genomicType;
  }

  /**
   * Get the attribute id.
   * @return Returns the attribute id
   */
  protected String getAttributeId() {
    return this.attributeId;
  }

  /**
   * Get the name of the counter to use.
   * @return Returns the counterName
   */
  protected String getCounterName() {
    return this.counterName;
  }

  /**
   * Get the stranded mode.
   * @return the stranded mode as a String
   */
  protected StrandUsage getStranded() {
    return this.stranded;
  }

  /**
   * Get the overlap mode.
   * @return the overlap mode as a String
   */
  protected OverlapMode getOverlapMode() {
    return this.overlapmode;
  }

  /**
   * Get the ambiguous case mode.
   * @return the ambiguous case mode
   */
  protected boolean isRemoveAmbiguousCases() {
    return this.removeAmbiguousCases;
  }

  /**
   * Get the split attribute values mode.
   * @return the split attribute values mode
   */
  protected boolean isSplitAttributeValues() {

    return this.splitAttributeValues;
  }

  /**
   * Get the counter object.
   * @return the counter object
   */
  protected ExpressionCounter getCounter() {

    return ExpressionCounterService.getInstance().newService(this.counterName);
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return STEP_NAME;
  }

  @Override
  public String getDescription() {

    return "This step compute the expression.";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputPorts() {

    final InputPortsBuilder builder = new InputPortsBuilder();

    builder.addPort("alignments", MAPPER_RESULTS_SAM);
    builder.addPort("featuresannotation", ANNOTATION_GFF);
    builder.addPort("genomedescription", GENOME_DESC_TXT);

    return builder.create();
  }

  @Override
  public OutputPorts getOutputPorts() {
    return singleOutputPort(EXPRESSION_RESULTS_TSV);
  }

  @Override
  public void configure(final StepConfigurationContext context,
      final Set<Parameter> stepParameters) throws EoulsanException {

    String counterName = null;

    for (Parameter p : stepParameters) {

      // Check if the parameter is deprecated
      checkDeprecatedParameter(p);

      switch (p.getName()) {

      case "genomictype":
        renamedParameter(context, p, GENOMIC_TYPE_PARAMETER_NAME);
      case GENOMIC_TYPE_PARAMETER_NAME:
        this.genomicType = p.getStringValue();
        break;

      case "attributeid":
        renamedParameter(context, p, ATTRIBUTE_ID_PARAMETER_NAME);
      case ATTRIBUTE_ID_PARAMETER_NAME:
        this.attributeId = p.getStringValue();
        break;

      case COUNTER_PARAMETER_NAME:
        counterName = p.getStringValue();
        break;

      case STRANDED_PARAMETER_NAME:

        this.stranded = StrandUsage.getStrandUsageFromName(p.getStringValue());

        if (this.stranded == null) {
          Steps.badParameterValue(context, p, "Unknown strand mode");
        }
        break;

      case "overlapmode":
        renamedParameter(context, p, OVERLAP_MODE_PARAMETER_NAME);
      case OVERLAP_MODE_PARAMETER_NAME:

        this.overlapmode =
            OverlapMode.getOverlapModeFromName(p.getStringValue());

        if (this.overlapmode == null) {
          Steps.badParameterValue(context, p, "Unknown overlap mode");
        }
        break;

      case "removeambiguouscases":
        renamedParameter(context, p, REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME);
      case REMOVE_AMBIGUOUS_CASES_PARAMETER_NAME:
        this.removeAmbiguousCases = p.getBooleanValue();
        break;

      case "splitattributevalues":
        renamedParameter(context, p, SPLIT_ATTRIBUTE_VALUES_PARAMETER_NAME);
      case SPLIT_ATTRIBUTE_VALUES_PARAMETER_NAME:
        this.splitAttributeValues = p.getBooleanValue();
        break;

      default:
        Steps.unknownParameter(context, p);
      }

    }

    if (this.genomicType == null) {
      Steps.invalidConfiguration(context, "No parent type set");
    }

    if (this.attributeId == null) {
      Steps.invalidConfiguration(context, "No attribute id set");
    }

    if (counterName == null) {
      counterName = HTSeqCounter.COUNTER_NAME;
    }

    // Test if counter engine exists
    if (ExpressionCounterService.getInstance()
        .newService(counterName) == null) {
      Steps.invalidConfiguration(context, "Unknown counter: " + counterName);
    }

    // Set the counter name to use
    this.counterName = counterName;

    // Configure Checker
    if (context.getRuntime().getMode() != EoulsanExecMode.CLUSTER_TASK) {
      CheckerStep.configureChecker(ANNOTATION_GFF, stepParameters);
    }

    // Log Step parameters
    getLogger().info("In " + getName() + ", counter=" + this.counterName);
    getLogger().info("In "
        + getName() + ", stranded=" + this.stranded + ", overlapmode="
        + this.overlapmode);
  }

  //
  // Other methods
  //

  /**
   * Check deprecated parameters.
   * @param parameter the parameter to check
   * @throws EoulsanException if the parameter is no more supported
   */
  private static void checkDeprecatedParameter(final Parameter parameter)
      throws EoulsanException {

    if (parameter == null) {
      return;
    }

    switch (parameter.getName()) {

    case COUNTER_PARAMETER_NAME:

      if (OLD_EOULSAN_COUNTER_NAME.toLowerCase()
          .equals(parameter.getLowerStringValue())) {
        getLogger().warning("The "
            + OLD_EOULSAN_COUNTER_NAME + " counter support has been removed");
      }

      break;

    default:
      break;
    }
  }
}
