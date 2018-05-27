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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * This interface define methods to compare files.
 * @since 2.0
 * @author Sandrine Perrin
 */
public interface Comparator {

  /**
   * Return collector name.
   * @return collector name.
   */
  String getName();

  /**
   * Compare two files no ordered, check if they are the same contents.
   * @param pathA the path to the first file, used like reference.
   * @param pathB the path to the second file,
   * @return boolean true if files are same.
   * @throws IOException if an error occurs while comparing the files.
   */
  boolean compareFiles(final String pathA, final String pathB)
      throws IOException;

  /**
   * Compare two files no ordered, check if they are the same contents.
   * @param fileA the path to the first file, used like reference.
   * @param fileB the path to the second file,
   * @return boolean true if files are same.
   * @throws IOException if an error occurs while comparing the files.
   */
  boolean compareFiles(final File fileA, final File fileB) throws IOException;

  /**
   * Compare two files no ordered, check if they are the same contents.
   * @param isA the path to the first file, used like reference.
   * @param isB the path to the second file,
   * @throws IOException if an error occurs while comparing the files.
   */
  boolean compareFiles(final InputStream isA, final InputStream isB)
      throws IOException;

  /**
   * Return all extensions treated by comparator files.
   * @return list extensions.
   */
  Collection<String> getExtensions();

  /**
   * Return number elements compared by comparator.
   * @return number elements compared
   */
  int getNumberElementsCompared();

  /**
   * Return line which fail comparison between to file from tested file.
   * @return line which fail comparison between to file from tested file
   */
  String getCauseFailComparison();

  /**
   * Set line which fail comparison between to file from tested file, it can
   * compile few lines.
   */
  void setCauseFailComparison(final String line);

}
