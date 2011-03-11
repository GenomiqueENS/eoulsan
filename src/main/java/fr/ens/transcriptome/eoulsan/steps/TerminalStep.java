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

package fr.ens.transcriptome.eoulsan.steps;

import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.design.Design;

/**
 * This class define a terminal step that do nothing. After this execution the
 * workflow will stop.
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class TerminalStep extends AbstractStep {

  @Override
  public String getName() {

    return "terminal";
  }

  @Override
  public String getLogName() {
    return null;
  }

  @Override
  public StepResult execute(Design design, Context context) {

    return new StepResult(context, System.currentTimeMillis(), "Terminal step.");
  }

  @Override
  public boolean isTerminalStep() {

    return true;
  }

}
