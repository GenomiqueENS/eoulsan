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

import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStep.StepState;
import fr.ens.transcriptome.eoulsan.core.workflow.WorkflowStepEvent;

public class BasicUI implements WorkflowStepEvent {

  @Override
  public void updateStepState(final WorkflowStep step) {

    if (step == null)
      return;

    System.out.println("Step "
        + step.getId() + " (" + step.getNumber() + ") status changed to "
        + step.getState());

    if (step.getState() == StepState.WORKING)
      updateStepState(step, 0.0);

  }

  @Override
  public void updateStepState(final WorkflowStep step, double progress) {

    if (step == null)
      return;

    System.out.printf("Step %s (%d) %s progress: %.0f %%\n", step.getId(),
        step.getNumber(), step.getState().toString(), progress * 100.0);

  }

  @Override
  public void updateStepState(final WorkflowStep step, final String note) {

    if (step == null)
      return;

    System.out.printf("Step "
        + step.getId() + " " + step.getState().toString() + " note: " + note);
  }

}
