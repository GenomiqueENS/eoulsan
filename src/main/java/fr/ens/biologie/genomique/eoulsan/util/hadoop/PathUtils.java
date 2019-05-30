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

package fr.ens.biologie.genomique.eoulsan.util.hadoop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import fr.ens.biologie.genomique.eoulsan.io.FileCharsets;
import fr.ens.biologie.genomique.eoulsan.util.FileUtils;
import fr.ens.biologie.genomique.eoulsan.util.StringUtils;

/**
 * This class define utility method to manipulate the Hadoop Path object.
 * @since 1.0
 * @author Laurent Jourdren
 */
public final class PathUtils {

  /**
   * Simple PathFilter to filter Paths with their suffix
   * @author Laurent Jourdren
   */
  public static final class SuffixPathFilter implements PathFilter {

    private final String suffix;
    private final boolean allowCompressedFile;

    @Override
    public boolean accept(final Path path) {

      if (path == null) {
        return false;
      }

      final String myName;

      if (this.allowCompressedFile) {
        myName =
            StringUtils.removeCompressedExtensionFromFilename(path.getName());
      } else {
        myName = path.getName();
      }

      if (myName.endsWith(this.suffix)) {
        return true;
      }

      return false;
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param suffix suffix to use by ExtensionPathFilter
     */
    public SuffixPathFilter(final String suffix) {

      this(suffix, false);
    }

    /**
     * Public constructor.
     * @param suffix suffix to use by ExtensionPathFilter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public SuffixPathFilter(final String suffix,
        final boolean allowCompressedFile) {

      if (suffix == null) {
        throw new NullPointerException("The suffix is null");
      }

      this.suffix = suffix;
      this.allowCompressedFile = allowCompressedFile;
    }

  }

  /**
   * Simple PathFilter to filter Paths with their beginning
   * @author Laurent Jourdren
   */
  public static final class PrefixPathFilter implements PathFilter {

    private final String prefix;
    private final boolean allowCompressedFile;

    @Override
    public boolean accept(final Path path) {

      if (path == null) {
        return false;
      }

      final String myName;

      if (this.allowCompressedFile) {
        myName =
            StringUtils.removeCompressedExtensionFromFilename(path.getName());
      } else {
        myName = path.getName();
      }

      if (myName.startsWith(this.prefix)) {
        return true;
      }

      return false;
    }

    //
    // Constructor
    //

    /**
     * Public constructor.
     * @param prefix extension to use by ExtensionPathFilter
     */
    public PrefixPathFilter(final String prefix) {

      this(prefix, false);
    }

    /**
     * Public constructor.
     * @param prefix extension to use by ExtensionPathFilter
     * @param allowCompressedFile allow files with a compressed extension
     */
    public PrefixPathFilter(final String prefix,
        final boolean allowCompressedFile) {

      if (prefix == null) {
        throw new NullPointerException("The prefix is null");
      }

      this.prefix = prefix;
      this.allowCompressedFile = allowCompressedFile;
    }

  }

  /**
   * Create an input stream from a path.
   * @param path Path of the file to open
   * @param conf configuration
   * @return an InputStream
   * @throws IOException if an error occurs while creating InputStream
   */
  public static final InputStream createInputStream(final Path path,
      final Configuration conf) throws IOException {

    if (path == null) {
      throw new NullPointerException("Path to create is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(conf);

    if (fs == null) {
      throw new IOException(
          "Unable to create InputSteam, The FileSystem is null");
    }

    return fs.open(path);
  }

  /**
   * Create an output stream from a path.
   * @param path Path of the file to open
   * @param conf configuration
   * @return an InputStream
   * @throws IOException if an error occurs while creating InputStream
   */
  public static final OutputStream createOutputStream(final Path path,
      final Configuration conf) throws IOException {

    if (path == null) {
      throw new NullPointerException("Path to create is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(conf);

    if (fs == null) {
      throw new IOException(
          "Unable to create InputSteam, The FileSystem is null");
    }

    return fs.create(path);
  }

  /**
   * Get the length of a file.
   * @param path Path of the file to open
   * @param conf configuration
   * @return an InputStream
   * @throws IOException if an error occurs while creating InputStream
   */
  public static final long getSize(final Path path, final Configuration conf)
      throws IOException {

    if (path == null) {
      throw new NullPointerException("Path to create is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(conf);

    if (fs == null) {
      throw new IOException(
          "Unable to create InputSteam, The FileSystem is null");
    }

    return fs.getFileStatus(path).getLen();
  }

  /**
   * Copy a file from a path to a local file. Don't remove original file.
   * @param srcPath Path of the file to copy
   * @param destFile Destination file
   * @param conf Configuration object * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyFromPathToLocalFile(final Path srcPath,
      final File destFile, final Configuration conf) throws IOException {

    return copyFromPathToLocalFile(srcPath, destFile, false, conf);
  }

  /**
   * Copy a file from a path to a local file
   * @param srcPath Path of the file to copy
   * @param destFile Destination file
   * @param removeOriginalFile true if the original file must be deleted
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyFromPathToLocalFile(final Path srcPath,
      final File destFile, final boolean removeOriginalFile,
      final Configuration conf) throws IOException {

    if (srcPath == null) {
      throw new NullPointerException("The source path is null");
    }
    if (destFile == null) {
      throw new NullPointerException("The destination file is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = FileSystem.get(srcPath.toUri(), conf);
    return FileUtil.copy(fs, srcPath, destFile, removeOriginalFile, conf);
  }

  /**
   * Copy a local file to a path
   * @param srcFile source file
   * @param destPath destination path
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyLocalFileToPath(final File srcFile,
      final Path destPath, final Configuration conf) throws IOException {

    return copyLocalFileToPath(srcFile, destPath, false, conf);
  }

  /**
   * Copy a local file to a path
   * @param srcFile source file
   * @param destPath destination path
   * @param removeSrcFile true if the source file must be removed
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyLocalFileToPath(final File srcFile,
      final Path destPath, final boolean removeSrcFile,
      final Configuration conf) throws IOException {

    if (srcFile == null) {
      throw new NullPointerException("The source file is null");
    }
    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    return FileUtil.copy(srcFile, FileSystem.get(destPath.toUri(), conf),
        destPath, removeSrcFile, conf);
  }

  /**
   * Copy bytes from an InputStream to a path.
   * @param is the InputStream to read from
   * @param destPath destination path
   * @param conf Configuration object
   * @return the number of bytes copied
   * @throws IOException In case of an I/O problem
   */
  public static long copyInputStreamToPath(final InputStream is,
      final Path destPath, final Configuration conf) throws IOException {

    if (is == null) {
      throw new NullPointerException("The input stream is null");
    }
    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = FileSystem.get(destPath.toUri(), conf);
    final OutputStream os = fs.create(destPath);
    return FileUtils.copy(is, os);
  }

  /**
   * Copy a local file to a path
   * @param srcFile source file
   * @param destPath destination path
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyAndCompressLocalFileToPath(final File srcFile,
      final Path destPath, final Configuration conf) throws IOException {

    return copyAndCompressLocalFileToPath(srcFile, destPath, false, conf);
  }

  /**
   * Copy a local file to a path
   * @param srcFile source file
   * @param destPath destination path
   * @param removeSrcFile true if the source file must be removed
   * @param conf Configuration object
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying file
   */
  public static boolean copyAndCompressLocalFileToPath(final File srcFile,
      final Path destPath, final boolean removeSrcFile,
      final Configuration conf) throws IOException {

    if (srcFile == null) {
      throw new NullPointerException("The source file is null");
    }
    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    return copyAndCompressInputStreamToPath(
        FileUtils.createInputStream(srcFile), destPath, conf);
  }

  /**
   * Copy bytes from an InputStream to a path.
   * @param is the InputStream to read from
   * @param destPath destination path
   * @param conf Configuration object
   * @return the number of bytes copied
   * @throws IOException In case of an I/O problem
   */
  public static boolean copyAndCompressInputStreamToPath(final InputStream is,
      final Path destPath, final Configuration conf) throws IOException {

    if (is == null) {
      throw new NullPointerException("The input stream is null");
    }
    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = FileSystem.get(destPath.toUri(), conf);

    final CompressionCodecFactory factory = new CompressionCodecFactory(conf);
    final CompressionCodec codec = factory.getCodec(destPath);

    if (codec == null) {
      throw new IOException("No codec found for: " + destPath);
    }

    final OutputStream os = codec.createOutputStream(fs.create(destPath));
    FileUtils.copy(is, os);

    return true;
  }

  /**
   * Unzip a zip file on local file system. Don't remove original zip file.
   * @param path Path of the zip file
   * @param outputDir Output directory of the content of the zip file
   * @param conf Configuration object
   * @throws IOException if an error occurs while unzipping the file
   */
  public static void unZipPathToLocalFile(final Path path, final File outputDir,
      final Configuration conf) throws IOException {

    unZipPathToLocalFile(path, outputDir, false, conf);
  }

  /**
   * Unzip a zip file on local file system.
   * @param srcPath Path of the zip file
   * @param outputDir Output directory of the content of the zip file
   * @param removeOriginalZipFile true if the original zip file must be removed
   * @param conf Configuration object
   * @throws IOException if an error occurs while unzipping the file
   */
  public static void unZipPathToLocalFile(final Path srcPath,
      final File outputDir, final boolean removeOriginalZipFile,
      final Configuration conf) throws IOException {

    if (srcPath == null) {
      throw new NullPointerException("The source path is null");
    }
    if (outputDir == null) {
      throw new NullPointerException("The destination directory file is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final File tmpZipFile = FileUtils.createTempFile("", ".zip");

    copyFromPathToLocalFile(srcPath, tmpZipFile, removeOriginalZipFile, conf);

    FileUtils.unzip(tmpZipFile, outputDir);

    if (!tmpZipFile.delete()) {
      throw new IOException(
          "Can't remove temporary zip file: " + tmpZipFile.getAbsolutePath());
    }
  }

  /**
   * Fully delete a file of the content of a directory
   * @param path Path of the file
   * @param conf Configuration Object
   * @return true if the Path is successfully removed
   * @throws IOException if cannot delete the file
   */
  public static boolean fullyDelete(final Path path, final Configuration conf)
      throws IOException {

    if (path == null) {
      throw new NullPointerException("Path to delete is null");
    }
    if (conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(conf);

    if (fs == null) {
      throw new IOException("Unable to delete path, The FileSystem is null");
    }

    return fs.delete(path, true);
  }

  /**
   * Merge several file of a directory into one file.
   * @param srcPath source directory path
   * @param destPath destination path
   * @param conf Configuration object
   * @throws IOException if an error occurs while merging files
   */
  public static void copyMerge(final Path srcPath, final Path destPath,
      final Configuration conf) throws IOException {

    copyMerge(srcPath, destPath, false, conf, null);
  }

  /**
   * Merge several file of a directory into one file.
   * @param srcPath source directory path
   * @param destPath destination path
   * @param deleteSource delete source files
   * @param conf Configuration object
   * @throws IOException if an error occurs while merging files
   */
  public static void copyMerge(final Path srcPath, final Path destPath,
      final boolean deleteSource, final Configuration conf) throws IOException {

    copyMerge(srcPath, destPath, deleteSource, conf, null);
  }

  /**
   * Merge several file of a directory into one file.
   * @param srcPath source directory path
   * @param destPath destination path
   * @param deleteSource delete source files
   * @param conf Configuration object
   * @param addString string to add
   * @throws IOException if an error occurs while merging files
   */
  public static void copyMerge(final Path srcPath, final Path destPath,
      final boolean deleteSource, final Configuration conf,
      final String addString) throws IOException {

    if (srcPath == null) {
      throw new NullPointerException("The source path is null.");
    }

    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem srcFs = srcPath.getFileSystem(conf);
    final FileSystem destFs = destPath.getFileSystem(conf);

    FileUtil.copyMerge(srcFs, srcPath, destFs, destPath, deleteSource, conf,
        addString);

  }

  /**
   * Create a new path with the same parent directory and basename but without
   * another extension.
   * @param path base path to use
   * @param extension extension to add
   * @return a new Path object
   */
  public static Path newPathWithOtherExtension(final Path path,
      final String extension) {

    if (path == null) {
      throw new NullPointerException("Path is null");
    }

    if (extension == null) {
      throw new NullPointerException("Extension is null");
    }

    return new Path(path.getParent(),
        StringUtils.basename(path.getName()) + extension);
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param prefix filter on suffix
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsByPrefix(final Path dir,
      final String prefix, final Configuration conf) throws IOException {

    return listPathsByPrefix(dir, prefix, false, conf);
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param prefix filter on suffix
   * @param allowCompressedExtension Allow compressed extensions
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsByPrefix(final Path dir,
      final String prefix, final boolean allowCompressedExtension,
      final Configuration conf) throws IOException {

    if (dir == null) {
      throw new NullPointerException("Directory path is null");
    }

    if (prefix == null) {
      throw new NullPointerException("Prefix is null");
    }

    if (conf == null) {
      throw new NullPointerException("Configuration is null");
    }

    final FileSystem fs = dir.getFileSystem(conf);
    if (!fs.getFileStatus(dir).isDirectory()) {
      throw new IOException("Directory path is not a directory: " + dir);
    }

    final FileStatus[] filesStatus = fs.listStatus(dir,
        new PrefixPathFilter(prefix, allowCompressedExtension));

    if (filesStatus == null) {
      return Collections.emptyList();
    }

    final List<Path> result = new ArrayList<>(filesStatus.length);

    for (FileStatus fst : filesStatus) {
      result.add(fst.getPath());
    }

    return result;
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param suffix filter on suffix
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsBySuffix(final Path dir,
      final String suffix, final Configuration conf) throws IOException {

    return listPathsBySuffix(dir, suffix, false, conf);
  }

  /**
   * Return a list of the file of a path
   * @param dir Path of the directory
   * @param suffix filter on suffix
   * @param allowCompressedExtension Allow compressed extensions
   * @param conf Configuration
   * @return a list of Path
   * @throws IOException if an error occurs while listing the directory
   */
  public static List<Path> listPathsBySuffix(final Path dir,
      final String suffix, final boolean allowCompressedExtension,
      final Configuration conf) throws IOException {

    if (dir == null) {
      throw new NullPointerException("Directory path is null");
    }

    if (suffix == null) {
      throw new NullPointerException("Suffix is null");
    }

    if (conf == null) {
      throw new NullPointerException("Configuration is null");
    }

    final FileSystem fs = dir.getFileSystem(conf);
    if (!fs.getFileStatus(dir).isDirectory()) {
      throw new IOException("Directory path is not a directory: " + dir);
    }

    final FileStatus[] filesStatus = fs.listStatus(dir,
        new SuffixPathFilter(suffix, allowCompressedExtension));

    if (filesStatus == null) {
      return Collections.emptyList();
    }

    final List<Path> result = new ArrayList<>(filesStatus.length);

    for (FileStatus fst : filesStatus) {
      result.add(fst.getPath());
    }

    return result;
  }

  /**
   * Create a new temporary path. Nothing is created on the file system.
   * @param directory parent directory of the temporary file to create
   * @param prefix Prefix of the temporary file
   * @param suffix suffix of the temporary file
   * @return the new temporary file
   * @throws IOException if there is an error creating the temporary directory
   */
  public static Path createTempPath(final Path directory, final String prefix,
      final String suffix, final Configuration conf) throws IOException {

    final Path myDir;
    final String myPrefix;
    final String mySuffix;

    if (directory == null) {
      throw new NullPointerException("Directory is null");
    }

    if (conf == null) {
      throw new NullPointerException("Configuration is null");
    }

    myDir = directory;

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

    final FileSystem fs = directory.getFileSystem(conf);
    Path tempFile;

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
      tempFile = new Path(myDir, filename);
    } while (fs.isFile(tempFile));

    return tempFile;
  }

  /**
   * Copy all files in a directory to one output file (merge).
   * @param paths list of path files to concat
   * @param dstPath destination path
   * @param conf Configuration
   */
  public static boolean concat(final List<Path> paths, final Path dstPath,
      final Configuration conf) throws IOException {

    return concat(paths, dstPath, false, true, null);
  }

  /**
   * Copy all files in a directory to one output file (merge).
   * @param paths list of path files to concat
   * @param dstPath destination path
   * @param deleteSource true if the original files must be deleted
   * @param overwrite true if an existing destination file must be deleted
   * @param conf Configuration
   */
  public static boolean concat(final List<Path> paths, final Path dstPath,
      final boolean deleteSource, final boolean overwrite,
      final Configuration conf) throws IOException {

    return concat(paths, dstPath, deleteSource, overwrite, conf, null);
  }

  /**
   * Copy all files in a directory to one output file (merge).
   * @param paths list of path files to concat
   * @param dstPath destination path
   * @param deleteSource true if the original files must be deleted
   * @param overwrite true if an existing destination file must be deleted
   * @param conf Configuration
   * @param addString string to add
   */
  public static boolean concat(final List<Path> paths, final Path dstPath,
      final boolean deleteSource, final boolean overwrite,
      final Configuration conf, final String addString) throws IOException {

    if (paths == null) {
      throw new NullPointerException("The list of path to concat is null");
    }

    if (paths.size() == 0) {
      return false;
    }

    if (dstPath == null) {
      throw new NullPointerException("The destination path is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null.");
    }

    final FileSystem srcFs = paths.get(0).getFileSystem(conf);
    final FileSystem dstFs = dstPath.getFileSystem(conf);

    if (!overwrite && dstFs.exists(dstPath)) {
      throw new IOException("The output file already exists: " + dstPath);
    }

    try (OutputStream out = dstFs.create(dstPath)) {
      // FileStatus contents[] = srcFS.listStatus(srcDir);
      // for (int i = 0; i < contents.length; i++) {
      for (Path p : paths) {
        if (!srcFs.getFileStatus(p).isDirectory()) {
          try (InputStream in = srcFs.open(p)) {
            IOUtils.copyBytes(in, out, conf, false);
            if (addString != null) {
              out.write(addString.getBytes(FileCharsets.UTF8_CHARSET));
            }

          }
        }
      }

    }

    if (deleteSource) {
      for (Path p : paths) {
        if (!srcFs.delete(p, false)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Check if a file exists
   * @param file File to test
   * @param conf Configuration
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingFile(final Path file,
      final Configuration conf, final String msgFileType) throws IOException {

    if (msgFileType == null) {
      throw new NullPointerException("Message file type for check is null");
    }

    if (file == null) {
      throw new NullPointerException("The " + msgFileType + " is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem fs = file.getFileSystem(conf);

    if (!fs.exists(file)) {
      throw new IOException("The " + msgFileType + " does not exists: " + file);
    }
  }

  /**
   * Check if a directory exists
   * @param directory directory to test * @param conf Configuration
   * @param conf the configuration object
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingDirectoryFile(final Path directory,
      final Configuration conf, final String msgFileType) throws IOException {

    checkExistingFile(directory, conf, msgFileType);

    final FileSystem fs = directory.getFileSystem(conf);

    if (!fs.getFileStatus(directory).isDirectory()) {
      throw new IOException(
          "The " + msgFileType + " is not a directory: " + directory);
    }
  }

  /**
   * Check if a directory exists
   * @param directory directory to test * @param conf Configuration
   * @param conf the configuration object
   * @return true is the directory exists
   */
  public static final boolean isExistingDirectoryFile(final Path directory,
      final Configuration conf) throws IOException {

    if (directory == null) {
      throw new NullPointerException("The directory is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem fs = directory.getFileSystem(conf);

    try {
      return fs.getFileStatus(directory).isDirectory();
    } catch (FileNotFoundException e) {
      return false;
    }
  }

  /**
   * Check if a file exists
   * @param file file to test
   * @param conf Configuration
   * @return true is the directory exists
   */
  public static final boolean isFile(final Path file, final Configuration conf)
      throws IOException {

    if (file == null) {
      throw new NullPointerException("The path is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem fs = file.getFileSystem(conf);

    return fs.isFile(file);
  }

  /**
   * Check if a file exists
   * @param file File to test * @param conf Configuration
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingStandardFile(final Path file,
      final Configuration conf, final String msgFileType) throws IOException {

    checkExistingFile(file, conf, msgFileType);

    final FileSystem fs = file.getFileSystem(conf);

    if (!fs.isFile(file)) {
      throw new IOException(
          "The " + msgFileType + " is  not a standard file: " + file);
    }
  }

  /**
   * Check if a file exists
   * @param file File to test * @param conf Configuration
   * @param msgFileType message for the description of the file
   * @throws IOException if the file doesn't exists
   */
  public static final void checkExistingStandardFileOrDirectory(final Path file,
      final Configuration conf, final String msgFileType) throws IOException {

    checkExistingDirectoryFile(file, conf, msgFileType);

    final FileSystem fs = file.getFileSystem(conf);

    if (!fs.isFile(file) && !fs.getFileStatus(file).isDirectory()) {
      throw new IOException("The "
          + msgFileType + " is  not a standard file or a directory: " + file);
    }
  }

  /**
   * Copy file from a path to another path.
   * @param srcPath source path
   * @param destPath destination path
   * @param conf Configuration
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying
   */
  public static final boolean copy(final Path srcPath, final Path destPath,
      final Configuration conf) throws IOException {

    return copy(srcPath, destPath, true, conf);
  }

  /**
   * Copy file from a path to another path.
   * @param srcPath source path
   * @param destPath destination path
   * @param overwrite true if existing files must be overwritten
   * @param conf Configuration
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying
   */
  public static final boolean copy(final Path srcPath, final Path destPath,
      final boolean overwrite, final Configuration conf) throws IOException {

    if (srcPath == null) {
      throw new NullPointerException("The source path is null.");
    }

    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem srcFs = srcPath.getFileSystem(conf);
    final FileSystem destFs = destPath.getFileSystem(conf);

    return FileUtil.copy(srcFs, srcPath, destFs, destPath, false, overwrite,
        conf);
  }

  /**
   * Move file from a path to another path.
   * @param srcPath source path
   * @param destPath destination path
   * @param conf Configuration
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying
   */
  public static final boolean move(final Path srcPath, final Path destPath,
      final Configuration conf) throws IOException {

    return move(srcPath, destPath, true, conf);
  }

  /**
   * Move file from a path to another path.
   * @param srcPath source path
   * @param destPath destination path
   * @param overwrite true if existing files must be overwritten
   * @param conf Configuration
   * @return true if the copy is successful
   * @throws IOException if an error occurs while copying
   */
  public static final boolean move(final Path srcPath, final Path destPath,
      final boolean overwrite, final Configuration conf) throws IOException {

    if (srcPath == null) {
      throw new NullPointerException("The source path is null.");
    }

    if (destPath == null) {
      throw new NullPointerException("The destination path is null");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem srcFs = srcPath.getFileSystem(conf);
    final FileSystem destFs = destPath.getFileSystem(conf);

    return FileUtil.copy(srcFs, srcPath, destFs, destPath, true, overwrite,
        conf);
  }

  /**
   * Create a directory. If parent directories don't exists create it.
   * @param path Path of the directory to create
   * @param conf Configuration
   * @return true if the directory is successfully created
   * @throws IOException if an error occurs while creating the directory
   */
  public static final boolean mkdirs(final Path path, final Configuration conf)
      throws IOException {

    if (path == null) {
      throw new NullPointerException(
          "The path of the directory to create is null.");
    }

    if (conf == null) {
      throw new NullPointerException("The configuration is null");
    }

    final FileSystem fs = path.getFileSystem(conf);
    return fs.mkdirs(path);
  }

  /**
   * Test if a path exists
   * @param path Path to test
   * @param conf Configuration
   * @return true if the path exists
   * @throws IOException if an error occurs while creating the directory
   */
  public static final boolean exists(final Path path, final Configuration conf)
      throws IOException {

    final FileSystem fs = path.getFileSystem(conf);

    return fs.exists(path);
  }

  //
  // Constructor
  //

  /**
   * Private constructor.
   */
  private PathUtils() {
  }

}
