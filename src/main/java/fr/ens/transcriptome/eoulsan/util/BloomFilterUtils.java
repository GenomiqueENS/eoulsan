package fr.ens.transcriptome.eoulsan.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * This class define Bloom filters utils.
 * @author Sandrine Perrin
 * @since 2.0
 */
public class BloomFilterUtils implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final double FALSE_POSITIVE_PROBABILITY_DEFAULT = 0.03;

  private final BloomFilter<String> bf;
  private int addedNumberOfElements;
  private final int expectedNumberOfElements;
  private final double falsePositiveProbability;

  public void put(final String element) {
    this.bf.put(element);
    this.addedNumberOfElements++;
  }

  public boolean mightContain(final String element) {
    return this.bf.mightContain(element);
  }

  /**
   * Build or retrieve a bloomFilterUtils from a file used .
   * @param fileSer filename to serialization
   * @return BloomFilter completed
   * @throws IOException if an error occurs during deserialization
   */
  public static BloomFilterUtils deserializationBloomFilter(final File fileSer)
      throws IOException {

    ObjectInputStream ois = null;
    BloomFilterUtils bloomFilter = null;

    try {

      ois = new ObjectInputStream(new FileInputStream(fileSer));
      bloomFilter = (BloomFilterUtils) ois.readObject();
      ois.close();

    } catch (Exception e) {
      throw new IOException(e);
    }
    return bloomFilter;
  }

  /**
   * Build a serialization file with the instance of bloomFilterUtils
   * @param fileSer filename to serialization
   * @param bloomFilter bloomFilter to serialization
   * @throws IOException if an error occurs during serialization
   */
  public static void serializationBloomFilter(final File fileSer,
      final BloomFilterUtils bloomFilter) throws IOException {

    if (bloomFilter == null) {
      throw new IOException("Bloom filter not exists");
    }

    // Serialization BloomFilter
    ObjectOutputStream oos;
    try {
      oos = new ObjectOutputStream(new FileOutputStream(fileSer));
      oos.writeObject(bloomFilter);
      oos.flush();
      oos.close();

    } catch (FileNotFoundException e) {
      throw new IOException(e);
    }

  }

  /**
   * Compare parameters used to create bloom filter
   * @param that bloom filter to compare
   * @return same parameters true else false
   */
  public boolean sameConfigurationFilter(final BloomFilterUtils that) {
    return getExpectedNumberOfElements() == that.getExpectedNumberOfElements()
        && getFalsePositiveProbability() == that.getFalsePositiveProbability();
  }

  //
  // Getter and Setter
  //
  /** Return the bloom filter */
  public BloomFilter<String> getBf() {
    return this.bf;
  }

  /** Return number of elements added in bloom filter */
  public int getAddedNumberOfElements() {
    return this.addedNumberOfElements;
  }

  /** Return parameter used to create bloom filter: expected number element */
  public int getExpectedNumberOfElements() {
    return this.expectedNumberOfElements;
  }

  /**
   * Return parameter used to create bloom filter: false positive probability
   * expected
   */
  public double getFalsePositiveProbability() {
    return this.falsePositiveProbability;
  }

  @Override
  public String toString() {

    return "Bloom filter features" + "\n\tfalse positive probability "
        + getFalsePositiveProbability() + "%" + "\n\tnumber elements expected " + getExpectedNumberOfElements() + "\n\tnumber elements added " + getAddedNumberOfElements();
  }

  //
  // Constructor
  //

  /**
   * Public constructor
   * @param expectedNumberOfElements parameter to create bloom filter
   */
  public BloomFilterUtils(final int expectedNumberOfElements) {
    this(expectedNumberOfElements, FALSE_POSITIVE_PROBABILITY_DEFAULT);
  }

  /**
   * Public constructor
   * @param expectedNumberOfElements parameter to create bloom filter, must be
   *          positive
   * @param falsePositiveProbability parameter to create bloom filter, must be
   *          between 0 and 100%
   */
  public BloomFilterUtils(final int expectedNumberOfElements,
      final double falsePositiveProbability) {

    // Check parameter
    if (expectedNumberOfElements <= 0) {
      throw new IllegalArgumentException(
          "Parameter 'expectedNumberOfElements' to create bloom filter invalid "
              + expectedNumberOfElements);
    }

    if (falsePositiveProbability <= 0 || falsePositiveProbability >= 1.0) {
      throw new IllegalArgumentException(
          "Parameter 'falsePositiveProbability' to create bloom filter invalid "
              + falsePositiveProbability);
    }

    this.addedNumberOfElements = 0;
    this.expectedNumberOfElements = expectedNumberOfElements;
    this.falsePositiveProbability = falsePositiveProbability;

    this.bf = BloomFilter.create(new Funnel<String>() {

      private static final long serialVersionUID = 1L;

      @Override
      public void funnel(final String from, final PrimitiveSink into) {
        into.putString(from, StandardCharsets.UTF_8);
      }

    }, expectedNumberOfElements, falsePositiveProbability);

  }

}
