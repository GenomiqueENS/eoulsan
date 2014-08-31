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

import static fr.ens.transcriptome.eoulsan.EoulsanLogger.getLogger;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.annotations.HadoopCompatible;
import fr.ens.transcriptome.eoulsan.core.Parameter;
import fr.ens.transcriptome.eoulsan.core.StepContext;
import fr.ens.transcriptome.eoulsan.core.StepResult;
import fr.ens.transcriptome.eoulsan.core.StepStatus;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This step is a fake step.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopCompatible
public class FakeStep extends AbstractStep {

  

  @Override
  public String getName() {

    return "fakestep";
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public boolean isCreateLogFiles() {
    return false;
  }

  @Override
  public void configure(Set<Parameter> stepParameters) {

    for (Parameter p : stepParameters)
      getLogger().info("s: " + p.getName() + "\t" + p.getStringValue());

  }

  @Override
  public StepResult execute(final StepContext context, final StepStatus status) {

    return status.createStepResult();
  }

}
