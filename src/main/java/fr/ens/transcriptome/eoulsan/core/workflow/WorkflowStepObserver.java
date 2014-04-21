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

package fr.ens.transcriptome.eoulsan.core.workflow;

import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This interface define workflow step events
 * @author Laurent Jourdren
 * @since 1.3
 */
public interface WorkflowStepObserver {

  /**
   * The status of the step has been changed.
   * @param step step that the status has been changed
   */
  void notifyStepState(WorkflowStep step);

  /**
   * The progress of the step for a sample has been changed.
   * @param step step that the progress has been changed
   * @param sample sample witch progress has been changed
   */
  void notifyStepState(WorkflowStep step, Sample sample, double progress);

  /**
   * The progress of the step has been changed.
   * @param step step that the progress has been changed
   */
  void notifyStepState(WorkflowStep step, double progress);

  /**
   * The note of the step has been changed.
   * @param step step that the note has been changed
   */
  void notifyStepState(WorkflowStep step, String note);

}
