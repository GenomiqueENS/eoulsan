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

package fr.ens.transcriptome.eoulsan.io;

import java.io.IOException;
import java.io.InputStream;

import fr.ens.transcriptome.eoulsan.util.FileUtils;

/**
 * This class allow to compare two binary files.
 * @author Laurent Jourdren
 * @since 1.3
 */
public class BinaryCompareFile extends AbstractCompareFiles {

  @Override
  public boolean compareNonOrderedFiles(InputStream inA, InputStream inB)
      throws IOException {

    throw new UnsupportedOperationException();
  }

  @Override
  public boolean compareOrderedFiles(InputStream inA, InputStream inB)
      throws IOException {

    return FileUtils.compareFile(inA, inB);
  }

}
