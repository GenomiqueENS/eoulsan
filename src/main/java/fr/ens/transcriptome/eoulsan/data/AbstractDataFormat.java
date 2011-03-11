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

package fr.ens.transcriptome.eoulsan.data;

import fr.ens.transcriptome.eoulsan.checkers.Checker;
import fr.ens.transcriptome.eoulsan.design.Sample;
import fr.ens.transcriptome.eoulsan.steps.Step;

abstract class AbstractDataFormat implements DataFormat {

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
  public boolean isGenerator() {

    return false;
  }

  @Override
  public boolean isChecker() {

    return false;
  }

  @Override
  public Step getGenerator() {

    return null;
  }

  @Override
  public Checker getChecker() {

    return null;
  }

  @Override
  public String getContentType() {

    return "text/plain";
  }

  @Override
  public String toString() {
    return getFormatName();
  }

}
