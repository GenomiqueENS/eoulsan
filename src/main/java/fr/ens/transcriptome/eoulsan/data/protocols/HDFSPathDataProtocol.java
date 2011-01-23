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

package fr.ens.transcriptome.eoulsan.data.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.google.common.collect.Lists;

import fr.ens.transcriptome.eoulsan.annotations.HadoopOnly;
import fr.ens.transcriptome.eoulsan.data.DataFile;
import fr.ens.transcriptome.eoulsan.io.PathConcatInputStream;

/**
 * This class define the HDFS protocol in Hadoop mode.
 * @author Laurent Jourdren
 */
@HadoopOnly
public class HDFSPathDataProtocol extends PathDataProtocol {

  @Override
  public String getName() {

    return "hdfs";
  }

  @Override
  public InputStream getData(final DataFile src) throws IOException {

    final Path path = getPath(src);

    if (path == null)
      throw new NullPointerException("Path to create is null");
    if (this.conf == null)
      throw new NullPointerException("The configuration object is null");

    final FileSystem fs = path.getFileSystem(this.conf);

    if (fs == null)
      throw new IOException(
          "Unable to create InputSteam, The FileSystem is null");

    final FileStatus fStatus = fs.getFileStatus(path);

    if (fStatus.isDir()) {

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
    final FileStatus[] files = fs.listStatus(path, new PathFilter() {

      @Override
      public boolean accept(final Path p) {

        return p.getName().matches("^part-.*[0-9]+$");
      }
    });

    // Sort the list
    Arrays.sort(files, new Comparator<FileStatus>() {

      @Override
      public int compare(final FileStatus f1, FileStatus f2) {

        return f1.getPath().getName().compareTo(f2.getPath().getName());
      }
    });

    // Create final result
    final List<Path> result = Lists.newArrayListWithCapacity(files.length);
    for (FileStatus file : files) {
      result.add(file.getPath());
    }

    return result;
  }

}
