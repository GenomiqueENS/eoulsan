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

package fr.ens.biologie.genomique.eoulsan.data.protocols;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import fr.ens.biologie.genomique.eoulsan.annotations.HadoopOnly;
import fr.ens.biologie.genomique.eoulsan.data.DataFile;
import fr.ens.biologie.genomique.eoulsan.io.PathConcatInputStream;

/**
 * This class define the HDFS protocol in Hadoop mode.
 * @since 1.0
 * @author Laurent Jourdren
 */
@HadoopOnly
public class HDFSPathDataProtocol extends PathDataProtocol {

  /** Protocol name. */
  public static final String PROTOCOL_NAME = "hdfs";

  @Override
  public String getName() {

    return PROTOCOL_NAME;
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    final Path path = getPath(src);

    if (path == null) {
      throw new NullPointerException("Path to create is null");
    }
    if (this.conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(this.conf);

    if (fs == null) {
      throw new IOException(
          "Unable to create InputSteam, The FileSystem is null");
    }

    final FileStatus fStatus = fs.getFileStatus(path);

    if (fStatus.isDirectory()) {

      final List<Path> paths = getPathToConcat(fs, path);

      if (paths != null && paths.size() > 0) {
        return new PathConcatInputStream(paths, this.conf);
      }
    }

    return fs.open(path);
  }

  private List<Path> getPathToConcat(final FileSystem fs, final Path path)
      throws IOException {

    // Get the list of files to contact
    final FileStatus[] files = fs.listStatus(path, p -> p.getName().matches("^part-.*[0-9]+$"));

    // Sort the list
    Arrays.sort(files, Comparator.comparing(f -> f.getPath().getName()));

    // Create final result
    final List<Path> result = new ArrayList<>(files.length);
    for (FileStatus file : files) {
      result.add(file.getPath());
    }

    return result;
  }

  @Override
  public void mkdir(final DataFile dir) throws IOException {

    mkdirs(dir);
  }

  @Override
  public void mkdirs(final DataFile dir) throws IOException {

    final Path path = getPath(dir);

    if (path == null) {
      throw new NullPointerException("Path to create is null");
    }
    if (this.conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(this.conf);

    if (fs == null) {
      throw new IOException(
          "Unable to create the directory, The FileSystem is null");
    }

    if (!fs.mkdirs(path)) {
      throw new IOException("Unable to create the directory: " + dir);
    }
  }

  @Override
  public boolean canMkdir() {

    return true;
  }

  @Override
  public void delete(final DataFile file, final boolean recursive)
      throws IOException {

    final Path path = getPath(file);

    if (path == null) {
      throw new NullPointerException("Path to delete is null");
    }
    if (this.conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(this.conf);

    if (fs == null) {
      throw new IOException(
          "Unable to delete the file, The FileSystem is null");
    }

    if (!fs.delete(path, recursive)) {
      throw new IOException("Unable to delete the directory: " + file);
    }
  }

  @Override
  public boolean canDelete() {

    return true;
  }

  @Override
  public List<DataFile> list(final DataFile file) throws IOException {

    final Path path = getPath(file);

    if (path == null) {
      throw new NullPointerException("Path to delete is null");
    }
    if (this.conf == null) {
      throw new NullPointerException("The configuration object is null");
    }

    final FileSystem fs = path.getFileSystem(this.conf);

    if (fs == null) {
      throw new IOException(
          "Unable to delete the file, The FileSystem is null");
    }

    FileStatus fileStatus = fs.getFileStatus(path);

    if (!fs.exists(path)) {
      throw new FileNotFoundException("File not found: " + file);
    }

    if (!fileStatus.isDirectory()) {
      throw new IOException("The file is not a directory: " + file);
    }

    // List directory
    final FileStatus[] files = fs.listStatus(path);

    // Convert the File array to a list of DataFile
    final List<DataFile> result = new ArrayList<>(files.length);
    for (FileStatus f : files) {
      result.add(new DataFile(f.getPath().toUri().toString()));
    }

    // Return an unmodifiable list
    return Collections.unmodifiableList(result);
  }

  @Override
  public boolean canList() {

    return true;
  }

  @Override
  public void rename(final DataFile file, final DataFile dest)
      throws IOException {

    if (dest == null) {
      throw new NullPointerException("dest argument is null");
    }

    if (dest.getProtocol() != this) {
      throw new IOException("the protocol of the dest is not "
          + getName() + " protocol: " + dest);
    }

    final Path path = getPath(file);
    final Path newPath = getPath(dest);

    final FileSystem fs = path.getFileSystem(this.conf);

    fs.rename(path, newPath);
  }

  @Override
  public boolean canRename() {

    return true;
  }

}
