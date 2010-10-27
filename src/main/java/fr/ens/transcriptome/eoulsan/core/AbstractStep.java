/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.core;

import java.util.Set;

import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.datatypes.DataType;
import fr.ens.transcriptome.eoulsan.util.Version;

/**
 * This class define an abstract Step.
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
  public ExecutionMode getExecutionMode() {

    return Step.ExecutionMode.BOTH;
  }

  @Override
  public DataType[] getInputTypes() {

    return null;
  }

  @Override
  public DataType[] getOutputType() {

    return null;
  }

  @Override
  public String getLogName() {

    return getName();
  }

  @Override
  public void configure(final Set<Parameter> stepParameters,
      final Set<Parameter> globalParameters) throws EoulsanException {
  }

}
