package fr.ens.transcriptome.eoulsan.io.comparator;

import static fr.ens.transcriptome.eoulsan.io.CompressionType.getCompressionTypeByFilename;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import fr.ens.transcriptome.eoulsan.io.CompressionType;
import fr.ens.transcriptome.eoulsan.util.BloomFilterUtils;

abstract class AbstractComparatorWithBloomFilter extends AbstractComparator {

  private double falsePositiveProba = 0.1;
  private int expectedNumberOfElements = 30000000;

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

    final InputStream isB =
        getCompressionTypeByFilename(fileB.getAbsolutePath())
            .createInputStream(new FileInputStream(fileB));

    return compareFiles(getBloomFilter(fileA, useSerializeFile), isB);
  }

  @Override
  public boolean compareFiles(InputStream isA, InputStream isB)
      throws IOException {
    return compareFiles(buildBloomFilter(isA), isB);
  }

  abstract public boolean compareFiles(BloomFilterUtils filter, InputStream is)
      throws IOException;

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
  public BloomFilterUtils getBloomFilter(final File file,
      final boolean isSerializeBloomFilter) throws IOException {

    final File bloomFilterSer = new File(file.getAbsolutePath() + ".ser");
    final BloomFilterUtils bloomFilter;

    if (isSerializeBloomFilter && bloomFilterSer.exists()) {
      // Retrieve marshalling bloom filter
      bloomFilter = BloomFilterUtils.deserializationBloomFilter(bloomFilterSer);

      return bloomFilter;
    }

    final CompressionType zType =
        getCompressionTypeByFilename(file.getAbsolutePath());

    // Create new filter
    bloomFilter =
        buildBloomFilter(zType.createInputStream(new FileInputStream(file)));

    // If need serialize bloomFilter in file 
    if (isSerializeBloomFilter) {
      BloomFilterUtils.serializationBloomFilter(bloomFilterSer, bloomFilter);
    }
    
    return bloomFilter;
  }

  public BloomFilterUtils buildBloomFilter(InputStream is) throws IOException {
    final BloomFilterUtils filter =
        initBloomFilter(getExpectedNumberOfElements());

    final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

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
        + getFalsePositiveProba();

  }

  //
  // Getter & setter
  //

  public int getExpectedNumberOfElements() {
    return expectedNumberOfElements;
  }

  public void setExpectedNumberOfElements(int expectedNumberOfElements) {
    this.expectedNumberOfElements = expectedNumberOfElements;
  }

  public void setFalsePositiveProba(double falsePositiveProba) {
    this.falsePositiveProba = falsePositiveProba;
  }

  public double getFalsePositiveProba() {
    return falsePositiveProba;
  }

}
