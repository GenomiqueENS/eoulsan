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
package fr.ens.transcriptome.eoulsan.galaxytools;

import java.util.Map;
import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.data.DataFormat;
import fr.ens.transcriptome.eoulsan.steps.galaxytool.elements.ToolElement;

/**
 * The interface on tool interpreter.
 * @author Sandrine Perrin
 * @since 2.1
 */
public interface ToolInterpreter {

  /**
   * Parse tool file to extract useful data to run tool.
   * @param setStepParameters the set step parameters
   * @throws EoulsanException if an data missing
   */
  void configure(final Set<Parameter> setStepParameters)
      throws EoulsanException;

  /**
   * Convert command tag from tool file in string, variable are replace by
   * value.
   * @param context the context
   * @return the string
   * @throws EoulsanException the Eoulsan exception
   */
  ToolExecutorResult execute(final StepContext context) throws EoulsanException;

  /**
   * Gets the in data format expected associated with variable found in command
   * line.
   * @return the in data format expected
   */
  Map<DataFormat, ToolElement> getInDataFormatExpected();

  /**
   * Gets the out data format expected associated with variable found in command
   * line.
   * @return the out data format expected
   */
  Map<DataFormat, ToolElement> getOutDataFormatExpected();

  /**
   * Gets the tool data.
   * @return the tool data
   */
  ToolData getToolData();

}
