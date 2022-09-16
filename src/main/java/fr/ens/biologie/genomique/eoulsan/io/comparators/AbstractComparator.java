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

import static fr.ens.biologie.genomique.kenetre.io.CompressionType.getCompressionTypeByFilename;
import static fr.ens.biologie.genomique.kenetre.io.FileUtils.checkExistingStandardFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * This abstract class define methods to compare files.
 * @since 2.0
 * @author Sandrine Perrin
 */
public abstract class AbstractComparator implements Comparator {

  // Line from tested file which fail comparison
  private String causeFailComparison = null;

  @Override
  public boolean compareFiles(final String pathA, final String pathB)
      throws IOException {

    return compareFiles(new File(pathA), new File(pathB));
  }

  @Override
  public boolean compareFiles(final File fileA, final File fileB)
      throws IOException {

    // Check input files
    if (!checkFiles(fileA, fileB) && checkFileSize()) {
      return false;
    }

    // The files are not equals
    if (fileA.equals(fileB)) {
      return false;
    }

    // Check path file (abstract and symbolic) is the same
    if (fileA.getCanonicalFile().equals(fileB.getCanonicalFile())) {
      return true;
    }

    try (InputStream isA = new FileInputStream(fileA);
        InputStream isB = new FileInputStream(fileB)) {

      return compareFiles(
          getCompressionTypeByFilename(fileA.getAbsolutePath())
              .createInputStream(isA),
          getCompressionTypeByFilename(fileB.getAbsolutePath())
              .createInputStream(isB));
    }
  }

  /**
   * Test if files size can be used to detect if the two files are not the same.
   * @return true if the files size can be used to detect if the two files are
   *         not the same
   */
  protected boolean checkFileSize() {
    // TODO to verify interesting to implement
    return true;
  }

  //
  // Other methods
  //

  /**
   * Check input files of methods of the class.
   * @param fileA first file to check
   * @param fileB second file to check
   * @return false if the files does not have the same length
   * @throws IOException if an input file is invalid
   */
  protected static boolean checkFiles(final File fileA, final File fileB)
      throws IOException {

    checkExistingStandardFile(fileA, fileA.getAbsolutePath());
    checkExistingStandardFile(fileB, fileB.getAbsolutePath());

    // Check if try to compare the same file
    if (fileA.equals(fileB)) {
      throw new IOException("Try to compare the same file: " + fileA);
    }

    return true;
  }

  @Override
  public String toString() {
    return getName() + " compares files with extensions " + getExtensions();
  }

  @Override
  abstract public Collection<String> getExtensions();

  @Override
  abstract public String getName();

  //
  // Getter
  //

  @Override
  public String getCauseFailComparison() {
    return this.causeFailComparison;
  }

  @Override
  public void setCauseFailComparison(final String line) {
    this.causeFailComparison = (this.causeFailComparison == null)
        ? "[" + line + "]" : this.causeFailComparison + "\n[" + line + "]";

  }
}
