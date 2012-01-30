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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import fr.ens.transcriptome.eoulsan.io.CompressionType;

public class FileUtils {

  /** The default size of the buffer. */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
  /** The charset to use. */
  private static final String CHARSET = "ISO-8859-1";

  private static final boolean USE_CHANNEL = false;

  /**
   * Simple FilenameFilter to filter Paths with their prefix.
   * @author Laurent Jourdren
   */
  public static final class PrefixFilenameFilter implements FilenameFilter {

    private String prefix;
    private boolean allowCompressedFile;

    @Override
    public boolean accept(final File file, final String name) {

      if (name == null)
        return false;

      final String myName;

      if (this.allowCompressedFile)
        myName = StringUtils.removeCompressedExtensionFromFilename(name);
      else
        myName = name;

      return myName.startsWith(this.prefix);
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param prefix the prefix for the filter
     */
    public PrefixFilenameFilter(final String prefix) {

      this(prefix, false);
    }

    /**
     * Public constructor.
     * @param prefix the prefix for the filter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public PrefixFilenameFilter(final String prefix,
        final boolean allowCompressedFile) {

      if (prefix == null)
        throw new NullPointerException("The prefix is null");

      this.prefix = prefix;
      this.allowCompressedFile = allowCompressedFile;
    }
  };

  /**
   * Simple FilenameFilter to filter Paths with their suffix.
   * @author Laurent Jourdren
   */
  public static final class SuffixFilenameFilter implements FilenameFilter {

    private String suffix;
    private boolean allowCompressedFile;

    @Override
    public boolean accept(final File file, final String name) {

      if (name == null)
        return false;

      final String myName;

      if (this.allowCompressedFile)
        myName = StringUtils.removeCompressedExtensionFromFilename(name);
      else
        myName = name;

      return myName.endsWith(this.suffix);
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param suffix the suffix for the filter
     */
    public SuffixFilenameFilter(final String suffix) {

      this(suffix, false);
    }

    /**
     * Public constructor.
     * @param suffix the suffix for the filter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public SuffixFilenameFilter(final String suffix,
        final boolean allowCompressedFile) {

      if (suffix == null)
        throw new NullPointerException("The suffix is null");

      this.suffix = suffix;
      this.allowCompressedFile = allowCompressedFile;
    }
  };

  /**
   * Utility method to create a fast InputStream from a file.
   * @param file File to read
   * @return an InputStream
   * @throws FileNotFoundException if the file is not found
   */
  public static final InputStream createInputStream(final String filename)
      throws FileNotFoundException {

    if (filename == null)
      throw new NullPointerException("The filename is null.");

    return createInputStream(new File(filename));
  }

  /**
   * Utility method to create a fast InputStream from a file.
   * @param file File to read
   * @return an InputStream
   * @throws FileNotFoundException if the file is not found
   */
  public static final InputStream createInputStream(final File file)
      throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("The file is null.");

    if (USE_CHANNEL) {

      final FileInputStream inFile = new FileInputStream(file);
      final FileChannel inChannel = inFile.getChannel();

      return Channels.newInputStream(inChannel);
    }

    return new FileInputStream(file);
  }

  /**
   * Utility method to create a fast OutputStream from a file.
   * @param filename Name of the file to read
   * @return an OutputStream
   * @throws IOException if the file is not found
   */
  public static final OutputStream createOutputStream(final String filename)
      throws IOException {

    if (filename == null)
      throw new NullPointerException("The filename is null.");

    return createOutputStream(new File(filename));
  }

  /**
   * Utility method to create a fast OutputStream from a file.
   * @param file File to read
   * @return an InputStream
   * @throws IOException if the file is not found
   */
  public static final OutputStream createOutputStream(final File file)
      throws IOException {

    if (file == null)
      throw new NullPointerException("The file is null.");

    if (file.isFile())
      if (!file.delete())
        throw new IOException("Can not remove existing file: "
            + file.getAbsolutePath());

    if (USE_CHANNEL) {
      final FileOutputStream outFile = new FileOutputStream(file);
      final FileChannel outChannel = outFile.getChannel();

      return Channels.newOutputStream(outChannel);
    }

    return new FileOutputStream(file);
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param filename Name of the file to read
   * @return a BufferedReader
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedReader createBufferedReader(final String filename)
      throws FileNotFoundException {

    if (filename == null)
      throw new NullPointerException("The filename is null");

    return createBufferedReader(new File(filename));
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param file File to read
   * @return a BufferedReader
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedReader createBufferedReader(final File file)
      throws FileNotFoundException {

    return createBufferedReader(file, null);
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param file File to read
   * @param charset Charset to use
   * @return a BufferedReader
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedReader createBufferedReader(final File file,
      final Charset charset) throws FileNotFoundException {

    final InputStream is = createInputStream(file);

    if (is == null)
      return null;

    if (charset != null)
      return new BufferedReader(new InputStreamReader(is, charset));

    return new BufferedReader(new InputStreamReader(is, Charset.forName(System
        .getProperty("file.encoding"))));
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param is InputStream to read
   * @return a BufferedReader
   */
  public static final BufferedReader createBufferedReader(final InputStream is) {

    return createBufferedReader(is, null);
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param is InputStream to read
   * @param charset Charset to use
   * @return a BufferedReader
   */
  public static final BufferedReader createBufferedReader(final InputStream is,
      final Charset charset) {

    if (is == null)
      throw new NullPointerException("The input stream is null");

    if (charset != null)
      return new BufferedReader(new InputStreamReader(is, charset));

    return new BufferedReader(new InputStreamReader(is, Charset.forName(System
        .getProperty("file.encoding"))));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param filename Name of the file to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final String filename) throws IOException {

    if (filename == null)
      throw new NullPointerException("The filename is null");

    return createFastBufferedWriter(new File(filename));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final File file) throws IOException {

    final OutputStream os = createOutputStream(file);

    if (os == null)
      return null;

    return new UnSynchronizedBufferedWriter(new OutputStreamWriter(os,
        Charset.forName(CHARSET)));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param os OutputStream to write
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final OutputStream os) throws FileNotFoundException {

    if (os == null)
      throw new NullPointerException("The output stream is null");

    return new UnSynchronizedBufferedWriter(new OutputStreamWriter(os,
        Charset.forName(CHARSET)));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if an error occurs while creating the Writer
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedGZipWriter(
      final File file) throws IOException {

    if (file == null)
      return null;

    // Remove file if exists
    if (file.exists())
      if (!file.delete())
        throw new IOException("Can not remove existing file: "
            + file.getAbsolutePath());

    final FileOutputStream outFile = new FileOutputStream(file);
    final FileChannel outChannel = outFile.getChannel();

    final OutputStream gzos =
        CompressionType.createGZipOutputStream(Channels
            .newOutputStream(outChannel));

    return new UnSynchronizedBufferedWriter(new OutputStreamWriter(gzos,
        Charset.forName(CHARSET)));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param filename Name of the file to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final String filename)
      throws IOException {

    if (filename == null)
      throw new NullPointerException("The filename is null");

    return createBufferedWriter(new File(filename));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final File file)
      throws IOException {

    final OutputStream os = createOutputStream(file);

    if (os == null)
      return null;

    return new BufferedWriter(new OutputStreamWriter(os,
        Charset.forName(CHARSET)));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param os OutputStream to write
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final OutputStream os)
      throws FileNotFoundException {

    if (os == null)
      throw new NullPointerException("The output stream is null");

    return new BufferedWriter(new OutputStreamWriter(os,
        Charset.forName(CHARSET)));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use ISO-8859-1 encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if an error occurs while creating the Writer
   */
  public static final BufferedWriter createBufferedGZipWriter(final File file)
      throws IOException {

    if (file == null)
      return null;

    // Remove file if exists
    if (file.exists())
      if (!file.delete())
        throw new IOException("Can not remove existing file: "
            + file.getAbsolutePath());

    final FileOutputStream outFile = new FileOutputStream(file);
    final FileChannel outChannel = outFile.getChannel();

    final OutputStream gzos =
        CompressionType.createGZipOutputStream(Channels
            .newOutputStream(outChannel));

    return new BufferedWriter(new OutputStreamWriter(gzos,
        Charset.forName(CHARSET)));
  }

  /**
   * Utility method to create fast ObjectOutput.
   * @param file File to write
   * @return a ObjectOutput
   * @throws IOException if an error occurs while creating the Writer
   */
  public static final ObjectOutputStream createObjectOutputWriter(
      final File file) throws IOException {

    if (file == null)
      return null;

    // Remove file if exists
    if (file.exists())
      if (!file.delete())
        throw new IOException("Can not remove existing file: "
            + file.getAbsolutePath());

    final FileOutputStream outFile = new FileOutputStream(file);
    final FileChannel outChannel = outFile.getChannel();

    return new ObjectOutputStream(Channels.newOutputStream(outChannel));
  }

  /**
   * Utility method to create fast ObjectInputStream.
   * @param file File to read
   * @return a ObjectInputStream
   * @throws IOException if an error occurs while creating the reader
   */
  public static final ObjectInputStream createObjectInputReader(final File file)
      throws IOException {

    if (file == null)
      return null;

    final FileInputStream inFile = new FileInputStream(file);
    final FileChannel inChannel = inFile.getChannel();

    return new ObjectInputStream(Channels.newInputStream(inChannel));
  }

  /**
   * Copy bytes from an InputStream to an OutputStream.
   * @param input the InputStream to read from
   * @param output the OutputStream to write to
   * @return the number of bytes copied
   * @throws IOException In case of an I/O problem
   */
  public static long copy(InputStream input, OutputStream output)
      throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }

    input.close();
    output.close();

    return count;
  }

  /**
   * Copy a file.
   * @param srcFile File to copy
   * @param destFile Destination file
   * @param overwrite overwrite existing file
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyFile(final File srcFile, final File destFile)
      throws IOException {

    return copyFile(srcFile, destFile, false);
  }

  /**
   * Copy a file.
   * @param srcFile File to copy
   * @param destFile Destination file
   * @param overwrite overwrite existing file
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyFile(final File srcFile, final File destFile,
      final boolean overwrite) throws IOException {

    if (srcFile == null)
      throw new NullPointerException("Input file is null");

    if (destFile == null)
      throw new NullPointerException("output file is null");

    if (!srcFile.exists())
      throw new IOException("Source file doesn't exists: " + srcFile);

    if (srcFile.isDirectory())
      throw new IOException("Can't copy/move a directory: " + srcFile);

    final File myDestFile;

    if (destFile.isDirectory())
      myDestFile = new File(destFile, srcFile.getName());
    else
      myDestFile = destFile;

    if (destFile.exists()) {

      if (!overwrite)
        return false;

      if (!myDestFile.delete())
        throw new IOException("Can not remove existing file: "
            + myDestFile.getAbsolutePath());

    }

    final FileChannel inChannel = new FileInputStream(srcFile).getChannel();
    final FileChannel outChannel =
        new FileOutputStream(myDestFile).getChannel();

    try {
      inChannel.transferTo(0, inChannel.size(), outChannel);
    } catch (IOException e) {
      throw e;
    } finally {
      if (inChannel != null)
        inChannel.close();
      if (outChannel != null)
        outChannel.close();
    }

    return true;
  }

  /**
   * Copy a file.
   * @param in File to copy
   * @param out Destination file
   * @param overwrite overwrite existing file
   * @throws IOException if an error occurs while copying file
   */
  public static boolean moveFile(final File srcFile, final File destFile)
      throws IOException {

    return moveFile(srcFile, destFile, true);
  }

  /**
   * Copy a file.
   * @param in File to copy
   * @param out Destination file
   * @param overwrite overwrite existing file
   * @throws IOException if an error occurs while copying file
   */
  public static boolean moveFile(final File srcFile, final File destFile,
      final boolean overwrite) throws IOException {

    return copyFile(srcFile, destFile, overwrite) && srcFile.delete();
  }

  /**
   * Create a zip archive with the content of a directory.
   * @param directory directory to compress
   * @param zipFile output file
   * @throws IOException if an error occurs while compressing data
   */
  public static void createZip(final File directory, final File zipFile)
      throws IOException {

    if (directory == null)
      throw new IOException("Input directory is null");

    if (!(directory.exists() && directory.isDirectory()))
      throw new IOException("Invalid directory (" + directory + ")");

    if (zipFile == null)
      throw new IOException("Output file is null");

    final ZipOutputStream out =
        new ZipOutputStream(new FileOutputStream(zipFile));

    zipFolder(directory, "", out);

    out.close();
  }

  private static void zipFolder(final File directory, final String path,
      final ZipOutputStream out) throws IOException {

    // Add directory even empty
    if (!"".equals(path)) {
      out.putNextEntry(new ZipEntry(path));
    }

    // Get the list of files to add
    final File[] filesToAdd = directory.listFiles(new FileFilter() {
      @Override
      public boolean accept(final File file) {
        return file.isFile();
      }
    });

    // Add the files
    if (filesToAdd != null) {

      final byte data[] = new byte[DEFAULT_BUFFER_SIZE];
      BufferedInputStream origin = null;

      for (final File f : filesToAdd) {
        out.putNextEntry(new ZipEntry(path + f.getName()));
        final FileInputStream fi = new FileInputStream(f);

        origin = new BufferedInputStream(fi, DEFAULT_BUFFER_SIZE);

        int count;
        while ((count = origin.read(data, 0, DEFAULT_BUFFER_SIZE)) != -1)
          out.write(data, 0, count);

        origin.close();
      }
    }

    // Get the list of directories to add
    final File[] directoriesToAdd = directory.listFiles(new FileFilter() {
      @Override
      public boolean accept(final File file) {

        return file.isDirectory();
      }
    });

    // Add directories
    if (directoriesToAdd != null) {
      for (final File dir : directoriesToAdd) {
        zipFolder(dir, path + File.separator + dir.getName() + File.separator,
            out);
      }
    }

  }

  /**
   * Unzip a zip file in a directory.
   * @param is input stream of the zip file
   * @param outputDirectory
   * @throws IOException
   */
  public static void unzip(final InputStream is, final File outputDirectory)
      throws IOException {

    if (is == null)
      throw new IOException("The inputStream is null");

    if (outputDirectory == null)
      throw new IOException("The output directory is null");

    if (!(outputDirectory.exists() && outputDirectory.isDirectory()))
      throw new IOException("The output directory is invalid ("
          + outputDirectory + ")");

    BufferedOutputStream dest = null;

    final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
    ZipEntry entry;

    while ((entry = zis.getNextEntry()) != null) {

      final File newFile =
          new File(outputDirectory + File.separator + entry.getName());

      if (entry.isDirectory()) {

        if (!newFile.exists()) {
          if (!newFile.mkdirs()) {
            throw new IOException("Cannot create directory: " + newFile);
          }
        }

      } else {

        final File parentFile = newFile.getParentFile();
        if (!parentFile.exists()) {
          if (!parentFile.mkdirs()) {
            throw new IOException("Cannot create directory: " + parentFile);
          }
        }

        int count;
        byte data[] = new byte[DEFAULT_BUFFER_SIZE];
        // write the files to the disk
        FileOutputStream fos = new FileOutputStream(newFile);
        dest = new BufferedOutputStream(fos, DEFAULT_BUFFER_SIZE);

        while ((count = zis.read(data, 0, DEFAULT_BUFFER_SIZE)) != -1)
          dest.write(data, 0, count);

        dest.flush();
        dest.close();
      }
    }
    zis.close();
  }

  /**
   * Unzip a zip file in a directory.
   * @param zipFile
   * @param outputDirectory
   * @throws IOException
   */
  public static void unzip(final File zipFile, final File outputDirectory)
      throws IOException {

    if (zipFile == null)
      throw new IOException("The zip file is null");

    if (!(zipFile.exists() && zipFile.isFile()))
      throw new IOException("Invalid zip file (" + zipFile.getName() + ")");

    unzip(new FileInputStream(zipFile), outputDirectory);
  }

  /**
   * Get the files of a directory.
   * @param directory Directory to list files
   * @param extension extension of the file
   * @return an array of File objects
   */
  public static File[] listFilesByExtension(final File directory,
      final String extension) {

    if (directory == null || extension == null)
      return null;

    return directory.listFiles(new FilenameFilter() {

      public boolean accept(File arg0, String arg1) {

        return arg1.endsWith(extension);
      }
    });

  }

  /**
   * Remove a list of files.
   * @param filesToRemove An array with the files to remove
   * @param recursive true if the remove must be recursive
   */
  public static boolean removeFiles(final File[] filesToRemove,
      final boolean recursive) {

    if (filesToRemove == null)
      return false;

    for (int i = 0; i < filesToRemove.length; i++) {

      final File f = filesToRemove[i];

      if (f.isDirectory()) {
        if (recursive) {
          if (!removeFiles(listFilesByExtension(f, ""), true))
            return false;
          if (!f.delete())
            return false;
        }

      } else if (!f.delete())
        return false;
    }

    return true;
  }

  /**
   * Get the prefix of a list of files.
   * @param files Files that we wants the prefix
   * @return the prefix of the files
   */
  public static String getPrefix(final List<File> files) {

    if (files == null)
      return null;

    File[] param = new File[files.size()];
    files.toArray(param);

    return getPrefix(param);
  }

  /**
   * Get the prefix of a list of files.
   * @param files Files that we wants the prefix
   * @return the prefix of the files
   */
  public static String getPrefix(final File[] files) {

    if (files == null)
      return null;

    String prefix = null;
    final StringBuilder sb = new StringBuilder();

    for (int i = 0; i < files.length; i++) {

      String filename = files[i].getName();

      if (prefix == null)
        prefix = filename;
      else if (!filename.startsWith(prefix)) {

        int max = Math.min(prefix.length(), filename.length());

        for (int j = 0; j < max; j++) {

          if (prefix.charAt(j) == filename.charAt(j))
            sb.append(prefix.charAt(j));
        }

        prefix = sb.toString();
        sb.setLength(0);
      }

    }

    return prefix;
  }

  /**
   * Set executable bits on file on *nix.
   * @param file File to handle
   * @param executable If true, sets the access permission to allow execute
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setExecutable(final File file,
      final boolean executable, final boolean ownerOnly) throws IOException {

    if (file == null)
      return false;

    if (!file.exists() || !file.isFile())
      throw new FileNotFoundException(file.getAbsolutePath());

    final char op = executable ? '+' : '-';

    final String cmd =
        "chmod "
            + (ownerOnly ? "u" + op + "x " : "ugo" + op + "x ")
            + file.getAbsolutePath();

    ProcessUtils.exec(cmd, false);

    return true;
  }

  /**
   * Set executable bits on file on *nix.
   * @param file File to handle
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setExecutable(final File file, boolean executable)
      throws IOException {
    return setExecutable(file, executable, false);
  }

  /**
   * Set readable bits on file on *nix.
   * @param file File to handle
   * @param readable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setReadable(final File file, final boolean readable,
      final boolean ownerOnly) throws IOException {

    if (file == null)
      return false;

    if (!file.exists() || !file.isFile())
      throw new FileNotFoundException(file.getAbsolutePath());

    final char op = readable ? '+' : '-';

    final String cmd =
        "chmod "
            + (ownerOnly ? "u" + op + "r " : "ugo" + op + "r ")
            + file.getAbsolutePath();

    ProcessUtils.exec(cmd, true);

    return true;
  }

  /**
   * Set readable bits on file on *nix.
   * @param file File to handle
   * @param readable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setReadable(final File file, boolean readable)
      throws IOException {
    return setReadable(file, readable, true);
  }

  /**
   * Set writable bits on file on *nix.
   * @param file File to handle
   * @param writable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setWritable(final File file, final boolean writable,
      final boolean ownerOnly) throws IOException {

    if (file == null)
      return false;

    if (!file.exists() || !file.isFile())
      throw new FileNotFoundException(file.getAbsolutePath());

    final char op = writable ? '+' : '-';

    final String cmd =
        "chmod "
            + (ownerOnly ? "u" + op + "w " : "ugo" + op + "w ")
            + file.getAbsolutePath();

    ProcessUtils.exec(cmd, true);

    return true;
  }

  /**
   * Set writable bits on file on *nix.
   * @param file File to handle
   * @param writable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setWritable(final File file, boolean writable)
      throws IOException {
    return setWritable(file, writable, true);
  }

  /**
   * Set writable bits on directory on *nix.
   * @param file File to handle
   * @param writable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @param ownerOnly If true, the execute permission applies only to the
   *          owner's execute permission; otherwise, it applies to everybody. If
   *          the underlying file system can not distinguish the owner's execute
   *          permission from that of others, then the permission will apply to
   *          everybody, regardless of this value.
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setDirectoryWritable(final File file,
      final boolean writable, final boolean ownerOnly) throws IOException {

    if (file == null)
      return false;

    if (!file.exists() || !file.isDirectory())
      throw new FileNotFoundException(file.getAbsolutePath());

    final char op = writable ? '+' : '-';

    final String cmd =
        "chmod "
            + (ownerOnly ? "u" + op + "w " : "ugo" + op + "w ")
            + file.getAbsolutePath();

    ProcessUtils.exec(cmd, true);

    return true;
  }

  /**
   * Set writable bits on directory on *nix.
   * @param file File to handle
   * @param writable If true, sets the access permission to allow read
   *          operations; if false to disallow execute operations
   * @return true if and only if the operation succeeded
   * @throws IOException
   */
  public static boolean setDirectoryWritable(final File file, boolean writable)
      throws IOException {
    return setDirectoryWritable(file, writable, true);
  }

  /**
   * Delete a directory and its content. It is not a recurse method.
   * @param directory Directory to remove
   * @return false if one of the files can't be removed
   */
  public static boolean removeDirectory(final File directory) {

    if (directory == null)
      return false;

    final File[] files = directory.listFiles();
    for (int i = 0; i < files.length; i++)
      if (!files[i].delete())
        return false;

    return directory.delete();
  }

  /**
   * Concat a list of files
   * @param files files to concat
   * @param outputFile output file
   * @throws IOException if an error occurs while read or writing data
   */
  public static void concat(final List<File> files, final File outputFile)
      throws IOException {

    if (files == null)
      throw new NullPointerException("Files to concat is null");

    if (outputFile == null)
      throw new NullPointerException("Output file is null");

    UnSynchronizedBufferedWriter writer = createFastBufferedWriter(outputFile);

    for (File f : files) {

      BufferedReader reader = createBufferedReader(f);

      String line;

      while ((line = reader.readLine()) != null)
        writer.write(line + "\n");

    }

    writer.close();
  }

  /**
   * Create a new temporary file.
   * @param prefix Prefix of the temporary file
   * @param suffix suffix of the temporary file
   * @return the new temporary file
   * @throws IOException if there is an error creating the temporary directory
   */
  public static File createTempFile(final String prefix, final String suffix)
      throws IOException {

    return createTempFile(null, prefix, suffix);
  }

  /**
   * Create a file in the temporary directory.
   * @param filename The filename to create
   * @return The new File
   */
  public static File createFileInTempDir(final String filename) {

    return new File(System.getProperty("java.io.tmpdir"), filename);
  }

  /**
   * Create a new temporary file.
   * @param directory parent directory of the temporary file to create
   * @param prefix Prefix of the temporary file
   * @param suffix suffix of the temporary file
   * @return the new temporary file
   * @throws IOException if there is an error creating the temporary directory
   */
  public static File createTempFile(final File directory, final String prefix,
      final String suffix) throws IOException {

    final File myDir;
    final String myPrefix;
    final String mySuffix;

    if (directory == null)
      myDir = new File(System.getProperty("java.io.tmpdir"));
    else
      myDir = directory;

    if (prefix == null)
      myPrefix = "";
    else
      myPrefix = prefix;

    if (suffix == null)
      mySuffix = "";
    else
      mySuffix = suffix;

    File tempFile;

    final int maxAttempts = 9;
    int attemptCount = 0;
    do {
      attemptCount++;
      if (attemptCount > maxAttempts)

        throw new IOException("The highly improbable has occurred! Failed to "
            + "create a unique temporary directory after " + maxAttempts
            + " attempts.");

      final String filename =
          myPrefix + UUID.randomUUID().toString() + mySuffix;
      tempFile = new File(myDir, filename);
    } while (tempFile.exists());

    if (!tempFile.createNewFile())
      throw new IOException("Failed to create temp file named "
          + tempFile.getAbsolutePath());

    return tempFile;
  }

  /**
   * Create a new temporary directory.
   * @return the new directory
   * @throws IOException if there is an error creating the temporary directory
   */
  public static File createTempDir() throws IOException {

    return createTempDir(null, null);
  }

  /**
   * Create a new temporary directory.
   * @param prefix prefix of the temporary directory
   * @return the new directory
   * @throws IOException if there is an error creating the temporary directory
   */
  public static File createTempDir(final String prefix) throws IOException {

    return createTempDir(null, prefix);
  }

  /**
   * Create a new temporary directory.
   * @param parentDirectory parent directory for the temporary directory
   * @return the new directory
   * @throws IOException if there is an error creating the temporary directory
   */
  public static File createTempDir(final File parentDirectory)
      throws IOException {

    return createTempDir(parentDirectory, null);
  }

  /**
   * Create a new temporary directory.
   * @param parentDirectory parent directory for the temporary directory
   * @param prefix Prefix of the directory name
   * @return the new directory
   * @throws IOException if there is an error creating the temporary directory
   */
  public static File createTempDir(final File parentDirectory,
      final String prefix) throws IOException {

    final File myTempParentDir;
    final String myPrefix;

    if (parentDirectory == null)
      myTempParentDir = new File(System.getProperty("java.io.tmpdir"));
    else
      myTempParentDir = parentDirectory;

    if (prefix == null)
      myPrefix = "";
    else
      myPrefix = prefix;

    File newTempDir;
    final int maxAttempts = 9;
    int attemptCount = 0;
    do {
      attemptCount++;
      if (attemptCount > maxAttempts)

        throw new IOException("The highly improbable has occurred! Failed to "
            + "create a unique temporary directory after " + maxAttempts
            + " attempts.");

      String dirName = myPrefix + UUID.randomUUID().toString();
      newTempDir = new File(myTempParentDir, dirName);
    } while (newTempDir.exists());

    if (newTempDir.mkdirs())
      return newTempDir;

    throw new IOException("Failed to create temp dir named "
        + newTempDir.getAbsolutePath());

  }

  /**
   * Recursively delete file or directory
   * @param fileOrDir the file or dir to delete
   * @return true if all files are successfully deleted
   */
  public static boolean recursiveDelete(final File fileOrDir) {

    if (fileOrDir == null)
      return false;

    if (fileOrDir.isDirectory())
      // recursively delete contents
      for (File innerFile : fileOrDir.listFiles())
        if (!recursiveDelete(innerFile))
          return false;

    return fileOrDir.delete();
  }

  /**
   * Check if a file exists
   * @param file File to test
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingFile(final File file,
      final String msgFileType) throws IOException {

    if (msgFileType == null)
      throw new NullPointerException("Message file type for check is null");

    if (file == null)
      throw new NullPointerException("The " + msgFileType + " is null");

    if (!file.exists())
      throw new IOException("The "
          + msgFileType + " does not exists: " + file.getAbsolutePath());
  }

  /**
   * Check if a directory exists
   * @param directory directory to test
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingDirectoryFile(final File directory,
      final String msgFileType) throws IOException {

    checkExistingFile(directory, msgFileType);
    if (!directory.isDirectory())
      throw new IOException("The "
          + msgFileType + " is not a directory: " + directory.getAbsolutePath());
  }

  /**
   * Check if a file exists
   * @param file File to test
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingStandardFile(final File file,
      final String msgFileType) throws IOException {

    checkExistingFile(file, msgFileType);
    if (!file.isFile())
      throw new IOException("The "
          + msgFileType + " is  not a standard file: " + file.getAbsolutePath());
  }

  /**
   * Check if a file exists
   * @param file File to test
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingStandardFileOrDirectory(
      final File file, final String msgFileType) throws IOException {

    checkExistingDirectoryFile(file, msgFileType);
    if (!file.isFile() && !file.isDirectory())
      throw new IOException("The "
          + msgFileType + " is  not a standard file or a directory: "
          + file.getAbsolutePath());
  }

  /**
   * Create an hard link on an unix system.
   * @param target target file
   * @param link link file
   * @return true if the link has been successfully created
   */
  public static final boolean createHardLink(final File target, final File link) {

    if (target == null || link == null || !target.exists())
      return false;

    final String cmd =
        "ln "
            + StringUtils.bashEscaping(target.getAbsolutePath()) + " "
            + StringUtils.bashEscaping(link.getAbsolutePath());
    try {

      Process p = Runtime.getRuntime().exec(cmd);
      if (p.waitFor() == 0)
        return true;

      return false;

    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      return false;
    }

  }

  /**
   * Create an symbolic link on an unix system.
   * @param target target file
   * @param link link file
   * @return true if the link has been successfully created
   */
  public static final boolean createSymbolicLink(final File target,
      final File link) {

    if (target == null || link == null || !target.exists())
      return false;

    final String cmd =
        "ln -s "
            + StringUtils.bashEscaping(target.getAbsolutePath()) + " "
            + StringUtils.bashEscaping(link.getAbsolutePath());
    try {

      Process p = Runtime.getRuntime().exec(cmd);

      if (p.waitFor() == 0)
        return true;

      return false;

    } catch (IOException e) {
      return false;
    } catch (InterruptedException e) {
      return false;
    }

  }

  /**
   * Test if two stream are equals
   * @param a First stream to compare
   * @param b Second stream to compare
   * @return true if the two stream are equals
   * @throws IOException if an error occurs while reading the streams
   */
  public static boolean compareFile(final InputStream a, final InputStream b)
      throws IOException {

    if (a == null && b == null)
      return true;
    if (a == null || b == null)
      return false;

    boolean end = false;
    boolean result = true;

    while (!end) {

      int ca = a.read();
      int cb = b.read();

      if (ca != cb) {
        result = false;
        end = true;
      }

      if (ca == -1)
        end = true;

    }

    a.close();
    b.close();

    return result;
  }

  /**
   * Test if two stream are equals
   * @param filenameA First filename to compare
   * @param filenameB Second filename to compare
   * @return true if the two stream are equals
   * @throws IOException if an error occurs while reading the streams
   */
  public static boolean compareFile(final String filenameA,
      final String filenameB) throws IOException {

    return compareFile(new File(filenameA), new File(filenameB));
  }

  /**
   * Test if two stream are equals
   * @param fileA First filename to compare
   * @param fileB Second filename to compare
   * @return true if the two stream are equals
   * @throws IOException if an error occurs while reading the streams
   */
  public static boolean compareFile(final File fileA, final File fileB)
      throws IOException {

    final InputStream isa = new FileInputStream(fileA);
    final InputStream isb = new FileInputStream(fileB);

    try {
      return compareFile(isa, isb);
    } catch (IOException e) {
      throw e;
    } finally {
      isa.close();
      isb.close();
    }
  }

}
