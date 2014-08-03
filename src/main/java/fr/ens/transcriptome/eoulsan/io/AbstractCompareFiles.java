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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This class define an abstract class that implements some methods of the
 * CompareFiles interface.
 * @author Laurent Jourdren
 * @since 1.3
 */
public abstract class AbstractCompareFiles implements CompareFiles {

  @Override
  public boolean compareNonOrderedFiles(final String pathA, final String pathB)
      throws IOException {

    return compareNonOrderedFiles(new File(pathA), new File(pathB));
  }

  @Override
  public boolean compareNonOrderedFiles(final File fileA, final File fileB)
      throws IOException {

    // Check input files
    if (!checkFiles(fileA, fileB) && checkFileSize())
      return false;

    return compareNonOrderedFiles(new FileInputStream(fileA),
        new FileInputStream(fileB));
  }

  @Override
  public boolean compareOrderedFiles(final String pathA, final String pathB)
      throws IOException {

    return compareNonOrderedFiles(new File(pathA), new File(pathB));
  }

  @Override
  public boolean compareOrderedFiles(final File fileA, final File fileB)
      throws FileNotFoundException, IOException {

    // Check input files
    if (!checkFiles(fileA, fileB) && checkFileSize())
      return false;

    return compareNonOrderedFiles(new FileInputStream(fileA),
        new FileInputStream(fileB));
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
  private static boolean checkFiles(final File fileA, final File fileB)
      throws IOException {

    checkFile(fileA, "fileA");
    checkFile(fileB, "fileB");

    // Check if try to compare the same file
    if (fileA.equals(fileB))
      throw new IOException("Try to compare the same file: " + fileA);

    // The files are not equals if size if not equals
    if (fileA.length() != fileB.length())
      return false;

    return true;
  }

  /**
   * Check file argument of methods of the class.
   * @param file first file to check
   * @throws IOException
   */
  private static void checkFile(final File file, String argumentName)
      throws IOException {

    if (file == null)
      throw new NullPointerException("The "
          + argumentName + " argument is null");

    if (!file.exists())
      throw new IOException("The " + argumentName + " does not exist");

    if (!file.isFile())
      throw new IOException("The " + argumentName + " is not a standard file");
  }

}
