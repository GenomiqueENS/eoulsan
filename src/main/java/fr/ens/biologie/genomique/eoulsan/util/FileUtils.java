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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import fr.ens.biologie.genomique.eoulsan.io.CompressionType;
import fr.ens.biologie.genomique.eoulsan.io.FileCharsets;

/**
 * This class define useful method to handle files.
 * @since 1.0
 * @author Laurent Jourdren
 */
public class FileUtils {

  /** The default size of the buffer. */
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  private static final boolean USE_CHANNEL = false;

  /**
   * Simple FilenameFilter to filter Paths with their prefix.
   * @author Laurent Jourdren
   */
  public static final class PrefixFilenameFilter implements FilenameFilter {

    private final String prefix;
    private final boolean allowCompressedFile;

    @Override
    public boolean accept(final File file, final String name) {

      if (name == null) {
        return false;
      }

      final String myName;

      if (this.allowCompressedFile) {
        myName = StringUtils.removeCompressedExtensionFromFilename(name);
      } else {
        myName = name;
      }

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

      if (prefix == null) {
        throw new NullPointerException("The prefix is null");
      }

      this.prefix = prefix;
      this.allowCompressedFile = allowCompressedFile;
    }
  }

  /**
   * Simple FilenameFilter to filter Paths with their suffix.
   * @author Laurent Jourdren
   */
  public static final class SuffixFilenameFilter implements FilenameFilter {

    private final String suffix;
    private final boolean allowCompressedFile;

    @Override
    public boolean accept(final File file, final String name) {

      if (name == null) {
        return false;
      }

      final String myName;

      if (this.allowCompressedFile) {
        myName = StringUtils.removeCompressedExtensionFromFilename(name);
      } else {
        myName = name;
      }

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

      if (suffix == null) {
        throw new NullPointerException("The suffix is null");
      }

      this.suffix = suffix;
      this.allowCompressedFile = allowCompressedFile;
    }
  }

