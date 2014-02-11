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

package fr.ens.transcriptome.eoulsan.core;

import static fr.ens.transcriptome.eoulsan.core.InputPortsBuilder.noInputPort;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define an abstract Step.
 * @since 1.0
 * @author Laurent Jourdren
 */
public abstract class AbstractStep implements Step {

  @Override
  public String getDescription() {

    return "Description of " + getName();
  }

  @Override
  public Version getVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public Version getRequiedEoulsanVersion() {

    return Globals.APP_VERSION;
  }

  @Override
  public InputPorts getInputFormats() {

    return noInputPort();
  }

  @Override
  public OutputPorts getOutputFormats() {

    return OutputPortsBuilder.noOutputPort();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters)
      throws EoulsanException {
  }

  @Override
  public boolean isTerminalStep() {
    return false;
  }

  @Override
  public boolean isCreateLogFiles() {
    return true;
  }

}
