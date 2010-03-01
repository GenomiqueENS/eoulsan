/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.eoulsan.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for strings.
 * @author Laurent Jourdren
 */
public final class StringUtils {

  /**
   * Get the basename of the filename.
   * @param filename The filename
   * @return the basename of the file
   */
  public static String basename(final String filename) {

    return basename(filename, true);
  }

  /**
   * Get the basename of the filename.
   * @param filename The filename
   * @param withoutCompressedExtension true if the compression extension must be
   *          removed
   * @return the basename of the file
   */
  public static String basename(final String filename,
      final boolean withoutCompressedExtension) {

    if (filename == null)
      return null;

    final String myFilename;

    if (withoutCompressedExtension)
      myFilename = removeCompressedExtensionFromFilename(filename);
    else
      myFilename = filename;

    final File f = new File(myFilename);
    final String shortName = f.getName();

    final int pos = shortName.indexOf('.');

    if (pos == -1)
      return myFilename;

    return myFilename.substring(0, myFilename.length()
        - (shortName.length() - pos));
  }

  /**
   * Get the extension of a filename.
   * @param filename The filename
   * @return the exstension of the filename
   */
  public static String extension(String filename) {

    if (filename == null)
      return null;

    final File f = new File(filename);
    final String shortName = f.getName();

    final int pos = shortName.indexOf('.');

    if (pos == -1)
      return "";

    return filename.substring(filename.length() - (shortName.length() - pos),
        filename.length());
  }

  /**
   * Remove non alpha char at the end of String.
   * @param s String to handle
   * @return the string without the last non end of string
   */
  public static final String removeNonAlphaAtEndOfString(final String s) {

    if (s == null)
      return null;

    int len = s.length();
    if (len == 0)
      return s;

    char c = s.charAt(len - 1);
    if (!Character.isLetter(c))
      return s.substring(0, len - 1);

    return s;
  }

  /**
   * Convert a number of milliseconds into a human reading string.
   * @param time time in ms
   * @return a the time in ms
   */
  public static final String toTimeHumanReadable(final long time) {

    long min = time / (60 * 1000);
    long minRest = time % (60 * 1000);
    long sec = minRest / 1000;

    long mili = minRest % 1000;

    return String.format("%02d:%02d.%03d", min, sec, mili);
  }

  /**
   * Split a string. \t is the separator character.
   * @param s the String to split
   * @param array The result array.
   * @return the array with the new values
   */
  public static final List<String> fastSplit(final String s,
      final List<String> list) {

    if (s == null)
      return null;

    List<String> result;

    if (list == null)
      result = new ArrayList<String>();
    else
      result = list;

    result.clear();
    int lastPos = 0;
    int pos = -1;

    while ((pos = s.indexOf("\t", lastPos)) != -1) {

      result.add(s.substring(lastPos, pos));
      lastPos = pos + 1;
    }

    result.add(s.substring(lastPos, s.length()));

    return result;
  }

  /**
   * Split a string. \t is the separator character.
   * @param s the String to split
   * @param array The result array.
   * @return the array with the new values
   */
  public static final String[] fastSplit(final String s, final String[] array) {

    if (array == null || s == null)
      return null;

    int lastPos = 0;
    final int len = array.length - 1;

    for (int i = 0; i < len; i++) {

      final int pos = s.indexOf("\t", lastPos);

      if (pos == -1)
        throw new ArrayIndexOutOfBoundsException();
      array[i] = s.substring(lastPos, pos);
      lastPos = pos + 1;
    }
    array[len] = s.substring(lastPos, s.length());

    return array;
  }

  /**
   * Get the content of a line without the first field. Tabulation is the
   * separator
   * @param s String to parse
   * @return a String without the first field of the string
   */
  public static final String subStringAfterFirstTab(final String s) {

    if (s == null)
      return null;

    final int indexFirstTab = s.indexOf('\t');

    if (indexFirstTab == -1)
      return s;

    return s.substring(indexFirstTab + 1);
  }

  /**
   * Get the first field of a line
   * @param s String to parse
   * @return a String with the first field of the string
   */
  public static final String subStringBeforeFirstTab(final String s) {

    if (s == null)
      return null;

    final int indexFirstTab = s.indexOf('\t');

    if (indexFirstTab == -1)
      return s;

    return s.substring(0, indexFirstTab);
  }

  /**
   * Get the current date in an easy sorted format (e.g. 20100225151635)
   * @return the current date formated in a string
   */
  public static final String currentDateTimeToEasySortedDateTime() {

    return toEasySortedDateTime(new Date(System.currentTimeMillis()));
  }

  /**
   * Get the date in an easy sorted format (e.g. 20100225151635)
   * @param date date to format
   * @return a formated date in a string
   */
  public static final String toEasySortedDateTime(final Date date) {

    if (date == null)
      return null;

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);

    return String.format("%04d%02d%02d%02d%02d%02d", cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal
            .get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal
            .get(Calendar.SECOND));
  }

  /**
   * Remove the compression extension of the filename.
   * @param filename Filename to use
   * @return the filename without the compressed extension if needed
   */
  public static final String removeCompressedExtensionFromFilename(
      final String filename) {

    if (filename == null)
      return null;

    if (filename.endsWith(".gz"))
      return filename.substring(0, filename.length() - 3);
    if (filename.endsWith(".bz2"))
      return filename.substring(0, filename.length() - 4);
    if (filename.endsWith(".zip"))
      return filename.substring(0, filename.length() - 4);

    return filename;
  }

}
