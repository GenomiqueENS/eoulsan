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

import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByFilename;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

/**
 * This abstract class define methods to compare files with use BloomFilter.
 * @since 2.0
 * @author Sandrine Perrin
 */
public abstract class AbstractComparatorWithBloomFilter extends
    AbstractComparator {

  // Limited create serialize bloomfilter file for size file inferior to
  // size of serialize bloomfilter file 27369839 bytes with default parameters
  private static final long SIZE_MINIMAL_CREATE_SERIALIZE_FILE = 40000000;

  private double falsePositiveProbability = 0.1;
  private int expectedNumberOfElements = 30000000;
  private boolean useSerializeFile = false;

  @Override
  public boolean compareFiles(final File fileA, final File fileB)
      throws FileNotFoundException, IOException {

    // Check input files
    if (!checkFiles(fileA, fileB) && checkFileSize()) {
      return false;
    }

    // Check path file (abstract and symbolic) is the same
    if (fileA.getCanonicalFile().equals(fileB.getCanonicalFile())) {
      return true;
    }

    try (InputStream isB = new FileInputStream(fileB)) {

      return compareFiles(getBloomFilter(fileA),
          getCompressionTypeByFilename(fileB.getAbsolutePath())
              .createInputStream(isB));
    }
  }

  @Override
  public boolean compareFiles(InputStream isA, InputStream isB)
      throws IOException {
    return compareFiles(buildBloomFilter(isA), isB);
  }

  /**
   * Compare two files no ordered, check if they are the same contents.
   * @param filter from BloomFilterUtils represented the first file
   * @param is the path to the second file,
   * @return boolean true if files are same.
   * @throws IOException if an error occurs while comparing the files.
   */
  abstract public boolean compareFiles(BloomFilterUtils filter, InputStream is)
      throws IOException;

  /**
   * Initialize BloomFilter with the expected number of elements.
   * @param expectedNumberOfElements expected number of elements
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

    if (this.useSerializeFile && bloomFilterSer.exists()) {
      // Retrieve marshalling bloom filter
      return BloomFilterUtils.deserializationBloomFilter(bloomFilterSer);
    }

    final CompressionType zType =
        getCompressionTypeByFilename(file.getAbsolutePath());

    // Create new filter
    try (InputStream is = new FileInputStream(file);) {
      final BloomFilterUtils bloomFilter =
          buildBloomFilter(zType.createInputStream(is));

      // If need serialize bloomFilter in file only for file
      if (isCreateSerializeFile(file, zType)) {
        BloomFilterUtils.serializationBloomFilter(bloomFilterSer, bloomFilter);
      }

      return bloomFilter;
    }
  }

  /**
   * Build BloomFilter represented the input stream.
   * @param is the input stream source
   * @return BloomFilter corresponding to the input stream
   * @throws IOException
   */
  protected BloomFilterUtils buildBloomFilter(InputStream is)
      throws IOException {
    final BloomFilterUtils filter =
        initBloomFilter(getExpectedNumberOfElements());

    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(is, Globals.DEFAULT_CHARSET));

    String line = null;

    // Read the first file and store hashcodes
    while ((line = reader.readLine()) != null) {
      filter.put(line);
    }

    reader.close();
    return filter;
  }

  @Override
  public String toString() {

    return getName()
        + " compares files with extensions " + getExtensions()
        + " use Bloom filter with parameters: expected numbers elements "
        + getExpectedNumberOfElements() + " and false positif probability "
        + getFalsePositiveProbability();

  }

  /**
   * Define if serialization bloomfilter file is necessary according parameter
   * useSerializeFile and size file.
   * @param file file source for build bloomfilter
   * @param zType compression type of file
   * @return true if creating serialization file is necessary
   */
  private boolean isCreateSerializeFile(final File file,
      final CompressionType zType) {

    // No serialize file require
    if (!this.useSerializeFile) {
      return false;
    }

    // Compressed file and serialize require
    if (zType != CompressionType.NONE) {
      return true;
    }

    // File size in bytes
    final long fileSize = file.length();
    // Check to choice
    return fileSize > SIZE_MINIMAL_CREATE_SERIALIZE_FILE;
  }

  //
  // Getters & setters
  //

  public boolean isUseSerializeFile() {
    return useSerializeFile;
  }

  public void setUseSerializeFile(boolean useSerializeFile) {
    this.useSerializeFile = useSerializeFile;
  }

  protected int getExpectedNumberOfElements() {
    return expectedNumberOfElements;
  }

  protected void setExpectedNumberOfElements(int expectedNumberOfElements) {
    this.expectedNumberOfElements = expectedNumberOfElements;
  }

  protected void setFalsePositiveProbability(double falsePositiveProba) {
    this.falsePositiveProbability = falsePositiveProba;
  }

  protected double getFalsePositiveProbability() {
    return falsePositiveProbability;
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param useSerializeFile true if it needed to save BloomFilter in file with
   *          extension '.ser'
   */
  public AbstractComparatorWithBloomFilter(final boolean useSerializeFile) {
    this.useSerializeFile = useSerializeFile;
  }
}
