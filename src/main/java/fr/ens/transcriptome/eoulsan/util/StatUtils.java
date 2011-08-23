package fr.ens.transcriptome.eoulsan.util;

import java.util.Arrays;

public final class StatUtils {

  /**
   * Get the mean of an array of integers.
   * @param values The array of integers
   * @return the median
   */
  public static double mean(final int[] values) {

    if (values == null) {
      throw new IllegalArgumentException("input value array is null");
    }

    final int len = values.length;
    int sum = 0;
    for (int i = 0; i < len; i++)
      sum += values[i];

    return (double) sum / len;
  }

  /**
   * Get the mean of an array of doubles.
   * @param values The array of doubles
   * @return the median
   */
  public static double mean(final double[] values) {

    if (values == null) {
      throw new IllegalArgumentException("input value array is null");
    }

    final int len = values.length;
    double sum = 0;
    for (int i = 0; i < len; i++)
      sum += values[i];

    return sum / len;
  }

  /**
   * Get the median of an array of integer.
   * @param values The array of integer
   * @return the median
   */
  public static double median(final int[] values) {

    return percentile(values, 50.0);
  }

  /**
   * Get the median of an array of doubles.
   * @param values The array of doubles
   * @return the median
   */
  public static double median(final double[] values) {

    return percentile(values, 50.0);
  }

  /**
   * Get the percentile of a array of integers.
   * @param values The array of integers
   * @param p the percentile to compute
   * @return the percentile
   */
  public static double percentile(final int[] values, final double p) {

    if (values == null)
      throw new IllegalArgumentException("values is null");

    return percentile(values, 0, values.length, p);
  }

  /**
   * Get the percentile of a array of doubles.
   * @param values The array of doubles
   * @param p the percentile to compute
   * @return the percentile
   */
  public static double percentile(final double[] values, final double p) {

    if (values == null)
      throw new IllegalArgumentException("values is null");

    return percentile(values, 0, values.length, p);
  }

  private static double percentile(final int[] values, final int begin,
      final int length, final double p) {

    test(values, begin, length);

    if ((p > 100) || (p <= 0)) {
      throw new IllegalArgumentException("invalid quantile value: " + p);
    }
    if (length == 0) {
      return Double.NaN;
    }
    if (length == 1) {
      return values[begin]; // always return single value for n = 1
    }
    double n = length;
    double pos = p * (n + 1) / 100;
    double fpos = Math.floor(pos);
    int intPos = (int) fpos;
    double dif = pos - fpos;
    double[] sorted = new double[length];
    System.arraycopy(values, begin, sorted, 0, length);
    Arrays.sort(sorted);

    if (pos < 1) {
      return sorted[0];
    }
    if (pos >= n) {
      return sorted[length - 1];
    }
    double lower = sorted[intPos - 1];
    double upper = sorted[intPos];
    return lower + dif * (upper - lower);
  }

  private static double percentile(final double[] values, final int begin,
      final int length, final double p) {

    test(values, begin, length);

    if ((p > 100) || (p <= 0)) {
      throw new IllegalArgumentException("invalid quantile value: " + p);
    }
    if (length == 0) {
      return Double.NaN;
    }
    if (length == 1) {
      return values[begin]; // always return single value for n = 1
    }
    double n = length;
    double pos = p * (n + 1) / 100;
    double fpos = Math.floor(pos);
    int intPos = (int) fpos;
    double dif = pos - fpos;
    double[] sorted = new double[length];
    System.arraycopy(values, begin, sorted, 0, length);
    Arrays.sort(sorted);

    if (pos < 1) {
      return sorted[0];
    }
    if (pos >= n) {
      return sorted[length - 1];
    }
    double lower = sorted[intPos - 1];
    double upper = sorted[intPos];
    return lower + dif * (upper - lower);
  }

  private static boolean test(final int[] values, final int begin,
      final int length) {

    if (values == null) {
      throw new IllegalArgumentException("input value array is null");
    }

    if (begin < 0) {
      throw new IllegalArgumentException("start position cannot be negative");
    }

    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative");
    }

    if (begin + length > values.length) {
      throw new IllegalArgumentException("begin + length > values.length");
    }

    if (length == 0) {
      return false;
    }

    return true;
  }

  private static boolean test(final double[] values, final int begin,
      final int length) {

    if (values == null) {
      throw new IllegalArgumentException("input value array is null");
    }

    if (begin < 0) {
      throw new IllegalArgumentException("start position cannot be negative");
    }

    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative");
    }

    if (begin + length > values.length) {
      throw new IllegalArgumentException("begin + length > values.length");
    }

    if (length == 0) {
      return false;
    }

    return true;
  }

}
