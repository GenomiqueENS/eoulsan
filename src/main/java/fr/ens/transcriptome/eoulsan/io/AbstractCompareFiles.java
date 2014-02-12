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

import static com.google.common.base.Preconditions.checkNotNull;
import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByFilename;
import static fr.ens.transcriptome.eoulsan.util.StringUtils.toTimeHumanReadable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.google.common.base.Stopwatch;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

public abstract class AbstractCompareFiles implements CompareFiles {

  private String pathFileA;
  private String pathFileB;

  /** LOGGER */
  private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);

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

    checkAndInit(fileA.getAbsolutePath(), fileB.getAbsolutePath());

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

    if (useSerializeFile && isUseBloomfilterAvailable())
      return compareFiles(getBloomFilter(fileA), isB);

    return compareFiles(isA, isB);
  }

  /**
   * @return
   */
  protected static BloomFilterUtils initBloomFilter(
      final int expectedNumberOfElements) {

    return new BloomFilterUtils(expectedNumberOfElements);
  }

  /**
   * In case Serialization is asked, check if the file.ser exists : true
   * retrieve the bloom filter else create the filter and file.Ser
   * corresponding.
   * @param file source to create bloom filter
   * @return bloomFilter completed with the file
   */
  public BloomFilterUtils getBloomFilter(final File file) throws IOException {

    final File bloomFilterSer = new File(file.getAbsolutePath() + ".ser");
    // final File bloomFilterSer = new File("/tmp/" + file.getName() + ".ser");

    final BloomFilterUtils bloomFilter;
    final Stopwatch timer = Stopwatch.createStarted();

    if (bloomFilterSer.exists()) {
      // Retrieve marshalling bloom filter
      bloomFilter = BloomFilterUtils.deserializationBloomFilter(bloomFilterSer);

      LOGGER.info("Retrieve bloom filter serialized number elements "
          + bloomFilter.getAddedNumberOfElements() + " in file "
          + bloomFilterSer.getAbsolutePath());

      return bloomFilter;
    }

    // TODO
    // System.out.println("create file " + bloomFilterSer.getAbsolutePath());

    // Create new filter and marshaling
    final CompressionType zType =
        getCompressionTypeByFilename(file.getAbsolutePath());

    bloomFilter =
        buildBloomFilter(zType.createInputStream(new FileInputStream(file)));

    BloomFilterUtils.serializationBloomFilter(bloomFilterSer, bloomFilter);

    timer.stop();

    LOGGER.info("Create bloom filter serialized number elements "
        + bloomFilter.getAddedNumberOfElements() + " in file "
        + bloomFilterSer.getAbsolutePath() + " in "
        + toTimeHumanReadable(timer.elapsed(TimeUnit.MILLISECONDS)));

    return bloomFilter;
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

    checkFile(fileA, fileA.getAbsolutePath());
    checkFile(fileB, fileB.getAbsolutePath());

    // Check if try to compare the same file
    if (fileA.equals(fileB))
      throw new IOException("Try to compare the same file: " + fileA);

    return true;
  }

  private void checkAndInit(final String pathA, final String pathB) {

    checkNotNull(pathA, "File " + pathA + "doesn't exist");
    checkNotNull(pathB, "File " + pathB + "doesn't exist");

    // Set Pathfile
    if (this.pathFileA == null) {
      this.pathFileA = pathA;
      this.pathFileB = pathB;
    }
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

  @Override
  public String toString() {

    if (isUseBloomfilterAvailable())
      return getName()
          + " compares files with extensions " + getExtensionReaded()
          + " use Bloom filter with parameters: expected numbers elements "
          + getExpectedNumberOfElements() + " and false positif probability "
          + getFalsePositiveProba();

    else
      return getName()
          + " compares files with extensions " + getExtensionReaded();
  }

  public abstract int getExpectedNumberOfElements();

  public abstract double getFalsePositiveProba();

  public abstract List<String> getExtensionReaded();

  public abstract String getName();

  public abstract boolean isUseBloomfilterAvailable();

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
