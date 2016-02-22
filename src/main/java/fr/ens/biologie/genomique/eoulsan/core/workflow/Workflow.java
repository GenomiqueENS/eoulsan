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

package fr.ens.biologie.genomique.eoulsan.core.workflow;

import java.io.Serializable;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.design.Design;

/**
 * This interface define a Workflow.
 * @author Laurent Jourdren
 * @since 2.0
 */
public interface Workflow extends Serializable {

  /**
   * Get the design used by the workflow.
   * @return a Design Object
   */
  Design getDesign();

  /**
   * Get the steps of the workflow.
   * @return a Set with the steps of the workflow.
   */
  Set<WorkflowStep> getSteps();

  /**
   * Get the first steps of the workflow.
   * @return the root step of the workflow
   */
  WorkflowStep getRootStep();

  /**
   * Get the design step of the workflow.
   * @return the design step of the workflow
   */
  WorkflowStep getDesignStep();

  /**
   * Get the first step of the workflow (after generator steps).
   * @return the first step of the workflow
   */
  WorkflowStep getFirstStep();

  /**
   * Get the list of the files used by the workflow from the begging of the
   * workflow.
   * @return a WorkflowFile object
   */
  WorkflowFiles getWorkflowFilesAtRootStep();

  /**
   * Get the list of the files used by the workflow from the first real step of
   * the workflow (after the generators).
   * @return a WorkflowFile object
   */
  WorkflowFiles getWorkflowFilesAtFirstStep();

  /**
   * Delete a file on the exit of the workflow.
   * @param file file to remove
   */
  public void deleteOnExit(final DataFile file);

}
