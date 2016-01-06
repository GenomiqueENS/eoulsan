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

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * This class define objects that contains the input files, the output files and
 * the reused files of a Workflow.
 * @author Laurent Jourdren
 * @since 2.0
 */
public final class WorkflowFiles {

  private final Set<WorkflowStepOutputDataFile> inFiles;
  private final Set<WorkflowStepOutputDataFile> reusedFiles;
  private final Set<WorkflowStepOutputDataFile> outFiles;

  /**
   * Get the input files of a workflow.
   * @return an unmodifiable set that contains the input files
   */
  public Set<WorkflowStepOutputDataFile> getInputFiles() {

    return Collections.unmodifiableSet(this.inFiles);
  }

  /**
   * Get the reused files of a workflow.
   * @return an unmodifiable set that contains the reused files
   */
  public Set<WorkflowStepOutputDataFile> getReusedFiles() {

    return Collections.unmodifiableSet(this.reusedFiles);
  }

  /**
   * Get the output files of a workflow.
   * @return an unmodifiable set that contains the output files
   */
  public Set<WorkflowStepOutputDataFile> getOutputFiles() {

    return Collections.unmodifiableSet(this.outFiles);
  }

  //
  // Constructor
  //

  /**
   * Public constructor.
   * @param inFiles input files of the workflow
   * @param reusedFiles reused files of the workflow
   * @param outFiles output files of the workflow
   */
  public WorkflowFiles(final Set<WorkflowStepOutputDataFile> inFiles,
      final Set<WorkflowStepOutputDataFile> reusedFiles,
      final Set<WorkflowStepOutputDataFile> outFiles) {

    this.inFiles = Sets.newHashSet(inFiles);
    this.reusedFiles = Sets.newHashSet(reusedFiles);
    this.outFiles = Sets.newHashSet(outFiles);
  }

}
