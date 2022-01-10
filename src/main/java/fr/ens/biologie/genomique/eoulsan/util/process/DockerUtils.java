package fr.ens.biologie.genomique.eoulsan.util.process;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fr.ens.biologie.genomique.eoulsan.log.GenericLogger;

/**
 * This class contains utility methods for Docker processes.
 * @author Laurent Jourdren
 * @since 2.6
 */
public class DockerUtils {

  /**
   * Convert a file path to a mount point path if the file is on a NFS server.
   * @param files the list of the files to convert
   * @param convertNFSFilesToMountRoots true if files must be converted
   * @param logger the logger
   * @return a set of files
   * @throws IOException if mount of a file cannot be found
   */
  static Set<File> convertNFSFileToMountPoint(final Collection<File> files,
      final boolean convertNFSFilesToMountRoots, final GenericLogger logger)
      throws IOException {

    requireNonNull(files);
    requireNonNull(logger);

    Set<File> result = new LinkedHashSet<>();

    for (File file : files) {

      if (file != null && file.exists()) {
        result.add(convertNFSFilesToMountRoots
            ? convertNFSFileToMountPoint(file, logger) : file);
      }
    }

    return result;
  }

  /**
   * Convert a file path to a mount point path if the file is on a NFS server.
   * @param file the file to convert
   * @param logger the logger
   * @return a converted file
   * @throws IOException if mount of a file cannot be found
   */
  static File convertNFSFileToMountPoint(final File file,
      final GenericLogger logger) throws IOException {

    requireNonNull(file);
    requireNonNull(logger);

    FileStore fileStore = Files.getFileStore(file.toPath());
    logger.info("file: " + file + ", fileSystem type: " + fileStore.type());

    // If the file is on an NFS mount
    if ("nfs".equals(fileStore.type()) || "nfs4".equals(fileStore.type())) {

      // Get Mount point
      String info = fileStore.toString();
      String mountPoint =
          info.substring(0, info.length() - fileStore.name().length() - 3);

      return new File(mountPoint);
    }

    return file;
  }

  /**
   * List all the indirections of files.
   * @param files the files
   * @return a set with the file indirections
   * @throws IOException if an error occurs while searching indirections
   */
  static Set<File> fileIndirections(final Collection<File> files)
      throws IOException {

    requireNonNull(files);

    Set<File> result = new LinkedHashSet<>();

    for (File f : files) {

      if (f != null) {
        result.addAll(fileIndirections(f));
      }
    }

    return result;
  }

  /**
   * List all the file indirections.
   * @param file the file
   * @return a set with the file indirections
   * @throws IOException if an error occurs while searching indirections
   */
  private static Set<File> fileIndirections(final File file)
      throws IOException {

    requireNonNull(file);

    Set<File> result = new LinkedHashSet<>();

    DockerUtils.fileIndirections(file, result);

    return result;
  }

  /**
   * List all the file indirections.
   * @param file the file
   * @param result the result object
   * @throws IOException if an error occurs while searching indirections
   */
  private static void fileIndirections(final File file, Set<File> result)
      throws IOException {

    if (file == null) {
      return;
    }

    // Case has been already processed
    if (result.contains(file)) {
      return;
    }

    File previousFile = new File("/");

    for (File f : parentDirectories(file)) {

      Path path = f.toPath();

      if (Files.isSymbolicLink(path)) {

        // Get the target of the link
        Path link = Files.readSymbolicLink(path);

        // If the target is not an absolute path
        if (!link.isAbsolute()) {
          link = new File(previousFile, link.toString()).toPath();
        }

        // Process the target of the link
        fileIndirections(link.toFile().getAbsoluteFile(), result);
        result.add(f);
      }

      previousFile = f;
    }

    result.add(file);
  }

  /**
   * Get all the parent directories of a file.
   * @param file the file
   * @return a list with all the parent directories of the file
   */
  private static List<File> parentDirectories(final File file) {

    List<File> result = new ArrayList<>();
    File f = file;

    do {

      result.add(0, f);
      f = f.getParentFile();

    } while (f != null);

    return result;
  }

  //
  // Constructor
  //

  private DockerUtils() {
    throw new IllegalStateException();
  }

}
