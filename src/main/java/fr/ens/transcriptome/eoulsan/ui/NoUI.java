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

package fr.ens.transcriptome.eoulsan.ui;

import fr.ens.transcriptome.eoulsan.core.workflow.Workflow;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;

/**
 * This class define an UI that do nothing.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class NoUI implements UI {

  @Override
  public String getName() {

    return "no";
  }

  @Override
  public void init(Workflow workflow) {
    // Do nothing
  }

  @Override
  public void notifyStepState(WorkflowStep step) {
    // Do nothing
  }

  @Override
  public void notifyStepState(WorkflowStep step, int contextId,
      String contextName, double progress) {
    // Do nothing
  }

  @Override
  public void notifyStepState(WorkflowStep step, double progress) {
    // Do nothing
  }

  @Override
  public void notifyStepState(WorkflowStep step, String note) {
    // Do nothing
  }

}
