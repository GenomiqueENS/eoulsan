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

package fr.ens.biologie.genomique.eoulsan.util;

import java.util.Arrays;

/**
 * This class define some statistical functions.
 * @since 1.1
 * @author Laurent Jourdren
 */
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
    for (int value : values) {
      sum += value;
    }

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
    for (double value : values) {
      sum += value;
    }

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

    if (values == null) {
      throw new IllegalArgumentException("values is null");
    }

    return percentile(values, 0, values.length, p);
  }

  /**
   * Get the percentile of a array of doubles.
   * @param values The array of doubles
   * @param p the percentile to compute
   * @return the percentile
   */
  public static double percentile(final double[] values, final double p) {

    if (values == null) {
      throw new IllegalArgumentException("values is null");
    }

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
    double pos = p * ((double) length + 1) / 100;
    double fpos = Math.floor(pos);
    int intPos = (int) fpos;
    double dif = pos - fpos;
    double[] sorted = new double[length];
    System.arraycopy(values, begin, sorted, 0, length);
    Arrays.sort(sorted);

    if (pos < 1) {
      return sorted[0];
    }
    if (pos >= (double) length) {
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
    double pos = p * ((double) length + 1) / 100;
    double fpos = Math.floor(pos);
    int intPos = (int) fpos;
    double dif = pos - fpos;
    double[] sorted = new double[length];
    System.arraycopy(values, begin, sorted, 0, length);
    Arrays.sort(sorted);

    if (pos < 1) {
      return sorted[0];
    }
    if (pos >= (double) length) {
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
