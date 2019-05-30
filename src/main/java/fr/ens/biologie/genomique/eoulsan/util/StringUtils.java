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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for Strings.
 * @since 1.0
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

    if (filename == null) {
      return null;
    }

    final String myFilename;

    if (withoutCompressedExtension) {
      myFilename = removeCompressedExtensionFromFilename(filename);
    } else {
      myFilename = filename;
    }

    final File f = new File(myFilename);
    final String shortName = f.getName();

    final int pos = shortName.indexOf('.');

    if (pos == -1) {
      return myFilename;
    }

    return myFilename.substring(0,
        myFilename.length() - (shortName.length() - pos));
  }

  /**
   * Get the extension of a filename.
   * @param filename The filename
   * @return the extension of the filename
   */
  public static String extension(final String filename) {

    if (filename == null) {
      return null;
    }

    final File f = new File(filename);
    final String shortName = f.getName();

    final int pos = shortName.lastIndexOf('.');

    if (pos == -1) {
      return "";
    }

    return filename.substring(filename.length() - (shortName.length() - pos));
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

    if (filename == null) {
      return null;
    }

    final File f = new File(filename);
    final String shortName = f.getName();

    final int pos = shortName.lastIndexOf('.');

    if (pos == -1) {
      return filename;
    }

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

    if (filename == null) {
      return null;
    }

    final String ext = extension(filename);

    if (".gz".equals(ext)) {
      return ext;
    }

    if (".bz2".equals(ext)) {
      return ext;
    }

    if (".zip".equals(ext)) {
      return ext;
    }

    if (".deflate".equals(ext)) {
      return ext;
    }

    if (".lzo".equals(ext)) {
      return ext;
    }

    return "";
  }

  /**
   * Get the filename without the compression extension.
   * @param filename The filename
   * @return the filename without the compression extension
   */
  public static String filenameWithoutCompressionExtension(
      final String filename) {

    if (filename == null) {
      return null;
    }

    if (filename.endsWith(".gz")) {
      return filename.substring(0, filename.length() - 3);
    }

    if (filename.endsWith(".bz2")) {
      return filename.substring(0, filename.length() - 4);
    }

    // if (filename.endsWith(".zip"))
    // return filename.substring(0, filename.length() - 4);

    if (filename.endsWith(".deflate")) {
      return filename.substring(0, filename.length() - 8);
    }

    if (filename.endsWith(".lzo")) {
      return filename.substring(0, filename.length() - 4);
    }

    return filename;
  }

  /**
   * Remove non alpha char at the end of String.
   * @param s String to handle
   * @return the string without the last non end of string
   */
  public static String removeNonAlphaAtEndOfString(final String s) {

    if (s == null) {
      return null;
    }

    int len = s.length();
    if (len == 0) {
      return s;
    }

    char c = s.charAt(len - 1);
    if (!Character.isLetter(c)) {
      return s.substring(0, len - 1);
    }

    return s;
  }

  /**
   * Convert a number of milliseconds into a human reading string.
   * @param time time in ms
   * @return a the time in ms
   */
  public static String toTimeHumanReadable(final long time) {

    long hour = time / (60 * 60 * 1000);
    long hourRest = time % (60 * 60 * 1000);

    long min = hourRest / (60 * 1000);
    long minRest = hourRest % (60 * 1000);

    long sec = minRest / 1000;

    long mili = minRest % 1000;

    return String.format("%02d:%02d:%02d.%03d", hour, min, sec, mili);
  }

  /**
   * Convert a number of milliseconds into a human reading string.
   * @param millisSinceEpoch time in ms
   * @return a string with the compact time
   */
  public static String toCompactTime(final long millisSinceEpoch) {

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);
    cal.setTime(new Date(millisSinceEpoch));

    return String.format("%04d%02d%02d-%02d%02d%02d", cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
        cal.get(Calendar.SECOND));
  }

  /**
   * Split a string. \t is the separator character.
   * @param s the String to split
   * @param list The result list.
   * @return the array with the new values
   */
  public static List<String> fastSplit(final String s,
      final List<String> list) {

    if (s == null) {
      return null;
    }

    List<String> result;

    if (list == null) {
      result = new ArrayList<>();
    } else {
      result = list;
    }

    result.clear();
    int lastPos = 0;
    int pos = -1;

    while ((pos = s.indexOf("\t", lastPos)) != -1) {

      result.add(s.substring(lastPos, pos));
      lastPos = pos + 1;
    }

    result.add(s.substring(lastPos));

    return result;
  }

  /**
   * Split a string. \t is the separator character.
   * @param s the String to split
   * @param array The result array.
   * @return the array with the new values
   */
  public static String[] fastSplit(final String s, final String[] array) {

    return fastSplit(s, array, false);
  }

  /**
   * Split a string. \t is the separator character.
   * @param s the String to split
   * @param array The result array.
   * @param allowEmptyFields to allow empty fields
   * @return the array with the new values
   */
  public static String[] fastSplit(final String s, final String[] array,
      final boolean allowEmptyFields) {

    if (array == null || s == null) {
      return null;
    }

    int lastPos = 0;
    final int len = array.length - 1;

    for (int i = 0; i < len; i++) {

      final int pos = s.indexOf("\t", lastPos);

      if (pos == -1) {
        if (allowEmptyFields) {
          while (i <= len) {
            array[i++] = "";
          }
          return array;
        }
        throw new ArrayIndexOutOfBoundsException();
      }

      array[i] = s.substring(lastPos, pos);
      lastPos = pos + 1;
    }
    array[len] = s.substring(lastPos);

    return array;
  }

  /**
   * Get the content of a line without the first field. Tabulation is the
   * separator
   * @param s String to parse
   * @return a String without the first field of the string
   */
  public static String subStringAfterFirstTab(final String s) {

    if (s == null) {
      return null;
    }

    final int indexFirstTab = s.indexOf('\t');

    if (indexFirstTab == -1) {
      return s;
    }

    return s.substring(indexFirstTab + 1);
  }

  /**
   * Get the first field of a line
   * @param s String to parse
   * @return a String with the first field of the string
   */
  public static String subStringBeforeFirstTab(final String s) {

    if (s == null) {
      return null;
    }

    final int indexFirstTab = s.indexOf('\t');

    if (indexFirstTab == -1) {
      return s;
    }

    return s.substring(0, indexFirstTab);
  }

  /**
   * Get the current date in an easy sorted format (e.g. 20100225151635)
   * @return the current date formatted in a string
   */
  public static String currentDateTimeToEasySortedDateTime() {

    return toEasySortedDateTime(new Date(System.currentTimeMillis()));
  }

  /**
   * Get the date in an easy sorted format (e.g. 20100225151635)
   * @param date date to format
   * @return a formatted date in a string
   */
  public static String toEasySortedDateTime(final Date date) {

    if (date == null) {
      return null;
    }

    final Calendar cal = Calendar.getInstance(Locale.ENGLISH);

    return String.format("%04d%02d%02d%02d%02d%02d", cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE),
        cal.get(Calendar.SECOND));
  }

  /**
   * Remove the compression extension of the filename.
   * @param filename Filename to use
   * @return the filename without the compressed extension if needed
   */
  public static String removeCompressedExtensionFromFilename(
      final String filename) {

    if (filename == null) {
      return null;
    }

    if (filename.endsWith(".gz")) {
      return filename.substring(0, filename.length() - 3);
    }
    if (filename.endsWith(".bz2")) {
      return filename.substring(0, filename.length() - 4);
    }
    if (filename.endsWith(".zip")) {
      return filename.substring(0, filename.length() - 4);
    }

    return filename;
  }

  public static String protectGFF(final String s) {

    if (s == null) {
      return null;
    }

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
      } else {
        sb.append(c);
      }
    }

    final String r = sb.toString();
    sb.setLength(0);

    return r;
  }

  public static String deProtectGFF(final String s) {

    if (s == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final char c = s.charAt(i);

      if (c == '%') {

        if (i + 2 >= len) {
          break;
        }

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

    return sb.toString().replace("\\,", ",").replace("\\&", "&")
        .replace("\\%", "%").replace("\\=", "=").replace("\\;", ";")
        .replace("\\\\", "\\");
  }

  /**
   * Get an array without the first element of the input array.
   * @param array input array
   * @return an array without the first element of the input array
   */
  public static String[] arrayWithoutFirstElement(final String[] array) {

    return arrayWithoutFirstsElement(array, 1);
  }

  /**
   * Get an array without the first element of the input array.
   * @param array input array
   * @param elementsToRemove number of the first elements to remove
   * @return an array without the first element of the input array
   */
  public static String[] arrayWithoutFirstsElement(final String[] array,
      final int elementsToRemove) {

    if (array == null) {
      return null;
    }

    if (elementsToRemove < 1) {
      return array;
    }

    if (elementsToRemove > array.length) {
      return new String[0];
    }

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
  public static String bashEscaping(final String s) {

    if (s == null) {
      return null;
    }

    return s.replace("\\", "\\\\").replace(" ", "\\ ").replace("'", "\\'")
        .replace("\"", "\\\"").replace("&", "\\&").replace("!", "\\!")
        .replace("~", "\\~");
  }

  /**
   * Get the filename of an URI
   * @param s The URI in a string
   * @return the filename of the URI
   */
  public static String getURIFilename(final String s) {

    if (s == null) {
      return null;
    }

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
  public static String sizeToHumanReadable(final long bytes) {

    final double ki = 1024;
    final double mi = ki * 1024;
    final double gi = mi * 1024;
    final double ti = gi * 1024;

    if (bytes < ki) {
      return String.format("%d B", bytes);
    }

    if (bytes < mi) {
      return String.format("%.2f KiB", bytes / ki);
    }

    if (bytes < gi) {
      return String.format("%.2f MiB", bytes / mi);
    }

    if (bytes < ti) {
      return String.format("%.2f GiB", bytes / gi);
    }

    return String.format("%.2f TiB", bytes / ti);
  }

  /**
   * Test if a String starts with one of the prefix of a list in an array
   * @param s String to test
   * @param prefixes list of prefixes
   * @return true if the String starts with one of the prefix of a list in an
   *         array
   */
  public static boolean startsWith(final String s, final String[] prefixes) {

    if (s == null || prefixes == null) {
      return false;
    }

    for (String p : prefixes) {
      if (s.startsWith(p)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Get the content type of a file for common file extensions
   * @param extension extension to use
   * @return the content type of an empty string if the content type was not
   *         found
   */
  public static String getCommonContentTypeFromExtension(
      final String extension) {

    if (extension == null) {
      return null;
    }

    switch (extension.toLowerCase()) {

    case ".htm":
    case ".html":
      return "text/html";

    case ".xml":
      return "text/xml";

    case ".txt":
    case ".pl":
    case ".pm":
    case ".py":
    case ".r":
    case ".rb":
    case ".java":
      return "text/plain";

    case ".jpeg":
    case ".jpg":
    case ".jpe":
      return "image/jpeg";

    case ".tif":
    case ".tiff":
      return "image/tiff";

    case ".png":
      return "image/png";

    case ".pdf":
      return "application/pdf";

    default:
      return "";
    }
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

    if (s == null) {
      return null;
    }

    if (oldPrefix == null || newPrefix == null) {
      return s;
    }

    if (!s.startsWith(oldPrefix)) {
      return s;
    }

    final int prefixLen = oldPrefix.length();

    return newPrefix + s.substring(prefixLen);
  }

  /**
   * Serialize a collection of strings in a string.
   * @param strings strings to serialize
   * @return a String with all strings serialized
   */
  public static String serializeStringArray(final Collection<String> strings) {

    if (strings == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();
    sb.append('[');

    boolean first = true;

    for (String s : strings) {
      if (first) {
        first = false;
      } else {
        sb.append(',');
      }

      if (s != null) {
        sb.append(s.replace("\\", "\\\\").replace(",", "\\,"));
      }
    }

    sb.append(']');

    return sb.toString();
  }

  /**
   * Deserialize a string to a list of strings.
   * @param serializedString string to deserialize
   * @return a list of string
   */
  public static List<String> deserializeStringArray(
      final String serializedString) {

    if (serializedString == null) {
      return null;
    }

    String s = serializedString.trim();

    if (s.charAt(0) != '[' || s.charAt(s.length() - 1) != ']') {
      return Collections.singletonList(serializedString);
    }

    s = s.substring(1, s.length() - 1);

    final List<String> result = new ArrayList<>();
    int last = 0;
    boolean escapeNext = false;

    for (int i = 0; i < s.length(); i++) {

      if (escapeNext) {
        escapeNext = false;
        continue;
      }

      if (s.charAt(i) == '\\') {
        escapeNext = true;
        continue;
      }

      if (s.charAt(i) == ',') {
        result.add(
            s.substring(last, i).replace("\\\\", "\\").replace("\\,", ","));
        last = i + 1;
      }
    }

    result.add(s.substring(last).replace("\\\\", "\\").replace("\\,", ","));

    return result;
  }

  /**
   * Convert 0-15 integer number to a letter
   * @param i the integer to convert
   * @return a letter as a char
   */
  public static char toLetter(final int i) {

    if (i < 0) {
      return '-';
    }

    if (i > 25) {
      return '-';
    }

    return (char) (i + 97);
  }

  /**
   * Trim a string
   * @param s the string to trim
   * @return null if the parameter is null or a trimmed string
   */
  public static String trim(final String s) {

    return s == null ? null : s.trim();
  }

  /**
   * Join elements of a collection of strings into a string.
   * @param collection collection of strings to join
   * @param separator separator to use
   */
  public static String join(final Collection<String> collection,
      final String separator) {

    if (collection == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    boolean first = true;
    for (String s : collection) {

      if (!first && separator != null) {
        sb.append(separator);
      }

      if (first) {
        first = false;
      }

      sb.append(s);
    }

    return sb.toString();
  }

  /**
   * Join elements of an array of strings into a string.
   * @param array array of strings to join
   * @param separator separator to use
   */
  public static String join(final String[] array, final String separator) {

    if (array == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < array.length; i++) {

      if (i > 0 && separator != null) {
        sb.append(separator);
      }
      sb.append(array[i]);
    }

    return sb.toString();
  }

  /**
   * Join elements of an array of objects into a string.
   * @param array array of objects to join
   * @param separator separator to use
   */
  public static String join(final Object[] array, final String separator) {

    if (array == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < array.length; i++) {

      if (i > 0 && separator != null) {
        sb.append(separator);
      }
      sb.append(array[i]);
    }

    return sb.toString();
  }

  /**
   * Split a string in substring of same length
   * @param s String to split
   * @param length length of the output strings
   * @return a Iterable object
   */
  public static Iterable<String> splitStringIterator(final String s,
      final int length) {

    if (s == null || length < 1) {
      return null;
    }

    return new IterableString() {

      int pos = 0;
      final int len = s.length();

      @Override
      public boolean hasNext() {

        return this.pos < this.len;
      }

      @Override
      public String next() {

        final int endPos =
            (this.pos + length) > this.len ? this.len : this.pos + length;

        final String result = s.substring(this.pos, endPos);

        this.pos += length;

        return result;
      }

      @Override
      public void remove() {
      }

      @Override
      public Iterator<String> iterator() {

        return this;
      }
    };

  }

  private interface IterableString extends Iterable<String>, Iterator<String> {
  }

  /**
   * Test if a string is null or empty
   * @param s string to test
   * @return true if the input string is null or empty
   */
  public static boolean isNullOrEmpty(final String s) {

    return s == null || s.isEmpty();
  }

  /**
   * Split a shell command line.
   * @param commandline the command to parse
   * @return a list with the command line arguments
   */
  public static List<String> splitShellCommandLine(final String commandline) {

    if (commandline == null) {
      return null;
    }

    final String s = commandline.trim();

    final List<String> result = new ArrayList<>();

    final StringBuilder sb = new StringBuilder();
    boolean escape = false;
    boolean inArgument = false;
    char quote = ' ';

    for (int i = 0; i < s.length(); i++) {

      final char c = s.charAt(i);

      if (escape) {

        if (c == '\"') {
          sb.append(c);
        }

        escape = false;
        continue;
      }

      if (c == '\\') {
        escape = true;
        continue;
      }

      if ((c == '"' || c == '\'') && !inArgument) {
        quote = c;
        inArgument = true;
        continue;
      }

      if ((c == ' ' && !inArgument) || (c == quote && inArgument)) {

        if (inArgument) {
          result.add(sb.toString());
        } else {

          String s2 = sb.toString().trim();
          if (!s2.isEmpty()) {
            result.add(s2);
          }
        }

        sb.setLength(0);
        inArgument = false;
        continue;
      }

      sb.append(c);
    }

    if (inArgument) {
      result.add(sb.toString());
    } else {

      String s2 = sb.toString().trim();
      if (!s2.isEmpty()) {
        result.add(s2);
      }
    }

    return Collections.unmodifiableList(result);
  }

  /**
   * Returns a string with double quotes at the beginning and at the end of the
   * string.
   * @param s the string
   * @return a string with double quotes at the beginning and at the end of the
   *         string or null if s is null
   */
  public static String doubleQuotes(final String s) {

    if (s == null) {
      return null;
    }

    return '"' + s + '"';
  }

  /**
   * Remove double quotes at the beginning and at the end of the string.
   * @param s the string.
   * @return a string without double quote at the beginning and at the end of
   *         the string or null if s is null
   */
  public static String unDoubleQuotes(final String s) {

    if (s == null) {
      return null;
    }

    final int len = s.length();

    if (len < 2) {
      return s;
    }

    if (s.charAt(0) == '"' && s.charAt(len - 1) == '"') {
      return s.substring(1, len - 1);
    }

    return s;
  }

  /**
   * Convert a stack trace of an exception into a string.
   * @param t the throwable exception
   * @return a string with the stack strace
   */
  public static String stackTraceToString(final Throwable t) {

    if (t == null) {
      return null;
    }

    final StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));

    return sw.toString();
  }

  /**
   * Escape XML string.
   * @param s the string to escape
   * @return an escaped string
   */
  public static String xmlEscape(final String s) {

    if (s == null) {
      return null;
    }

    final StringBuilder sb = new StringBuilder();

    final int len = s.length();

    for (int i = 0; i < len; i++) {

      final char c = s.charAt(i);

      switch (c) {

      case '"':
        sb.append("&quot;");
        break;

      case '\'':
        sb.append("&apos;");
        break;

      case '<':
        sb.append("&lt;");
        break;

      case '>':
        sb.append("&gt;");
        break;

      case '&':
        sb.append("&amp;");
        break;

      default:
        sb.append(c);
      }
    }

    return sb.toString();
  }

  /**
   * Convert a MD5 message digest to a string.
   * @param md5Digest the message digest
   * @return the MD5 digest as a string
   */
  public static String md5DigestToString(final MessageDigest md5Digest) {

    requireNonNull(md5Digest, "md argument cannot be null");

    if (!"MD5".equals(md5Digest.getAlgorithm())) {
      throw new IllegalArgumentException(
          "the md argument must be a MD5 MessageDigest but found: "
              + md5Digest.getAlgorithm());
    }

    return new BigInteger(1, md5Digest.digest()).toString(16);
  }

  //
  // Constructor
  //

  private StringUtils() {
  }

}
