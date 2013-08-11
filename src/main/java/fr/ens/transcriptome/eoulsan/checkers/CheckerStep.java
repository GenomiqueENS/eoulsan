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

package fr.ens.transcriptome.eoulsan.checkers;

import java.util.List;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.Context;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.design.Design;
import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * This class is a step that launch checkers.
 * @author Laurent Jourdren
 * @since 1.3
 */
@HadoopCompatible
public class CheckerStep extends AbstractStep {

  private List<Checker> checkers = Lists.newArrayList();

  //
  // Step methods
  //

  @Override
  public String getName() {

    return "_checker";
  }

  @Override
  public StepResult execute(final Design design, final Context context,
      final StepStatus status) {

    // Get the checkstore
    final CheckStore checkStore = CheckStore.getCheckStore();

    int count = 0;

    try {

      for (Checker checker : this.checkers) {
        for (Sample sample : design.getSamples())
          checker.check(context, sample, checkStore);

        count++;

        status.setProgress(((double) count) / this.checkers.size());
      }

    } catch (EoulsanException e) {

      return status.createStepResult(e);
    } finally {

      // Clear the checker
      this.checkers.clear();
    }

    return status.createStepResult();
  }

  //
  // Other methods
  //

  public void addChecker(final Checker checker) {

    if (checker == null)
      return;

    this.checkers.add(checker);
  }

}
