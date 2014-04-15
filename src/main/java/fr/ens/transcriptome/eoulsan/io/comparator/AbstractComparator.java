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

package fr.ens.transcriptome.eoulsan.io.comparator;

import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByFilename;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import fr.ens.transcriptome.eoulsan.util.FileUtils;

public abstract class AbstractComparator implements Comparator {

  private String pathFileA;
  private String pathFileB;

  @Override
  public boolean compareFiles(final String pathA, final String pathB)
      throws IOException {

    // checkAndInit(pathA, pathB);

    return compareFiles(new File(pathA), new File(pathB), false);
  }

  @Override
  public boolean compareFiles(final String pathA, final String pathB,
      final boolean useSerializeFile) throws IOException {

    // checkAndInit(pathA, pathB);

    return compareFiles(new File(pathA), new File(pathB), useSerializeFile);
  }

  @Override
  public boolean compareFiles(final File fileA, final File fileB)
      throws FileNotFoundException, IOException {

    // checkAndInit(fileA.getAbsolutePath(), fileB.getAbsolutePath());

    return compareFiles(fileA, fileB, false);
  }

  @Override
  public boolean compareFiles(final File fileA, final File fileB,
      final boolean useSerializeFile) throws FileNotFoundException, IOException {

    // Check input files
    if (!checkFiles(fileA, fileB) && checkFileSize())
      return false;

    // The files are not equals
    if (fileA.equals(fileB.length()))
      return false;

    // Check path file (abstract and symbolic) is the same
    if (fileA.getCanonicalFile().equals(fileB.getCanonicalFile())) {
      return true;
    }

    final InputStream isA =
        getCompressionTypeByFilename(fileA.getAbsolutePath())
            .createInputStream(new FileInputStream(fileA));

    final InputStream isB =
        getCompressionTypeByFilename(fileB.getAbsolutePath())
            .createInputStream(new FileInputStream(fileB));

    return compareFiles(isA, isB);
  }

  /**
   * Test if files size can be used to detect if the two files are not the same.
   * @return true if the files size can be used to detect if the two files are
   *         not the same
   */
  protected boolean checkFileSize() {

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

    FileUtils.checkExistingFile(fileA, fileA.getAbsolutePath());
    FileUtils.checkExistingFile(fileB, fileB.getAbsolutePath());

    // Check if try to compare the same file
    if (fileA.equals(fileB))
      throw new IOException("Try to compare the same file: " + fileA);

    return true;
  }

  @Override
  public String toString() {
    return getName() + " compares files with extensions " + getExtensions();
  }

  @Override
  public abstract Collection<String> getExtensions();

  @Override
  public abstract String getName();

  //
  // Getter
  //

  public String getPathFileA() {
    return pathFileA;
  }

  public String getPathFileB() {
    return pathFileB;
  }

  public String getPathDirectoryFileA() {
    return new File(pathFileA).getParent();
  }

  public String getPathDirectoryFileB() {
    return new File(pathFileB).getParent();
  }
}
