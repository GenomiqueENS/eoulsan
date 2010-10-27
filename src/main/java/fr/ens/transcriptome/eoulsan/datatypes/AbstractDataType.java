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

package fr.ens.transcriptome.eoulsan.datatypes;

import fr.ens.transcriptome.eoulsan.design.Sample;

/**
 * Define an abstract DataType.
 * @author Laurent Jourdren
 */
public abstract class AbstractDataType implements DataType {

  @Override
  public String getDescription() {

    return getName();
  }

  @Override
  public String[] getExtensions() {

    return new String[] {getDefaultExtention()};
  }

  @Override
  public String getSourcePathForSample(final Sample sample,
      final String ExecInfo) {

    return null;
  }

  @Override
  public boolean isOneFilePerAnalysis() {

    return false;
  }

  @Override
  public String toString() {
    return getName();
  }

}
