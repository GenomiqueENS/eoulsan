/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.steps.expression;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.data.DataFormats;

/**
 * This abstract class define and parse arguments for the expression step.
 * @author Laurent Jourdren
 */
public abstract class ExpressionStep extends AbstractStep {

  public static final String GENOMIC_TYPE_PARAMETER_NAME = "genomictype";

  private static final String STEP_NAME = "expression";
  private static final String DEFAULT_GENOMIC_TYPE = "exon";

  private String genomicType = DEFAULT_GENOMIC_TYPE;
  private String tmpDir;

  //
  // Getters
  //

  /**
   * Get the genomic type
   * @return Returns the genomicType
   */
  protected String getGenomicType() {
    return genomicType;
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

    return "This class compute the expression.";
  }

  @Override
  public DataFormat[] getInputFormats() {
    return new DataFormat[] {DataFormats.FILTERED_SOAP_RESULTS_TXT,
        DataFormats.ANNOTATION_GFF};
  }

  @Override
  public DataFormat[] getOutputFormats() {
    return new DataFormat[] {DataFormats.EXPRESSION_RESULTS_TXT};
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {

    for (Parameter p : stepParameters) {

      if (GENOMIC_TYPE_PARAMETER_NAME.equals(p.getName()))
        this.genomicType = p.getStringValue();
      else
        throw new EoulsanException("Unknown parameter for "
            + getName() + " step: " + p.getName());

    }

    if (this.genomicType == null)
      throw new EoulsanException("Parent type in no set for "
          + getName() + " step.");

    for (Parameter p : globalParameters) {

      if ("tmpdir".equals(p.getName()))
        this.tmpDir = p.getStringValue();

    }

  }

}
