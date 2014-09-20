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

package fr.ens.transcriptome.eoulsan.steps.expression;

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;
import static fr.ens.transcriptome.eoulsan.core.OutputPortsBuilder.singleOutputPort;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.ANNOTATION_GFF;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.EXPRESSION_RESULTS_TSV;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.GENOME_DESC_TXT;
import static fr.ens.transcriptome.eoulsan.data.DataFormats.MAPPER_RESULTS_SAM;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.EoulsanRuntime;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.ExpressionCounter;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.ExpressionCounterService;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.OverlapMode;
import fr.ens.transcriptome.eoulsan.bio.expressioncounters.StrandUsage;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.steps.AbstractStep;
import fr.ens.transcriptome.eoulsan.steps.CheckerStep;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This abstract class define and parse arguments for the expression step.
 * @since 1.0
 * @author Laurent Jourdren
 * @author Maria Bernard
 * @author Claire Wallon
 */
public abstract class AbstractExpressionStep extends AbstractStep {

  public static final String REMOVEAMBIGUOUSCASES_PARAMETER_NAME =
      "removeambiguouscases";

  public static final String OVERLAPMODE_PARAMETER_NAME = "overlapmode";

  public static final String STRANDED_PARAMETER_NAME = "stranded";

  public static final String COUNTER_PARAMETER_NAME = "counter";

  public static final String GENOMIC_TYPE_PARAMETER_NAME = "genomictype";
  public static final String ATTRIBUTE_ID_PARAMETER_NAME = "attributeid";

  protected static final String COUNTER_GROUP = "expression";

  private static final String STEP_NAME = "expression";
  private static final String DEFAULT_GENOMIC_TYPE = "exon";
  private static final String DEFAULT_ATTRIBUTE_ID = "PARENT";

  private String genomicType = DEFAULT_GENOMIC_TYPE;
  private String attributeId = DEFAULT_ATTRIBUTE_ID;
  private String tmpDir;

  private String counterName;
  private StrandUsage stranded = StrandUsage.NO;
  private OverlapMode overlapmode = OverlapMode.UNION;
  private boolean removeAmbiguousCases = true;

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
   * Get the stranded mode
   * @return the stranded mode as a String
   */
  protected StrandUsage getStranded() {
    return this.stranded;
  }

  /**
   * Get the overlap mode
   * @return the overlap mode as a String
   */
  protected OverlapMode getOverlapMode() {
    return this.overlapmode;
  }

  /**
   * Get the ambiguous case mode
   * @return the ambiguous case mode
   */
  protected boolean isRemoveAmbiguousCases() {
    return this.removeAmbiguousCases;
  }

  /**
   * Get the counter object.
   * @return the counter object
   */
  protected ExpressionCounter getCounter() {

    return ExpressionCounterService.getInstance().newService(counterName);
  }

  /**
   * Get the temporary directory
   * @return Returns the tmpDir
   */
  protected String getTmpDir() {
    return tmpDir;
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
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {

    String counterName = null;

    for (Parameter p : stepParameters) {

      if (GENOMIC_TYPE_PARAMETER_NAME.equals(p.getName()))
        this.genomicType = p.getStringValue();
      else if (ATTRIBUTE_ID_PARAMETER_NAME.equals(p.getName()))
        this.attributeId = p.getStringValue();
      else if (COUNTER_PARAMETER_NAME.equals(p.getName()))
        counterName = p.getStringValue();
      else if (STRANDED_PARAMETER_NAME.equals(p.getName())) {

        this.stranded = StrandUsage.getStrandUsageFromName(p.getStringValue());

        if (this.stranded == null)
          throw new EoulsanException("Unknown strand mode in "
              + getName() + " step: " + p.getStringValue());

      } else if (OVERLAPMODE_PARAMETER_NAME.equals(p.getName())) {

        this.overlapmode =
            OverlapMode.getOverlapModeFromName(p.getStringValue());

        if (this.overlapmode == null)
          throw new EoulsanException("Unknown overlap mode in "
              + getName() + " step: " + p.getStringValue());

      } else if (REMOVEAMBIGUOUSCASES_PARAMETER_NAME.equals(p.getName()))
        this.removeAmbiguousCases = p.getBooleanValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.genomicType == null)
      throw new EoulsanException("Parent type not set for "
          + getName() + " step.");

    if (this.attributeId == null)
      throw new EoulsanException("Attribute id not set for "
          + getName() + " step.");

    if (counterName == null)
      counterName = "eoulsanCounter";

    // Test if counter engine exists
    if (ExpressionCounterService.getInstance().newService(counterName) == null) {
      throw new EoulsanException("Unknown counter: " + counterName);
    }

    // Set the counter name to use
    this.counterName = counterName;

    // Configure Checker
    CheckerStep.configureChecker(ANNOTATION_GFF, stepParameters);

    // Set temporary directory
    this.tmpDir = EoulsanRuntime.getRuntime().getSettings().getTempDirectory();

    // Log Step parameters
    getLogger().info("In " + getName() + ", counter=" + this.counterName);
    getLogger().info(
        "In "
            + getName() + ", stranded=" + this.stranded + ", overlapmode="
            + this.overlapmode);
  }
}
