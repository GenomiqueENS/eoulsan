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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.AbstractStep;
import fr.ens.transcriptome.eoulsan.core.InputPorts;
import fr.ens.transcriptome.eoulsan.core.InputPortsBuilder;
import fr.ens.transcriptome.eoulsan.core.OutputPorts;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;

/**
 * This class is a step that launch checkers.
 * @author Laurent Jourdren
 * @since 1.3
 */
@HadoopCompatible
public class CheckerStep extends AbstractStep {

  private List<Checker> checkers = Lists.newArrayList();
  private InputPorts inputPorts = InputPortsBuilder.noInputPort();
  private boolean inputPortsConfigured;

  public void configureInputPorts(final OutputPorts designOutputPorts) {

    Preconditions.checkState(!this.inputPortsConfigured,
        "inputPorts has been already configured");

    // TODO configure the inputPorts

    this.inputPortsConfigured = true;
  }

  //
  // Step methods
  //

  @Override
  public String getName() {

    return "_checker";
  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    // Get the checkstore
    final CheckStore checkStore = CheckStore.getCheckStore();

    int count = 0;

    try {

      for (Checker checker : this.checkers) {

        // TODO implement checker launch with Data object
        // for (Sample sample : design.getSamples())
        // checker.check(context, sample, checkStore);
        // TODO remove this
        if (false)
          throw new EoulsanException();

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
