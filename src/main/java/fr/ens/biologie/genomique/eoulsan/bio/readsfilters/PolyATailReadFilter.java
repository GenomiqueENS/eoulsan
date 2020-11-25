package fr.ens.biologie.genomique.eoulsan.bio.readsfilters;

import static java.util.Objects.requireNonNull;

import fr.ens.biologie.genomique.eoulsan.EoulsanException;
import fr.ens.biologie.genomique.eoulsan.bio.ReadSequence;

/**
 * This class define a read filter that identify polyA and polyT tails.
 * @since 2.4
 * @author Laurent Jourdren
 */
public class PolyATailReadFilter extends AbstractReadFilter {

  public static final String FILTER_NAME = "polyatail";

  private static final String POLYA = "polyA";
  private static final String POLYT = "polyT";
  private static final String AMBIGUOUS = "ambiguous";
  private static final String INVALID = "invalid";

  private double maximalErrorRate = 0.1;
  private int minimalLength = 10;
  private int minStatLength = 5;

  @Override
  public String getName() {

    return FILTER_NAME;
  }

  @Override
  public String getDescription() {

    return "PolyA identifier";
  }

  @Override
  public void setParameter(final String key, final String value)
      throws EoulsanException {

    switch (key) {

    case "maximal.error.rate":
      try {
        this.maximalErrorRate = Double.parseDouble(value);

        if (maximalErrorRate < 0.0 || maximalErrorRate > 1.0) {
          throw new EoulsanException(getName()
              + "." + key
              + " argument must be greater than 0 and lower than 1");
        }

      } catch (NumberFormatException e) {
        throw new EoulsanException(
            "Invalid value for " + getName() + "." + key + ": " + value);
      }
      break;

    case "minimal.length":
      try {
        this.minimalLength = Integer.parseInt(value);

        if (this.minimalLength < 0) {
          throw new EoulsanException("value for "
              + getName() + "." + key + "cannot be lower than zero: " + value);
        }

      } catch (NumberFormatException e) {
        throw new EoulsanException(
            "Invalid value for " + getName() + "." + key + ": " + value);
      }
      break;

    case "minimal.length.for.error.rate.computation":
      try {
        this.minStatLength = Integer.parseInt(value);

        if (this.minStatLength < 0) {
          throw new EoulsanException("value for "
              + getName() + "." + key + "cannot be lower than zero: " + value);
        }

      } catch (NumberFormatException e) {
        throw new EoulsanException(
            "Invalid value for " + getName() + "." + key + ": " + value);
      }
      break;

    default:
      throw new EoulsanException(
          "Unknown parameter for " + getName() + " read filter: " + key);
    }

  }

  @Override
  public boolean accept(ReadSequence read) {

    String sequence = read.getSequence();
    int polyAlength =
        polyATailLength(sequence, this.minStatLength, this.maximalErrorRate);
    int polyTlength =
        polyTTailLength(sequence, this.minStatLength, this.maximalErrorRate);

    String tailType;

    if (polyAlength < this.minimalLength && polyTlength < this.minimalLength) {
      tailType = INVALID;
    } else {

      if (polyAlength >= this.minimalLength
          && polyTlength >= this.minimalLength) {

        if (polyAlength == polyTlength) {
          tailType = AMBIGUOUS;
        } else {

          int max = Math.max(polyAlength, polyTlength);
          tailType = polyAlength == max ? POLYA : POLYT;
        }

      } else {
        tailType = polyAlength >= this.minimalLength ? POLYA : POLYT;
      }

    }

    read.setName(read.getName() + " tail_type=" + tailType);

    return true;
  }

  //
  // PolyA tail finding methods
  //

  static int polyTTailLength(String sequence, int minLen,
      double maximalErrorRate) {

    requireNonNull(sequence);

    if (minLen < 0) {
      throw new IllegalArgumentException(
          "minLen argument cannot be lower than 0");
    }

    if (maximalErrorRate < 0.0 || maximalErrorRate > 1.0) {
      throw new IllegalArgumentException(
          "maximalErrorRate argument must be greater than 0 and lower than 1");
    }

    final int len = sequence.length();
    final float threshold = 1.0f - ((float) maximalErrorRate);

    int countA = 0;
    int lastA = 0;
    int pos = 0;

    while (pos < len && (pos < minLen || ((float) countA / pos) > threshold)) {

      if (sequence.charAt(pos) == 'T') {
        countA++;
        lastA = pos + 1;
      }
      pos++;
    }

    return lastA;
  }

  static int polyATailLength(String sequence, int minLen, double threshold) {

    requireNonNull(sequence);

    if (minLen < 0) {
      throw new IllegalArgumentException(
          "minLen argument cannot be lower than 0");
    }

    if (threshold < 0.0 || threshold > 1.0) {
      throw new IllegalArgumentException(
          "threshold argument must be greater than 0 and lower than 1");
    }

    final int len = sequence.length();
    final float thresholdFloat = 1.0f - ((float) threshold);

    int countA = 0;
    int pos = len - 1;
    int lastA = 0;

    int count = 0;

    while (pos >= 0
        && (count < minLen || ((float) countA / count) > thresholdFloat)) {

      if (sequence.charAt(pos) == 'A') {
        countA++;
        lastA = len - pos;
      }
      pos--;
      count++;
    }

    return lastA;
  }

}
