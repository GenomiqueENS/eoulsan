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
package fr.ens.transcriptome.eoulsan.io.comparators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * This interface define methods to compare files.
 * @since 1.3
 * @author Sandrine Perrin
 */
public interface Comparator {

  public String getName();

  public boolean compareFiles(final String pathA, final String pathB)
      throws IOException;

  public boolean compareFiles(final String pathA, final String pathB,
      final boolean useSerialize) throws IOException;

  public boolean compareFiles(final File fileA, final File fileB,
      final boolean useSerialize) throws FileNotFoundException, IOException;

  public boolean compareFiles(final File fileA, final File fileB)
      throws FileNotFoundException, IOException;

  public boolean compareFiles(final InputStream isA, final InputStream isB)
      throws IOException;

  public Collection<String> getExtensions();

  public int getNumberElementsCompared();

}
