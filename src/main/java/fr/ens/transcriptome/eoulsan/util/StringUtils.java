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

package fr.ens.transcriptome.eoulsan.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
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
   * @return the extension of the filename
   */
  public static String extension(final String filename) {

    if (filename == null)
      return null;

    final File f = new File(filename);
    final String shortName = f.getName();

    final int pos = shortName.lastIndexOf('.');

    if (pos == -1)
      return "";

    return filename.substring(filename.length() - (shortName.length() - pos),
        filename.length());
  }

  /**
   * Get the extension without compression extension
   * @param filename The filename
   * @return the extension without the compression extension
   */
  public static String extensionWithoutCompressionExtension(
      final String filename) {

    return extension(filenameWithoutCompressionExtension(filename));
  }

  /**
   * Get the filename without the extension.
   * @param filename The filename
   * @return the filename without the extension
   */
  public static String filenameWithoutExtension(final String filename) {

    if (filename == null)
      return null;

    final File f = new File(filename);
    final String shortName = f.getName();

    final int pos = shortName.lastIndexOf('.');

    if (pos == -1)
      return filename;

    return filename.substring(0, filename.length() - shortName.length())
        + shortName.subSequence(0, pos);
  }

  /**
   * Get the compression extension if exists.
   * @param filename The filename
   * @return the compression extension or an empty string if there is no
   *         compression extension
   */
  public static String compressionExtension(final String filename) {

    if (filename == null)
      return null;

    final String ext = extension(filename);

    if (".gz".equals(ext))
      return ext;

    if (".bz2".equals(ext))
      return ext;

    if (".zip".equals(ext))
      return ext;

    if (".deflate".equals(ext))
      return ext;

    if (".lzo".equals(ext))
      return ext;

    return "";
  }

  /**
   * Get the filename without the compression extension.
   * @param filename The filename
   * @return the filename without the compression extension
   */
  public static String filenameWithoutCompressionExtension(final String filename) {

    if (filename == null)
      return null;

    if (filename.endsWith(".gz"))
      return filename.substring(0, filename.length() - 3);

    if (filename.endsWith(".bz2"))
      return filename.substring(0, filename.length() - 4);

    // if (filename.endsWith(".zip"))
    // return filename.substring(0, filename.length() - 4);

    if (filename.endsWith(".deflate"))
      return filename.substring(0, filename.length() - 8);

    if (filename.endsWith(".lzo"))
      return filename.substring(0, filename.length() - 4);

    return filename;
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
   * @param allowEmptyFields to allow empty fields
   * @return the array with the new values
   */
  public static final String[] fastSplit(final String s, final String[] array) {

    return fastSplit(s, array, false);
  }

  /**
   * Split a string. \t is the separator character.
   * @param s the String to split
   * @param array The result array.
   * @param allowEmptyFields to allow empty fields
   * @return the array with the new values
   */
  public static final String[] fastSplit(final String s, final String[] array,
      final boolean allowEmptyFields) {

    if (array == null || s == null)
      return null;

    int lastPos = 0;
    final int len = array.length - 1;

    for (int i = 0; i < len; i++) {

      final int pos = s.indexOf("\t", lastPos);

      if (pos == -1) {
        if (allowEmptyFields) {
          while (i <= len)
            array[i++] = "";
          return array;
        }
        throw new ArrayIndexOutOfBoundsException();
      }

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

  public static final String protectGFF(final String s) {

    if (s == null)
      return null;

    final String rTmp =
        s.replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=")
            .replace("%", "\\%").replace("&", "\\&").replace(",", "\\,");

    final StringBuilder sb = new StringBuilder();

    final int len = rTmp.length();

    for (int i = 0; i < len; i++) {

      final char c = rTmp.charAt(i);
      if (c <= 32) {
        sb.append('%');
        sb.append(String.format("%02X", (int) c));
      } else
        sb.append(c);
    }

    final String r = sb.toString();
    sb.setLength(0);

    return r;
  }

  public static final String deprotectGFF(final String s) {

    if (s == null)
      return null;

    final StringBuilder sb = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final char c = s.charAt(i);

      if (c == '%') {

        if (i + 2 >= len)
          break;

        final char d1 = s.charAt(i + 1);
        final char d2 = s.charAt(i + 2);

        if (Character.isDigit(d1) && Character.isDigit(d2)) {
          sb.append((char) Integer.parseInt("" + d1 + d2, 16));

          i += 2;
          continue;
        }
      }

      sb.append(c);
    }

    return sb.toString().replace("\\,", ",").replace("\\&", "&").replace("\\%",
        "%").replace("\\=", "=").replace("\\;", ";").replace("\\\\", "\\");
  }

  /**
   * Get an array without the first element of the input array.
   * @param array input array
   * @return an array without the first element of the input array
   */
  public static final String[] arrayWithoutFirstElement(final String[] array) {

    return arrayWithoutFirstsElement(array, 1);
  }

  /**
   * Get an array without the first element of the input array.
   * @param array input array
   * @param elementsToRemove number of the first elements to remove
   * @return an array without the first element of the input array
   */
  public static final String[] arrayWithoutFirstsElement(final String[] array,
      final int elementsToRemove) {

    if (array == null)
      return null;

    if (elementsToRemove < 1 || elementsToRemove > array.length)
      return new String[0];

    final int newLen = array.length - elementsToRemove;
    final String[] result = new String[newLen];
    System.arraycopy(array, elementsToRemove, result, 0, newLen);

    return result;
  }

  /**
   * Escape a bash filename
   * @param s bash string to escape
   * @return a escaped string
   */
  public static final String bashEscaping(final String s) {

    if (s == null)
      return null;

    return s.replace("\\", "\\\\").replace(" ", "\\ ").replace("'", "\\'")
        .replace("\"", "\\\"").replace("&", "\\&").replace("!", "\\!").replace(
            "~", "\\~");
  }

  /**
   * Get the filename of an URI
   * @param s The URI in a string
   * @return the filename of the URI
   */
  public static final String getURIFilename(final String s) {

    if (s == null)
      return null;

    try {
      final URI uri = new URI(s);

      return new File(uri.getPath()).getName();
    } catch (URISyntaxException e) {

      return null;
    }

  }

  /**
   * Get a file size in a human readable way
   * @param bytes size of a file
   * @return a string with the size of the file
   */
  public static final String sizeToHumanReadable(final long bytes) {

    final double ki = 1024;
    final double mi = ki * 1024;
    final double gi = mi * 1024;
    final double ti = gi * 1024;

    if (bytes < ki)
      return String.format("%d B", bytes);

    if (bytes < mi)
      return String.format("%.2f KiB", bytes / ki);

    if (bytes < gi)
      return String.format("%.2f MiB", bytes / mi);

    if (bytes < ti)
      return String.format("%.2f GiB", bytes / gi);

    return String.format("%.2f TiB", bytes / ti);
  }

  /**
   * Test if a String starts with one of the prefix of a list in an array
   * @param s String to test
   * @param prefixes list of prefixes
   * @return true if the String starts with one of the prefix of a list in an
   *         array
   */
  public static final boolean startsWith(final String s, final String[] prefixes) {

    if (s == null || prefixes == null)
      return false;

    for (String p : prefixes)
      if (s.startsWith(p))
        return true;

    return false;
  }

  /**
   * Get the content type of a file for common file extensions
   * @param extension extension to use
   * @return the content type of an empty string if the content type was not
   *         found
   */
  public static final String getCommonContentTypeFromExtension(
      final String extension) {

    if (extension == null)
      return null;

    final String lower = extension.toLowerCase();

    if (".htm".equals(lower) || ".html".equals(lower))
      return "text/html";

    if (".xml".equals(lower))
      return "text/xml";

    if (".txt".equals(lower)
        || ".pl".equals(lower) || ".pm".equals(lower) || ".py".equals(lower)
        || ".r".equals(lower) || ".rb".equals(lower) || ".java".equals(lower))
      return "text/plain";

    if (".jpeg".equals(lower) || ".jpg".equals(lower) || ".jpe".equals(lower))
      return "image/jpeg";

    if (".jpeg".equals(lower) || ".jpg".equals(lower) || ".jpe".equals(lower))
      return "image/jpeg";

    if (".tif".equals(lower) || ".tiff".equals(lower))
      return "image/tiff";

    if (".png".equals(lower))
      return "image/png";

    if (".pdf".equals(lower))
      return "application/pdf";

    return "";
  }

  /**
   * Replace the prefix of a string.
   * @param s the string to process
   * @param oldPrefix prefix to replace
   * @param newPrefix new prefix
   * @return the string with the new prefix
   */
  public static String replacePrefix(final String s, final String oldPrefix,
      final String newPrefix) {

    if (s == null)
      return null;

    if (oldPrefix == null || newPrefix == null)
      return s;

    if (!s.startsWith(oldPrefix))
      return s;

    final int prefixLen = oldPrefix.length();

    return newPrefix + s.substring(prefixLen);
  }

}
