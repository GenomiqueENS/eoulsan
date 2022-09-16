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
 * of the Institut de Biologie de l'École normale supérieure and
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

package fr.ens.biologie.genomique.eoulsan.io.comparators;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.google.common.collect.Sets;

import fr.ens.biologie.genomique.kenetre.io.FileUtils;

/**
 * This class allow to compare two binary files.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BinaryComparator extends AbstractComparator {

  private static final String COMPARATOR_NAME = "BinaryComparator";

  // Only file corresponding to the report step log
  private static final Collection<String> EXTENSIONS =
      Sets.newHashSet(".bai", "ebwt");

  private int numberElementsCompared;

  @Override
  public boolean compareFiles(final InputStream isA, final InputStream isB)
      throws IOException {

    if (isA == null || isB == null) {
      throw new IOException("Comparison invalid, input stream null");
    }

    // Set cause, can not identified the position who fail comparison
    setCauseFailComparison("Binary comparison.");

    return FileUtils.compareFile(isA, isB);
  }

  @Override
  public int getNumberElementsCompared() {
    return this.numberElementsCompared;
  }

  @Override
  public Collection<String> getExtensions() {
    return EXTENSIONS;
  }

  @Override
  public String getName() {
    return COMPARATOR_NAME;
  }

}