  /**
   * Utility method to create a fast InputStream from a file.
   * @param filename name of the file to read
   * @return an InputStream
   * @throws FileNotFoundException if the file is not found
   */
  public static final InputStream createInputStream(final String filename)
      throws FileNotFoundException {

    if (filename == null) {
      throw new NullPointerException("The filename is null.");
    }

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

    if (file == null) {
      throw new NullPointerException("The file is null.");
    }

    if (file.isDirectory()) {
      throw new FileNotFoundException("The file is a directory: " + file);
    }

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

    if (filename == null) {
      throw new NullPointerException("The filename is null.");
    }

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

    if (file == null) {
      throw new NullPointerException("The file is null.");
    }

    if (file.isFile()) {
      if (!file.delete()) {
        throw new IOException(
            "Can not remove existing file: " + file.getAbsolutePath());
      }
    }

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

    return createBufferedReader(filename, null);
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param filename Name of the file to read
   * @param charset Charset to use
   * @return a BufferedReader
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedReader createBufferedReader(final String filename,
      final Charset charset) throws FileNotFoundException {

    if (filename == null) {
      throw new NullPointerException("The filename is null");
    }

    return createBufferedReader(new File(filename), charset);
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

    if (charset != null) {
      return new BufferedReader(new InputStreamReader(is, charset));
    }

    return new BufferedReader(
        new InputStreamReader(is, FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedReader.
   * @param is InputStream to read
   * @return a BufferedReader
   */
  public static final BufferedReader createBufferedReader(
      final InputStream is) {

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

    if (is == null) {
      throw new NullPointerException("The input stream is null");
    }

    if (charset != null) {
      return new BufferedReader(new InputStreamReader(is, charset));
    }

    return new BufferedReader(
        new InputStreamReader(is, FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param filename Name of the file to write
   * @param charset Charset to use
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final String filename, final Charset charset) throws IOException {

    if (filename == null) {
      throw new NullPointerException("The filename is null");
    }

    return createFastBufferedWriter(new File(filename), charset);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param filename Name of the file to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final String filename) throws IOException {

    return createFastBufferedWriter(filename, null);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final File file) throws IOException {

    return createFastBufferedWriter(file, null);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param file File to write
   * @param charset Charset to use
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final File file, final Charset charset) throws IOException {

    final OutputStream os = createOutputStream(file);

    return new UnSynchronizedBufferedWriter(new OutputStreamWriter(os,
        charset != null ? charset : FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param os OutputStream to write
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final OutputStream os) throws FileNotFoundException {

    return createFastBufferedWriter(os, null);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param os OutputStream to write
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedWriter(
      final OutputStream os, final Charset charset)
      throws FileNotFoundException {

    if (os == null) {
      throw new NullPointerException("The output stream is null");
    }

    return new UnSynchronizedBufferedWriter(new OutputStreamWriter(os,
        charset != null ? charset : FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use default encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if an error occurs while creating the Writer
   */
  public static final UnSynchronizedBufferedWriter createFastBufferedGZipWriter(
      final File file) throws IOException {

    if (file == null) {
      return null;
    }

    // Remove file if exists
    if (file.exists()) {
      if (!file.delete()) {
        throw new IOException(
            "Can not remove existing file: " + file.getAbsolutePath());
      }
    }

    final FileOutputStream outFile = new FileOutputStream(file);
    final FileChannel outChannel = outFile.getChannel();

    final OutputStream gzos = CompressionType
        .createGZipOutputStream(Channels.newOutputStream(outChannel));

    return new UnSynchronizedBufferedWriter(
        new OutputStreamWriter(gzos, FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param filename Name of the file to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final String filename)
      throws IOException {

    return createBufferedWriter(filename, null);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param filename Name of the file to write
   * @param charset Charset to use
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final String filename,
      final Charset charset) throws IOException {

    if (filename == null) {
      throw new NullPointerException("The filename is null");
    }

    return createBufferedWriter(new File(filename), charset);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final File file)
      throws IOException {

    return createBufferedWriter(file, null);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param file File to write
   * @param charset Charset to use
   * @return a BufferedWriter
   * @throws IOException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final File file,
      final Charset charset) throws IOException {

    final OutputStream os = createOutputStream(file);

    return new BufferedWriter(new OutputStreamWriter(os,
        charset != null ? charset : FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param os OutputStream to write
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final OutputStream os)
      throws FileNotFoundException {

    return createBufferedWriter(os, null);
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread.
   * @param os OutputStream to write
   * @param charset Charset to use
   * @return a BufferedWriter
   * @throws FileNotFoundException if the file is not found
   */
  public static final BufferedWriter createBufferedWriter(final OutputStream os,
      final Charset charset) throws FileNotFoundException {

    if (os == null) {
      throw new NullPointerException("The output stream is null");
    }

    return new BufferedWriter(new OutputStreamWriter(os,
        charset != null ? charset : FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast BufferedWriter. Warning the buffer is not
   * safe-thread. The created file use default encoding.
   * @param file File to write
   * @return a BufferedWriter
   * @throws IOException if an error occurs while creating the Writer
   */
  public static final BufferedWriter createBufferedGZipWriter(final File file)
      throws IOException {

    if (file == null) {
      return null;
    }

    // Remove file if exists
    if (file.exists()) {
      if (!file.delete()) {
        throw new IOException(
            "Can not remove existing file: " + file.getAbsolutePath());
      }
    }

    final FileOutputStream outFile = new FileOutputStream(file);
    final FileChannel outChannel = outFile.getChannel();

    final OutputStream gzos = CompressionType
        .createGZipOutputStream(Channels.newOutputStream(outChannel));

    return new BufferedWriter(
        new OutputStreamWriter(gzos, FileCharsets.SYSTEM_CHARSET));
  }

  /**
   * Utility method to create fast ObjectOutput.
   * @param file File to write
   * @return a ObjectOutput
   * @throws IOException if an error occurs while creating the Writer
   */
  public static final ObjectOutputStream createObjectOutputWriter(
      final File file) throws IOException {

    if (file == null) {
      return null;
    }

    // Remove file if exists
    if (file.exists()) {
      if (!file.delete()) {
        throw new IOException(
            "Can not remove existing file: " + file.getAbsolutePath());
      }
    }

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

    if (file == null) {
      return null;
    }

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
  public static long copy(final InputStream input, final OutputStream output)
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
   * Copy bytes from an InputStream to an OutputStream without closing the
   * outputStream.
   * @param input the InputStream to read from
   * @param output the OutputStream to write to
   * @return the number of bytes copied
   * @throws IOException In case of an I/O problem
   */
  public static long append(final InputStream input, final OutputStream output)
      throws IOException {
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }

    input.close();

    return count;
  }

  /**
   * Copy a file.
   * @param srcFile File to copy
   * @param destFile Destination file
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

    if (srcFile == null) {
      throw new NullPointerException("Input file is null");
    }

    if (destFile == null) {
      throw new NullPointerException("output file is null");
    }

    if (!srcFile.exists()) {
      throw new IOException("Source file doesn't exists: " + srcFile);
    }

    if (srcFile.isDirectory()) {
      throw new IOException("Can't copy/move a directory: " + srcFile);
    }

    final File myDestFile;

    if (destFile.isDirectory()) {
      myDestFile = new File(destFile, srcFile.getName());
    } else {
      myDestFile = destFile;
    }

    if (destFile.exists()) {

      if (!overwrite) {
        return false;
      }

      if (!myDestFile.delete()) {
        throw new IOException(
            "Can not remove existing file: " + myDestFile.getAbsolutePath());
      }

    }

    final FileChannel inChannel = new FileInputStream(srcFile).getChannel();
    final FileChannel outChannel =
        new FileOutputStream(myDestFile).getChannel();

    try {
      inChannel.transferTo(0, inChannel.size(), outChannel);
    } finally {
      if (inChannel != null) {
        inChannel.close();
      }
      if (outChannel != null) {
        outChannel.close();
      }
    }

    return true;
  }

  /**
   * Copy a file.
   * @param srcFile File to copy
   * @param destFile Destination file
   * @throws IOException if an error occurs while copying file
   */
  public static boolean moveFile(final File srcFile, final File destFile)
      throws IOException {

    return moveFile(srcFile, destFile, true);
  }

  /**
   * Copy a file.
   * @param srcFile File to copy
   * @param destFile Destination file
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
    createZip(directory, zipFile, false);
  }

  /**
   * Create a zip archive with the content of a directory.
   * @param directory directory to compress
   * @param zipFile output file
   * @throws IOException if an error occurs while compressing data
   */
  public static void createZip(final File directory, final File zipFile,
      final boolean store) throws IOException {

    if (directory == null) {
      throw new IOException("Input directory is null");
    }

    if (!(directory.exists() && directory.isDirectory())) {
      throw new IOException("Invalid directory (" + directory + ")");
    }

    if (zipFile == null) {
      throw new IOException("Output file is null");
    }

    final ZipOutputStream out =
        new ZipOutputStream(new FileOutputStream(zipFile));

    zipFolder(directory, "", out, store);

    out.close();
  }

  /**
   * Add a directory to a ZipOutputStream.
   * @param directory directory to add to the ZIP file
   * @param path path of the directory in the ZIP file
   * @param out ZipOutputStream stream
   * @param store compress or store the files to add to the ZIP file
   */
  public static void zipFolder(final File directory, final String path,
      final ZipOutputStream out, boolean store) throws IOException {

    if (directory == null) {
      throw new IOException("Input directory is null");
    }

    if (!(directory.exists() && directory.isDirectory())) {
      throw new IOException("Invalid directory (" + directory + ")");
    }

    if (path == null) {
      throw new NullPointerException("path argument cannot be null");
    }
    if (out == null) {
      throw new NullPointerException("out argument cannot be null");
    }

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
        ZipEntry ze = new ZipEntry(path + f.getName());

        if (store) {

          // Compute checksum for storage
          final FileInputStream fi = new FileInputStream(f);
          CheckedInputStream originCheck = new CheckedInputStream(
              new BufferedInputStream(fi, DEFAULT_BUFFER_SIZE), new CRC32());
          while (originCheck.read(data, 0, DEFAULT_BUFFER_SIZE) != -1) {
            ;
          }
          fi.close();

          ze.setMethod(ZipEntry.STORED);
          ze.setCrc(originCheck.getChecksum().getValue());
        } else {
          ze.setMethod(ZipEntry.DEFLATED);
        }

        ze.setSize(f.length());
        out.putNextEntry(ze);

        final FileInputStream fi = new FileInputStream(f);

        origin = new BufferedInputStream(fi, DEFAULT_BUFFER_SIZE);

        int count;
        while ((count = origin.read(data, 0, DEFAULT_BUFFER_SIZE)) != -1) {
          out.write(data, 0, count);
        }

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
        zipFolder(dir, path + dir.getName() + File.separator, out, store);
      }
    }

  }

  /**
   * Unzip a zip file in a directory.
   * @param is input stream of the zip file
   * @param outputDirectory output directory
   * @throws IOException if an error occurs while unzipping the file
   */
  public static void unzip(final InputStream is, final File outputDirectory)
      throws IOException {

    if (is == null) {
      throw new IOException("The inputStream is null");
    }

    if (outputDirectory == null) {
      throw new IOException("The output directory is null");
    }

    if (!(outputDirectory.exists() && outputDirectory.isDirectory())) {
      throw new IOException(
          "The output directory is invalid (" + outputDirectory + ")");
    }

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

        while ((count = zis.read(data, 0, DEFAULT_BUFFER_SIZE)) != -1) {
          dest.write(data, 0, count);
        }

        dest.flush();
        dest.close();
      }

    }
    zis.close();
  }

  /**
   * Unzip a zip file in a directory.
   * @param zipFile The zip file
   * @param outputDirectory The output directory
   * @throws IOException if an issue occurs while unzipping the file
   */
  public static void unzip(final File zipFile, final File outputDirectory)
      throws IOException {

    if (zipFile == null) {
      throw new IOException("The zip file is null");
    }

    if (!(zipFile.exists() && zipFile.isFile())) {
      throw new IOException("Invalid zip file (" + zipFile.getName() + ")");
    }

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

    if (directory == null || extension == null) {
      return null;
    }

    return directory.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(final File arg0, final String arg1) {

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

    if (filesToRemove == null) {
      return false;
    }

    for (final File f : filesToRemove) {

      if (f.isDirectory()) {
        if (recursive) {
          if (!removeFiles(listFilesByExtension(f, ""), true)) {
            return false;
          }
          if (!f.delete()) {
            return false;
          }
        }

      } else if (!f.delete()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Get the prefix of a list of files.
   * @param files Files that we wants the prefix
   * @return the prefix of the files
   */
  public static String getPrefix(final List<File> files) {

    if (files == null) {
      return null;
    }

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

    if (files == null) {
      return null;
    }

    String prefix = null;
    final StringBuilder sb = new StringBuilder();

    for (File file : files) {

      String filename = file.getName();

      if (prefix == null) {
        prefix = filename;
      } else if (!filename.startsWith(prefix)) {

        int max = Math.min(prefix.length(), filename.length());

        for (int j = 0; j < max; j++) {

          if (prefix.charAt(j) == filename.charAt(j)) {
            sb.append(prefix.charAt(j));
          }
        }

        prefix = sb.toString();
        sb.setLength(0);
      }

    }

    return prefix;
  }

  /**
   * Delete a directory and its content. It is not a recursive method.
   * @param directory Directory to remove
   * @return false if one of the files can't be removed
   */
  public static boolean removeDirectory(final File directory) {

    if (directory == null) {
      return false;
    }

    final File[] files = directory.listFiles();
    for (File file : files) {
      if (!file.delete()) {
        return false;
      }
    }

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

    if (files == null) {
      throw new NullPointerException("Files to concat is null");
    }

    if (outputFile == null) {
      throw new NullPointerException("Output file is null");
    }

    UnSynchronizedBufferedWriter writer = createFastBufferedWriter(outputFile);

    for (File f : files) {

      BufferedReader reader = createBufferedReader(f);

      String line;

      while ((line = reader.readLine()) != null) {
        writer.write(line + "\n");
      }

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

    if (directory == null) {
      myDir = new File(System.getProperty("java.io.tmpdir"));
    } else {
      myDir = directory;
    }

    if (prefix == null) {
      myPrefix = "";
    } else {
      myPrefix = prefix;
    }

    if (suffix == null) {
      mySuffix = "";
    } else {
      mySuffix = suffix;
    }

    File tempFile;

    final int maxAttempts = 9;
    int attemptCount = 0;
    do {
      attemptCount++;
      if (attemptCount > maxAttempts) {
        throw new IOException("The highly improbable has occurred! Failed to "
            + "create a unique temporary directory after " + maxAttempts
            + " attempts.");
      }

      final String filename =
          myPrefix + UUID.randomUUID().toString() + mySuffix;
      tempFile = new File(myDir, filename);
    } while (tempFile.exists());

    if (!tempFile.createNewFile()) {
      throw new IOException(
          "Failed to create temp file named " + tempFile.getAbsolutePath());
    }

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

    if (parentDirectory == null) {
      myTempParentDir = new File(System.getProperty("java.io.tmpdir"));
    } else {
      myTempParentDir = parentDirectory;
    }

    if (prefix == null) {
      myPrefix = "";
    } else {
      myPrefix = prefix;
    }

    File newTempDir;
    final int maxAttempts = 9;
    int attemptCount = 0;
    do {
      attemptCount++;
      if (attemptCount > maxAttempts) {
        throw new IOException("The highly improbable has occurred! Failed to "
            + "create a unique temporary directory after " + maxAttempts
            + " attempts.");
      }

      String dirName = myPrefix + UUID.randomUUID().toString();
      newTempDir = new File(myTempParentDir, dirName);
    } while (newTempDir.exists());

    if (newTempDir.mkdirs()) {
      return newTempDir;
    }

    throw new IOException(
        "Failed to create temp dir named " + newTempDir.getAbsolutePath());

  }

  /**
   * Recursively delete file or directory
   * @param fileOrDir the file or dir to delete
   * @return true if all files are successfully deleted
   */
  public static boolean recursiveDelete(final File fileOrDir) {

    if (fileOrDir == null) {
      return false;
    }

    if (fileOrDir.isDirectory()) {
      // recursively delete contents
      for (File innerFile : fileOrDir.listFiles()) {
        if (!recursiveDelete(innerFile)) {
          return false;
        }
      }
    }

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

    if (msgFileType == null) {
      throw new NullPointerException(
          "Message file type for check isn't defined");
    }

    if (file == null) {
      throw new NullPointerException("The "
          + msgFileType + " is not defined. Please check and define "
          + msgFileType + " path and/or files.");
    }

    if (!file.exists()) {
      throw new IOException(
          "The " + msgFileType + " does not exists: " + file.getAbsolutePath());
    }
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
    if (!directory.isDirectory()) {
      throw new IOException("The "
          + msgFileType + " is not a directory: "
          + directory.getAbsolutePath());
    }
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
    if (!file.isFile()) {
      throw new IOException("The "
          + msgFileType + " is  not a standard file: "
          + file.getAbsolutePath());
    }
  }

  /**
   * Check if a file exists
   * @param file File to test
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingStandardFileOrDirectory(final File file,
      final String msgFileType) throws IOException {

    checkExistingDirectoryFile(file, msgFileType);
    if (!file.isFile() && !file.isDirectory()) {
      throw new IOException("The "
          + msgFileType + " is  not a standard file or a directory: "
          + file.getAbsolutePath());
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

    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }

    boolean end = false;
    boolean result = true;

    while (!end) {

      int ca = a.read();
      int cb = b.read();

      if (ca != cb) {
        result = false;
        end = true;
      }

      if (ca == -1) {
        end = true;
      }

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

    InputStream isa = null;
    InputStream isb = null;

    try {

      isa = new FileInputStream(fileA);
      isb = new FileInputStream(fileB);

      return compareFile(isa, isb);
    } finally {
      if (isa != null) {
        isa.close();
      }
      if (isb != null) {
        isb.close();
      }
    }
  }

  /**
   * Compute MD5 sum of a file.
   * @param file the input file
   * @return a string with the MD5 sum
   * @throws IOException In case of an I/O problem or digest error
   */
  public static String computeMD5Sum(final File file) throws IOException {

    if (file == null) {
      throw new NullPointerException("The file argument is null");
    }

    return computeMD5Sum(new FileInputStream(file));
  }

  /**
   * Compute MD5 sum of a file.
   * @param inputStream the InputStream to read from
   * @return a string with the MD5 sum
   * @throws IOException In case of an I/O problem or digest error
   */
  public static String computeMD5Sum(final InputStream inputStream)
      throws IOException {

    requireNonNull(inputStream, "inputStream argument cannot be null");

    final MessageDigest md5Digest;

    // Get the algorithm
    try {
      md5Digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("No MD5 digest algorithm found: " + e.getMessage());
    }

    try (InputStream in = inputStream) {
      byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

      int n = 0;

      while (-1 != (n = in.read(buffer))) {
        md5Digest.update(buffer, 0, n);
      }
    }

    return StringUtils.md5DigestToString(md5Digest);
  }

  /**
   * Relativize a path from a base path.
   * @param path path to relativize
   * @param base base path (must be a directory)
   * @return a File object with the relative path
   */
  public static final File relativizePath(final File path, final File base) {

    if (path == null) {
      throw new NullPointerException("The path is null");
    }

    if (base == null) {
      return path;
    }

    final File absPath = path.getAbsoluteFile();
    final File absBase = base.getAbsoluteFile();

    List<String> pathNodes = new ArrayList<>();
    List<String> baseNodes = new ArrayList<>();

    File parent = absPath;
    do {
      pathNodes.add(parent.getName());
    } while ((parent = parent.getParentFile()) != null);

    parent = absBase;
    do {
      baseNodes.add(parent.getName());
    } while ((parent = parent.getParentFile()) != null);

    Collections.reverse(pathNodes);
    Collections.reverse(baseNodes);

    final int minSize = Math.min(pathNodes.size(), baseNodes.size());
    int i = 0;

    while (i < minSize && pathNodes.get(i).equals(baseNodes.get(i))) {
      i++;
    }

    final List<String> resultNodes = new ArrayList<>();

    if (i < baseNodes.size()) {
      for (int j = 0; j < baseNodes.size() - i; j++) {
        resultNodes.add("..");
      }
    }

    resultNodes.addAll(pathNodes.subList(i, pathNodes.size()));

    return createFile(resultNodes);
  }

  /**
   * Create a File object from a list of the node of the file path.
   * @param pathNodes the list of the nodes of the path
   * @return a new File object with the requested path
   */
  private static File createFile(final List<String> pathNodes) {

    File result = null;

    for (String f : pathNodes) {
      if (result == null) {
        result = new File(f);
      } else {
        result = new File(result, f);
      }
    }

    return result;
  }

  /**
   * Create a named pipe.
   * @param file path of the named pipe
   * @throws IOException if an error occurs while creating the named pipe
   */
  public static void createNamedPipe(final File file) throws IOException {

    if (file == null) {
      throw new NullPointerException("file argument cannot be null");
    }

    if (file.exists()) {
      throw new IOException("Named pipe to create already exists: " + file);
    }

    final Process process =
        new ProcessBuilder("mkfifo", file.getAbsolutePath()).start();

    int exitCode;
    try {
      exitCode = process.waitFor();

      if (exitCode != 0) {
        throw new IOException("Unable to create named pipe: " + file);
      }

    } catch (InterruptedException e) {
      throw new IOException("Unable to create named pipe: " + file, e);
    }

  }

  /**
   * Check if an executable is in the PATH.
   * @param executableName the name of the executable
   * @return true if an executable is in the PATH
   */
  public static boolean checkIfExecutableIsInPATH(final String executableName) {

    if (executableName == null) {
      throw new NullPointerException("executableName argument cannot be null");
    }

    final String pathEnv = System.getenv("PATH");

    if (pathEnv == null) {
      return false;
    }

    final FilenameFilter filter = new FilenameFilter() {

      @Override
      public boolean accept(final File dir, final String name) {

        return executableName.equals(name);
      }
    };

    final String[] paths = pathEnv.split(":");

    for (String path : paths) {

      path = path.trim();

      if (path.isEmpty()) {
        continue;
      }

      File f = new File(path);

      if (!f.exists()) {
        continue;
      }

      if (f.isFile()) {

        if (executableName.equals(f.getName())) {
          return true;
        }
      } else {

        final File[] files = f.listFiles(filter);

        if (files != null && files.length > 0) {
          return true;
        }
      }
    }

    return false;
  }

}
