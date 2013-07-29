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

import java.util.Set;

public final class WorkflowFiles {

  private final Set<WorkflowStepOutputDataFile> inFiles;
  private final Set<WorkflowStepOutputDataFile> reusedFiles;
  private final Set<WorkflowStepOutputDataFile> outFiles;

  public Set<WorkflowStepOutputDataFile> getInputFiles() {

    return this.inFiles;
  }

  public Set<WorkflowStepOutputDataFile> getReusedFiles() {

    return this.reusedFiles;
  }

  public Set<WorkflowStepOutputDataFile> getOutputFiles() {

    return this.outFiles;
  }

  //
  // Constructor
  //

  public WorkflowFiles(final Set<WorkflowStepOutputDataFile> inFiles,
      final Set<WorkflowStepOutputDataFile> reusedFiles,
      final Set<WorkflowStepOutputDataFile> outFiles) {

    this.inFiles = inFiles;
    this.reusedFiles = reusedFiles;
    this.outFiles = outFiles;
  }

}